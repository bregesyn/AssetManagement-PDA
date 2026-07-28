package com.ruoyi.asset.pda.feature.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.session.PdaPermissions;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.AuthRepository;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.Map;

public final class HomeViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final CommonRepository commonRepository;
    private final MutableLiveData<HomeUiState> uiState =
            new MutableLiveData<>(HomeUiState.loading());
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int operationVersion;
    private boolean initialized;

    public HomeViewModel(AuthRepository authRepository, CommonRepository commonRepository) {
        if (authRepository == null || commonRepository == null) {
            throw new IllegalArgumentException("首页 Repository 不能为空");
        }
        this.authRepository = authRepository;
        this.commonRepository = commonRepository;
    }

    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public void initialize() {
        if (initialized || authRepository.getSessionState() != SessionManager.State.VALID) {
            return;
        }
        initialized = true;
        loadBootstrap();
    }

    public void retry() {
        HomeUiState state = uiState.getValue();
        if (state != null && state.getMode() == HomeUiState.Mode.ERROR) {
            loadBootstrap();
        }
    }

    public void switchAccount() {
        HomeUiState state = uiState.getValue();
        if (state == null || state.getMode() != HomeUiState.Mode.ERROR) {
            return;
        }
        cancelCurrentRequest();
        operationVersion++;
        // 启动配置持续失败时必须保留离开旧账号的通道，不能要求用户清除应用数据。
        authRepository.clearLocalSession();
    }

    public void logout() {
        HomeUiState state = uiState.getValue();
        if (state == null || state.getMode() != HomeUiState.Mode.CONTENT) {
            return;
        }
        cancelCurrentRequest();
        int requestVersion = ++operationVersion;
        uiState.setValue(state.asLoggingOut());
        RequestHandle request = authRepository.logout(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Repository 已无条件清理 Session，Activity 的统一监听器负责导航。
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                // 服务端退出失败也不能恢复本地登录态。
            }
        });
        if (requestVersion == operationVersion) {
            currentRequest = request;
        }
    }

    private void loadBootstrap() {
        cancelCurrentRequest();
        int requestVersion = ++operationVersion;
        uiState.setValue(HomeUiState.loading());
        RequestHandle request = commonRepository.bootstrap(
                new RepositoryCallback<PdaBootstrapDto>() {
                    @Override
                    public void onSuccess(PdaBootstrapDto data) {
                        if (requestVersion != operationVersion) {
                            return;
                        }
                        PdaUserDto user = data == null ? null : data.getCurrentUser();
                        if (user == null || user.getUserId() == null
                                || !hasText(user.getLoginName())) {
                            uiState.setValue(HomeUiState.error(R.string.api_protocol_error));
                            return;
                        }
                        Map<String, Boolean> features = data.getFeatures();
                        uiState.setValue(HomeUiState.content(
                                user.getLoginName(),
                                user.getUserName(),
                                user.getDeptName(),
                                data.getServerTime(),
                                isEnabled(features, PdaPermissions.RFID_TAG_ADD),
                                isEnabled(features, PdaPermissions.RFID_BIND),
                                isEnabled(features, PdaPermissions.RFID_UNBIND),
                                isEnabled(features, PdaPermissions.INBOUND_SCAN),
                                isEnabled(features, PdaPermissions.INBOUND_CONFIRM),
                                isEnabled(features, PdaPermissions.RECEIVE_SCAN),
                                isEnabled(features, PdaPermissions.RECEIVE_CONFIRM),
                                isEnabled(features, PdaPermissions.INVENTORY_LIST),
                                isEnabled(features, PdaPermissions.INVENTORY_SUBMIT)));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (requestVersion == operationVersion) {
                            uiState.setValue(HomeUiState.error(error.getMessage()));
                        }
                    }
                });
        if (requestVersion == operationVersion) {
            currentRequest = request;
        }
    }

    private boolean isEnabled(Map<String, Boolean> features, String permission) {
        return features != null && Boolean.TRUE.equals(features.get(permission));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
        private final CommonRepository commonRepository;

        public Factory(AuthRepository authRepository, CommonRepository commonRepository) {
            this.authRepository = authRepository;
            this.commonRepository = commonRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(HomeViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new HomeViewModel(authRepository, commonRepository);
        }
    }
}
