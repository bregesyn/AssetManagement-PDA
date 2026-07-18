package com.ruoyi.asset.pda.app;

import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class AppContainerTest {
    @Test
    public void returnsSameProcessScopedInstances() {
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient("http://127.0.0.1/", cookieJar, sessionManager);
        FakeUhfScanner scanner = new FakeUhfScanner();
        AppContainer container = new AppContainer(cookieJar, sessionManager, apiClient, scanner);

        assertSame(cookieJar, container.getSessionCookieJar());
        assertSame(cookieJar, container.getSessionCookieJar());
        assertSame(sessionManager, container.getSessionManager());
        assertSame(apiClient, container.getApiClient());
        assertSame(scanner, container.getUhfScanner());
        assertSame(container.getAuthRepository(), container.getAuthRepository());
        assertSame(container.getCommonRepository(), container.getCommonRepository());
    }
}
