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
import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

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

/** 借还协议边界测试：验证真实路径、请求字段和本地输入短路。 */
public class BorrowRepositoryTest {
    private MockWebServer server;
    private BorrowRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        PdaApiService apiService = apiClient.create(PdaApiService.class);
        repository = new DefaultBorrowRepository(apiService,
                new ApiCallExecutor(apiClient.getErrorMapper()));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void borrowerSearchUsesBorrowerEndpoint() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":[{\"id\":7,\"code\":\"zhangsan\","
                + "\"name\":\"张三\",\"parentId\":9,\"parentName\":\"资产部\","
                + "\"phonenumber\":\"13800000000\"}]}"));

        Awaited<List<PdaMasterDataDto>> result = await(callback ->
                repository.searchBorrowers(" 张 ", callback));

        assertNull(result.error);
        assertEquals("张三", result.data.get(0).getName());
        assertEquals("13800000000", result.data.get(0).getPhoneNumber());
        RecordedRequest request = takeRequest("GET", "/asset/pda/borrow/borrowers");
        assertEquals("张", request.getRequestUrl().queryParameter("keyword"));
    }

    @Test
    public void issueCheckSendsInternalContractAndNormalizesEpc() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"totalCount\":2,"
                + "\"eligibleCount\":1,\"ineligibleCount\":0,\"unknownCount\":1,"
                + "\"duplicateCount\":0,\"rows\":[]}}"));
        List<PdaAssetIdentifyRequest> identifiers = Arrays.asList(
                new PdaAssetIdentifyRequest("EPC", " e20001 "),
                new PdaAssetIdentifyRequest("ASSET_CODE", " ZC-001 "));

        Awaited<PdaBorrowBatchCheckDto> result = await(callback ->
                repository.batchCheckIssue(" internal ", 7L, 9L, "旧公司", "13800000000",
                        "2026-08-06", identifiers, callback));

        assertNull(result.error);
        JsonObject body = body(takeRequest("POST", "/asset/pda/borrow/issue/batch-check"));
        assertEquals("INTERNAL", body.get("borrowerType").getAsString());
        assertEquals(7L, body.get("borrowUserId").getAsLong());
        assertEquals(9L, body.get("borrowDeptId").getAsLong());
        assertEquals("2026-08-06", body.get("expectedReturnDate").getAsString());
        assertTrue(!body.has("borrowOrgName") && !body.has("borrowContactPhone"));
        assertEquals("E20001", body.getAsJsonArray("identifiers").get(0)
                .getAsJsonObject().get("identifyValue").getAsString());
        assertEquals("ZC-001", body.getAsJsonArray("identifiers").get(1)
                .getAsJsonObject().get("identifyValue").getAsString());
    }

    @Test
    public void externalSubmitSendsContactAndRemark() throws Exception {
        // 响应只需证明 Retrofit 能解析提交回执；逐项回执校验由 ViewModel 测试覆盖。
        server.enqueue(json("{\"code\":0,\"data\":{}}"));

        Awaited<PdaBorrowIssueBatchSubmitDto> result = await(callback ->
                repository.batchSubmitIssue("EXTERNAL", 7L, 9L, "外部公司", "13800000000",
                        "2026-08-06", Collections.singletonList(
                                new PdaAssetIdentifyRequest("ASSET_CODE", "ZC-001")),
                        "现场临时借用", callback));

        assertNull(result.error);
        JsonObject body = body(takeRequest("POST", "/asset/pda/borrow/issue/batch-submit"));
        assertEquals("EXTERNAL", body.get("borrowerType").getAsString());
        assertEquals("外部公司", body.get("borrowOrgName").getAsString());
        assertEquals("13800000000", body.get("borrowContactPhone").getAsString());
        assertEquals("现场临时借用", body.get("remark").getAsString());
        assertTrue(!body.has("borrowTime") && !body.has("confirmTime")
                && !body.has("assetStatus"));
    }

    @Test
    public void returnEndpointsOnlySendIdentifiers() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"totalCount\":1,"
                + "\"eligibleCount\":1,\"ineligibleCount\":0,\"unknownCount\":0,"
                + "\"duplicateCount\":0,\"rows\":[]}}"));
        server.enqueue(json("{\"code\":0,\"data\":{\"totalCount\":1,"
                + "\"successCount\":1,\"rows\":[]}}"));
        List<PdaAssetIdentifyRequest> identifiers = Collections.singletonList(
                new PdaAssetIdentifyRequest("EPC", "E20001"));

        Awaited<PdaBorrowBatchCheckDto> check = await(callback ->
                repository.batchCheckReturn(identifiers, callback));
        Awaited<PdaBorrowReturnBatchSubmitDto> submit = await(callback ->
                repository.batchSubmitReturn(identifiers, callback));

        assertNull(check.error);
        assertNull(submit.error);
        JsonObject checkBody = body(takeRequest("POST", "/asset/pda/borrow/return/batch-check"));
        JsonObject submitBody = body(takeRequest("POST", "/asset/pda/borrow/return/batch-submit"));
        assertEquals(1, checkBody.getAsJsonArray("identifiers").size());
        assertEquals(1, submitBody.getAsJsonArray("identifiers").size());
        assertEquals(1, submitBody.entrySet().size());
        assertTrue(!submitBody.has("orderId") && !submitBody.has("warehouseId")
                && !submitBody.has("returnDate") && !submitBody.has("remark"));
    }

    @Test
    public void invalidInputStopsBeforeNetwork() throws Exception {
        Awaited<List<PdaMasterDataDto>> keyword = await(callback ->
                repository.searchBorrowers(String.join("", Collections.nCopies(31, "A")), callback));
        Awaited<PdaBorrowBatchCheckDto> date = await(callback -> repository.batchCheckIssue(
                "INTERNAL", 7L, 9L, null, null, "2026/08/06", Collections.singletonList(
                        new PdaAssetIdentifyRequest("EPC", "E20001")), callback));
        Awaited<PdaBorrowBatchCheckDto> external = await(callback -> repository.batchCheckIssue(
                "EXTERNAL", 7L, 9L, null, "13800000000", "2026-08-06",
                Collections.singletonList(new PdaAssetIdentifyRequest("EPC", "E20001")), callback));

        assertNotNull(keyword.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, keyword.error.getKind());
        assertNotNull(date.error);
        assertNotNull(external.error);
        assertEquals(0, server.getRequestCount());
    }

    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody(body);
    }

    private RecordedRequest takeRequest(String method, String path) throws Exception {
        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getRequestUrl().encodedPath());
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
            @Override public void onSuccess(T data) {
                result.data = data;
                latch.countDown();
            }

            @Override public void onError(ApiErrorMapper.ApiError error) {
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
