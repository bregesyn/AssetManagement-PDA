package com.ruoyi.asset.pda.feature.repair;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.databinding.ActivityRepairFinishBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 维修完工登记。写入前重新读取工单，避免审批或其他维修人员已变更状态时重复完工。 */
public final class RepairFinishActivity extends SessionAwareActivity {
    public static final String EXTRA_REPAIR_ID = "repair_id";

    private ActivityRepairFinishBinding binding;
    private RepairFinishViewModel viewModel;
    private String finishDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairFinishBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        long repairId = getIntent().getLongExtra(EXTRA_REPAIR_ID, 0L);
        if (repairId < 1L) {
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this,
                new RepairFinishViewModel.Factory(repairId, container.getRepairRepository()))
                .get(RepairFinishViewModel.class);
        binding.repairFinishToolbar.setNavigationOnClickListener(view -> finish());
        binding.repairFinishDateButton.setOnClickListener(view -> showDatePicker());
        binding.repairFinishRetry.setOnClickListener(view -> viewModel.load());
        binding.repairFinishAction.setOnClickListener(view -> submit());
        viewModel.getState().observe(this, this::render);
        viewModel.load();
    }

    private void submit() {
        CharSequence result = binding.repairFinishResultInput.getText();
        CharSequence cost = binding.repairFinishCostInput.getText();
        viewModel.submit(finishDate, result == null ? null : result.toString(),
                cost == null ? null : cost.toString());
    }

    private void render(RepairFinishUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean loading = state.getMode() == RepairFinishUiState.Mode.LOADING;
        boolean busy = state.isBusy();
        boolean success = state.getMode() == RepairFinishUiState.Mode.SUCCESS;
        boolean hasOrder = state.getOrder() != null;
        setVisible(binding.repairFinishProgress, busy);
        setVisible(binding.repairFinishForm, hasOrder);
        setVisible(binding.repairFinishMessage, state.getMode() == RepairFinishUiState.Mode.ERROR
                && RepairUi.hasText(state.getMessage()));
        binding.repairFinishMessage.setText(state.getMessage());
        setVisible(binding.repairFinishRetry, !hasOrder && state.getMode() == RepairFinishUiState.Mode.ERROR);
        if (!hasOrder) {
            setVisible(binding.repairFinishAction, false);
            return;
        }
        renderOrder(state.getOrder());
        binding.repairFinishDateButton.setEnabled(!busy);
        binding.repairFinishResultInput.setEnabled(!busy);
        binding.repairFinishCostInput.setEnabled(!busy);
        binding.repairFinishDateButton.setText(RepairUi.hasText(finishDate)
                ? getString(R.string.repair_finish_date_format, finishDate)
                : getString(R.string.repair_finish_date_empty));
        setVisible(binding.repairFinishAction, !success);
        binding.repairFinishAction.setEnabled(!busy);
        binding.repairFinishAction.setText(busy ? R.string.repair_finish_submitting
                : R.string.repair_finish_submit);
        if (success) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void renderOrder(PdaRepairOrderDto order) {
        binding.repairFinishOrderNo.setText(getString(R.string.repair_detail_no_format,
                display(order.getRepairNo())));
        binding.repairFinishAsset.setText(getString(R.string.repair_asset_name_format,
                display(order.getAssetName())));
        binding.repairFinishStart.setText(getString(R.string.repair_detail_start_format,
                display(order.getRepairStartTime())));
    }

    private void showDatePicker() {
        RepairFinishUiState state = viewModel.getState().getValue();
        if (state == null || state.isBusy() || state.getOrder() == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        Date selected = parseDate(finishDate);
        if (selected != null) {
            calendar.setTime(selected);
        }
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            finishDate = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            render(state);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        Date startDate = parseDate(RepairUi.datePart(state.getOrder().getRepairStartTime()));
        if (startDate != null) {
            dialog.getDatePicker().setMinDate(startDate.getTime());
        }
        dialog.setTitle(R.string.repair_finish_date_picker_title);
        dialog.show();
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

    private String display(String value) {
        return RepairUi.hasText(value) ? value.trim() : getString(R.string.common_unknown);
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
