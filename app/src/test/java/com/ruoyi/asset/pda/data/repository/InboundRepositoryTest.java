package com.ruoyi.asset.pda.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;

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

public class InboundRepositoryTest {
    private MockWebServer server;
    private InboundRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(),
                cookieJar, sessionManager);
        PdaApiService apiService = apiClient.create(PdaApiService.class);
        repository = new DefaultInboundRepository(apiService,
                new ApiCallExecutor(apiClient.getErrorMapper()));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void assetCodeEligibilityUsesOnlyAssetCodeQueryParameter() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"assetId\":11,"
                + "\"assetCode\":\"ZC-001\",\"assetName\":\"手持终端\","
                + "\"assetStatus\":\"PENDING_INBOUND\","
                + "\"assetStatusLabel\":\"待入库\",\"eligible\":true}}"));

        Awaited<PdaInboundEligibilityDto> result = await(callback ->
                repository.queryByAssetCode(" ZC-001 ", callback));

        assertNull(result.error);
        assertEquals(11L, result.data.getAssetId().longValue());
        RecordedRequest request = takeRequest("GET",
                "/asset/pda/inbound/asset?assetCode=ZC-001");
        assertNull(request.getRequestUrl().queryParameter("epcCode"));
    }

    @Test
    public void batchCheckNormalizesAndDeduplicatesEpcs() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"totalCount\":2,"
                + "\"eligibleCount\":1,\"ineligibleCount\":0,\"unknownCount\":1,"
                + "\"rows\":[{\"epcCode\":\"E20001\",\"assetId\":11,"
                + "\"assetCode\":\"ZC-001\",\"assetName\":\"手持终端\","
                + "\"status\":\"ELIGIBLE\"},{\"epcCode\":\"E20002\","
                + "\"status\":\"UNKNOWN\",\"message\":\"未绑定\"}]}}"));

        Awaited<PdaInboundBatchCheckDto> result = await(callback ->
                repository.batchCheck(Arrays.asList("e20001", "E20001", "e20002"),
                        callback));

        assertNull(result.error);
        assertEquals(2, result.data.getRows().size());
        RecordedRequest request = takeRequest("POST",
                "/asset/pda/inbound/batch-check");
        JsonObject body = body(request);
        assertEquals(2, body.getAsJsonArray("epcCodes").size());
        assertEquals("E20001",
                body.getAsJsonArray("epcCodes").get(0).getAsString());
        assertEquals("E20002",
                body.getAsJsonArray("epcCodes").get(1).getAsString());
    }

    @Test
    public void confirmSendsOnlyDestinationAssetsAndRemark() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"orderId\":91,"
                + "\"inboundNo\":\"RK-001\",\"warehouseName\":\"一号仓\","
                + "\"locationName\":\"A-01\",\"inboundUserName\":\"管理员\","
                + "\"inboundTime\":\"2026-07-24 10:20:30\","
                + "\"totalCount\":2,\"successCount\":2,\"rows\":["
                + "{\"assetId\":11,\"status\":\"SUCCESS\"},"
                + "{\"assetId\":12,\"status\":\"SUCCESS\"}]}}"));

        Awaited<PdaInboundBatchConfirmDto> result = await(callback ->
                repository.batchConfirm(5L, 6L, Arrays.asList(11L, 12L),
                        " 现场接收 ", callback));

        assertNull(result.error);
        assertEquals("管理员", result.data.getInboundUserName());
        RecordedRequest request = takeRequest("POST",
                "/asset/pda/inbound/batch-confirm");
        JsonObject body = body(request);
        assertEquals(5L, body.get("warehouseId").getAsLong());
        assertEquals(6L, body.get("locationId").getAsLong());
        assertEquals(2, body.getAsJsonArray("assetIds").size());
        assertEquals("现场接收", body.get("remark").getAsString());
        assertTrue(!body.has("inboundUserName") && !body.has("inboundTime"));
    }

    @Test
    public void invalidInputsFailBeforeNetwork() throws Exception {
        Awaited<PdaInboundEligibilityDto> invalidCode = await(callback ->
                repository.queryByAssetCode(String.join("",
                        Collections.nCopies(65, "A")), callback));
        assertNotNull(invalidCode.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, invalidCode.error.getKind());

        Awaited<PdaInboundBatchCheckDto> invalidEpc = await(callback ->
                repository.batchCheck(Collections.singletonList("E2000G"), callback));
        assertNotNull(invalidEpc.error);

        Awaited<PdaInboundBatchCheckDto> tooMany = await(callback ->
                repository.batchCheck(Collections.nCopies(101, "E20001"), callback));
        assertNotNull(tooMany.error);

        Awaited<PdaInboundBatchConfirmDto> duplicateIds = await(callback ->
                repository.batchConfirm(5L, 6L, Arrays.asList(11L, 11L),
                        null, callback));
        assertNotNull(duplicateIds.error);
        assertEquals(0, server.getRequestCount());
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody(body);
    }

    private RecordedRequest takeRequest(String method, String path) throws Exception {
        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getPath());
        return request;
    }

    private JsonObject body(RecordedRequest request) {
        return JsonParser.parseString(request.getBody().readUtf8())
                .getAsJsonObject();
    }

    private <T> Awaited<T> await(Function<RepositoryCallback<T>, RequestHandle> starter)
            throws Exception {
        Awaited<T> result = new Awaited<>();
        CountDownLatch latch = new CountDownLatch(1);
        starter.apply(new RepositoryCallback<T>() {
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
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        return result;
    }

    private static final class Awaited<T> {
        private T data;
        private ApiErrorMapper.ApiError error;
    }
}
