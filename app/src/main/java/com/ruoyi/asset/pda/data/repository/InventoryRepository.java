package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchLossDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;

import java.util.List;

public interface InventoryRepository {
    RequestHandle loadTasks(int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaInventoryTaskDto>> callback);

    RequestHandle loadTask(Long taskId, RepositoryCallback<PdaInventoryTaskDto> callback);

    RequestHandle loadItems(Long taskId, Boolean inventoried, String keyword,
            int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaInventoryItemDto>> callback);

    RequestHandle loadSurpluses(Long taskId, int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaInventorySurplusDto>> callback);

    RequestHandle scan(Long taskId, String identifyType, String identifyValue,
            RepositoryCallback<PdaInventoryScanDto> callback);

    RequestHandle batchScan(Long taskId, String taskNo, Long warehouseId, Long locationId,
            List<String> epcCodes, RepositoryCallback<PdaInventoryBatchScanDto> callback);

    RequestHandle batchConfirm(Long taskId, String taskNo, Long warehouseId, Long locationId,
            List<String> epcCodes, String remark,
            RepositoryCallback<PdaInventoryBatchConfirmDto> callback);

    RequestHandle saveItemResult(Long taskId, Long itemId, String inventoryResult,
            Long warehouseId, Long locationId, String remark,
            RepositoryCallback<PdaInventoryItemDto> callback);

    RequestHandle saveSurplus(Long taskId, String identifyMethod, String assetCode,
            String assetName, Long categoryId, String specModel, String brand,
            String epcCode, Long warehouseId, Long locationId, String remark,
            RepositoryCallback<PdaInventorySurplusDto> callback);

    RequestHandle removeSurplus(Long taskId, Long surplusId, RepositoryCallback<Void> callback);

    RequestHandle markPendingAsLoss(Long taskId, String taskNo, String remark,
            RepositoryCallback<PdaInventoryBatchLossDto> callback);

    RequestHandle submit(Long taskId, String taskNo, String remark,
            RepositoryCallback<PdaInventoryTaskDto> callback);
}
