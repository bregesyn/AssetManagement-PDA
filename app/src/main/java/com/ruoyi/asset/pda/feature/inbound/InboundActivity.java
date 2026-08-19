package com.ruoyi.asset.pda.feature.inbound;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ActivityInboundBinding;

import java.util.ArrayList;
import java.util.List;

/** 工业 PDA 入库页：顶部完成仓位与采集，中部核对清单，底部只保留整批动作。 */
public final class InboundActivity extends SessionAwareActivity {
    public static final String EXTRA_CAN_CONFIRM = "inbound_can_confirm";
    static final String EXTRA_SKIP_INITIAL_LOAD = "inbound_skip_initial_load";

    private ActivityInboundBinding binding;
    private InboundViewModel viewModel;
    private InboundAssetAdapter assetAdapter;
    private ScanKeyDispatcher scanKeyDispatcher;
    private boolean selectorCallbackReady;
    private int observedAssetCodeClearVersion = -1;
    private int observedBatchResetVersion = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInboundBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        boolean canConfirm = getIntent().getBooleanExtra(EXTRA_CAN_CONFIRM, false);
        viewModel = new ViewModelProvider(this,
                new InboundViewModel.Factory(container.getInboundRepository(),
                        container.getCommonRepository(), container.getUhfScanner(),
                        canConfirm))
                .get(InboundViewModel.class);
        assetAdapter = new InboundAssetAdapter(new InboundAssetAdapter.Listener() {
            @Override
            public void onOpen(InboundAssetItem item) {
                showAssetDetails(item);
            }

            @Override
            public void onRemove(InboundAssetItem item) {
                confirmRemove(item);
            }
        });
        binding.inboundAssetList.setLayoutManager(new LinearLayoutManager(this));
        binding.inboundAssetList.setAdapter(assetAdapter);
        bindSelectors();
        bindActions();
        viewModel.getUiState().observe(this, this::render);
        // 仪器测试可只校验页面骨架；正常运行必须加载真实用户和主数据。
        if (!BuildConfig.DEBUG
                || !getIntent().getBooleanExtra(EXTRA_SKIP_INITIAL_LOAD, false)) {
            viewModel.initialize();
        }
    }

    private void bindSelectors() {
        // MaterialAutoCompleteTextView 在 inputType=none 时不会依赖输入事件自动展开，
        // PDA 现场通常直接点按字段，因此点击必须显式打开已加载的主数据列表。
        binding.inboundWarehouseInput.setOnClickListener(view -> showSelectorDropdown(binding.inboundWarehouseInput));
        binding.inboundLocationInput.setOnClickListener(view -> showSelectorDropdown(binding.inboundLocationInput));
        binding.inboundWarehouseInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (!selectorCallbackReady || viewModel == null) {
                        return;
                    }
                    InboundUiState state = currentState();
                    if (state == null || position < 0
                            || position >= state.getWarehouses().size()) {
                        return;
                    }
                    PdaMasterDataDto selected = state.getWarehouses().get(position);
                    renderSelectorValues(state);
                    runAfterClearConfirmation("切换入库仓库会清空当前已扫资产和异常记录。",
                            () -> viewModel.changeWarehouse(selected.getId()));
                });
        binding.inboundLocationInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (!selectorCallbackReady || viewModel == null) {
                        return;
                    }
                    InboundUiState state = currentState();
                    if (state == null || position < 0
                            || position >= state.getLocations().size()) {
                        return;
                    }
                    PdaMasterDataDto selected = state.getLocations().get(position);
                    renderSelectorValues(state);
                    runAfterClearConfirmation("切换入库位置会清空当前已扫资产和异常记录。",
                            () -> viewModel.changeLocation(selected.getId()));
                });
        selectorCallbackReady = true;
    }

    private void showSelectorDropdown(
            com.google.android.material.textfield.MaterialAutoCompleteTextView selector) {
        if (!selectorCallbackReady || viewModel == null || !selector.isEnabled()) {
            return;
        }
        // 没有主数据时不弹空菜单；初始化错误会在页面顶部保留可读的错误提示。
        if (selector.getAdapter() == null || selector.getAdapter().getCount() == 0) {
            return;
        }
        selector.requestFocus();
        selector.showDropDown();
    }

    private void bindActions() {
        binding.inboundToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.inboundScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.inboundAssetCodeButton.setOnClickListener(view -> addAssetCode());
        binding.inboundAssetCodeInput.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        addAssetCode();
                        return true;
                    }
                    return false;
                });
        binding.inboundIssueButton.setOnClickListener(view -> showIssues());
        binding.inboundRetryButton.setOnClickListener(
                view -> viewModel.retryInitialization());
        binding.inboundClearButton.setOnClickListener(view -> runAfterClearConfirmation("确认清空当前入库批次？",
                () -> viewModel.clearBatch()));
        binding.inboundConfirmButton.setOnClickListener(view -> showConfirmDialog());
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBack();
                    }
                });
    }

    private void addAssetCode() {
        CharSequence text = binding.inboundAssetCodeInput.getText();
        viewModel.addByAssetCode(text == null ? null : text.toString());
    }

    private void render(InboundUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean ready = state.isInitialReady() && !state.getWarehouses().isEmpty();
        boolean busy = state.isBusy();
        boolean destinationSelected = state.getSelectedWarehouseId() != null
                && state.getSelectedLocationId() != null;

        renderSelectorValues(state);
        renderResetTokens(state);
        assetAdapter.submit(state.getAssets());
        setVisible(binding.inboundEmptyText, state.getAssets().isEmpty());
        binding.inboundListTitle.setText(getString(
                R.string.inbound_list_count, state.getAssets().size()));
        binding.inboundOperatorTime.setText(getString(
                R.string.inbound_operator_format, value(state.getOperatorName())));
        binding.inboundWorkSummary.setText(getString(
                R.string.inbound_work_summary_format, state.getAssets().size(),
                state.getRawEpcCount()));
        setVisible(binding.inboundLatestEpc, hasText(state.getLatestEpc()));
        if (hasText(state.getLatestEpc())) {
            binding.inboundLatestEpc.setText(getString(
                    R.string.inbound_latest_epc_format, state.getLatestEpc()));
        }

        binding.inboundScanButton.setText(scanActionText(state));
        binding.inboundScanButton.setEnabled(ready && !busy);
        binding.inboundAssetCodeInput.setEnabled(
                ready && destinationSelected && !busy && !state.isScanning());
        binding.inboundAssetCodeButton.setEnabled(
                ready && destinationSelected && !busy && !state.isScanning());
        binding.inboundAssetCodeButton.setText(
                state.getOperation() == InboundUiState.Operation.ASSET_QUERY
                        ? R.string.inbound_asset_querying
                        : R.string.inbound_add_asset);
        binding.inboundRemarkInput.setEnabled(!busy && !state.isScanning());
        binding.inboundClearButton.setEnabled(
                state.hasPendingWork() && !busy && !state.isScanning());
        binding.inboundConfirmButton.setEnabled(state.isCanConfirm()
                && destinationSelected && !state.getAssets().isEmpty()
                && !busy && !state.isScanning());
        binding.inboundConfirmButton.setText(
                state.getOperation() == InboundUiState.Operation.CONFIRM
                        ? getString(R.string.inbound_confirming)
                        : getString(R.string.inbound_confirm_count,
                                state.getAssets().size()));

        setVisible(binding.inboundProgress, state.isInitialLoading() || busy);
        setVisible(binding.inboundInfoText, hasText(state.getInfoMessage()));
        binding.inboundInfoText.setText(value(state.getInfoMessage()));
        setVisible(binding.inboundErrorText, hasText(state.getErrorMessage()));
        binding.inboundErrorText.setText(value(state.getErrorMessage()));
        boolean canRetry = !state.isInitialLoading() && hasText(state.getErrorMessage())
                && (state.isInitialLoadFailed() || state.getWarehouses().isEmpty()
                        || state.getSelectedWarehouseId() != null
                                && state.getLocations().isEmpty());
        setVisible(binding.inboundRetryButton, canRetry);
        setVisible(binding.inboundIssueButton, !state.getIssues().isEmpty());
        binding.inboundIssueButton.setText(getString(
                R.string.inbound_issue_count, state.getIssues().size()));
        setVisible(binding.inboundPermissionHint, !state.isCanConfirm());
        renderConfirmation(state.getLastConfirmation());
    }

    private void renderSelectorValues(InboundUiState state) {
        List<String> warehouseLabels = labels(state.getWarehouses());
        binding.inboundWarehouseInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, warehouseLabels));
        binding.inboundWarehouseInput.setText(findLabel(
                state.getWarehouses(), state.getSelectedWarehouseId()), false);

        List<String> locationLabels = labels(state.getLocations());
        binding.inboundLocationInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, locationLabels));
        binding.inboundLocationInput.setText(findLabel(
                state.getLocations(), state.getSelectedLocationId()), false);

        boolean unlocked = state.isInitialReady() && !state.isBusy()
                && !state.isScanning();
        binding.inboundWarehouseLayout.setEnabled(unlocked
                && !state.getWarehouses().isEmpty());
        binding.inboundLocationLayout.setEnabled(unlocked
                && state.getSelectedWarehouseId() != null
                && !state.getLocations().isEmpty());
    }

    private void renderResetTokens(InboundUiState state) {
        if (observedAssetCodeClearVersion < 0) {
            observedAssetCodeClearVersion = state.getAssetCodeClearVersion();
        } else if (observedAssetCodeClearVersion != state.getAssetCodeClearVersion()) {
            observedAssetCodeClearVersion = state.getAssetCodeClearVersion();
            binding.inboundAssetCodeInput.setText(null);
        }
        if (observedBatchResetVersion < 0) {
            observedBatchResetVersion = state.getBatchResetVersion();
        } else if (observedBatchResetVersion != state.getBatchResetVersion()) {
            observedBatchResetVersion = state.getBatchResetVersion();
            binding.inboundRemarkInput.setText(null);
        }
    }

    private String scanActionText(InboundUiState state) {
        if (state.getOperation() == InboundUiState.Operation.PRECHECK) {
            return getString(R.string.inbound_prechecking);
        }
        if (state.getOperation() == InboundUiState.Operation.LOCATION) {
            return getString(R.string.inbound_location_loading);
        }
        if (state.isScanning()) {
            return getString(R.string.inbound_scan_stop);
        }
        if (state.getRawEpcCount() > 0) {
            return getString(R.string.inbound_precheck_retry,
                    state.getRawEpcCount());
        }
        return getString(R.string.inbound_scan_start);
    }

    private void renderConfirmation(PdaInboundBatchConfirmDto result) {
        setVisible(binding.inboundResultText, result != null);
        if (result == null) {
            return;
        }
        binding.inboundResultText.setText(getString(R.string.inbound_result_format,
                value(result.getInboundNo()), value(result.getWarehouseName()),
                value(result.getLocationName()), result.getSuccessCount(),
                value(result.getInboundUserName()), value(result.getInboundTime())));
    }

    private void showAssetDetails(InboundAssetItem item) {
        if (item == null) {
            return;
        }
        StringBuilder message = new StringBuilder();
        appendFact(message, "资产编码", item.getAssetCode());
        appendFact(message, "资产名称", item.getAssetName());
        appendFact(message, "资产类别", item.getCategoryName());
        appendFact(message, "规格型号", item.getSpecModel());
        appendFact(message, "品牌", item.getBrand());
        appendFact(message, "当前状态", item.getAssetStatusLabel());
        appendFact(message, "加入方式", item.getSource() == InboundAssetItem.Source.RFID
                ? "RFID"
                : "资产编码");
        appendFact(message, "EPC", item.getEpcCode());
        appendFact(message, "标签编码", item.getTagCode());
        new AlertDialog.Builder(this)
                .setTitle("资产明细")
                .setMessage(message.toString())
                .setPositiveButton("关闭", null)
                .show();
    }

    private void confirmRemove(InboundAssetItem item) {
        if (item == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("移除资产？")
                .setMessage(value(item.getAssetCode()) + " · "
                        + value(item.getAssetName()))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.inbound_remove,
                        (dialog, which) -> viewModel.removeAsset(item.getAssetId()))
                .show();
    }

    private void showIssues() {
        InboundUiState state = currentState();
        if (state == null || state.getIssues().isEmpty()) {
            return;
        }
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        InboundIssueAdapter adapter = new InboundIssueAdapter();
        adapter.submit(state.getIssues());
        list.setAdapter(adapter);
        int padding = getResources().getDimensionPixelSize(R.dimen.pda_item_spacing);
        list.setPadding(padding, padding, padding, padding);
        list.setClipToPadding(false);
        new AlertDialog.Builder(this)
                .setTitle("本批次异常 " + state.getIssues().size() + " 项")
                .setMessage("异常资产不会进入确认清单")
                .setView(list)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void showConfirmDialog() {
        InboundUiState state = currentState();
        if (state == null || state.getAssets().isEmpty()
                || state.getSelectedWarehouseId() == null
                || state.getSelectedLocationId() == null) {
            return;
        }
        String warehouse = findLabel(
                state.getWarehouses(), state.getSelectedWarehouseId());
        String location = findLabel(
                state.getLocations(), state.getSelectedLocationId());
        new AlertDialog.Builder(this)
                .setTitle("确认整批入库？")
                .setMessage("仓库：" + warehouse + "\n位置：" + location
                        + "\n资产：" + state.getAssets().size() + " 件")
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.inbound_confirm,
                        (dialog, which) -> viewModel.confirm(textOfRemark()))
                .show();
    }

    private void runAfterClearConfirmation(String message, Runnable action) {
        InboundUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            action.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("清空当前批次？")
                .setMessage(message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton("清空并继续",
                        (dialog, which) -> action.run())
                .show();
    }

    private void handleBack() {
        InboundUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("离开入库页？")
                .setMessage("当前未提交资产和扫描异常只保存在本页，离开后将丢失。")
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton("丢弃并离开",
                        (dialog, which) -> finish())
                .show();
    }

    private InboundUiState currentState() {
        return viewModel == null ? null : viewModel.getUiState().getValue();
    }

    private List<String> labels(List<PdaMasterDataDto> values) {
        List<String> result = new ArrayList<>();
        for (PdaMasterDataDto value : values) {
            result.add(optionLabel(value));
        }
        return result;
    }

    private String findLabel(List<PdaMasterDataDto> values, Long id) {
        if (id != null) {
            for (PdaMasterDataDto value : values) {
                if (value != null && id.equals(value.getId())) {
                    return optionLabel(value);
                }
            }
        }
        return "";
    }

    private String optionLabel(PdaMasterDataDto value) {
        if (value == null) {
            return "";
        }
        if (hasText(value.getCode()) && hasText(value.getName())) {
            return value.getName().trim() + " · " + value.getCode().trim();
        }
        return value(value.getName());
    }

    private void appendFact(StringBuilder output, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        if (output.length() > 0) {
            output.append('\n');
        }
        output.append(label).append("：").append(value.trim());
    }

    private String textOfRemark() {
        CharSequence value = binding.inboundRemarkInput.getText();
        return value == null ? null : value.toString().trim();
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
