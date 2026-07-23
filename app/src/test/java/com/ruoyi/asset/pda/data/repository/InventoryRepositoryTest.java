package com.ruoyi.asset.pda.data.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ruoyi.asset.pda.core.network.ApiClient;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.SessionCookieJar;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InventoryRepositoryTest {
    private MockWebServer server;
    private InventoryRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SessionCookieJar cookieJar = TestNetworkFactory.newCookieJar();
        SessionManager sessionManager = new SessionManager(cookieJar, Runnable::run);
        ApiClient apiClient = new ApiClient(server.url("/").toString(), cookieJar, sessionManager);
        PdaApiService service = apiClient.create(PdaApiService.class);
        repository = new DefaultInventoryRepository(service,
                new ApiCallExecutor(apiClient.getErrorMapper()));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void taskPageUsesHundredRowsAndPreservesTaskFields() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"total\":1,\"pageNum\":1,\"pageSize\":100,\"rows\":[{"
                + "\"taskId\":7,\"taskNo\":\"PD-001\",\"taskStatus\":\"INVENTORYING\","
                + "\"warehouseId\":5,\"warehouseName\":\"一号仓\",\"totalCount\":42,"
                + "\"inventoriedCount\":18,\"pendingCount\":24,\"editable\":true}]}}"));

        Awaited<PdaPageResultDto<PdaInventoryTaskDto>> result = await(callback ->
                repository.loadTasks(1, 100, callback));

        assertNull(result.error);
        assertEquals(1, result.data.getRows().size());
        assertEquals("PD-001", result.data.getRows().get(0).getTaskNo());
        assertEquals(24L, result.data.getRows().get(0).getPendingCount());
        RecordedRequest request = takeRequest("GET", "/asset/pda/inventory/tasks?pageNum=1&pageSize=100");
        assertNotNull(request);
    }

    @Test
    public void batchScanNormalizesEpcAndSendsOneReadOnlyRequest() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"totalCount\":2,\"confirmableCount\":1,"
                + "\"unresolvedCount\":1,\"duplicateCount\":0,\"expectedCount\":1,\"surplusCount\":0,"
                + "\"normalCount\":1,\"rows\":["
                + "{\"rowNumber\":1,\"epcCode\":\"E20001\",\"matchType\":\"EXPECTED_ITEM\","
                + "\"confirmable\":true,\"proposedResult\":\"NORMAL\"},"
                + "{\"rowNumber\":2,\"epcCode\":\"E20002\",\"matchType\":\"UNKNOWN_OBJECT\","
                + "\"confirmable\":false,\"reasonCode\":\"TAG_NOT_FOUND\"}]}}"));

        Awaited<PdaInventoryBatchScanDto> result = await(callback -> repository.batchScan(
                7L, " TASK-001 ", 5L, null,
                Arrays.asList("e20001", "E20001", "e20002"), callback));

        assertNull(result.error);
        assertEquals(2, result.data.getRows().size());
        RecordedRequest request = takeRequest("POST", "/asset/pda/inventory/tasks/7/batch-scan");
        JsonObject body = body(request);
        assertEquals("TASK-001", body.get("taskNo").getAsString());
        assertEquals(5L, body.get("inventoryWarehouseId").getAsLong());
        assertTrue(!body.has("inventoryLocationId") || body.get("inventoryLocationId").isJsonNull());
        assertEquals("E20001", body.getAsJsonArray("epcCodes").get(0).getAsString());
        assertEquals(2, body.getAsJsonArray("epcCodes").size());
    }

    @Test
    public void singleResultAndSurplusUseBusinessSpecificPayloads() throws Exception {
        server.enqueue(json("{\"code\":0,\"data\":{\"itemId\":9,\"inventoryResult\":\"NORMAL\"}}"));
        Awaited<PdaInventoryItemDto> item = await(callback -> repository.saveItemResult(
                7L, 9L, "NORMAL", 5L, null, "现场仓库确认", callback));
        assertNull(item.error);
        JsonObject itemBody = body(takeRequest("POST", "/asset/pda/inventory/tasks/7/items/9/result"));
        assertEquals("NORMAL", itemBody.get("inventoryResult").getAsString());
        assertEquals(5L, itemBody.get("inventoryWarehouseId").getAsLong());
        assertTrue(!itemBody.has("inventoryLocationId") || itemBody.get("inventoryLocationId").isJsonNull());

        server.enqueue(json("{\"code\":0,\"data\":{\"surplusId\":11,\"epcCode\":\"E20003\"}}"));
        Awaited<com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto> surplus = await(callback ->
                repository.saveSurplus(7L, "EPC", null, null, null,
                null, null, "e20003", 5L, 6L, null, callback));
        assertNull(surplus.error);
        JsonObject surplusBody = body(takeRequest("POST", "/asset/pda/inventory/tasks/7/surpluses"));
        assertEquals("EPC", surplusBody.get("identifyMethod").getAsString());
        assertEquals("E20003", surplusBody.get("epcCode").getAsString());
        assertEquals(5L, surplusBody.get("inventoryWarehouseId").getAsLong());
        assertEquals(6L, surplusBody.get("inventoryLocationId").getAsLong());
    }

    @Test
    public void unsupportedItemResultFailsBeforeNetwork() throws Exception {
        Awaited<PdaInventoryItemDto> result = await(callback -> repository.saveItemResult(
                7L, 9L, "SURPLUS", 5L, 6L, null, callback));

        assertNotNull(result.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, result.error.getKind());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void invalidEpcBatchFailsBeforeNetworkAndKeepsFiveHundredBoundary() throws Exception {
        Awaited<PdaInventoryBatchScanDto> invalid = await(callback -> repository.batchScan(
                7L, "TASK-001", 5L, null, Collections.singletonList("E2000G"), callback));
        assertNotNull(invalid.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, invalid.error.getKind());

        String[] values = new String[501];
        Arrays.fill(values, "E20001");
        Awaited<PdaInventoryBatchScanDto> tooMany = await(callback -> repository.batchScan(
                7L, "TASK-001", 5L, null, Arrays.asList(values), callback));
        assertNotNull(tooMany.error);
        assertEquals(ApiErrorMapper.Kind.PROTOCOL, tooMany.error.getKind());
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
        assertEquals(path, request.getPath());
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
