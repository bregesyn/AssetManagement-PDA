package com.ruoyi.asset.pda.app;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.core.uhf.UhfDeviceManager;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;

/**
 * 一期使用显式依赖装配，避免为当前规模引入 Hilt 和隐式生命周期。
 */
public final class AppContainer {
    private final SessionCookieJar sessionCookieJar;
    private final SessionManager sessionManager;
    private final ApiClient apiClient;
    private final UhfScanner uhfScanner;

    public AppContainer(Context context) {
        Context applicationContext = context.getApplicationContext();
        sessionCookieJar = new SessionCookieJar(applicationContext);
        sessionManager = new SessionManager(sessionCookieJar,
                ContextCompat.getMainExecutor(applicationContext));
        apiClient = new ApiClient(BuildConfig.BASE_URL, sessionCookieJar, sessionManager);
        // 未标定参数会在真正开始扫描时明确失败，Application 启动不会触碰硬件。
        uhfScanner = new UhfDeviceManager(BuildConfig.UHF_OUTPUT_POWER, BuildConfig.UHF_WORK_AREA);
    }

    AppContainer(SessionCookieJar sessionCookieJar, SessionManager sessionManager,
            ApiClient apiClient, UhfScanner uhfScanner) {
        this.sessionCookieJar = requireNonNull(sessionCookieJar, "SessionCookieJar");
        this.sessionManager = requireNonNull(sessionManager, "SessionManager");
        this.apiClient = requireNonNull(apiClient, "ApiClient");
        this.uhfScanner = requireNonNull(uhfScanner, "UhfScanner");
    }

    public SessionCookieJar getSessionCookieJar() {
        return sessionCookieJar;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public UhfScanner getUhfScanner() {
        return uhfScanner;
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
