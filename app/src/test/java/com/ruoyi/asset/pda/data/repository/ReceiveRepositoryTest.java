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
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchSubmitDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class ReceiveRepositoryTest {
    private MockWebServer server;
    private ReceiveRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(),
                cookieJar, sessionManager);
        PdaApiService apiService = apiClient.create(PdaApiService.class);
        repository = new DefaultReceiveRepository(apiService,
                new ApiCallExecutor(apiClient.getErrorMapper()));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void recipientSearchUsesKeywordAndParsesOnlyMasterData() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":[{\"id\":7,\"code\":\"zhangsan\","
                + "\"name\":\"张三\",\"parentId\":9,\"parentName\":\"资产部\"}]}"));

        Awaited<List<PdaMasterDataDto>> result = await(callback ->
                repository.searchRecipients(" 张 ", callback));

        assertNull(result.error);
        assertEquals(1, result.data.size());
        assertEquals("张三", result.data.get(0).getName());
        RecordedRequest request = takeRequest("GET", "/asset/pda/receive/recipients");
        assertEquals("张", request.getRequestUrl().queryParameter("keyword"));
    }

    @Test
    public void batchCheckSendsRecipientAndPreservesIdentifierRows() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"totalCount\":2,"
                + "\"eligibleCount\":1,\"ineligibleCount\":0,\"unknownCount\":0,"
                + "\"duplicateCount\":1,\"rows\":[{\"identifyType\":\"EPC\","
                + "\"identifyValue\":\"E20001\",\"assetId\":11,"
                + "\"assetCode\":\"ZC-001\",\"assetName\":\"手持终端\","
                + "\"status\":\"ELIGIBLE\"},{\"identifyType\":\"EPC\","
                + "\"identifyValue\":\"E20001\",\"status\":\"DUPLICATE\","
                + "\"message\":\"重复扫描\"}]}}"));
        List<PdaAssetIdentifyRequest> identifiers = Arrays.asList(
                new PdaAssetIdentifyRequest("EPC", "e20001"),
                new PdaAssetIdentifyRequest("EPC", "E20001"));

        Awaited<PdaReceiveBatchCheckDto> result = await(callback ->
                repository.batchCheck(7L, 9L, identifiers, callback));

        assertNull(result.error);
        assertEquals(1, result.data.getDuplicateCount());
        RecordedRequest request = takeRequest("POST", "/asset/pda/receive/batch-check");
        JsonObject body = body(request);
        assertEquals(7L, body.get("receiveUserId").getAsLong());
        assertEquals(9L, body.get("receiveDeptId").getAsLong());
        assertEquals(2, body.getAsJsonArray("identifiers").size());
        assertEquals("E20001", body.getAsJsonArray("identifiers")
                .get(0).getAsJsonObject().get("identifyValue").getAsString());
        assertEquals("E20001", body.getAsJsonArray("identifiers")
                .get(1).getAsJsonObject().get("identifyValue").getAsString());
    }

    @Test
    public void submitSendsOnlyEligibleIdentifiersAndRemark() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"orderId\":91,"
                + "\"receiveNo\":\"LY-001\",\"receiveUserId\":7,"
                + "\"receiveUserName\":\"张三\",\"receiveDeptId\":9,"
                + "\"receiveDeptName\":\"资产部\",\"applicantUserId\":7,"
                + "\"applicantUserName\":\"管理员\","
                + "\"submitTime\":\"2026-07-24 10:20:30\","
                + "\"orderStatus\":\"PENDING_CONFIRM\",\"taskId\":7001,"
                + "\"taskRound\":1,\"taskStatus\":\"PENDING\",\"totalCount\":1,"
                + "\"successCount\":1,\"rows\":[{\"assetId\":11,"
                + "\"assetCode\":\"ZC-001\",\"assetName\":\"手持终端\","
                + "\"status\":\"SUCCESS\"}]}}"));

        Awaited<PdaReceiveBatchSubmitDto> result = await(callback ->
                repository.batchSubmit(7L, 9L, Collections.singletonList(
                        new PdaAssetIdentifyRequest("ASSET_CODE", " ZC-001 ")),
                        " 现场交接 ", callback));

        assertNull(result.error);
        assertEquals("2026-07-24 10:20:30", result.data.getSubmitTime());
        assertEquals("PENDING", result.data.getTaskStatus());
        RecordedRequest request = takeRequest("POST", "/asset/pda/receive/batch-submit");
        JsonObject body = body(request);
        assertEquals(7L, body.get("receiveUserId").getAsLong());
        assertEquals(9L, body.get("receiveDeptId").getAsLong());
        assertEquals("现场交接", body.get("remark").getAsString());
        assertEquals("ASSET_CODE", body.getAsJsonArray("identifiers")
                .get(0).getAsJsonObject().get("identifyType").getAsString());
        assertTrue(!body.has("receiveTime") && !body.has("confirmUserName")
                && !body.has("confirmTime"));
    }

    @Test
    public void invalidInputsFailBeforeNetwork() throws Exception {
        Awaited<List<PdaMasterDataDto>> keyword = await(callback ->
                repository.searchRecipients(String.join("",
                        Collections.nCopies(31, "A")), callback));
        assertNotNull(keyword.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, keyword.error.getKind());

        Awaited<PdaReceiveBatchCheckDto> invalidEpc = await(callback ->
                repository.batchCheck(7L, 9L, Collections.singletonList(
                        new PdaAssetIdentifyRequest("EPC", "E2000G")), callback));
        assertNotNull(invalidEpc.error);

        Awaited<PdaReceiveBatchSubmitDto> remarkTooLong = await(callback ->
                repository.batchSubmit(7L, 9L, Collections.singletonList(
                        new PdaAssetIdentifyRequest("ASSET_CODE", "ZC-001")),
                        String.join("", Collections.nCopies(501, "备")), callback));
        assertNotNull(remarkTooLong.error);
        assertEquals(0, server.getRequestCount());
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody(body);
    }

    private RecordedRequest takeRequest(String method, String expectedPath) throws Exception {
        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(method, request.getMethod());
        assertEquals(expectedPath, request.getRequestUrl().encodedPath());
        return request;
    }

    private JsonObject body(RecordedRequest request) {
        return JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
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
