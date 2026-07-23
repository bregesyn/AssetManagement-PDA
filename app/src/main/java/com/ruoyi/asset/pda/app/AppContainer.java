package com.ruoyi.asset.pda.app;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.core.uhf.UhfDeviceManager;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.repository.ApiCallExecutor;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.data.repository.AuthRepository;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.DefaultAssetRepository;
import com.ruoyi.asset.pda.data.repository.DefaultAuthRepository;
import com.ruoyi.asset.pda.data.repository.DefaultCommonRepository;
import com.ruoyi.asset.pda.data.repository.DefaultInventoryRepository;
import com.ruoyi.asset.pda.data.repository.DefaultRfidRepository;
import com.ruoyi.asset.pda.data.repository.InventoryRepository;
import com.ruoyi.asset.pda.data.repository.RfidRepository;

/**
 * 一期使用显式依赖装配，避免为当前规模引入 Hilt 和隐式生命周期。
 */
public final class AppContainer {
    private final SessionCookieJar sessionCookieJar;
    private final SessionManager sessionManager;
    private final ApiClient apiClient;
    private final UhfScanner uhfScanner;
    private final AuthRepository authRepository;
    private final CommonRepository commonRepository;
    private final AssetRepository assetRepository;
    private final RfidRepository rfidRepository;
    private final InventoryRepository inventoryRepository;

    public AppContainer(Context context) {
        Context applicationContext = context.getApplicationContext();
        sessionCookieJar = new SessionCookieJar(applicationContext);
        sessionManager = new SessionManager(sessionCookieJar,
                ContextCompat.getMainExecutor(applicationContext));
        apiClient = new ApiClient(BuildConfig.BASE_URL, sessionCookieJar, sessionManager);
        // 未标定参数会在真正开始扫描时明确失败，Application 启动不会触碰硬件。
        uhfScanner = new UhfDeviceManager(BuildConfig.UHF_OUTPUT_POWER, BuildConfig.UHF_WORK_AREA);
        PdaApiService pdaApiService = apiClient.create(PdaApiService.class);
        ApiCallExecutor callExecutor = new ApiCallExecutor(apiClient.getErrorMapper());
        authRepository = new DefaultAuthRepository(
                pdaApiService, callExecutor, sessionCookieJar, sessionManager);
        commonRepository = new DefaultCommonRepository(pdaApiService, callExecutor);
        assetRepository = new DefaultAssetRepository(pdaApiService, callExecutor);
        rfidRepository = new DefaultRfidRepository(pdaApiService, callExecutor);
        inventoryRepository = new DefaultInventoryRepository(pdaApiService, callExecutor);
    }

    AppContainer(SessionCookieJar sessionCookieJar, SessionManager sessionManager,
            ApiClient apiClient, UhfScanner uhfScanner) {
        this.sessionCookieJar = requireNonNull(sessionCookieJar, "SessionCookieJar");
        this.sessionManager = requireNonNull(sessionManager, "SessionManager");
        this.apiClient = requireNonNull(apiClient, "ApiClient");
        this.uhfScanner = requireNonNull(uhfScanner, "UhfScanner");
        PdaApiService pdaApiService = apiClient.create(PdaApiService.class);
        ApiCallExecutor callExecutor = new ApiCallExecutor(apiClient.getErrorMapper());
        authRepository = new DefaultAuthRepository(
                pdaApiService, callExecutor, sessionCookieJar, sessionManager);
        commonRepository = new DefaultCommonRepository(pdaApiService, callExecutor);
        assetRepository = new DefaultAssetRepository(pdaApiService, callExecutor);
        rfidRepository = new DefaultRfidRepository(pdaApiService, callExecutor);
        inventoryRepository = new DefaultInventoryRepository(pdaApiService, callExecutor);
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

    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    public CommonRepository getCommonRepository() {
        return commonRepository;
    }

    public AssetRepository getAssetRepository() {
        return assetRepository;
    }

    public RfidRepository getRfidRepository() {
        return rfidRepository;
    }

    public InventoryRepository getInventoryRepository() {
        return inventoryRepository;
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
