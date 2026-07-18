package com.ruoyi.asset.pda.data.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultRepositoriesTest {
    private MockWebServer server;
    private SessionCookieJar cookieJar;
    private SessionManager sessionManager;
    private AuthRepository authRepository;
    private CommonRepository commonRepository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        cookieJar = TestNetworkFactory.newCookieJar();
        sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        PdaApiService apiService = apiClient.create(PdaApiService.class);
        ApiCallExecutor callExecutor = new ApiCallExecutor(apiClient.getErrorMapper());
        authRepository = new DefaultAuthRepository(
                apiService, callExecutor, cookieJar, sessionManager);
        commonRepository = new DefaultCommonRepository(apiService, callExecutor);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void loginUsesExactJsonAndRequiresServerCookie() throws Exception {
        server.enqueue(jsonResponse("{\"code\":0,\"msg\":\"登录成功\",\"data\":{"
                + "\"userId\":102,\"loginName\":\"ceshi\",\"userName\":\"测试用户\","
                + "\"deptId\":100,\"deptName\":\"资产管理部\",\"permissions\":[]}}")
                .addHeader("Set-Cookie", "JSESSIONID=session-value; Path=/; HttpOnly"));

        Awaited<PdaUserDto> result = await(callback -> authRepository.login(
                new PdaLoginRequest("ceshi", "test-password", null), callback));

        assertNull(result.error);
        assertEquals(Long.valueOf(102L), result.data.getUserId());
        assertEquals(SessionManager.State.VALID, sessionManager.getState());
        assertFalse(cookieJar.isEmpty());

        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/asset/pda/login", request.getPath());
        JsonObject requestBody = JsonParser.parseString(request.getBody().readUtf8())
                .getAsJsonObject();
        assertEquals("ceshi", requestBody.get("username").getAsString());
        assertEquals("test-password", requestBody.get("password").getAsString());
        assertFalse(requestBody.has("deviceNo"));
    }

    @Test
    public void successfulLoginWithoutCookieIsProtocolFailure() throws Exception {
        server.enqueue(jsonResponse("{\"code\":0,\"msg\":\"登录成功\",\"data\":{"
                + "\"userId\":102,\"loginName\":\"ceshi\",\"permissions\":[]}}"));

        Awaited<PdaUserDto> result = await(callback -> authRepository.login(
                new PdaLoginRequest("ceshi", "test-password", null), callback));

        assertEquals(ApiErrorMapper.Kind.PROTOCOL, result.error.getKind());
        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void loginBusinessFailureClearsAnonymousServerCookie() throws Exception {
        server.enqueue(jsonResponse("{\"code\":500,\"msg\":\"用户名或密码错误\"}")
                .addHeader("Set-Cookie", "JSESSIONID=anonymous-session; Path=/; HttpOnly"));

        Awaited<PdaUserDto> result = await(callback -> authRepository.login(
                new PdaLoginRequest("ceshi", "test-password", null), callback));

        assertEquals(ApiErrorMapper.Kind.BUSINESS, result.error.getKind());
        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void profile401ClearsCurrentSession() throws Exception {
        saveSessionCookie();
        sessionManager.markValid();
        server.enqueue(jsonResponse(
                "{\"code\":401,\"msg\":\"登录状态已失效，请重新登录\"}")
                .setResponseCode(401));

        Awaited<PdaUserDto> result = await(authRepository::profile);

        assertEquals(ApiErrorMapper.Kind.SESSION_EXPIRED, result.error.getKind());
        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void profileNetworkFailurePreservesPotentiallyValidCookie() throws Exception {
        saveSessionCookie();
        sessionManager.markValid();
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        Awaited<PdaUserDto> result = await(authRepository::profile);

        assertEquals(ApiErrorMapper.Kind.NETWORK, result.error.getKind());
        assertEquals(SessionManager.State.VALID, sessionManager.getState());
        assertFalse(cookieJar.isEmpty());
    }

    @Test
    public void delayedBusiness401FromOldSessionDoesNotClearNewSession() throws Exception {
        CountDownLatch requestArrived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        CountDownLatch callbackCompleted = new CountDownLatch(1);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                requestArrived.countDown();
                releaseResponse.await(3L, TimeUnit.SECONDS);
                return jsonResponse("{\"code\":401,\"msg\":\"登录状态已失效\"}");
            }
        });
        saveSessionCookie("old-session");
        sessionManager.markValid();
        Awaited<PdaBootstrapDto> result = new Awaited<>();

        commonRepository.bootstrap(new RepositoryCallback<PdaBootstrapDto>() {
            @Override
            public void onSuccess(PdaBootstrapDto data) {
                result.data = data;
                callbackCompleted.countDown();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                result.error = error;
                callbackCompleted.countDown();
            }
        });
        assertTrue("旧会话请求未到达测试服务器",
                requestArrived.await(3L, TimeUnit.SECONDS));

        // 请求在途时登录态已经切换，旧响应只能报告自身失败，不能清理新 Cookie。
        saveSessionCookie("new-session");
        sessionManager.markValid();
        releaseResponse.countDown();

        assertTrue("旧会话响应未在超时内完成",
                callbackCompleted.await(3L, TimeUnit.SECONDS));
        assertEquals(ApiErrorMapper.Kind.SESSION_EXPIRED, result.error.getKind());
        assertEquals(SessionManager.State.VALID, sessionManager.getState());
        List<Cookie> cookies = cookieJar.loadForRequest(server.url("/asset/pda/profile"));
        assertEquals(1, cookies.size());
        assertEquals("new-session", cookies.get(0).value());
    }

    @Test
    public void logoutBusinessFailureStillClearsLocalSession() throws Exception {
        saveSessionCookie();
        sessionManager.markValid();
        server.enqueue(jsonResponse("{\"code\":500,\"msg\":\"退出服务暂时不可用\"}"));

        Awaited<Void> result = await(authRepository::logout);

        assertEquals(ApiErrorMapper.Kind.BUSINESS, result.error.getKind());
        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void logoutSuccessClearsLocalSession() throws Exception {
        saveSessionCookie();
        sessionManager.markValid();
        server.enqueue(jsonResponse("{\"code\":0,\"msg\":\"退出成功\"}"));

        Awaited<Void> result = await(authRepository::logout);

        assertNull(result.error);
        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void cancellingLogoutStillClearsLocalSession() throws Exception {
        CountDownLatch requestArrived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                requestArrived.countDown();
                releaseResponse.await(3L, TimeUnit.SECONDS);
                return jsonResponse("{\"code\":0,\"msg\":\"退出成功\"}");
            }
        });
        saveSessionCookie();
        sessionManager.markValid();

        RequestHandle handle = authRepository.logout(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
            }
        });
        assertTrue("退出请求未到达测试服务器", requestArrived.await(3L, TimeUnit.SECONDS));

        handle.cancel();
        releaseResponse.countDown();

        assertEquals(SessionManager.State.INVALID, sessionManager.getState());
        assertTrue(cookieJar.isEmpty());
    }

    @Test
    public void commonRepositoryUsesExactPathsAndParsesRealShapes() throws Exception {
        server.enqueue(jsonResponse("{\"code\":0,\"msg\":\"操作成功\",\"data\":{"
                + "\"serverTime\":\"2026-07-17 18:30:00\","
                + "\"currentUser\":{\"userId\":102,\"loginName\":\"ceshi\","
                + "\"permissions\":[\"asset:pda:inventory:list\"]},"
                + "\"dicts\":{},\"features\":{\"asset:pda:inventory:list\":true}}}"));
        Awaited<PdaBootstrapDto> bootstrap = await(commonRepository::bootstrap);
        assertNull(bootstrap.error);
        assertEquals("ceshi", bootstrap.data.getCurrentUser().getLoginName());
        assertEquals("/asset/pda/config/bootstrap", takePath());

        server.enqueue(jsonResponse("{\"code\":0,\"msg\":\"操作成功\",\"data\":[{"
                + "\"dictType\":\"ams_asset_status\",\"label\":\"在库\","
                + "\"value\":\"IN_STOCK\",\"listClass\":\"success\","
                + "\"cssClass\":null,\"isDefault\":\"N\"}]}"));
        Awaited<List<PdaDictItemDto>> dict = await(callback ->
                commonRepository.dict("ams_asset_status", callback));
        assertEquals("IN_STOCK", dict.data.get(0).getValue());
        assertEquals("/asset/pda/dict/ams_asset_status", takePath());

        server.enqueue(masterListResponse(1L, "WH001", "一号仓", null, null));
        Awaited<List<PdaMasterDataDto>> warehouses = await(commonRepository::warehouses);
        assertEquals("一号仓", warehouses.data.get(0).getName());
        assertEquals("/asset/pda/master/warehouses", takePath());

        server.enqueue(masterListResponse(2L, "LOC001", "A区", 1L, "一号仓"));
        Awaited<List<PdaMasterDataDto>> locations = await(callback ->
                commonRepository.locations(1L, callback));
        assertEquals(Long.valueOf(1L), locations.data.get(0).getParentId());
        assertEquals("/asset/pda/master/locations?warehouseId=1", takePath());

        server.enqueue(masterListResponse(3L, "CAT001", "电子设备", null, null));
        Awaited<List<PdaMasterDataDto>> categories = await(commonRepository::categories);
        assertEquals("电子设备", categories.data.get(0).getName());
        assertEquals("/asset/pda/master/categories", takePath());
    }

    @Test
    public void malformedBootstrapIsProtocolFailure() throws Exception {
        server.enqueue(jsonResponse("{\"code\":0,\"data\":"));

        Awaited<PdaBootstrapDto> result = await(commonRepository::bootstrap);

        assertEquals(ApiErrorMapper.Kind.PROTOCOL, result.error.getKind());
    }

    @Test
    public void emptyDictionaryTypeFailsBeforeNetworkCall() throws Exception {
        Awaited<List<PdaDictItemDto>> result = await(callback ->
                commonRepository.dict("  ", callback));

        assertEquals(ApiErrorMapper.Kind.PROTOCOL, result.error.getKind());
        assertEquals(0, server.getRequestCount());
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody(body);
    }

    private MockResponse masterListResponse(Long id, String code, String name,
            Long parentId, String parentName) {
        JsonObject item = new JsonObject();
        item.addProperty("id", id);
        item.addProperty("code", code);
        item.addProperty("name", name);
        if (parentId == null) {
            item.add("parentId", null);
        } else {
            item.addProperty("parentId", parentId);
        }
        if (parentName == null) {
            item.add("parentName", null);
        } else {
            item.addProperty("parentName", parentName);
        }
        return jsonResponse("{\"code\":0,\"msg\":\"操作成功\",\"data\":["
                + item + "]}");
    }

    private void saveSessionCookie() {
        saveSessionCookie("session-value");
    }

    private void saveSessionCookie(String value) {
        HttpUrl url = server.url("/asset/pda/profile");
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value(value)
                .hostOnlyDomain(url.host())
                .path("/")
                .build();
        cookieJar.saveFromResponse(url, Collections.singletonList(cookie));
    }

    private String takePath() throws InterruptedException {
        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request);
        return request.getPath();
    }

    private <T> Awaited<T> await(
            Function<RepositoryCallback<T>, RequestHandle> requestStarter) throws Exception {
        Awaited<T> result = new Awaited<>();
        CountDownLatch latch = new CountDownLatch(1);
        requestStarter.apply(new RepositoryCallback<T>() {
            @Override
            public void onSuccess(T data) {
                result.data = data;
                latch.countDown();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                result.error = error;
                latch.countDown();
            }
        });
        assertTrue("Repository 请求未在超时内完成", latch.await(5L, TimeUnit.SECONDS));
        return result;
    }

    private static final class Awaited<T> {
        private T data;
        private ApiErrorMapper.ApiError error;
    }
}
