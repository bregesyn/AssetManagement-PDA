package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 认证 Repository 同时维护服务端 Cookie 与本地 Session 状态的一致性。
 */
public final class DefaultAuthRepository implements AuthRepository {
    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;
    private final SessionCookieJar cookieJar;
    private final SessionManager sessionManager;

    public DefaultAuthRepository(PdaApiService apiService, ApiCallExecutor callExecutor,
            SessionCookieJar cookieJar, SessionManager sessionManager) {
        if (apiService == null || callExecutor == null || cookieJar == null
                || sessionManager == null) {
            throw new IllegalArgumentException("认证 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
        this.cookieJar = cookieJar;
        this.sessionManager = sessionManager;
    }

    @Override
    public SessionManager.State getSessionState() {
        return sessionManager.getState();
    }

    @Override
    public RequestHandle login(PdaLoginRequest request,
            RepositoryCallback<PdaUserDto> callback) {
        if (request == null || callback == null) {
            throw new IllegalArgumentException("登录请求和回调不能为空");
        }
        // 新登录必须从干净 Cookie 开始，避免把旧账号 Session 附带到匿名登录接口。
        sessionManager.invalidate();
        return callExecutor.execute(apiService.login(request), true,
                new RepositoryCallback<PdaUserDto>() {
                    @Override
                    public void onSuccess(PdaUserDto user) {
                        if (!isValidUser(user) || cookieJar.isEmpty()) {
                            sessionManager.invalidate();
                            callback.onError(callExecutor.protocolError());
                            return;
                        }
                        try {
                            sessionManager.markValid();
                            callback.onSuccess(user);
                        } catch (IllegalStateException exception) {
                            sessionManager.invalidate();
                            callback.onError(callExecutor.protocolError());
                        }
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        // 登录失败时服务端可能仍创建匿名 Session，本地不能保留它。
                        sessionManager.invalidate();
                        callback.onError(error);
                    }
                });
    }

    @Override
    public RequestHandle profile(RepositoryCallback<PdaUserDto> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Profile 回调不能为空");
        }
        return callExecutor.execute(apiService.profile(), true,
                new RepositoryCallback<PdaUserDto>() {
                    @Override
                    public void onSuccess(PdaUserDto user) {
                        if (!isValidUser(user) || cookieJar.isEmpty()) {
                            sessionManager.invalidate();
                            callback.onError(callExecutor.protocolError());
                            return;
                        }
                        try {
                            sessionManager.markValid();
                            callback.onSuccess(user);
                        } catch (IllegalStateException exception) {
                            sessionManager.invalidate();
                            callback.onError(callExecutor.protocolError());
                        }
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        callback.onError(error);
                    }
                });
    }

    @Override
    public RequestHandle logout(RepositoryCallback<Void> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("退出回调不能为空");
        }
        AtomicBoolean cleared = new AtomicBoolean();
        Runnable clearOnce = () -> {
            if (cleared.compareAndSet(false, true)) {
                sessionManager.invalidate();
            }
        };
        RequestHandle delegate = callExecutor.execute(apiService.logout(), false,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        clearOnce.run();
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        clearOnce.run();
                        callback.onError(error);
                    }
                });
        return () -> {
            delegate.cancel();
            // 用户已明确退出时，即使页面销毁导致请求取消，本地凭据也必须清除。
            clearOnce.run();
        };
    }

    @Override
    public void clearLocalSession() {
        sessionManager.invalidate();
    }

    private boolean isValidUser(PdaUserDto user) {
        return user != null && user.getUserId() != null
                && user.getLoginName() != null && !user.getLoginName().trim().isEmpty();
    }
}
