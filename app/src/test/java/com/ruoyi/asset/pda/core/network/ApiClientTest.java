package com.ruoyi.asset.pda.core.network;

import com.ruoyi.asset.pda.core.session.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ApiClientTest {
    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void buildsOneClientWithConfiguredBaseUrlAndCookieJar() {
        SessionCookieJar cookieJar = new SessionCookieJar(new InMemoryCookieStorage());
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient client = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);

        assertEquals(server.url("/"), client.getRetrofit().baseUrl());
        assertSame(cookieJar, client.getOkHttpClient().cookieJar());
        assertSame(client.getOkHttpClient(), client.getRetrofit().callFactory());
    }

    @Test
    public void http401InvalidatesSessionOnceWithoutParsingBody() throws IOException {
        SessionCookieJar cookieJar = new SessionCookieJar(new InMemoryCookieStorage());
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        AtomicInteger invalidationCount = new AtomicInteger();
        sessionManager.addListener(invalidationCount::incrementAndGet);
        saveSessionCookie(cookieJar, server.url("/asset/pda/profile"), "session-value");
        sessionManager.markValid();
        ApiClient client = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":401,\"msg\":\"登录状态已失效，请重新登录\"}"));

        Request request = new Request.Builder().url(server.url("/asset/pda/profile")).build();
        try (Response response = client.getOkHttpClient().newCall(request).execute()) {
            assertEquals(401, response.code());
        }

        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertEquals(1, invalidationCount.get());
    }

    @Test
    public void delayed401FromOldSessionDoesNotInvalidateNewSession() throws Exception {
        CountDownLatch requestArrived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                requestArrived.countDown();
                releaseResponse.await(3L, TimeUnit.SECONDS);
                return new MockResponse().setResponseCode(401);
            }
        });
        HttpUrl requestUrl = server.url("/asset/pda/profile");
        SessionCookieJar cookieJar = new SessionCookieJar(new InMemoryCookieStorage());
        saveSessionCookie(cookieJar, requestUrl, "old-session");
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        sessionManager.markValid();
        ApiClient client = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        ExecutorService requestExecutor = Executors.newSingleThreadExecutor();

        try {
            Future<Integer> responseCode = requestExecutor.submit(() -> {
                Request request = new Request.Builder().url(requestUrl).build();
                try (Response response = client.getOkHttpClient().newCall(request).execute()) {
                    return response.code();
                }
            });
            assertTrue("旧会话请求未到达测试服务器", requestArrived.await(3L, TimeUnit.SECONDS));

            // 模拟旧请求在途时用户重新登录，新的 Session 不能被随后返回的旧 401 清除。
            saveSessionCookie(cookieJar, requestUrl, "new-session");
            sessionManager.markValid();
            releaseResponse.countDown();

            assertEquals(Integer.valueOf(401), responseCode.get(3L, TimeUnit.SECONDS));
            assertEquals(SessionManager.State.VALID, sessionManager.getState());
            List<Cookie> cookies = cookieJar.loadForRequest(requestUrl);
            assertEquals(1, cookies.size());
            assertEquals("new-session", cookies.get(0).value());
        } finally {
            releaseResponse.countDown();
            requestExecutor.shutdownNow();
        }
    }

    @Test
    public void repeatedValidationOfSameSessionDoesNotHideCurrent401() throws Exception {
        CountDownLatch requestArrived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                requestArrived.countDown();
                releaseResponse.await(3L, TimeUnit.SECONDS);
                return new MockResponse().setResponseCode(401);
            }
        });
        HttpUrl requestUrl = server.url("/asset/pda/profile");
        SessionCookieJar cookieJar = new SessionCookieJar(new InMemoryCookieStorage());
        saveSessionCookie(cookieJar, requestUrl, "same-session");
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        sessionManager.markValid();
        ApiClient client = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        ExecutorService requestExecutor = Executors.newSingleThreadExecutor();

        try {
            Future<Integer> responseCode = requestExecutor.submit(() -> {
                Request request = new Request.Builder().url(requestUrl).build();
                try (Response response = client.getOkHttpClient().newCall(request).execute()) {
                    return response.code();
                }
            });
            assertTrue("当前会话请求未到达测试服务器", requestArrived.await(3L, TimeUnit.SECONDS));

            // 同一 Cookie 的重复校验不能伪造新会话，否则真实 401 会被误当成旧响应。
            sessionManager.markValid();
            releaseResponse.countDown();

            assertEquals(Integer.valueOf(401), responseCode.get(3L, TimeUnit.SECONDS));
            assertEquals(SessionManager.State.INVALID, sessionManager.getState());
            assertTrue(cookieJar.isEmpty());
        } finally {
            releaseResponse.countDown();
            requestExecutor.shutdownNow();
        }
    }

    @Test
    public void rejectsMissingReleaseBaseUrl() {
        SessionCookieJar cookieJar = new SessionCookieJar(new InMemoryCookieStorage());
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);

        try {
            new ApiClient("", cookieJar, sessionManager);
            fail("空 BASE_URL 应被拒绝");
        } catch (IllegalStateException exception) {
            assertEquals("当前构建环境尚未配置 BASE_URL", exception.getMessage());
        }
    }

    private static void saveSessionCookie(SessionCookieJar cookieJar, HttpUrl url, String value) {
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value(value)
                .hostOnlyDomain(url.host())
                .path("/")
                .build();
        cookieJar.saveFromResponse(url, Collections.singletonList(cookie));
    }
}
