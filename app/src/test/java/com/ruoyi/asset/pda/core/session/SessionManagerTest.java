package com.ruoyi.asset.pda.core.session;

import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SessionManagerTest {
    @Test
    public void restoredCookieStartsAsPendingValidation() {
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        HttpUrl url = HttpUrl.get("http://192.168.0.105:8030/asset/pda/profile");
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("session-value")
                .hostOnlyDomain("192.168.0.105")
                .path("/")
                .build();
        cookieJar.saveFromResponse(url, Collections.singletonList(cookie));

        SessionManager manager = new SessionManager(cookieJar, Runnable::run);

        assertEquals(SessionManager.State.PENDING_VALIDATION, manager.getState());
    }

    @Test
    public void repeatedInvalidationClearsCookieAndNotifiesOnce() {
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        saveSessionCookie(cookieJar, "session-value");
        SessionManager manager = new SessionManager(cookieJar, Runnable::run);
        AtomicInteger invalidationCount = new AtomicInteger();
        manager.addListener(invalidationCount::incrementAndGet);
        manager.markValid();

        manager.invalidate();
        manager.invalidate();

        assertEquals(SessionManager.State.INVALID, manager.getState());
        assertEquals(1, invalidationCount.get());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void invalidationListenerRunsOnConfiguredExecutor() {
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        saveSessionCookie(cookieJar, "session-value");
        Queue<Runnable> callbackQueue = new ArrayDeque<>();
        SessionManager manager = new SessionManager(cookieJar, callbackQueue::add);
        AtomicInteger invalidationCount = new AtomicInteger();
        manager.addListener(invalidationCount::incrementAndGet);
        manager.markValid();

        manager.invalidate();

        assertEquals(SessionManager.State.INVALID, manager.getState());
        assertTrue(cookieJar.isEmpty());
        assertEquals(0, invalidationCount.get());
        assertEquals(1, callbackQueue.size());

        callbackQueue.remove().run();

        assertEquals(1, invalidationCount.get());
    }

    @Test
    public void emptyCookieJarCannotBeMarkedValid() {
        SessionManager manager = new SessionManager(
                TestNetworkFactory.newCookieJar(), Runnable::run);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, manager::markValid);

        assertEquals("没有可用的 Session Cookie", exception.getMessage());
        assertEquals(SessionManager.State.INVALID, manager.getState());
    }

    private static void saveSessionCookie(SessionCookieJar cookieJar, String value) {
        HttpUrl url = HttpUrl.get("http://192.168.0.105:8030/asset/pda/profile");
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value(value)
                .hostOnlyDomain(url.host())
                .path("/")
                .build();
        cookieJar.saveFromResponse(url, Collections.singletonList(cookie));
    }
}
