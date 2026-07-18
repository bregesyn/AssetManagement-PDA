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
import com.ruoyi.asset.pda.feature.login.LoginActivity;

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
        setVisible(binding.inventoryCard, state.isShowInventory());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
