package com.ruoyi.asset.pda.feature.repair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.databinding.ActivityRepairDetailBinding;

import java.math.BigDecimal;

/** 只读详情先展示服务器状态，再将开始/完工操作交给独立表单重新核对。 */
public final class RepairDetailActivity extends SessionAwareActivity {
    public static final String EXTRA_REPAIR_ID = "repair_id";
    public static final String EXTRA_CAN_LIST = "repair_detail_can_list";
    public static final String EXTRA_CAN_START = "repair_detail_can_start";
    public static final String EXTRA_CAN_FINISH = "repair_detail_can_finish";
    private static final int REQUEST_START = 4201;
    private static final int REQUEST_FINISH = 4202;

    private ActivityRepairDetailBinding binding;
    private RepairDetailViewModel viewModel;
    private Long repairId;
    private boolean canStart;
    private boolean canFinish;
    private boolean changed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        repairId = getIntent().hasExtra(EXTRA_REPAIR_ID)
                ? getIntent().getLongExtra(EXTRA_REPAIR_ID, 0L) : 0L;
        canStart = getIntent().getBooleanExtra(EXTRA_CAN_START, false);
        canFinish = getIntent().getBooleanExtra(EXTRA_CAN_FINISH, false);
        if (repairId == null || repairId < 1L) {
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this, new RepairDetailViewModel.Factory(repairId,
                container.getRepairRepository(), container.getCommonRepository(), canStart, canFinish))
                .get(RepairDetailViewModel.class);
        binding.repairDetailToolbar.setNavigationOnClickListener(view -> finishWithResult());
        binding.repairDetailRetry.setOnClickListener(view -> viewModel.load());
        binding.repairDetailAction.setOnClickListener(view -> openAction());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithResult();
            }
        });
        viewModel.getState().observe(this, this::render);
        viewModel.load();
    }

    private void render(RepairDetailUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean loading = state.getMode() == RepairDetailUiState.Mode.LOADING;
        boolean error = state.getMode() == RepairDetailUiState.Mode.ERROR;
        setVisible(binding.repairDetailProgress, loading);
        setVisible(binding.repairDetailError, error);
        binding.repairDetailError.setText(error ? RepairUi.displayText(state.getMessage()) : "");
        setVisible(binding.repairDetailRetry, error);
        setVisible(binding.repairDetailContent, !loading && !error);
        if (loading || error || state.getOrder() == null) {
            setVisible(binding.repairDetailAction, false);
            return;
        }
        renderOrder(state);
        if (state.canStartAction()) {
            binding.repairDetailAction.setText(R.string.repair_start_action);
            setVisible(binding.repairDetailAction, true);
        } else if (state.canFinishAction()) {
            binding.repairDetailAction.setText(R.string.repair_finish_action);
            setVisible(binding.repairDetailAction, true);
        } else {
            setVisible(binding.repairDetailAction, false);
        }
    }

    private void renderOrder(RepairDetailUiState state) {
        PdaRepairOrderDto order = state.getOrder();
        binding.repairDetailRail.setBackgroundColor(ContextCompat.getColor(this,
                RepairUi.statusRailColor(order.getOrderStatus())));
        binding.repairDetailNo.setText(getString(R.string.repair_detail_no_format,
                display(order.getRepairNo())));
        binding.repairDetailStatus.setText(getString(R.string.repair_detail_status_format,
                RepairUi.statusLabel(order.getOrderStatus(), state.getStatuses())));
        binding.repairDetailAssetName.setText(getString(R.string.repair_asset_name_format,
                display(order.getAssetName())));
        binding.repairDetailAssetCode.setText(getString(R.string.repair_asset_code_format,
                display(order.getAssetCode())));
        binding.repairDetailAssetCategory.setText(getString(R.string.repair_asset_category_format,
                display(order.getCategoryName())));
        binding.repairDetailAssetModel.setText(getString(R.string.repair_asset_model_format,
                display(order.getSpecModel())));
        binding.repairDetailReporter.setText(getString(R.string.repair_detail_reporter_format,
                display(order.getReportUserName())));
        binding.repairDetailFault.setText(getString(R.string.repair_detail_fault_format,
                display(order.getFaultDesc())));
        binding.repairDetailReportTime.setText(getString(R.string.repair_detail_report_time_format,
                display(order.getReportTime())));
        binding.repairDetailExpected.setText(getString(R.string.repair_detail_expected_format,
                display(order.getExpectedFinishTime())));
        binding.repairDetailReject.setText(getString(R.string.repair_detail_reject_format,
                display(order.getRejectReason())));
        binding.repairDetailRepairerType.setText(getString(R.string.repair_detail_repairer_type_format,
                repairerTypeLabel(order.getRepairerType())));
        binding.repairDetailRepairer.setText(getString(R.string.repair_detail_repairer_format,
                display(RepairUi.REPAIRER_EXTERNAL.equals(order.getRepairerType())
                        ? order.getRepairOrgName() : order.getRepairUserName())));
        binding.repairDetailContact.setText(getString(R.string.repair_detail_contact_format,
                display(RepairUi.REPAIRER_EXTERNAL.equals(order.getRepairerType())
                        ? order.getRepairUserName() : null)));
        binding.repairDetailPhone.setText(getString(R.string.repair_detail_phone_format,
                display(order.getRepairContactPhone())));
        binding.repairDetailStart.setText(getString(R.string.repair_detail_start_format,
                display(order.getRepairStartTime())));
        binding.repairDetailFinish.setText(getString(R.string.repair_detail_finish_format,
                display(order.getRepairFinishTime())));
        binding.repairDetailResult.setText(getString(R.string.repair_detail_result_format,
                display(order.getRepairResult())));
        binding.repairDetailCost.setText(getString(R.string.repair_detail_cost_format,
                displayCost(order.getRepairCost())));
        binding.repairDetailRemark.setText(getString(R.string.repair_detail_remark_format,
                display(order.getRemark())));
    }

    private void openAction() {
        RepairDetailUiState state = viewModel.getState().getValue();
        if (state == null || state.getOrder() == null) {
            return;
        }
        Intent intent;
        int requestCode;
        if (state.canStartAction()) {
            intent = new Intent(this, RepairStartActivity.class);
            requestCode = REQUEST_START;
        } else if (state.canFinishAction()) {
            intent = new Intent(this, RepairFinishActivity.class);
            requestCode = REQUEST_FINISH;
        } else {
            return;
        }
        intent.putExtra(RepairStartActivity.EXTRA_REPAIR_ID, repairId);
        startActivityForResult(intent, requestCode);
    }

    private String repairerTypeLabel(String type) {
        if (RepairUi.REPAIRER_INTERNAL.equals(type)) {
            return getString(R.string.repair_repairer_internal);
        }
        if (RepairUi.REPAIRER_EXTERNAL.equals(type)) {
            return getString(R.string.repair_repairer_external);
        }
        return display(type);
    }

    private String displayCost(BigDecimal cost) {
        return cost == null ? getString(R.string.common_unknown) : cost.toPlainString();
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
        if ((requestCode == REQUEST_START || requestCode == REQUEST_FINISH) && resultCode == RESULT_OK) {
            changed = true;
            viewModel.load();
        }
    }

    private void finishWithResult() {
        if (changed) {
            setResult(RESULT_OK);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
