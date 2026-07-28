package com.ruoyi.asset.pda.feature.receive;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmDto;
import com.ruoyi.asset.pda.databinding.ActivityReceiveBinding;

/** 工业 PDA 领用页：一位领用人对应一批资产，最终由服务端事务确认。 */
public final class ReceiveActivity extends SessionAwareActivity {
    public static final String EXTRA_CAN_CONFIRM = "receive_can_confirm";
    static final String EXTRA_SKIP_INITIAL_LOAD = "receive_skip_initial_load";

    private ActivityReceiveBinding binding;
    private ReceiveViewModel viewModel;
    private ReceiveAssetAdapter assetAdapter;
    private ScanKeyDispatcher scanKeyDispatcher;
    private ActivityResultLauncher<Intent> recipientPickerLauncher;
    private int observedAssetCodeClearVersion = -1;
    private int observedBatchResetVersion = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        recipientPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result ->
                        handleRecipientPickerResult(result.getResultCode(), result.getData()));

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        boolean canConfirm = getIntent().getBooleanExtra(EXTRA_CAN_CONFIRM, false);
        viewModel = new ViewModelProvider(this,
                new ReceiveViewModel.Factory(container.getReceiveRepository(),
                        container.getCommonRepository(), container.getUhfScanner(),
                        canConfirm))
                .get(ReceiveViewModel.class);
        assetAdapter = new ReceiveAssetAdapter(new ReceiveAssetAdapter.Listener() {
            @Override
            public void onOpen(ReceiveAssetItem item) {
                showAssetDetails(item);
            }

            @Override
            public void onRemove(ReceiveAssetItem item) {
                confirmRemove(item);
            }
        });
        binding.receiveAssetList.setLayoutManager(new LinearLayoutManager(this));
        binding.receiveAssetList.setAdapter(assetAdapter);
        bindActions();
        viewModel.getUiState().observe(this, this::render);
        // 仪器测试可只校验页面骨架；真实现场必须加载后台会话事实。
        if (!BuildConfig.DEBUG
                || !getIntent().getBooleanExtra(EXTRA_SKIP_INITIAL_LOAD, false)) {
            viewModel.initialize();
        }
    }

    private void bindActions() {
        binding.receiveToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.receiveRecipientCard.setOnClickListener(view -> openRecipientPicker());
        binding.receiveRecipientAction.setOnClickListener(view -> openRecipientPicker());
        binding.receiveScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.receiveAssetCodeButton.setOnClickListener(view -> addAssetCode());
        binding.receiveAssetCodeInput.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        addAssetCode();
                        return true;
                    }
                    return false;
                });
        binding.receiveIssueButton.setOnClickListener(view -> showIssues());
        binding.receiveRetryButton.setOnClickListener(
                view -> viewModel.retryInitialization());
        binding.receiveClearButton.setOnClickListener(view -> runAfterClearConfirmation(
                getString(R.string.receive_clear_message), viewModel::clearBatch));
        binding.receiveConfirmButton.setOnClickListener(view -> showConfirmDialog());
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBack();
                    }
                });
    }

    private void openRecipientPicker() {
        ReceiveUiState state = currentState();
        if (state == null || !state.isInitialReady() || state.isBusy()
                || state.isScanning()) {
            return;
        }
        recipientPickerLauncher.launch(new Intent(this,
                ReceiveRecipientPickerActivity.class));
    }

    private void addAssetCode() {
        CharSequence text = binding.receiveAssetCodeInput.getText();
        viewModel.addByAssetCode(text == null ? null : text.toString());
    }

    private void handleRecipientPickerResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        long recipientId = data.getLongExtra(
                ReceiveRecipientPickerActivity.EXTRA_RECIPIENT_ID, -1L);
        long deptId = data.getLongExtra(
                ReceiveRecipientPickerActivity.EXTRA_RECIPIENT_DEPT_ID, -1L);
        String code = data.getStringExtra(
                ReceiveRecipientPickerActivity.EXTRA_RECIPIENT_CODE);
        String name = data.getStringExtra(
                ReceiveRecipientPickerActivity.EXTRA_RECIPIENT_NAME);
        String deptName = data.getStringExtra(
                ReceiveRecipientPickerActivity.EXTRA_RECIPIENT_DEPT_NAME);
        if (recipientId < 1L || deptId < 1L || !hasText(name)
                || !hasText(deptName)) {
            return;
        }
        PdaMasterDataDto recipient = new PdaMasterDataDto(recipientId, code,
                name, deptId, deptName);
        runAfterRecipientChangeConfirmation(recipient);
    }

    private void render(ReceiveUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean ready = state.isInitialReady();
        boolean busy = state.isBusy();
        boolean recipientSelected = state.getSelectedRecipient() != null;
        boolean inputUnlocked = ready && recipientSelected && !busy && !state.isScanning();

        renderRecipient(state.getSelectedRecipient());
        renderResetTokens(state);
        assetAdapter.submit(state.getAssets());
        setVisible(binding.receiveEmptyText, state.getAssets().isEmpty());
        binding.receiveListTitle.setText(getString(
                R.string.receive_list_count, state.getAssets().size()));
        int operatorTimeFormat = state.getLastConfirmation() == null
                ? R.string.receive_operator_time_format
                : R.string.receive_operator_confirmed_time_format;
        binding.receiveOperatorTime.setText(getString(operatorTimeFormat,
                value(state.getOperatorName()), value(state.getServerTime())));
        binding.receiveWorkSummary.setText(getString(
                R.string.receive_work_summary_format, state.getAssets().size(),
                state.getRawEpcCount(), state.getDuplicateReadCount()));
        setVisible(binding.receiveLatestEpc, hasText(state.getLatestEpc()));
        if (hasText(state.getLatestEpc())) {
            binding.receiveLatestEpc.setText(getString(
                    R.string.receive_latest_epc_format, state.getLatestEpc()));
        }

        binding.receiveRecipientCard.setEnabled(ready && !busy && !state.isScanning());
        binding.receiveRecipientAction.setEnabled(ready && !busy && !state.isScanning());
        binding.receiveScanButton.setText(scanActionText(state));
        binding.receiveScanButton.setEnabled(ready && !busy);
        binding.receiveAssetCodeInput.setEnabled(inputUnlocked);
        binding.receiveAssetCodeButton.setEnabled(inputUnlocked);
        binding.receiveAssetCodeButton.setText(
                state.getOperation() == ReceiveUiState.Operation.ASSET_CODE
                        ? R.string.receive_asset_querying
                        : R.string.receive_add_asset);
        binding.receiveRemarkInput.setEnabled(ready && recipientSelected
                && !busy && !state.isScanning());
        binding.receiveClearButton.setEnabled(state.hasPendingWork()
                && !busy && !state.isScanning());
        binding.receiveConfirmButton.setEnabled(state.isCanConfirm()
                && recipientSelected && !state.getAssets().isEmpty()
                && !busy && !state.isScanning());
        binding.receiveConfirmButton.setText(
                state.getOperation() == ReceiveUiState.Operation.CONFIRM
                        ? getString(R.string.receive_confirming)
                        : getString(R.string.receive_confirm_count,
                                state.getAssets().size()));

        setVisible(binding.receiveProgress, state.isInitialLoading() || busy);
        setVisible(binding.receiveInfoText, hasText(state.getInfoMessage()));
        binding.receiveInfoText.setText(value(state.getInfoMessage()));
        setVisible(binding.receiveErrorText, hasText(state.getErrorMessage()));
        binding.receiveErrorText.setText(value(state.getErrorMessage()));
        setVisible(binding.receiveRetryButton, state.isInitialLoadFailed()
                && hasText(state.getErrorMessage()));
        setVisible(binding.receiveIssueButton, !state.getIssues().isEmpty());
        binding.receiveIssueButton.setText(getString(
                R.string.receive_issue_count, state.getIssues().size()));
        setVisible(binding.receivePermissionHint, !state.isCanConfirm());
        renderConfirmation(state.getLastConfirmation());
    }

    private void renderRecipient(PdaMasterDataDto recipient) {
        boolean selected = recipient != null && recipient.getId() != null
                && recipient.getParentId() != null;
        binding.receiveRecipientName.setText(selected
                ? getString(R.string.receive_recipient_name_format,
                        value(recipient.getName()), value(recipient.getCode()))
                : getString(R.string.receive_recipient_unselected));
        binding.receiveRecipientDept.setText(selected
                ? getString(R.string.receive_recipient_dept_format,
                        value(recipient.getParentName()))
                : getString(R.string.receive_recipient_dept_unselected));
        binding.receiveRecipientAction.setText(selected
                ? R.string.receive_change_recipient
                : R.string.receive_select_recipient);
    }

    private void renderResetTokens(ReceiveUiState state) {
        if (observedAssetCodeClearVersion < 0) {
            observedAssetCodeClearVersion = state.getAssetCodeClearVersion();
        } else if (observedAssetCodeClearVersion != state.getAssetCodeClearVersion()) {
            observedAssetCodeClearVersion = state.getAssetCodeClearVersion();
            binding.receiveAssetCodeInput.setText(null);
        }
        if (observedBatchResetVersion < 0) {
            observedBatchResetVersion = state.getBatchResetVersion();
        } else if (observedBatchResetVersion != state.getBatchResetVersion()) {
            observedBatchResetVersion = state.getBatchResetVersion();
            binding.receiveRemarkInput.setText(null);
        }
    }

    private String scanActionText(ReceiveUiState state) {
        if (state.getOperation() == ReceiveUiState.Operation.PRECHECK) {
            return getString(R.string.receive_prechecking);
        }
        if (state.getOperation() == ReceiveUiState.Operation.ASSET_CODE) {
            return getString(R.string.receive_asset_querying);
        }
        if (state.isScanning()) {
            return getString(R.string.receive_scan_stop);
        }
        if (state.getRawEpcCount() > 0) {
            return getString(R.string.receive_precheck_retry,
                    state.getRawEpcCount());
        }
        return getString(R.string.receive_scan_start);
    }

    private void renderConfirmation(PdaReceiveBatchConfirmDto result) {
        setVisible(binding.receiveResultText, result != null);
        if (result == null) {
            return;
        }
        binding.receiveResultText.setText(getString(R.string.receive_result_format,
                value(result.getReceiveNo()), value(result.getReceiveUserName()),
                value(result.getReceiveDeptName()), result.getSuccessCount(),
                value(result.getConfirmUserName()), value(result.getConfirmTime())));
    }

    private void showAssetDetails(ReceiveAssetItem item) {
        if (item == null) {
            return;
        }
        StringBuilder message = new StringBuilder();
        appendFact(message, R.string.receive_detail_asset_code, item.getAssetCode());
        appendFact(message, R.string.receive_detail_asset_name, item.getAssetName());
        appendFact(message, R.string.receive_detail_category, item.getCategoryName());
        appendFact(message, R.string.receive_detail_model, item.getSpecModel());
        appendFact(message, R.string.receive_detail_brand, item.getBrand());
        appendFact(message, R.string.receive_detail_status, item.getAssetStatusLabel());
        appendFact(message, R.string.receive_detail_source,
                item.getSource() == ReceiveAssetItem.Source.RFID
                        ? getString(R.string.receive_source_rfid)
                        : getString(R.string.receive_source_asset_code));
        appendFact(message, R.string.receive_detail_identifier,
                item.getIdentifier().getIdentifyValue());
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_detail_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.common_close, null)
                .show();
    }

    private void confirmRemove(ReceiveAssetItem item) {
        if (item == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_remove_title)
                .setMessage(getString(R.string.receive_remove_message,
                        value(item.getAssetCode()), value(item.getAssetName())))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.receive_remove,
                        (dialog, which) -> viewModel.removeAsset(item.getAssetId()))
                .show();
    }

    private void showIssues() {
        ReceiveUiState state = currentState();
        if (state == null || state.getIssues().isEmpty()) {
            return;
        }
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        ReceiveIssueAdapter adapter = new ReceiveIssueAdapter();
        adapter.submit(state.getIssues());
        list.setAdapter(adapter);
        int padding = getResources().getDimensionPixelSize(R.dimen.pda_item_spacing);
        list.setPadding(padding, padding, padding, padding);
        list.setClipToPadding(false);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.receive_issues_title,
                        state.getIssues().size()))
                .setMessage(R.string.receive_issues_hint)
                .setView(list)
                .setPositiveButton(R.string.common_close, null)
                .show();
    }

    private void showConfirmDialog() {
        ReceiveUiState state = currentState();
        PdaMasterDataDto recipient = state == null ? null : state.getSelectedRecipient();
        if (state == null || recipient == null || state.getAssets().isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_confirm_title)
                .setMessage(getString(R.string.receive_confirm_message,
                        value(recipient.getName()), value(recipient.getParentName()),
                        state.getAssets().size()))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.receive_confirm,
                        (dialog, which) -> viewModel.confirm(textOfRemark()))
                .show();
    }

    private void runAfterRecipientChangeConfirmation(PdaMasterDataDto recipient) {
        ReceiveUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            viewModel.selectRecipient(recipient);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_change_recipient_title)
                .setMessage(R.string.receive_change_recipient_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.receive_clear_and_change,
                        (dialog, which) -> viewModel.selectRecipient(recipient))
                .show();
    }

    private void runAfterClearConfirmation(String message, Runnable action) {
        ReceiveUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            action.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_clear_title)
                .setMessage(message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.receive_clear_and_continue,
                        (dialog, which) -> action.run())
                .show();
    }

    private void handleBack() {
        ReceiveUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_leave_title)
                .setMessage(R.string.receive_leave_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.receive_leave_confirm,
                        (dialog, which) -> finish())
                .show();
    }

    private ReceiveUiState currentState() {
        return viewModel == null ? null : viewModel.getUiState().getValue();
    }

    private String textOfRemark() {
        CharSequence value = binding.receiveRemarkInput.getText();
        return value == null ? null : value.toString().trim();
    }

    private void appendFact(StringBuilder output, int labelResId, String factValue) {
        if (!hasText(factValue)) {
            return;
        }
        if (output.length() > 0) {
            output.append('\n');
        }
        output.append(getString(labelResId)).append('：').append(factValue.trim());
    }

    private String value(String value) {
        return hasText(value) ? value.trim()
                : getString(R.string.common_unknown);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (scanKeyDispatcher == null) {
            scanKeyDispatcher = new ScanKeyDispatcher(this,
                    new ScanKeyDispatcher.Listener() {
                        @Override
                        public void onScanKeyPressed() {
                            if (viewModel != null) {
                                viewModel.onScanKeyPressed();
                            }
                        }
                    });
        }
        scanKeyDispatcher.start();
    }

    @Override
    protected void onStop() {
        if (scanKeyDispatcher != null) {
            scanKeyDispatcher.stop();
        }
        if (viewModel != null) {
            viewModel.releaseScanner();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
