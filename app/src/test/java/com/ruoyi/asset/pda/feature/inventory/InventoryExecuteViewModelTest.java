package com.ruoyi.asset.pda.feature.inventory;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchLossDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.InventoryRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 紧凑清单执行页的核心状态机测试，不依赖 Android 设备或网络。 */
public class InventoryExecuteViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    @Test
    public void loadsExpectedItemsByServerPageOrder() {
        CapturingInventoryRepository repository = new CapturingInventoryRepository();
        repository.itemPages.add(page("{\"total\":3,\"pageNum\":1,\"pageSize\":100,\"rows\":["
                + "{\"itemId\":101,\"assetCode\":\"A-001\"},"
                + "{\"itemId\":102,\"assetCode\":\"A-002\"}]}"));
        repository.itemPages.add(page("{\"total\":3,\"pageNum\":2,\"pageSize\":100,\"rows\":["
                + "{\"itemId\":103,\"assetCode\":\"A-003\"}]}"));
        InventoryExecuteViewModel viewModel = viewModel(repository);

        viewModel.initialize();

        InventoryExecuteUiState first = state(viewModel);
        assertEquals(Arrays.asList("A-001", "A-002"), assetCodes(first.getExpectedItems()));
        assertNull(repository.requestedInventoried.get(0));
        assertEquals(100, repository.requestedPageSize.get(0).intValue());
        assertTrue(first.hasMoreExpectedItems());

        viewModel.loadMoreExpectedItems();

        InventoryExecuteUiState second = state(viewModel);
        assertEquals(Arrays.asList("A-001", "A-002", "A-003"),
                assetCodes(second.getExpectedItems()));
        assertFalse(second.hasMoreExpectedItems());
    }

    @Test
    public void precheckDefaultsExpectedAndKnownOutOfScopeAndIgnoresUnknownEpc() {
        CapturingInventoryRepository repository = new CapturingInventoryRepository();
        repository.itemPages.add(page("{\"total\":1,\"pageNum\":1,\"pageSize\":100,\"rows\":["
                + "{\"itemId\":101,\"assetCode\":\"A-001\"}]}"));
        repository.preview = preview("{\"totalCount\":3,\"confirmableCount\":2,\"unresolvedCount\":1,"
                + "\"duplicateCount\":0,\"expectedCount\":1,\"surplusCount\":1,\"normalCount\":1,\"rows\":["
                + "{\"rowNumber\":1,\"epcCode\":\"E20001\",\"matchType\":\"EXPECTED_ITEM\","
                + "\"confirmable\":true,\"proposedResult\":\"NORMAL\",\"item\":{\"itemId\":101}},"
                + "{\"rowNumber\":2,\"epcCode\":\"E20002\",\"matchType\":\"KNOWN_OUT_OF_SCOPE\","
                + "\"confirmable\":true},"
                + "{\"rowNumber\":3,\"epcCode\":\"E20003\",\"matchType\":\"UNKNOWN_OBJECT\","
                + "\"confirmable\":false,\"reasonCode\":\"TAG_NOT_FOUND\"}]}");
        FakeUhfScanner scanner = new FakeUhfScanner();
        InventoryExecuteViewModel viewModel = viewModel(repository, scanner);
        viewModel.initialize();

        viewModel.toggleScan();
        scanner.emit("E20001", -40);
        scanner.emit("E20002", -40);
        scanner.emit("E20003", -40);
        viewModel.toggleScan();
        viewModel.precheck();

        InventoryExecuteUiState state = state(viewModel);
        assertTrue(state.getSelectedEpcs().contains("E20001"));
        assertTrue(state.getSelectedEpcs().contains("E20002"));
        assertFalse(state.getSelectedEpcs().contains("E20003"));
        assertEquals("E20001", state.getPreviewEpcByItemId().get(101L));
        assertEquals(2, state.getReadings().size());

        viewModel.resumeCollection();

        InventoryExecuteUiState resumed = state(viewModel);
        assertNull(resumed.getPreview());
        assertEquals(2, resumed.getReadings().size());
        assertTrue(resumed.getSelectedEpcs().isEmpty());
        assertTrue(resumed.getPreviewEpcByItemId().isEmpty());
    }

    @Test
    public void precheckIgnoresUnknownOnlyBatchWithoutLeavingPreview() {
        CapturingInventoryRepository repository = new CapturingInventoryRepository();
        repository.preview = preview("{\"totalCount\":1,\"confirmableCount\":0,\"unresolvedCount\":1,"
                + "\"duplicateCount\":0,\"expectedCount\":0,\"surplusCount\":0,\"normalCount\":0,\"rows\":["
                + "{\"rowNumber\":1,\"epcCode\":\"E20003\",\"matchType\":\"UNKNOWN_OBJECT\","
                + "\"confirmable\":false,\"reasonCode\":\"TAG_NOT_FOUND\"}]}");
        FakeUhfScanner scanner = new FakeUhfScanner();
        InventoryExecuteViewModel viewModel = viewModel(repository, scanner);
        viewModel.initialize();

        viewModel.toggleScan();
        scanner.emit("E20003", -40);
        scanner.emit("E20003", -40);
        viewModel.toggleScan();
        viewModel.precheck();

        InventoryExecuteUiState state = state(viewModel);
        assertNull(state.getPreview());
        assertTrue(state.getReadings().isEmpty());
        assertTrue(state.getSelectedEpcs().isEmpty());
        assertEquals(0, state.getDuplicateReadCount());
    }

    @Test
    public void confirmMarksExpectedItemNormalOnlyAfterServerSuccess() {
        CapturingInventoryRepository repository = new CapturingInventoryRepository();
        repository.itemPages.add(page("{\"total\":1,\"pageNum\":1,\"pageSize\":100,\"rows\":["
                + "{\"itemId\":101,\"assetCode\":\"A-001\"}]}"));
        repository.preview = preview("{\"totalCount\":1,\"confirmableCount\":1,\"unresolvedCount\":0,"
                + "\"duplicateCount\":0,\"expectedCount\":1,\"surplusCount\":0,\"normalCount\":1,\"rows\":["
                + "{\"rowNumber\":1,\"epcCode\":\"E20001\",\"matchType\":\"EXPECTED_ITEM\","
                + "\"confirmable\":true,\"proposedResult\":\"NORMAL\",\"item\":{\"itemId\":101}}]}");
        repository.confirm = confirm("{\"totalCount\":1,\"successCount\":1,\"failureCount\":0,\"rows\":["
                + "{\"rowNumber\":1,\"epcCode\":\"E20001\",\"success\":true,"
                + "\"matchType\":\"EXPECTED_ITEM\",\"item\":{\"itemId\":101}}],"
                + "\"task\":{\"taskId\":7,\"taskNo\":\"TASK-001\",\"warehouseId\":1,"
                + "\"totalCount\":1,\"inventoriedCount\":1,\"pendingCount\":0,\"normalCount\":1,"
                + "\"taskStatus\":\"INVENTORYING\"}}");
        FakeUhfScanner scanner = new FakeUhfScanner();
        InventoryExecuteViewModel viewModel = viewModel(repository, scanner);
        viewModel.initialize();

        viewModel.toggleScan();
        scanner.emit("E20001", -40);
        viewModel.toggleScan();
        viewModel.precheck();
        assertFalse(state(viewModel).getItemResultOverrides().containsKey(101L));

        viewModel.confirmSelected(null);

        InventoryExecuteUiState confirmed = state(viewModel);
        assertEquals("NORMAL", confirmed.getItemResultOverrides().get(101L));
        assertTrue(confirmed.getPreviewEpcByItemId().isEmpty());
        assertEquals(0, confirmed.getReadings().size());
    }

    @Test
    public void batchLossChangesOnlyLoadedBlankItemsToLossState() {
        CapturingInventoryRepository repository = new CapturingInventoryRepository();
        repository.itemPages.add(page("{\"total\":2,\"pageNum\":1,\"pageSize\":100,\"rows\":["
                + "{\"itemId\":101,\"assetCode\":\"A-001\"},"
                + "{\"itemId\":102,\"assetCode\":\"A-002\",\"inventoryResult\":\"NORMAL\"}]}"));
        repository.batchLoss = batchLoss("{\"affectedRows\":1,\"task\":{\"taskId\":7,"
                + "\"taskNo\":\"TASK-001\",\"warehouseId\":1,\"totalCount\":2,"
                + "\"inventoriedCount\":2,\"pendingCount\":0,\"lossCount\":1,"
                + "\"taskStatus\":\"INVENTORYING\"}}");
        InventoryExecuteViewModel viewModel = viewModel(repository);
        viewModel.initialize();

        viewModel.markPendingAsLoss(null);

        InventoryExecuteUiState state = state(viewModel);
        assertEquals("LOSS", state.getItemResultOverrides().get(101L));
        assertFalse(state.getItemResultOverrides().containsKey(102L));
    }

    private InventoryExecuteViewModel viewModel(CapturingInventoryRepository repository) {
        return viewModel(repository, new FakeUhfScanner());
    }

    private InventoryExecuteViewModel viewModel(CapturingInventoryRepository repository,
            FakeUhfScanner scanner) {
        return new InventoryExecuteViewModel(repository, new FixedCommonRepository(), scanner,
                7L, "TASK-001", true);
    }

    private InventoryExecuteUiState state(InventoryExecuteViewModel viewModel) {
        InventoryExecuteUiState state = viewModel.getUiState().getValue();
        assertTrue(state != null);
        return state;
    }

    private List<String> assetCodes(List<PdaInventoryItemDto> items) {
        List<String> values = new ArrayList<>();
        for (PdaInventoryItemDto item : items) {
            values.add(item.getAssetCode());
        }
        return values;
    }

    private PdaPageResultDto<PdaInventoryItemDto> page(String json) {
        Type type = new TypeToken<PdaPageResultDto<PdaInventoryItemDto>>() { }.getType();
        return new Gson().fromJson(json, type);
    }

    private PdaInventoryBatchScanDto preview(String json) {
        return new Gson().fromJson(json, PdaInventoryBatchScanDto.class);
    }

    private PdaInventoryBatchConfirmDto confirm(String json) {
        return new Gson().fromJson(json, PdaInventoryBatchConfirmDto.class);
    }

    private PdaInventoryBatchLossDto batchLoss(String json) {
        return new Gson().fromJson(json, PdaInventoryBatchLossDto.class);
    }

    private static final class FixedCommonRepository implements CommonRepository {
        @Override
        public RequestHandle bootstrap(RepositoryCallback<PdaBootstrapDto> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle dict(String dictType, RepositoryCallback<List<PdaDictItemDto>> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle warehouses(RepositoryCallback<List<PdaMasterDataDto>> callback) {
            callback.onSuccess(Collections.singletonList(new Gson().fromJson(
                    "{\"id\":1,\"name\":\"测试仓库\"}", PdaMasterDataDto.class)));
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle locations(Long warehouseId,
                RepositoryCallback<List<PdaMasterDataDto>> callback) {
            callback.onSuccess(Collections.emptyList());
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle categories(RepositoryCallback<List<PdaMasterDataDto>> callback) {
            return RequestHandle.NONE;
        }
    }

    private static final class CapturingInventoryRepository implements InventoryRepository {
        private final List<PdaPageResultDto<PdaInventoryItemDto>> itemPages = new ArrayList<>();
        private final List<Boolean> requestedInventoried = new ArrayList<>();
        private final List<Integer> requestedPageSize = new ArrayList<>();
        private PdaInventoryBatchScanDto preview;
        private PdaInventoryBatchConfirmDto confirm;
        private PdaInventoryBatchLossDto batchLoss;

        @Override
        public RequestHandle loadTasks(int pageNum, int pageSize,
                RepositoryCallback<PdaPageResultDto<PdaInventoryTaskDto>> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle loadTask(Long taskId, RepositoryCallback<PdaInventoryTaskDto> callback) {
            callback.onSuccess(new Gson().fromJson("{\"taskId\":7,\"taskNo\":\"TASK-001\","
                    + "\"warehouseId\":1,\"warehouseName\":\"测试仓库\",\"totalCount\":3,"
                    + "\"inventoriedCount\":0,\"pendingCount\":3,\"taskStatus\":\"INVENTORYING\"}",
                    PdaInventoryTaskDto.class));
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle loadItems(Long taskId, Boolean inventoried, String keyword,
                int pageNum, int pageSize,
                RepositoryCallback<PdaPageResultDto<PdaInventoryItemDto>> callback) {
            requestedInventoried.add(inventoried);
            requestedPageSize.add(pageSize);
            int index = pageNum - 1;
            callback.onSuccess(index >= 0 && index < itemPages.size()
                    ? itemPages.get(index) : page("{\"total\":0,\"pageNum\":1,\"pageSize\":100,\"rows\":[]}"));
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle loadSurpluses(Long taskId, int pageNum, int pageSize,
                RepositoryCallback<PdaPageResultDto<PdaInventorySurplusDto>> callback) {
            callback.onSuccess(new Gson().fromJson("{\"total\":0,\"pageNum\":1,"
                    + "\"pageSize\":100,\"rows\":[]}",
                    new TypeToken<PdaPageResultDto<PdaInventorySurplusDto>>() { }.getType()));
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle scan(Long taskId, String identifyType, String identifyValue,
                RepositoryCallback<PdaInventoryScanDto> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle batchScan(Long taskId, String taskNo, Long warehouseId,
                Long locationId, List<String> epcCodes,
                RepositoryCallback<PdaInventoryBatchScanDto> callback) {
            callback.onSuccess(preview);
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle batchConfirm(Long taskId, String taskNo, Long warehouseId,
                Long locationId, List<String> epcCodes, String remark,
                RepositoryCallback<PdaInventoryBatchConfirmDto> callback) {
            callback.onSuccess(confirm);
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle saveItemResult(Long taskId, Long itemId, String inventoryResult,
                Long warehouseId, Long locationId, String remark,
                RepositoryCallback<PdaInventoryItemDto> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle saveSurplus(Long taskId, String identifyMethod, String assetCode,
                String assetName, Long categoryId, String specModel, String brand,
                String epcCode, Long warehouseId, Long locationId, String remark,
                RepositoryCallback<PdaInventorySurplusDto> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle removeSurplus(Long taskId, Long surplusId,
                RepositoryCallback<Void> callback) {
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle markPendingAsLoss(Long taskId, String taskNo, String remark,
                RepositoryCallback<PdaInventoryBatchLossDto> callback) {
            callback.onSuccess(batchLoss);
            return RequestHandle.NONE;
        }

        @Override
        public RequestHandle submit(Long taskId, String taskNo, String remark,
                RepositoryCallback<PdaInventoryTaskDto> callback) {
            return RequestHandle.NONE;
        }

        private PdaPageResultDto<PdaInventoryItemDto> page(String json) {
            Type type = new TypeToken<PdaPageResultDto<PdaInventoryItemDto>>() { }.getType();
            return new Gson().fromJson(json, type);
        }
    }
}
