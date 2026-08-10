package com.ruoyi.asset.pda.feature.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.databinding.ActivityHomeBinding;
import com.ruoyi.asset.pda.feature.identify.AssetIdentifyActivity;
import com.ruoyi.asset.pda.feature.inbound.InboundActivity;
import com.ruoyi.asset.pda.feature.inventory.InventoryTaskListActivity;
import com.ruoyi.asset.pda.feature.login.LoginActivity;
import com.ruoyi.asset.pda.feature.receive.ReceiveActivity;
import com.ruoyi.asset.pda.feature.repair.RepairWorkbenchActivity;
import com.ruoyi.asset.pda.feature.borrow.BorrowReturnActivity;
import com.ruoyi.asset.pda.feature.rfid.RfidBindActivity;
import com.ruoyi.asset.pda.feature.rfid.RfidTagBatchActivity;
import com.ruoyi.asset.pda.feature.rfid.RfidUnbindActivity;

public final class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private SessionManager sessionManager;
    private boolean navigatingToLogin;
    private final SessionManager.Listener sessionListener = this::navigateToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        sessionManager = container.getSessionManager();
        viewModel = new ViewModelProvider(this,
                new HomeViewModel.Factory(
                        container.getAuthRepository(), container.getCommonRepository()))
                .get(HomeViewModel.class);
        binding.homeRetryButton.setOnClickListener(view -> viewModel.retry());
        binding.homeSwitchAccountButton.setOnClickListener(view -> viewModel.switchAccount());
        binding.logoutButton.setOnClickListener(view -> confirmLogout());
        binding.assetIdentifyCard.setOnClickListener(
                view -> openOperation(AssetIdentifyActivity.class));
        binding.tagCreateCard.setOnClickListener(
                view -> openOperation(RfidTagBatchActivity.class));
        binding.rfidBindCard.setOnClickListener(
                view -> openOperation(RfidBindActivity.class));
        binding.rfidUnbindCard.setOnClickListener(
                view -> openOperation(RfidUnbindActivity.class));
        binding.inboundCard.setOnClickListener(view -> openInbound());
        binding.receiveCard.setOnClickListener(view -> openReceive());
        binding.borrowCard.setOnClickListener(view -> openBorrow());
        binding.repairCard.setOnClickListener(view -> openRepair());
        binding.inventoryCard.setOnClickListener(view -> openInventoryTasks(false));
        viewModel.getUiState().observe(this, this::render);
        if (sessionManager.getState() != SessionManager.State.VALID) {
            navigateToLogin();
            return;
        }
        viewModel.initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionManager.addListener(sessionListener);
        if (sessionManager.getState() != SessionManager.State.VALID) {
            navigateToLogin();
        }
    }

    @Override
    protected void onStop() {
        sessionManager.removeListener(sessionListener);
        super.onStop();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.logout_cancel, null)
                .setPositiveButton(R.string.logout_confirm,
                        (dialog, which) -> viewModel.logout())
                .show();
    }

    private void render(HomeUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean loading = state.getMode() == HomeUiState.Mode.LOADING;
        boolean error = state.getMode() == HomeUiState.Mode.ERROR;
        boolean content = state.getMode() == HomeUiState.Mode.CONTENT
                || state.getMode() == HomeUiState.Mode.LOGGING_OUT;
        boolean loggingOut = state.getMode() == HomeUiState.Mode.LOGGING_OUT;

        setVisible(binding.homeLoadingContainer, loading);
        setVisible(binding.homeErrorContainer, error);
        setVisible(binding.homeContentContainer, content);
        if (error) {
            binding.homeErrorText.setText(resolveError(state));
        }
        if (!content) {
            return;
        }

        String loginName = displayValue(state.getLoginName());
        String userName = hasText(state.getUserName()) ? state.getUserName() : loginName;
        binding.homeUserText.setText(
                getString(R.string.home_user_format, userName, loginName));
        binding.homeDeptText.setText(
                getString(R.string.home_dept_format, displayValue(state.getDeptName())));
        binding.homeServerTimeText.setText(getString(
                R.string.home_server_time_format, displayValue(state.getServerTime())));

        setVisible(binding.tagCreateCard, state.isShowTagCreate());
        setVisible(binding.rfidBindCard, state.isShowRfidBind());
        setVisible(binding.rfidUnbindCard, state.isShowRfidUnbind());
        setVisible(binding.inboundCard, state.isShowInbound());
        setVisible(binding.receiveCard, state.isShowReceive());
        setVisible(binding.borrowCard, state.isShowBorrow());
        setVisible(binding.repairCard, state.isShowRepair());
        setVisible(binding.inventoryCard, state.isShowInventory());
        binding.inventoryCard.setAlpha(state.isShowInventory() ? 1.0f : 0.72f);
        binding.inventoryCard.setEnabled(state.isShowInventory());
        setVisible(binding.logoutProgress, loggingOut);
        binding.logoutButton.setEnabled(!loggingOut);
    }

    private String resolveError(HomeUiState state) {
        if (state.getErrorTextResId() != 0) {
            return getString(state.getErrorTextResId());
        }
        return displayValue(state.getErrorMessage());
    }

    private String displayValue(String value) {
        return hasText(value) ? value.trim() : getString(R.string.home_unknown_value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void navigateToLogin() {
        if (navigatingToLogin || isFinishing()) {
            return;
        }
        navigatingToLogin = true;
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openOperation(Class<? extends AppCompatActivity> activityClass) {
        if (sessionManager.getState() == SessionManager.State.VALID) {
            startActivity(new Intent(this, activityClass));
        }
    }

    private void openInventoryTasks(boolean ignored) {
        if (sessionManager.getState() != SessionManager.State.VALID) {
            return;
        }
        Intent intent = new Intent(this, InventoryTaskListActivity.class);
        HomeUiState state = viewModel.getUiState().getValue();
        intent.putExtra(InventoryTaskListActivity.EXTRA_CAN_SUBMIT,
                state != null && state.isShowInventorySubmit());
        startActivity(intent);
    }

    private void openInbound() {
        if (sessionManager.getState() != SessionManager.State.VALID) {
            return;
        }
        Intent intent = new Intent(this, InboundActivity.class);
        HomeUiState state = viewModel.getUiState().getValue();
        intent.putExtra(InboundActivity.EXTRA_CAN_CONFIRM,
                state != null && state.isShowInboundConfirm());
        startActivity(intent);
    }

    private void openReceive() {
        if (sessionManager.getState() != SessionManager.State.VALID) {
            return;
        }
        Intent intent = new Intent(this, ReceiveActivity.class);
        HomeUiState state = viewModel.getUiState().getValue();
        intent.putExtra(ReceiveActivity.EXTRA_CAN_SUBMIT,
                state != null && state.isShowReceiveSubmit());
        startActivity(intent);
    }

    private void openBorrow() {
        if (sessionManager.getState() != SessionManager.State.VALID) {
            return;
        }
        Intent intent = new Intent(this, BorrowReturnActivity.class);
        HomeUiState state = viewModel.getUiState().getValue();
        intent.putExtra(BorrowReturnActivity.EXTRA_CAN_ISSUE_SCAN,
                state != null && state.isShowBorrowIssue());
        intent.putExtra(BorrowReturnActivity.EXTRA_CAN_ISSUE_SUBMIT,
                state != null && state.isShowBorrowIssueSubmit());
        intent.putExtra(BorrowReturnActivity.EXTRA_CAN_RETURN_SCAN,
                state != null && state.isShowBorrowReturn());
        intent.putExtra(BorrowReturnActivity.EXTRA_CAN_RETURN_SUBMIT,
                state != null && state.isShowBorrowReturnSubmit());
        startActivity(intent);
    }

    private void openRepair() {
        if (sessionManager.getState() != SessionManager.State.VALID) {
            return;
        }
        Intent intent = new Intent(this, RepairWorkbenchActivity.class);
        HomeUiState state = viewModel.getUiState().getValue();
        intent.putExtra(RepairWorkbenchActivity.EXTRA_CAN_LIST,
                state != null && state.isShowRepairList());
        intent.putExtra(RepairWorkbenchActivity.EXTRA_CAN_SUBMIT,
                state != null && state.isShowRepairSubmit());
        intent.putExtra(RepairWorkbenchActivity.EXTRA_CAN_START,
                state != null && state.isShowRepairStart());
        intent.putExtra(RepairWorkbenchActivity.EXTRA_CAN_FINISH,
                state != null && state.isShowRepairFinish());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
