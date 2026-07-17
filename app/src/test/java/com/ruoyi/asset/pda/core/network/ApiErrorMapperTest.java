package com.ruoyi.asset.pda.core.network;

import com.ruoyi.asset.pda.core.session.SessionManager;
import com.google.gson.stream.MalformedJsonException;

import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;

public class ApiErrorMapperTest {
    private SessionCookieJar cookieJar;
    private SessionManager sessionManager;
    private ApiErrorMapper mapper;

    @Before
    public void setUp() {
        cookieJar = new SessionCookieJar(new InMemoryCookieStorage());
        sessionManager = new SessionManager(cookieJar, Runnable::run);
        mapper = new ApiErrorMapper(sessionManager);
    }

    @Test
    public void businessFailureKeepsBackendBusinessMessage() {
        ApiErrorMapper.ApiError error = mapper.mapResponse(
                new ApiResponse<>(500, "EPC 未绑定资产，请先完成 RFID 绑定", null));

        assertEquals(ApiErrorMapper.Kind.BUSINESS, error.getKind());
        assertEquals("EPC 未绑定资产，请先完成 RFID 绑定", error.getMessage());
    }

    @Test
    public void sessionFailureClearsOnceAndUsesStableMessage() {
        AtomicInteger invalidationCount = new AtomicInteger();
        saveSessionCookie();
        sessionManager.markValid();
        sessionManager.addListener(invalidationCount::incrementAndGet);

        ApiErrorMapper.ApiError first = mapper.mapResponse(new ApiResponse<>(401, "任意服务端文本", null));
        ApiErrorMapper.ApiError second = mapper.mapResponse(new ApiResponse<>(401, "任意服务端文本", null));

        assertEquals(ApiErrorMapper.Kind.SESSION_EXPIRED, first.getKind());
        assertEquals("登录状态已失效，请重新登录", first.getMessage());
        assertEquals(ApiErrorMapper.Kind.SESSION_EXPIRED, second.getKind());
        assertEquals(1, invalidationCount.get());
        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
    }

    @Test
    public void timeoutDoesNotExposeThrowableMessage() {
        ApiErrorMapper.ApiError error = mapper.mapThrowable(
                new SocketTimeoutException("192.168.0.105 internal detail"));

        assertEquals(ApiErrorMapper.Kind.TIMEOUT, error.getKind());
        assertEquals("请求超时，请检查网络后重试", error.getMessage());
    }

    @Test
    public void malformedJsonIsReportedAsProtocolFailure() {
        ApiErrorMapper.ApiError error = mapper.mapThrowable(
                new MalformedJsonException("unterminated object"));

        assertEquals(ApiErrorMapper.Kind.PROTOCOL, error.getKind());
        assertEquals("服务响应格式异常，请稍后重试", error.getMessage());
    }

    @Test
    public void truncatedJsonIsReportedAsProtocolFailure() {
        ApiErrorMapper.ApiError error = mapper.mapThrowable(
                new EOFException("response ended before JSON was complete"));

        assertEquals(ApiErrorMapper.Kind.PROTOCOL, error.getKind());
        assertEquals("服务响应格式异常，请稍后重试", error.getMessage());
    }

    private void saveSessionCookie() {
        HttpUrl url = HttpUrl.get("http://192.168.0.105:8030/asset/pda/profile");
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("session-value")
                .hostOnlyDomain(url.host())
                .path("/")
                .build();
        cookieJar.saveFromResponse(url, Collections.singletonList(cookie));
    }
}
