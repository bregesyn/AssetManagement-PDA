package com.ruoyi.asset.pda.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** 报修维修 Retrofit 契约测试：覆盖七个 PDA 路径和客户端字段裁剪。 */
public class RepairRepositoryTest {
    private MockWebServer server;
    private RepairRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        repository = new DefaultRepairRepository(apiClient.create(PdaApiService.class),
                new ApiCallExecutor(apiClient.getErrorMapper()));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readEndpointsUseExactPathsAndQueryContract() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":[]}"));
        server.enqueue(json("{\"code\":0,\"data\":{\"total\":0,\"pageNum\":1,\"pageSize\":20,\"rows\":[]}}"));
        server.enqueue(json("{\"code\":0,\"data\":{\"total\":0,\"pageNum\":1,\"pageSize\":20,\"rows\":[]}}"));
        server.enqueue(json("{\"code\":0,\"data\":{\"repairId\":7}}"));

        Awaited<List<PdaRepairerDto>> repairersResult = await(callback ->
                repository.searchRepairers(" 张 ", callback));
        Awaited<PdaPageResultDto<PdaRepairOrderDto>> mineResult = await(callback ->
                repository.loadMyOrders("WAIT_REPAIR", " BX ", 1, 20, callback));
        Awaited<PdaPageResultDto<PdaRepairOrderDto>> workResult = await(callback ->
                repository.loadWorkOrders(" ZC ", 1, 20, callback));
        Awaited<PdaRepairOrderDto> detailResult = await(callback ->
                repository.loadOrder(7L, callback));
        assertNull(repairersResult.error);
        assertNull(mineResult.error);
        assertNull(workResult.error);
        assertNull(detailResult.error);

        RecordedRequest repairers = takeRequest("GET", "/asset/pda/repair/repairers");
        assertEquals("张", repairers.getRequestUrl().queryParameter("keyword"));
        RecordedRequest mine = takeRequest("GET", "/asset/pda/repair/orders/mine");
        assertEquals("WAIT_REPAIR", mine.getRequestUrl().queryParameter("orderStatus"));
        assertEquals("BX", mine.getRequestUrl().queryParameter("keyword"));
        assertEquals("1", mine.getRequestUrl().queryParameter("pageNum"));
        assertEquals("20", mine.getRequestUrl().queryParameter("pageSize"));
        RecordedRequest work = takeRequest("GET", "/asset/pda/repair/orders/work");
        assertEquals("ZC", work.getRequestUrl().queryParameter("keyword"));
        takeRequest("GET", "/asset/pda/repair/orders/7");
    }

    @Test
    public void writeEndpointsOnlySendPdaContractFields() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"order\":{\"repairId\":8,\"repairNo\":\"BX-8\"},\"approvalTask\":{\"taskId\":9}}}"));
        server.enqueue(json("{\"code\":0,\"data\":{\"repairId\":8}}"));
        server.enqueue(json("{\"code\":0,\"data\":{\"repairId\":8}}"));

        Awaited<PdaRepairSubmitResultDto> submit = await(callback -> repository.submit(
                new PdaAssetIdentifyRequest("EPC", " e20001 "), "按键无响应", "2026-08-10",
                "现场报修", callback));
        assertNull(submit.error);
        Awaited<PdaRepairOrderDto> start = await(callback -> repository.startRepair(8L,
                "INTERNAL", 12L, "不应上传", "不应上传", "不应上传", callback));
        assertNull(start.error);
        Awaited<PdaRepairOrderDto> finish = await(callback -> repository.finishRepair(8L,
                "2026-08-10", "更换按键并测试通过", new BigDecimal("12.50"), callback));
        assertNull(finish.error);

        JsonObject submitBody = body(takeRequest("POST", "/asset/pda/repair/orders/submit"));
        assertEquals("EPC", submitBody.getAsJsonObject("identifier").get("identifyType").getAsString());
        assertEquals("E20001", submitBody.getAsJsonObject("identifier").get("identifyValue").getAsString());
        assertEquals("按键无响应", submitBody.get("faultDesc").getAsString());
        assertFalse(submitBody.has("assetId"));
        assertFalse(submitBody.has("reportUserId"));
        assertFalse(submitBody.has("reportTime"));

        JsonObject startBody = body(takeRequest("POST", "/asset/pda/repair/orders/8/start"));
        assertEquals("INTERNAL", startBody.get("repairerType").getAsString());
        assertEquals(12L, startBody.get("repairUserId").getAsLong());
        assertFalse(startBody.has("repairUserName"));
        assertFalse(startBody.has("repairOrgName"));
        assertFalse(startBody.has("repairContactPhone"));

        JsonObject finishBody = body(takeRequest("POST", "/asset/pda/repair/orders/8/finish"));
        assertEquals("2026-08-10", finishBody.get("repairFinishTime").getAsString());
        assertEquals("更换按键并测试通过", finishBody.get("repairResult").getAsString());
        assertEquals("12.50", finishBody.get("repairCost").getAsString());
    }

    @Test
    public void invalidValuesStopBeforeNetwork() throws Exception {
        Awaited<List<PdaRepairerDto>> keyword = await(callback -> repository.searchRepairers(
                "1234567890123456789012345678901", callback));
        Awaited<PdaRepairOrderDto> cost = await(callback -> repository.finishRepair(8L,
                "2026-08-10", "已完成", new BigDecimal("0.001"), callback));

        assertNotNull(keyword.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, keyword.error.getKind());
        assertNotNull(cost.error);
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void externalStartSendsOrganizationAndNeverInternalUserId() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"repairId\":8}}"));

        Awaited<PdaRepairOrderDto> result = await(callback -> repository.startRepair(8L,
                " external ", 12L, " 李工 ", " 外部维修单位 ", "13800000000", callback));

        assertNull(result.error);
        JsonObject body = body(takeRequest("POST", "/asset/pda/repair/orders/8/start"));
        assertEquals("EXTERNAL", body.get("repairerType").getAsString());
        assertEquals("外部维修单位", body.get("repairOrgName").getAsString());
        assertEquals("李工", body.get("repairUserName").getAsString());
        assertEquals("13800000000", body.get("repairContactPhone").getAsString());
        assertFalse(body.has("repairUserId"));
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
            @Override public void onSuccess(T data) { result.data = data; latch.countDown(); }
            @Override public void onError(ApiErrorMapper.ApiError error) { result.error = error; latch.countDown(); }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        return result;
    }

    private static final class Awaited<T> {
        private T data;
        private ApiErrorMapper.ApiError error;
    }
}
