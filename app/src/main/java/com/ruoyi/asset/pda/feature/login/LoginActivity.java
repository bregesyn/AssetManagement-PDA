package com.ruoyi.asset.pda.feature.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.Event;
import com.ruoyi.asset.pda.databinding.ActivityLoginBinding;
import com.ruoyi.asset.pda.feature.home.HomeActivity;

public final class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this,
                new LoginViewModel.Factory(container.getAuthRepository()))
                .get(LoginViewModel.class);
        bindActions();
        observeState();
        viewModel.initialize();
    }

    private void bindActions() {
        binding.loginButton.setOnClickListener(view -> submitLogin());
        binding.passwordEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin();
                return true;
            }
            return false;
        });
        binding.retryRecoveryButton.setOnClickListener(view -> viewModel.retryRecovery());
        binding.switchAccountButton.setOnClickListener(view -> {
            binding.usernameEditText.setText(null);
            binding.passwordEditText.setText(null);
            viewModel.switchAccount();
        });
    }

    private void observeState() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getNavigation().observe(this, event -> {
            Event<LoginViewModel.Destination> navigationEvent = event;
            LoginViewModel.Destination destination = navigationEvent == null
                    ? null : navigationEvent.getContentIfNotHandled();
            if (destination == LoginViewModel.Destination.HOME) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            }
        });
    }

    private void submitLogin() {
        hideKeyboard();
        viewModel.login(textOf(binding.usernameEditText), textOf(binding.passwordEditText));
    }

    private void render(LoginUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean showForm = state.getMode() == LoginUiState.Mode.FORM
                || state.getMode() == LoginUiState.Mode.SUBMITTING;
        boolean submitting = state.getMode() == LoginUiState.Mode.SUBMITTING;
        boolean showRecovery = state.getMode() == LoginUiState.Mode.RECOVERING
                || state.getMode() == LoginUiState.Mode.RECOVERY_ERROR;
        boolean recoveryError = state.getMode() == LoginUiState.Mode.RECOVERY_ERROR;

        setVisible(binding.loginFormContainer, showForm);
        setVisible(binding.recoveryContainer, showRecovery);
        setVisible(binding.loginProgress, submitting);
        binding.usernameEditText.setEnabled(!submitting);
        binding.passwordEditText.setEnabled(!submitting);
        binding.loginButton.setEnabled(!submitting);

        String errorMessage = resolveError(state);
        binding.loginErrorText.setText(errorMessage);
        setVisible(binding.loginErrorText, showForm && hasText(errorMessage));

        setVisible(binding.recoveryProgress, showRecovery && !recoveryError);
        setVisible(binding.retryRecoveryButton, recoveryError);
        setVisible(binding.switchAccountButton, recoveryError);
        binding.recoveryMessageText.setText(recoveryError && hasText(state.getErrorMessage())
                ? state.getErrorMessage() : getString(R.string.login_recovering_message));
    }

    private String resolveError(LoginUiState state) {
        if (state.getErrorTextResId() != 0) {
            return getString(state.getErrorTextResId());
        }
        return state.getErrorMessage();
    }

    private void hideKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView == null) {
            return;
        }
        InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    private String textOf(android.widget.TextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString();
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
