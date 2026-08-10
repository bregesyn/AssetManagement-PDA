package com.ruoyi.asset.pda.feature.repair;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaApprovalTaskSnapshotDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.databinding.ActivityRepairSubmitBinding;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 现场单资产报修。F6 与屏幕按钮进入同一 SINGLE 扫描入口。 */
public final class RepairSubmitActivity extends SessionAwareActivity {
    private ActivityRepairSubmitBinding binding;
    private RepairSubmitViewModel viewModel;
    private ScanKeyDispatcher scanKeyDispatcher;
    private String expectedFinishTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairSubmitBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this, new RepairSubmitViewModel.Factory(
                container.getAssetRepository(), container.getRepairRepository(),
                container.getCommonRepository(), container.getUhfScanner()))
                .get(RepairSubmitViewModel.class);
        scanKeyDispatcher = new ScanKeyDispatcher(this, viewModel::onScanKeyPressed);
        bindActions();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptLeave();
            }
        });
        viewModel.getState().observe(this, this::render);
        viewModel.initialize();
    }

    private void bindActions() {
        binding.repairSubmitToolbar.setNavigationOnClickListener(view -> attemptLeave());
        binding.repairSubmitTypeGroup.setOnCheckedChangeListener((group, checkedId) ->
                viewModel.selectIdentifyType(checkedId == R.id.repairSubmitEpcRadio
                        ? AssetRepository.IDENTIFY_TYPE_EPC
                        : AssetRepository.IDENTIFY_TYPE_ASSET_CODE));
        binding.repairSubmitScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.repairSubmitAssetCodeButton.setOnClickListener(view -> identifyAssetCode());
        binding.repairSubmitAssetCodeInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                identifyAssetCode();
                return true;
            }
            return false;
        });
        binding.repairSubmitExpectedButton.setOnClickListener(view -> showExpectedDatePicker());
        binding.repairSubmitAction.setOnClickListener(view -> submitOrFinish());
    }

    private void identifyAssetCode() {
        CharSequence value = binding.repairSubmitAssetCodeInput.getText();
        viewModel.identifyAssetCode(value == null ? null : value.toString());
    }

    private void submitOrFinish() {
        RepairSubmitUiState state = viewModel.getState().getValue();
        if (state != null && state.getMode() == RepairSubmitUiState.Mode.SUCCESS) {
            setResult(RESULT_OK);
            finish();
            return;
        }
        if (state != null && state.getCurrentUser() == null && !state.isBusy()) {
            viewModel.initialize();
            return;
        }
        CharSequence fault = binding.repairSubmitFaultInput.getText();
        CharSequence remark = binding.repairSubmitRemarkInput.getText();
        viewModel.submit(fault == null ? null : fault.toString(), expectedFinishTime,
                remark == null ? null : remark.toString());
    }

    private void render(RepairSubmitUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean epc = state.isEpcMode();
        int checkedId = epc ? R.id.repairSubmitEpcRadio : R.id.repairSubmitAssetCodeRadio;
        if (binding.repairSubmitTypeGroup.getCheckedRadioButtonId() != checkedId) {
            binding.repairSubmitTypeGroup.check(checkedId);
        }
        setVisible(binding.repairSubmitEpcContainer, epc);
        setVisible(binding.repairSubmitAssetCodeContainer, !epc);
        boolean busy = state.isBusy();
        binding.repairSubmitTypeGroup.setEnabled(!busy && state.getResult() == null);
        binding.repairSubmitScanButton.setEnabled(!busy);
        binding.repairSubmitAssetCodeInput.setEnabled(!busy);
        binding.repairSubmitAssetCodeButton.setEnabled(!busy);
        binding.repairSubmitFaultInput.setEnabled(!busy && state.getResult() == null);
        binding.repairSubmitRemarkInput.setEnabled(!busy && state.getResult() == null);
        binding.repairSubmitExpectedButton.setEnabled(!busy && state.getResult() == null);
        binding.repairSubmitScanButton.setText(state.isScanning()
                ? R.string.repair_scan_stop : R.string.repair_scan_start);
        binding.repairSubmitScanState.setText(scanStateText(state));
        binding.repairSubmitReporter.setText(getString(R.string.repair_reporter_format,
                state.getCurrentUser() == null ? getString(R.string.common_unknown)
                        : display(state.getCurrentUser().getUserName())));
        binding.repairSubmitExpectedButton.setText(RepairUi.hasText(expectedFinishTime)
                ? getString(R.string.repair_expected_finish_format, expectedFinishTime)
                : getString(R.string.repair_expected_finish_empty));
        setVisible(binding.repairSubmitProgress, busy);
        setVisible(binding.repairSubmitMessage, RepairUi.hasText(state.getMessage()));
        binding.repairSubmitMessage.setText(state.getMessage());
        renderAsset(state.getAsset());
        renderResult(state.getResult());
        boolean success = state.getMode() == RepairSubmitUiState.Mode.SUCCESS;
        binding.repairSubmitAction.setEnabled(!busy);
        binding.repairSubmitAction.setText(success ? R.string.repair_back_to_workbench
                : busy ? R.string.repair_submitting
                : state.getCurrentUser() == null ? R.string.common_retry
                : R.string.repair_submit_action);
    }

    private void renderAsset(PdaAssetIdentifyDto asset) {
        setVisible(binding.repairSubmitAssetCard, asset != null);
        if (asset == null) {
            return;
        }
        binding.repairSubmitAssetName.setText(getString(R.string.repair_asset_name_format,
                display(asset.getAssetName())));
        binding.repairSubmitAssetCode.setText(getString(R.string.repair_asset_code_format,
                display(asset.getAssetCode())));
        binding.repairSubmitAssetCategory.setText(getString(R.string.repair_asset_category_format,
                display(asset.getCategoryName())));
        binding.repairSubmitAssetModel.setText(getString(R.string.repair_asset_model_format,
                display(asset.getSpecModel())));
    }

    private void renderResult(PdaRepairSubmitResultDto result) {
        setVisible(binding.repairSubmitResult, result != null);
        if (result == null) {
            return;
        }
        PdaApprovalTaskSnapshotDto approval = result.getApprovalTask();
        binding.repairSubmitResult.setText(getString(R.string.repair_submit_result_format,
                result.getOrder() == null ? getString(R.string.common_unknown)
                        : display(result.getOrder().getRepairNo()),
                approval == null || approval.getTaskId() == null ? getString(R.string.common_unknown)
                        : String.valueOf(approval.getTaskId()),
                approval == null ? getString(R.string.common_unknown)
                        : display(approval.getTaskStatus())));
    }

    private void showExpectedDatePicker() {
        RepairSubmitUiState state = viewModel.getState().getValue();
        if (state == null || state.isBusy() || state.getResult() != null) {
            return;
        }
        Calendar calendar = calendarFor(expectedFinishTime);
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    expectedFinishTime = String.format(Locale.ROOT, "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    render(state);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        Date serverDate = parseDate(state.getServerDate());
        if (serverDate != null) {
            dialog.getDatePicker().setMinDate(serverDate.getTime());
        }
        dialog.setTitle(R.string.repair_date_picker_title);
        dialog.show();
    }

    private Calendar calendarFor(String value) {
        Calendar calendar = Calendar.getInstance();
        Date parsed = parseDate(value);
        if (parsed != null) {
            calendar.setTime(parsed);
        }
        return calendar;
    }

    private Date parseDate(String value) {
        if (!RepairUi.hasText(value)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setLenient(false);
        try {
            return format.parse(value);
        } catch (ParseException exception) {
            return null;
        }
    }

    private String scanStateText(RepairSubmitUiState state) {
        if (state.getMode() == RepairSubmitUiState.Mode.IDENTIFYING) {
            return getString(R.string.repair_scan_stop);
        }
        return state.isScanning() ? getString(R.string.repair_scan_stop)
                : getString(R.string.repair_single_scan_hint);
    }

    private void attemptLeave() {
        CharSequence fault = binding.repairSubmitFaultInput.getText();
        CharSequence remark = binding.repairSubmitRemarkInput.getText();
        if (viewModel.hasUnsavedWork(fault == null ? null : fault.toString(), expectedFinishTime,
                remark == null ? null : remark.toString())) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.repair_leave_title)
                    .setMessage(R.string.repair_leave_message)
                    .setNegativeButton(R.string.common_cancel, null)
                    .setPositiveButton(R.string.repair_leave_confirm,
                            (DialogInterface dialog, int which) -> finish())
                    .show();
            return;
        }
        finish();
    }

    private String display(String value) {
        return RepairUi.hasText(value) ? value.trim() : getString(R.string.common_unknown);
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (scanKeyDispatcher != null) {
            scanKeyDispatcher.start();
        }
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
