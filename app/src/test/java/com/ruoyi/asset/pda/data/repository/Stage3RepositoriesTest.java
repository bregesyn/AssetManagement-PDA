package com.ruoyi.asset.pda.data.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Stage3RepositoriesTest {
    private MockWebServer server;
    private AssetRepository assetRepository;
    private RfidRepository rfidRepository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(),
                cookieJar, sessionManager);
        PdaApiService service = apiClient.create(PdaApiService.class);
        ApiCallExecutor executor = new ApiCallExecutor(apiClient.getErrorMapper());
        assetRepository = new DefaultAssetRepository(service, executor);
        rfidRepository = new DefaultRfidRepository(service, executor);
    }

    @After
    public void tearDown() throws IOException { server.shutdown(); }

    @Test
    public void identifyUsesExactContractAndParsesAssetFields() throws Exception {
        server.enqueue(json("{\"code\":0,\"msg\":\"操作成功\",\"data\":{"
                + "\"assetId\":11,\"assetCode\":\"ASSET-001\",\"assetName\":\"测试资产\","
                + "\"categoryId\":3,\"categoryName\":\"电子设备\","
                + "\"specModel\":\"M1\",\"brand\":\"品牌A\","
                + "\"assetStatus\":\"IN_STOCK\",\"assetStatusName\":\"在库\","
                + "\"warehouseId\":5,\"warehouseName\":\"一号仓\","
                + "\"locationId\":6,\"locationName\":\"A区\","
                + "\"tagId\":21,\"tagCode\":\"TAG001\",\"epcCode\":\"E20001\","
                + "\"tagStatus\":\"NORMAL\",\"tagStatusName\":\"正常\","
                + "\"bindStatus\":\"BOUND\",\"bindStatusName\":\"已绑定\","
                + "\"rfidBound\":true}}"));

        Awaited<PdaAssetIdentifyDto> result = await(callback -> assetRepository.identify(
                AssetRepository.IDENTIFY_TYPE_EPC, " e20001 ", callback));

        assertNull(result.error);
        assertEquals(Long.valueOf(11L), result.data.getAssetId());
        assertEquals("一号仓", result.data.getWarehouseName());
        assertTrue(result.data.isRfidBound());
        RecordedRequest request = takeRequest("/asset/pda/asset/identify");
        JsonObject body = body(request);
        assertEquals("EPC", body.get("identifyType").getAsString());
        assertEquals("E20001", body.get("identifyValue").getAsString());
    }

    @Test
    public void queryPermissionFailureIsBusinessErrorFromHttp200() throws Exception {
        server.enqueue(json("{\"code\":500,\"msg\":\"没有权限，请联系管理员授权\"}"));

        Awaited<PdaRfidTagDto> result = await(callback ->
                rfidRepository.queryTag("e20002", callback));

        assertEquals(ApiErrorMapper.Kind.BUSINESS, result.error.getKind());
        assertEquals("没有权限，请联系管理员授权", result.error.getMessage());
        RecordedRequest request = takeRequest("/asset/pda/rfid/tag/query");
        assertEquals("E20002", body(request).get("epcCode").getAsString());
    }

    @Test
    public void batchUsesExactRowsAndNeverSendsOperName() throws Exception {
        server.enqueue(json("{\"code\":0,\"msg\":\"RFID标签批量建档完成\",\"data\":{"
                + "\"successCount\":1,\"duplicateCount\":1,\"failureCount\":0,\"rows\":["
                + "{\"rowNumber\":1,\"epcCode\":\"E20001\",\"success\":true,"
                + "\"duplicate\":false,\"tagId\":31,\"tagCode\":\"TAG031\",\"message\":\"建档成功\"},"
                + "{\"rowNumber\":2,\"epcCode\":\"E20002\",\"success\":false,"
                + "\"duplicate\":true,\"tagId\":null,\"tagCode\":null,\"message\":\"EPC已存在\"}]}}"));

        Awaited<RfidTagBatchResultDto> result = await(callback ->
                rfidRepository.batchCreate(Arrays.asList("e20001", "E20002"),
                        "现场测试", callback));

        assertNull(result.error);
        assertEquals(1, result.data.getSuccessCount());
        assertTrue(result.data.getRows().get(1).isDuplicate());
        RecordedRequest request = takeRequest("/asset/pda/rfid/tags/batch");
        JsonObject body = body(request);
        assertEquals("E20001", body.getAsJsonArray("epcCodes").get(0).getAsString());
        assertEquals("现场测试", body.get("remark").getAsString());
        assertFalse(body.has("operName"));
    }

    @Test
    public void bindAndUnbindUseOnlyConfirmedFields() throws Exception {
        server.enqueue(json(successTag(true, "ASSET-001")));
        Awaited<PdaRfidTagDto> bind = await(callback ->
                rfidRepository.bind(" ASSET-001 ", "e20001", callback));
        assertNull(bind.error);
        assertTrue(bind.data.isRfidBound());
        RecordedRequest bindRequest = takeRequest("/asset/pda/rfid/bind");
        JsonObject bindBody = body(bindRequest);
        assertEquals("ASSET-001", bindBody.get("assetCode").getAsString());
        assertEquals("E20001", bindBody.get("epcCode").getAsString());
        assertEquals(2, bindBody.size());

        server.enqueue(json(successTag(false, null)));
        Awaited<PdaRfidTagDto> unbind = await(callback ->
                rfidRepository.unbind(21L, callback));
        assertNull(unbind.error);
        assertFalse(unbind.data.isRfidBound());
        RecordedRequest unbindRequest = takeRequest("/asset/pda/rfid/unbind");
        JsonObject unbindBody = body(unbindRequest);
        assertEquals(21L, unbindBody.get("tagId").getAsLong());
        assertEquals(1, unbindBody.size());
    }

    @Test
    public void invalidLengthsFailBeforeNetwork() throws Exception {
        Awaited<PdaRfidTagDto> query = await(callback ->
                rfidRepository.queryTag(repeat('E', 129), callback));
        Awaited<PdaRfidTagDto> bind = await(callback ->
                rfidRepository.bind(repeat('A', 65), "E20001", callback));
        Awaited<RfidTagBatchResultDto> batch = await(callback ->
                rfidRepository.batchCreate(Collections.singletonList("E20001"),
                        repeat('R', 501), callback));

        assertEquals(ApiErrorMapper.Kind.PROTOCOL, query.error.getKind());
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, bind.error.getKind());
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, batch.error.getKind());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void missingDataAndMalformedJsonAreProtocolFailures() throws Exception {
        server.enqueue(json("{\"code\":0,\"msg\":\"操作成功\"}"));
        Awaited<PdaRfidTagDto> missing = await(callback ->
                rfidRepository.queryTag("E20001", callback));
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, missing.error.getKind());

        server.enqueue(json("{\"code\":0,\"data\":"));
        Awaited<PdaRfidTagDto> malformed = await(callback ->
                rfidRepository.queryTag("E20002", callback));
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, malformed.error.getKind());
    }

    private String successTag(boolean bound, String assetCode) {
        return "{\"code\":0,\"msg\":\"操作成功\",\"data\":{"
                + "\"tagId\":21,\"tagCode\":\"TAG021\",\"epcCode\":\"E20001\","
                + "\"tagStatus\":\"NORMAL\",\"tagStatusName\":\"正常\","
                + "\"bindStatus\":\"" + (bound ? "BOUND" : "UNBOUND") + "\","
                + "\"bindStatusName\":\"" + (bound ? "已绑定" : "未绑定") + "\","
                + "\"assetId\":" + (bound ? "11" : "null") + ","
                + "\"assetCode\":" + (assetCode == null ? "null" : "\"" + assetCode + "\"") + ","
                + "\"assetName\":" + (bound ? "\"测试资产\"" : "null") + ","
                + "\"rfidBound\":" + bound + "}}";
    }

    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody(body);
    }

    private RecordedRequest takeRequest(String path) throws InterruptedException {
        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals(path, request.getPath());
        return request;
    }

    private JsonObject body(RecordedRequest request) {
        return JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
    }

    private String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private <T> Awaited<T> await(
            Function<RepositoryCallback<T>, RequestHandle> starter) throws Exception {
        Awaited<T> result = new Awaited<>();
        CountDownLatch latch = new CountDownLatch(1);
        starter.apply(new RepositoryCallback<T>() {
            @Override public void onSuccess(T data) { result.data = data; latch.countDown(); }
            @Override public void onError(ApiErrorMapper.ApiError error) {
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
