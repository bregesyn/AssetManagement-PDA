package com.ruoyi.asset.pda.feature.inventory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchLossDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.repository.InventoryRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;

import org.junit.Test;
import org.junit.Rule;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InventoryTaskListViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    @Test
    public void splitsKnownStatesAndKeepsUnknownStateReadOnlyInServerOrder() {
        CapturingRepository repository = new CapturingRepository();
        InventoryTaskListViewModel viewModel = new InventoryTaskListViewModel(repository);

        viewModel.initialize();

        InventoryTaskListUiState state = viewModel.getUiState().getValue();
        assertFalse(state == null);
        assertEquals(100, repository.requestedPageSize);
        assertEquals(Arrays.asList("TASK-ISSUED", "TASK-INVENTORYING"),
                taskNos(state.getActionableTasks()));
        assertEquals(Arrays.asList("TASK-UNKNOWN", "TASK-PENDING"),
                taskNos(state.getReadonlyTasks()));
        assertEquals("UNRECOGNIZED_STATUS", state.getReadonlyTasks().get(0).getTaskStatus());
    }

    private List<String> taskNos(List<PdaInventoryTaskDto> tasks) {
        return tasks.stream().map(PdaInventoryTaskDto::getTaskNo).collect(java.util.stream.Collectors.toList());
    }

    private static final class CapturingRepository implements InventoryRepository {
        private int requestedPageSize;

        @Override
        public RequestHandle loadTasks(int pageNum, int pageSize,
                RepositoryCallback<PdaPageResultDto<PdaInventoryTaskDto>> callback) {
            requestedPageSize = pageSize;
            Type type = new TypeToken<PdaPageResultDto<PdaInventoryTaskDto>>() { }.getType();
            PdaPageResultDto<PdaInventoryTaskDto> page = new Gson().fromJson(
                    "{\"total\":4,\"pageNum\":1,\"pageSize\":100,\"rows\":["
                            + "{\"taskNo\":\"TASK-ISSUED\",\"taskStatus\":\"ISSUED\"},"
                            + "{\"taskNo\":\"TASK-UNKNOWN\",\"taskStatus\":\"UNRECOGNIZED_STATUS\"},"
                            + "{\"taskNo\":\"TASK-INVENTORYING\",\"taskStatus\":\"INVENTORYING\"},"
                            + "{\"taskNo\":\"TASK-PENDING\",\"taskStatus\":\"PENDING_RESULT_CONFIRM\"}]}",
                  type);
            callback.onSuccess(page);
            return RequestHandle.NONE;
        }

        @Override public RequestHandle loadTask(Long taskId, RepositoryCallback<PdaInventoryTaskDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle loadItems(Long taskId, Boolean inventoried, String keyword, int pageNum, int pageSize, RepositoryCallback<PdaPageResultDto<PdaInventoryItemDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle loadSurpluses(Long taskId, int pageNum, int pageSize, RepositoryCallback<PdaPageResultDto<PdaInventorySurplusDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle scan(Long taskId, String identifyType, String identifyValue, RepositoryCallback<PdaInventoryScanDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle batchScan(Long taskId, String taskNo, Long warehouseId, Long locationId, List<String> epcCodes, RepositoryCallback<PdaInventoryBatchScanDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle batchConfirm(Long taskId, String taskNo, Long warehouseId, Long locationId, List<String> epcCodes, String remark, RepositoryCallback<PdaInventoryBatchConfirmDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle saveItemResult(Long taskId, Long itemId, String inventoryResult, Long warehouseId, Long locationId, String remark, RepositoryCallback<PdaInventoryItemDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle saveSurplus(Long taskId, String identifyMethod, String assetCode, String assetName, Long categoryId, String specModel, String brand, String epcCode, Long warehouseId, Long locationId, String remark, RepositoryCallback<PdaInventorySurplusDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle removeSurplus(Long taskId, Long surplusId, RepositoryCallback<Void> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle markPendingAsLoss(Long taskId, String taskNo, String remark, RepositoryCallback<PdaInventoryBatchLossDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle submit(Long taskId, String taskNo, String remark, RepositoryCallback<PdaInventoryTaskDto> callback) { return RequestHandle.NONE; }
    }
}
