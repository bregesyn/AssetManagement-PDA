package com.ruoyi.asset.pda.feature.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.core.ui.Event;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.AuthRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

public final class LoginViewModel extends ViewModel {
    public enum Destination {
        HOME
    }

    private final AuthRepository authRepository;
    private final MutableLiveData<LoginUiState> uiState =
            new MutableLiveData<>(LoginUiState.form());
    private final MutableLiveData<Event<Destination>> navigation = new MutableLiveData<>();
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int operationVersion;
    private boolean initialized;

    public LoginViewModel(AuthRepository authRepository) {
        if (authRepository == null) {
            throw new IllegalArgumentException("AuthRepository 不能为空");
        }
        this.authRepository = authRepository;
    }

    public LiveData<LoginUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<Destination>> getNavigation() {
        return navigation;
    }

    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        SessionManager.State sessionState = authRepository.getSessionState();
        if (sessionState == SessionManager.State.VALID) {
            navigateHome();
        } else if (sessionState == SessionManager.State.PENDING_VALIDATION) {
            recoverSession();
        } else {
            uiState.setValue(LoginUiState.form());
        }
    }

    public void login(String username, String password) {
        LoginUiState currentState = uiState.getValue();
        if (isBusy(currentState)) {
            return;
        }
        String checkedUsername = username == null ? "" : username.trim();
        if (checkedUsername.isEmpty()) {
            uiState.setValue(LoginUiState.formError(R.string.login_username_required));
            return;
        }
        if (password == null || password.isEmpty()) {
            uiState.setValue(LoginUiState.formError(R.string.login_password_required));
            return;
        }

        cancelCurrentRequest();
        int requestVersion = ++operationVersion;
        uiState.setValue(LoginUiState.submitting());
        RequestHandle request = authRepository.login(
                new PdaLoginRequest(checkedUsername, password, null),
                new RepositoryCallback<PdaUserDto>() {
                    @Override
                    public void onSuccess(PdaUserDto data) {
                        if (requestVersion == operationVersion) {
                            navigateHome();
                        }
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (requestVersion == operationVersion) {
                            uiState.setValue(LoginUiState.formError(error.getMessage()));
                        }
                    }
                });
        if (requestVersion == operationVersion) {
            currentRequest = request;
        }
    }

    public void retryRecovery() {
        if (!isBusy(uiState.getValue())) {
            recoverSession();
        }
    }

    public void switchAccount() {
        cancelCurrentRequest();
        operationVersion++;
        authRepository.clearLocalSession();
        uiState.setValue(LoginUiState.form());
    }

    private void recoverSession() {
        cancelCurrentRequest();
        int requestVersion = ++operationVersion;
        uiState.setValue(LoginUiState.recovering());
        RequestHandle request = authRepository.profile(new RepositoryCallback<PdaUserDto>() {
            @Override
            public void onSuccess(PdaUserDto data) {
                if (requestVersion == operationVersion) {
                    navigateHome();
                }
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                if (requestVersion != operationVersion) {
                    return;
                }
                if (error.getKind() == ApiErrorMapper.Kind.SESSION_EXPIRED
                        || authRepository.getSessionState() == SessionManager.State.INVALID) {
                    uiState.setValue(LoginUiState.formError(error.getMessage()));
                } else {
                    // 临时网络失败不能把仍可能有效的加密 Cookie 当作失效凭据删除。
                    uiState.setValue(LoginUiState.recoveryError(error.getMessage()));
                }
            }
        });
        if (requestVersion == operationVersion) {
            currentRequest = request;
        }
    }

    private void navigateHome() {
        navigation.setValue(new Event<>(Destination.HOME));
    }

    private boolean isBusy(LoginUiState state) {
        return state != null && (state.getMode() == LoginUiState.Mode.SUBMITTING
                || state.getMode() == LoginUiState.Mode.RECOVERING);
    }

    private void cancelCurrentRequest() {
        currentRequest.cancel();
        currentRequest = RequestHandle.NONE;
    }

    @Override
    protected void onCleared() {
        cancelCurrentRequest();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final AuthRepository authRepository;

        public Factory(AuthRepository authRepository) {
            this.authRepository = authRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(LoginViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new LoginViewModel(authRepository);
        }
    }
}
