package com.ruoyi.asset.pda.feature.borrow;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

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
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ActivityBorrowReturnBinding;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** 工业 PDA 借出/归还页：现场只提交审批申请，资产状态由后台审批领域维护。 */
public final class BorrowReturnActivity extends SessionAwareActivity {
    public static final String EXTRA_CAN_ISSUE_SCAN = "borrow_can_issue_scan";
    public static final String EXTRA_CAN_ISSUE_SUBMIT = "borrow_can_issue_submit";
    public static final String EXTRA_CAN_RETURN_SCAN = "borrow_can_return_scan";
    public static final String EXTRA_CAN_RETURN_SUBMIT = "borrow_can_return_submit";
    public static final String EXTRA_SKIP_INITIAL_LOAD = "borrow_skip_initial_load";

    private ActivityBorrowReturnBinding binding;
    private BorrowReturnViewModel viewModel;
    private BorrowAssetAdapter assetAdapter;
    private ScanKeyDispatcher scanKeyDispatcher;
    private ActivityResultLauncher<Intent> borrowerPickerLauncher;
    private int observedAssetCodeClearVersion = -1;
    private int observedBatchResetVersion = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBorrowReturnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        borrowerPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result ->
                        handleBorrowerPickerResult(result.getResultCode(), result.getData()));

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        boolean canIssueScan = getIntent().getBooleanExtra(EXTRA_CAN_ISSUE_SCAN, false);
        boolean canIssueSubmit = getIntent().getBooleanExtra(EXTRA_CAN_ISSUE_SUBMIT, false);
        boolean canReturnScan = getIntent().getBooleanExtra(EXTRA_CAN_RETURN_SCAN, false);
        boolean canReturnSubmit = getIntent().getBooleanExtra(EXTRA_CAN_RETURN_SUBMIT, false);
        viewModel = new ViewModelProvider(this,
                new BorrowReturnViewModel.Factory(container.getBorrowRepository(),
                        container.getCommonRepository(), container.getUhfScanner(),
                        canIssueScan, canIssueSubmit, canReturnScan, canReturnSubmit))
                .get(BorrowReturnViewModel.class);
        assetAdapter = new BorrowAssetAdapter(new BorrowAssetAdapter.Listener() {
            @Override
            public void onOpen(BorrowAssetItem item) {
                showAssetDetails(item);
            }

            @Override
            public void onRemove(BorrowAssetItem item) {
                confirmRemove(item);
            }
        });
        binding.borrowAssetList.setLayoutManager(new LinearLayoutManager(this));
        binding.borrowAssetList.setAdapter(assetAdapter);
        bindActions();
        viewModel.getUiState().observe(this, this::render);
        // 测试资源可跳过网络启动；真实 PDA 必须以服务端用户、字典和权限为准。
        if (!BuildConfig.DEBUG
                || !getIntent().getBooleanExtra(EXTRA_SKIP_INITIAL_LOAD, false)) {
            viewModel.initialize();
        }
    }

    private void bindActions() {
        binding.borrowToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.borrowIssueModeButton.setOnClickListener(view -> requestMode(
                BorrowReturnUiState.Mode.ISSUE));
        binding.borrowReturnModeButton.setOnClickListener(view -> requestMode(
                BorrowReturnUiState.Mode.RETURN));
        binding.borrowInternalButton.setOnClickListener(view -> viewModel.setBorrowerType("INTERNAL"));
        binding.borrowExternalButton.setOnClickListener(view -> viewModel.setBorrowerType("EXTERNAL"));
        binding.borrowerCard.setOnClickListener(view -> openBorrowerPicker());
        binding.borrowerAction.setOnClickListener(view -> openBorrowerPicker());
        binding.borrowExpectedDateButton.setOnClickListener(view -> openDatePicker());
        binding.borrowScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.borrowAssetCodeButton.setOnClickListener(view -> addAssetCode());
        binding.borrowAssetCodeInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addAssetCode();
                return true;
            }
            return false;
        });
        binding.borrowIssueButton.setOnClickListener(view -> showIssues());
        binding.borrowClearButton.setOnClickListener(view -> runAfterClearConfirmation(
                getString(R.string.borrow_clear_message), viewModel::clearBatch));
        binding.borrowRetryButton.setOnClickListener(view -> viewModel.retryInitialization());
        binding.borrowSubmitButton.setOnClickListener(view -> showSubmitDialog());
        binding.borrowExternalOrgInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.setExternalOrgName(editable == null ? null : editable.toString());
            }
        });
        binding.borrowExternalPhoneInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.setExternalContactPhone(editable == null ? null : editable.toString());
            }
        });
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBack();
                    }
                });
    }

    private void requestMode(BorrowReturnUiState.Mode nextMode) {
        BorrowReturnUiState state = currentState();
        if (state == null || state.getMode() == nextMode) {
            return;
        }
        if (!state.hasPendingWork()) {
            viewModel.setMode(nextMode);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrow_change_mode_title)
                .setMessage(R.string.borrow_change_mode_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.borrow_change_mode_confirm,
                        (dialog, which) -> viewModel.setMode(nextMode))
                .show();
    }

    private void openBorrowerPicker() {
        BorrowReturnUiState state = currentState();
        if (state == null || !state.isInitialReady()
                || state.getMode() != BorrowReturnUiState.Mode.ISSUE
                || state.isBusy() || state.isScanning()) {
            return;
        }
        borrowerPickerLauncher.launch(new Intent(this, BorrowerPickerActivity.class));
    }

    private void addAssetCode() {
        CharSequence value = binding.borrowAssetCodeInput.getText();
        viewModel.addByAssetCode(value == null ? null : value.toString());
    }

    private void handleBorrowerPickerResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        long borrowerId = data.getLongExtra(BorrowerPickerActivity.EXTRA_BORROWER_ID, -1L);
        long deptId = data.getLongExtra(BorrowerPickerActivity.EXTRA_BORROWER_DEPT_ID, -1L);
        String code = data.getStringExtra(BorrowerPickerActivity.EXTRA_BORROWER_CODE);
        String name = data.getStringExtra(BorrowerPickerActivity.EXTRA_BORROWER_NAME);
        String deptName = data.getStringExtra(BorrowerPickerActivity.EXTRA_BORROWER_DEPT_NAME);
        String phoneNumber = data.getStringExtra(BorrowerPickerActivity.EXTRA_BORROWER_PHONE);
        if (borrowerId < 1L || deptId < 1L || !hasText(name) || !hasText(deptName)) {
            return;
        }
        viewModel.selectBorrower(new PdaMasterDataDto(borrowerId, code, name, deptId, deptName,
                phoneNumber));
    }

    private void render(BorrowReturnUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean ready = state.isInitialReady();
        boolean busy = state.isBusy();
        boolean issueMode = state.getMode() == BorrowReturnUiState.Mode.ISSUE;
        boolean scanning = state.isScanning();
        boolean bothModes = state.isCanIssueScan() && state.isCanReturnScan();
        boolean inputUnlocked = ready && !busy && !scanning;

        setVisible(binding.borrowModeGroup, bothModes);
        setVisible(binding.borrowIssueModeButton, state.isCanIssueScan());
        setVisible(binding.borrowReturnModeButton, state.isCanReturnScan());
        binding.borrowIssueModeButton.setChecked(issueMode);
        binding.borrowReturnModeButton.setChecked(!issueMode);
        binding.borrowIssueModeButton.setEnabled(inputUnlocked);
        binding.borrowReturnModeButton.setEnabled(inputUnlocked);

        setVisible(binding.borrowIssueForm, issueMode);
        renderBorrowerTypes(state, inputUnlocked);
        renderBorrower(state, inputUnlocked);
        renderResetTokens(state);

        binding.borrowOperatorTime.setText(getString(R.string.receive_operator_format,
                value(state.getOperatorName())));
        assetAdapter.submit(state.getAssets());
        setVisible(binding.borrowEmptyText, state.getAssets().isEmpty());
        binding.borrowEmptyText.setText(issueMode ? R.string.borrow_empty
                : R.string.borrow_return_empty);
        binding.borrowListTitle.setText(getString(R.string.borrow_list_count,
                state.getAssets().size()));
        binding.borrowWorkSummary.setText(getString(issueMode
                ? R.string.borrow_work_summary_format
                : R.string.borrow_return_work_summary_format,
                state.getAssets().size(), state.getRawEpcCount(),
                state.getDuplicateReadCount()));

        int displayCount = state.isScanning() || state.getRawEpcCount() > 0
                ? state.getRawEpcCount() : state.getAssets().size();
        binding.borrowScanCount.setText(String.format(Locale.ROOT, "%02d", displayCount));
        binding.borrowScanStatus.setText(scanStatusText(state));
        binding.borrowLatestEpc.setText(hasText(state.getLatestEpc())
                ? getString(R.string.borrow_latest_epc_format, state.getLatestEpc())
                : getString(R.string.borrow_latest_empty));

        binding.borrowScanButton.setText(scanActionText(state));
        binding.borrowScanButton.setEnabled(ready && !busy);
        binding.borrowAssetCodeInput.setEnabled(inputUnlocked);
        binding.borrowAssetCodeButton.setEnabled(inputUnlocked);
        binding.borrowAssetCodeButton.setText(state.getOperation()
                == BorrowReturnUiState.Operation.ASSET_CODE
                ? R.string.borrow_asset_querying : R.string.borrow_add_asset);
        binding.borrowClearButton.setEnabled(state.hasPendingWork() && !busy && !scanning);
        binding.borrowIssueButton.setVisibility(state.getIssues().isEmpty()
                ? View.GONE : View.VISIBLE);
        binding.borrowIssueButton.setText(getString(R.string.borrow_issue_count,
                state.getIssues().size()));

        binding.borrowProgress.setVisibility(state.isInitialLoading() || busy
                ? View.VISIBLE : View.GONE);
        setVisible(binding.borrowInfoText, hasText(state.getInfoMessage()));
        binding.borrowInfoText.setText(value(state.getInfoMessage()));
        setVisible(binding.borrowErrorText, hasText(state.getErrorMessage()));
        binding.borrowErrorText.setText(value(state.getErrorMessage()));
        setVisible(binding.borrowRetryButton, state.isInitialLoadFailed());
        renderPermissionHint(state);
        renderSubmission(state);

        binding.borrowRemarkLayout.setVisibility(issueMode ? View.VISIBLE : View.GONE);
        binding.borrowRemarkInput.setEnabled(issueMode && inputUnlocked);
        boolean canSubmit = issueMode ? state.isCanIssueSubmit() : state.isCanReturnSubmit();
        binding.borrowSubmitButton.setEnabled(canSubmit && !state.getAssets().isEmpty()
                && ready && !busy && !scanning);
        if (state.getOperation() == BorrowReturnUiState.Operation.SUBMIT) {
            binding.borrowSubmitButton.setText(R.string.borrow_submitting);
        } else {
            binding.borrowSubmitButton.setText(getString(issueMode
                    ? R.string.borrow_submit_issue_count
                    : R.string.borrow_submit_return_count, state.getAssets().size()));
        }
    }

    private void renderBorrowerTypes(BorrowReturnUiState state, boolean enabled) {
        List<PdaDictItemDto> types = state.getBorrowerTypes();
        PdaDictItemDto internal = findType(types, "INTERNAL");
        PdaDictItemDto external = findType(types, "EXTERNAL");
        binding.borrowInternalButton.setText(internal == null || !hasText(internal.getLabel())
                ? getString(R.string.borrow_type_internal) : internal.getLabel().trim());
        binding.borrowExternalButton.setText(external == null || !hasText(external.getLabel())
                ? getString(R.string.borrow_type_external) : external.getLabel().trim());
        setVisible(binding.borrowInternalButton, internal != null);
        setVisible(binding.borrowExternalButton, external != null);
        binding.borrowInternalButton.setEnabled(enabled);
        binding.borrowExternalButton.setEnabled(enabled);
        boolean externalSelected = "EXTERNAL".equalsIgnoreCase(state.getBorrowerType());
        binding.borrowInternalButton.setChecked(!externalSelected);
        binding.borrowExternalButton.setChecked(externalSelected);
    }

    private PdaDictItemDto findType(List<PdaDictItemDto> types, String value) {
        if (types == null) {
            return null;
        }
        for (PdaDictItemDto type : types) {
            if (type != null && value.equalsIgnoreCase(type.getValue())) {
                return type;
            }
        }
        return null;
    }

    private void renderBorrower(BorrowReturnUiState state, boolean enabled) {
        PdaMasterDataDto borrower = state.getSelectedBorrower();
        boolean selected = borrower != null && borrower.getId() != null;
        binding.borrowerRoleLabel.setText("EXTERNAL".equalsIgnoreCase(state.getBorrowerType())
                ? R.string.borrow_contact_label : R.string.borrow_borrower_label);
        if (selected) {
            binding.borrowerDept.setVisibility(View.VISIBLE);
            binding.borrowerName.setText(getString(R.string.borrow_borrower_name_format,
                    value(borrower.getName()), value(borrower.getCode())));
            binding.borrowerDept.setText(getString(R.string.borrow_borrower_dept_format,
                    value(borrower.getParentName())));
        } else {
            binding.borrowerDept.setVisibility(View.GONE);
            binding.borrowerName.setText(R.string.borrow_borrower_unselected);
        }
        binding.borrowerAction.setText(selected ? R.string.borrow_change_borrower
                : R.string.borrow_select_borrower);
        binding.borrowerCard.setEnabled(enabled);
        binding.borrowerAction.setEnabled(enabled);
        boolean showExternal = state.getMode() == BorrowReturnUiState.Mode.ISSUE
                && "EXTERNAL".equalsIgnoreCase(state.getBorrowerType());
        setVisible(binding.borrowExternalForm, showExternal);
        binding.borrowExternalOrgInput.setEnabled(enabled && showExternal);
        binding.borrowExternalPhoneInput.setEnabled(enabled && showExternal);
        setTextIfChanged(binding.borrowExternalOrgInput, state.getExternalOrgName());
        setTextIfChanged(binding.borrowExternalPhoneInput, state.getExternalContactPhone());
        binding.borrowExpectedDateButton.setEnabled(enabled);
        if (hasText(state.getExpectedReturnDate())) {
            binding.borrowExpectedDateButton.setText(getString(
                    R.string.borrow_expected_date_format, state.getExpectedReturnDate()));
        } else {
            binding.borrowExpectedDateButton.setText(R.string.borrow_expected_date_unselected);
        }
    }

    private void renderResetTokens(BorrowReturnUiState state) {
        if (observedAssetCodeClearVersion < 0) {
            observedAssetCodeClearVersion = state.getAssetCodeClearVersion();
        } else if (observedAssetCodeClearVersion != state.getAssetCodeClearVersion()) {
            observedAssetCodeClearVersion = state.getAssetCodeClearVersion();
            binding.borrowAssetCodeInput.setText(null);
        }
        if (observedBatchResetVersion < 0) {
            observedBatchResetVersion = state.getBatchResetVersion();
        } else if (observedBatchResetVersion != state.getBatchResetVersion()) {
            observedBatchResetVersion = state.getBatchResetVersion();
            binding.borrowRemarkInput.setText(null);
        }
    }

    private String scanStatusText(BorrowReturnUiState state) {
        if (state.isScanning()) {
            return getString(R.string.borrow_scan_active);
        }
        if (state.getOperation() == BorrowReturnUiState.Operation.PRECHECK
                || state.getOperation() == BorrowReturnUiState.Operation.ASSET_CODE) {
            return getString(R.string.borrow_prechecking);
        }
        if (state.getAssets().isEmpty() && state.getRawEpcCount() == 0) {
            return getString(R.string.borrow_scan_waiting);
        }
        return getString(R.string.borrow_scan_ready);
    }

    private String scanActionText(BorrowReturnUiState state) {
        if (state.getOperation() == BorrowReturnUiState.Operation.PRECHECK) {
            return getString(R.string.borrow_prechecking);
        }
        if (state.getOperation() == BorrowReturnUiState.Operation.ASSET_CODE) {
            return getString(R.string.borrow_asset_querying);
        }
        if (state.isScanning()) {
            return getString(R.string.borrow_scan_stop);
        }
        if (state.getRawEpcCount() > 0) {
            return getString(R.string.borrow_precheck_retry, state.getRawEpcCount());
        }
        return getString(R.string.borrow_scan_start);
    }

    private void renderPermissionHint(BorrowReturnUiState state) {
        boolean scanAllowed = state.getMode() == BorrowReturnUiState.Mode.ISSUE
                ? state.isCanIssueScan() : state.isCanReturnScan();
        boolean submitAllowed = state.getMode() == BorrowReturnUiState.Mode.ISSUE
                ? state.isCanIssueSubmit() : state.isCanReturnSubmit();
        if (!scanAllowed) {
            binding.borrowPermissionHint.setText(R.string.borrow_no_scan_permission);
            binding.borrowPermissionHint.setVisibility(View.VISIBLE);
        } else if (!submitAllowed) {
            binding.borrowPermissionHint.setText(R.string.borrow_no_submit_permission);
            binding.borrowPermissionHint.setVisibility(View.VISIBLE);
        } else {
            binding.borrowPermissionHint.setVisibility(View.GONE);
        }
    }

    private void renderSubmission(BorrowReturnUiState state) {
        PdaBorrowIssueBatchSubmitDto issue = state.getLastIssueSubmission();
        PdaBorrowReturnBatchSubmitDto returned = state.getLastReturnSubmission();
        if (issue != null) {
            String taskId = issue.getApprovalTask() == null
                    || issue.getApprovalTask().getTaskId() == null ? "-"
                    : String.valueOf(issue.getApprovalTask().getTaskId());
            binding.borrowResultText.setText(getString(R.string.borrow_issue_result_format,
                    value(issue.getBorrowNo()), value(issue.getBorrowUserName()),
                    value(issue.getBorrowDeptName()), issue.getSuccessCount(), taskId));
            binding.borrowResultText.setVisibility(View.VISIBLE);
        } else if (returned != null) {
            binding.borrowResultText.setText(getString(R.string.borrow_return_result_format,
                    returned.getSuccessCount()));
            binding.borrowResultText.setVisibility(View.VISIBLE);
        } else {
            binding.borrowResultText.setVisibility(View.GONE);
        }
    }

    private void openDatePicker() {
        BorrowReturnUiState state = currentState();
        if (state == null || !state.isInitialReady() || state.isBusy() || state.isScanning()) {
            return;
        }
        Calendar minimum = parseDate(state.getServerTime());
        Calendar selected = hasText(state.getExpectedReturnDate())
                ? parseDate(state.getExpectedReturnDate()) : (Calendar) minimum.clone();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> viewModel.setExpectedReturnDate(
                        String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1,
                                dayOfMonth)), selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
        dialog.setTitle(R.string.borrow_date_picker_title);
        dialog.getDatePicker().setMinDate(minimum.getTimeInMillis());
        dialog.show();
    }

    private Calendar parseDate(String value) {
        Calendar result = Calendar.getInstance();
        if (!hasText(value)) {
            return result;
        }
        String date = value.trim().length() >= 10 ? value.trim().substring(0, 10) : value.trim();
        try {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8, 10));
            result.set(Calendar.YEAR, year);
            result.set(Calendar.MONTH, month - 1);
            result.set(Calendar.DAY_OF_MONTH, day);
            result.set(Calendar.HOUR_OF_DAY, 0);
            result.set(Calendar.MINUTE, 0);
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MILLISECOND, 0);
        } catch (RuntimeException ignored) {
            // 服务时间异常时使用设备日期，提交时仍由后端再次校验日期门槛。
        }
        return result;
    }

    private void showAssetDetails(BorrowAssetItem item) {
        if (item == null) {
            return;
        }
        StringBuilder message = new StringBuilder();
        appendFact(message, R.string.borrow_detail_asset_code, item.getAssetCode());
        appendFact(message, R.string.borrow_detail_asset_name, item.getAssetName());
        appendFact(message, R.string.borrow_detail_category, item.getCategoryName());
        appendFact(message, R.string.borrow_detail_model, item.getSpecModel());
        appendFact(message, R.string.borrow_detail_brand, item.getBrand());
        appendFact(message, R.string.borrow_detail_status, item.getAssetStatusLabel());
        appendFact(message, R.string.borrow_detail_source, item.getSource()
                == BorrowAssetItem.Source.RFID ? getString(R.string.borrow_source_rfid)
                : getString(R.string.borrow_source_asset_code));
        appendFact(message, R.string.borrow_detail_identifier,
                item.getIdentifier().getIdentifyValue());
        appendFact(message, R.string.borrow_detail_order, item.getBorrowNo());
        appendFact(message, R.string.borrow_detail_before_location,
                hasText(item.getBeforeWarehouseName()) || hasText(item.getBeforeLocationName())
                        ? value(item.getBeforeWarehouseName()) + " / "
                        + value(item.getBeforeLocationName()) : null);
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrow_detail_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.common_close, null)
                .show();
    }

    private void confirmRemove(BorrowAssetItem item) {
        if (item == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.receive_remove_title)
                .setMessage(getString(R.string.receive_remove_message,
                        value(item.getAssetCode()), value(item.getAssetName())))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.borrow_remove,
                        (dialog, which) -> viewModel.removeAsset(item.getAssetId()))
                .show();
    }

    private void showIssues() {
        BorrowReturnUiState state = currentState();
        if (state == null || state.getIssues().isEmpty()) {
            return;
        }
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        BorrowIssueAdapter adapter = new BorrowIssueAdapter();
        adapter.submit(state.getIssues());
        list.setAdapter(adapter);
        int padding = getResources().getDimensionPixelSize(R.dimen.pda_item_spacing);
        list.setPadding(padding, padding, padding, padding);
        list.setClipToPadding(false);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.borrow_issues_title, state.getIssues().size()))
                .setMessage(R.string.borrow_issues_hint)
                .setView(list)
                .setPositiveButton(R.string.common_close, null)
                .show();
    }

    private void showSubmitDialog() {
        BorrowReturnUiState state = currentState();
        if (state == null || state.getAssets().isEmpty()) {
            return;
        }
        if (state.getMode() == BorrowReturnUiState.Mode.ISSUE) {
            PdaMasterDataDto borrower = state.getSelectedBorrower();
            if (borrower == null) {
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.borrow_submit_issue_title)
                    .setMessage(getString(R.string.borrow_submit_issue_message,
                            value(borrower.getName()), value(borrower.getParentName()),
                            borrowerTypeLabel(state), value(state.getExpectedReturnDate()),
                            state.getAssets().size()))
                    .setNegativeButton(R.string.common_cancel, null)
                    .setPositiveButton(R.string.borrow_submit,
                            (dialog, which) -> viewModel.submit(textOfRemark()))
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.borrow_submit_return_title)
                    .setMessage(getString(R.string.borrow_submit_return_message,
                            state.getAssets().size()))
                    .setNegativeButton(R.string.common_cancel, null)
                    .setPositiveButton(R.string.borrow_submit,
                            (dialog, which) -> viewModel.submit(null))
                    .show();
        }
    }

    private String borrowerTypeLabel(BorrowReturnUiState state) {
        PdaDictItemDto type = findType(state.getBorrowerTypes(), state.getBorrowerType());
        return type != null && hasText(type.getLabel()) ? type.getLabel().trim()
                : value(state.getBorrowerType());
    }

    private void runAfterClearConfirmation(String message, Runnable action) {
        BorrowReturnUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            action.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrow_clear_title)
                .setMessage(message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.borrow_clear_and_continue,
                        (dialog, which) -> action.run())
                .show();
    }

    private void handleBack() {
        BorrowReturnUiState state = currentState();
        if (state == null || !state.hasPendingWork()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrow_leave_title)
                .setMessage(R.string.borrow_leave_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.borrow_leave_confirm,
                        (dialog, which) -> finish())
                .show();
    }

    private BorrowReturnUiState currentState() {
        return viewModel == null ? null : viewModel.getUiState().getValue();
    }

    private String textOfRemark() {
        CharSequence value = binding.borrowRemarkInput.getText();
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

    private void setTextIfChanged(EditText input, String value) {
        String expected = value == null ? "" : value;
        CharSequence current = input.getText();
        if (current == null || !expected.contentEquals(current)) {
            input.setText(expected);
        }
    }

    private String value(String value) {
        return hasText(value) ? value.trim() : getString(R.string.common_unknown);
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
                    () -> {
                        if (viewModel != null) {
                            viewModel.onScanKeyPressed();
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

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }
}
