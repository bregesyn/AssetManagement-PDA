package com.ruoyi.asset.pda.core.network;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionCookieJarTest {
    private static final HttpUrl PROFILE_URL = HttpUrl.get(
            "http://192.168.0.105:8030/asset/pda/profile");

    @Test
    public void sessionCookieCanBeRestoredAfterStoreRecreation() {
        InMemoryCookieStorage storage = new InMemoryCookieStorage();
        SessionCookieJar firstJar = new SessionCookieJar(storage);
        Cookie sessionCookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("session-value")
                .hostOnlyDomain("192.168.0.105")
                .path("/")
                .httpOnly()
                .build();

        firstJar.saveFromResponse(PROFILE_URL, Collections.singletonList(sessionCookie));
        SessionCookieJar restoredJar = new SessionCookieJar(storage);
        List<Cookie> restoredCookies = restoredJar.loadForRequest(PROFILE_URL);

        assertEquals(1, restoredCookies.size());
        Cookie restored = restoredCookies.get(0);
        assertEquals("JSESSIONID", restored.name());
        assertEquals("session-value", restored.value());
        assertEquals("192.168.0.105", restored.domain());
        assertEquals("/", restored.path());
        assertTrue(restored.hostOnly());
        assertTrue(restored.httpOnly());
        assertFalse(restored.persistent());
    }

    @Test
    public void onlyCookiesMatchingHostAndPathAreLoaded() {
        InMemoryCookieStorage storage = new InMemoryCookieStorage();
        SessionCookieJar cookieJar = new SessionCookieJar(storage);
        Cookie apiCookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("session-value")
                .hostOnlyDomain("192.168.0.105")
                .path("/asset/pda")
                .build();
        cookieJar.saveFromResponse(PROFILE_URL, Collections.singletonList(apiCookie));

        assertEquals(1, cookieJar.loadForRequest(PROFILE_URL).size());
        assertTrue(cookieJar.loadForRequest(HttpUrl.get(
                "http://192.168.0.105:8030/system/main")).isEmpty());
        assertTrue(cookieJar.loadForRequest(HttpUrl.get(
                "http://192.168.0.106:8030/asset/pda/profile")).isEmpty());
    }

    @Test
    public void expiredCookieAndRepeatedClearLeaveJarEmpty() {
        InMemoryCookieStorage storage = new InMemoryCookieStorage();
        SessionCookieJar cookieJar = new SessionCookieJar(storage);
        Cookie expiredCookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("expired")
                .hostOnlyDomain("192.168.0.105")
                .path("/")
                .expiresAt(System.currentTimeMillis() - 1L)
                .build();

        cookieJar.saveFromResponse(PROFILE_URL, Collections.singletonList(expiredCookie));
        cookieJar.clear();
        cookieJar.clear();

        assertTrue(cookieJar.isEmpty());
        assertTrue(cookieJar.loadForRequest(PROFILE_URL).isEmpty());
    }

    @Test
    public void corruptStoredPayloadIsDiscarded() {
        InMemoryCookieStorage storage = new InMemoryCookieStorage();
        storage.setRawValue("not-json");

        SessionCookieJar cookieJar = new SessionCookieJar(storage);

        assertTrue(cookieJar.isEmpty());
    }
}
