package com.ruoyi.asset.pda.feature.repair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.databinding.ActivityRepairStartBinding;

/** 现场登记维修方；内部人员只向后端提交用户 ID。 */
public final class RepairStartActivity extends SessionAwareActivity {
    public static final String EXTRA_REPAIR_ID = "repair_id";
    private static final int REQUEST_REPAIRER = 4301;

    private ActivityRepairStartBinding binding;
    private RepairStartViewModel viewModel;
    private Long repairId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        repairId = getIntent().hasExtra(EXTRA_REPAIR_ID)
                ? getIntent().getLongExtra(EXTRA_REPAIR_ID, 0L) : 0L;
        if (repairId == null || repairId < 1L) {
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this,
                new RepairStartViewModel.Factory(repairId, container.getRepairRepository()))
                .get(RepairStartViewModel.class);
        bindActions();
        viewModel.getState().observe(this, this::render);
        viewModel.load();
    }

    private void bindActions() {
        binding.repairStartToolbar.setNavigationOnClickListener(view -> finish());
        binding.repairStartTypeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.selectRepairerType(checkedId == R.id.repairStartInternalButton
                        ? RepairUi.REPAIRER_INTERNAL : RepairUi.REPAIRER_EXTERNAL);
            }
        });
        binding.repairStartSelectRepairer.setOnClickListener(view -> startActivityForResult(
                new Intent(this, RepairerPickerActivity.class), REQUEST_REPAIRER));
        binding.repairStartRetry.setOnClickListener(view -> viewModel.load());
        binding.repairStartAction.setOnClickListener(view -> submit());
    }

    private void submit() {
        CharSequence org = binding.repairStartExternalOrg.getText();
        CharSequence contact = binding.repairStartExternalContact.getText();
        CharSequence phone = binding.repairStartExternalPhone.getText();
        viewModel.submit(org == null ? null : org.toString(),
                contact == null ? null : contact.toString(),
                phone == null ? null : phone.toString());
    }

    private void render(RepairStartUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean loading = state.getMode() == RepairStartUiState.Mode.LOADING;
        boolean success = state.getMode() == RepairStartUiState.Mode.SUCCESS;
        boolean hasOrder = state.getOrder() != null;
        setVisible(binding.repairStartProgress, loading || state.getMode() == RepairStartUiState.Mode.SUBMITTING);
        setVisible(binding.repairStartForm, hasOrder);
        setVisible(binding.repairStartRetry, !hasOrder && state.getMode() == RepairStartUiState.Mode.ERROR);
        setVisible(binding.repairStartMessage, state.getMode() == RepairStartUiState.Mode.ERROR
                && RepairUi.hasText(state.getMessage()));
        binding.repairStartMessage.setText(state.getMessage());
        if (!hasOrder) {
            setVisible(binding.repairStartAction, false);
            return;
        }
        renderOrder(state.getOrder());
        int selectedType = state.isInternal() ? R.id.repairStartInternalButton
                : R.id.repairStartExternalButton;
        if (binding.repairStartTypeGroup.getCheckedButtonId() != selectedType) {
            binding.repairStartTypeGroup.check(selectedType);
        }
        boolean busy = state.isBusy();
        binding.repairStartInternalButton.setEnabled(!busy);
        binding.repairStartExternalButton.setEnabled(!busy);
        setVisible(binding.repairStartInternalContainer, state.isInternal());
        setVisible(binding.repairStartExternalContainer, !state.isInternal());
        binding.repairStartSelectRepairer.setEnabled(!busy);
        binding.repairStartExternalOrg.setEnabled(!busy);
        binding.repairStartExternalContact.setEnabled(!busy);
        binding.repairStartExternalPhone.setEnabled(!busy);
        if (state.getRepairerId() == null) {
            binding.repairStartSelectRepairer.setText(R.string.repair_select_repairer);
        } else {
            binding.repairStartSelectRepairer.setText(getString(
                    R.string.repair_repairer_selected_format, display(state.getRepairerName()),
                    display(state.getRepairerCode())));
        }
        setVisible(binding.repairStartAction, !success);
        binding.repairStartAction.setEnabled(!busy);
        binding.repairStartAction.setText(busy ? R.string.repair_start_submitting
                : R.string.repair_start_submit);
        if (success) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void renderOrder(PdaRepairOrderDto order) {
        binding.repairStartOrderNo.setText(getString(R.string.repair_detail_no_format,
                display(order.getRepairNo())));
        binding.repairStartAsset.setText(getString(R.string.repair_asset_name_format,
                display(order.getAssetName())));
        binding.repairStartFault.setText(getString(R.string.repair_detail_fault_format,
                display(order.getFaultDesc())));
    }

    private String display(String value) {
        return RepairUi.hasText(value) ? value.trim() : getString(R.string.common_unknown);
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_REPAIRER || resultCode != RESULT_OK || data == null) {
            return;
        }
        long id = data.getLongExtra(RepairerPickerActivity.EXTRA_REPAIRER_ID, 0L);
        if (id > 0L) {
            viewModel.selectRepairer(id,
                    data.getStringExtra(RepairerPickerActivity.EXTRA_REPAIRER_NAME),
                    data.getStringExtra(RepairerPickerActivity.EXTRA_REPAIRER_CODE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
