package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchLossDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemResultRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskActionRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 盘点协议边界集中在此处，页面不会绕过校验直接拼 Retrofit 请求。 */
public final class DefaultInventoryRepository implements InventoryRepository {
    public static final int TASK_PAGE_SIZE = 100;
    public static final int MAX_BATCH_SIZE = 500;
    public static final int MAX_EPC_LENGTH = 128;
    public static final int MAX_REMARK_LENGTH = 500;
    public static final String RESULT_NORMAL = "NORMAL";
    public static final String RESULT_LOSS = "LOSS";
    public static final String IDENTIFY_EPC = "EPC";
    public static final String IDENTIFY_ASSET_CODE = "ASSET_CODE";
    public static final String IDENTIFY_MANUAL = "MANUAL";

    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultInventoryRepository(PdaApiService apiService, ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("盘点 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle loadTasks(int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaInventoryTaskDto>> callback) {
        if (!check(callback, pageNum >= 1 && pageSize >= 1 && pageSize <= TASK_PAGE_SIZE)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryTasks(pageNum, pageSize), true, callback);
    }

    @Override
    public RequestHandle loadTask(Long taskId, RepositoryCallback<PdaInventoryTaskDto> callback) {
        if (!check(callback, taskId != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryTask(taskId), true, callback);
    }

    @Override
    public RequestHandle loadItems(Long taskId, Boolean inventoried, String keyword,
            int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaInventoryItemDto>> callback) {
        String checkedKeyword = trim(keyword);
        if (!check(callback, taskId != null && pageNum >= 1
                && pageSize >= 1 && pageSize <= TASK_PAGE_SIZE)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryItems(taskId, inventoried,
                checkedKeyword, pageNum, pageSize), true, callback);
    }

    @Override
    public RequestHandle loadSurpluses(Long taskId, int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaInventorySurplusDto>> callback) {
        if (!check(callback, taskId != null && pageNum >= 1
                && pageSize >= 1 && pageSize <= TASK_PAGE_SIZE)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventorySurpluses(taskId, pageNum, pageSize),
                true, callback);
    }

    @Override
    public RequestHandle scan(Long taskId, String identifyType, String identifyValue,
            RepositoryCallback<PdaInventoryScanDto> callback) {
        String type = trim(identifyType);
        String value = type != null && IDENTIFY_EPC.equals(type)
                ? normalizeEpc(identifyValue) : trim(identifyValue);
        if (!check(callback, taskId != null
                && (IDENTIFY_EPC.equals(type) || IDENTIFY_ASSET_CODE.equals(type))
                && hasText(value))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryScan(taskId,
                new PdaInventoryScanRequestDto(type, value)), true, callback);
    }

    @Override
    public RequestHandle batchScan(Long taskId, String taskNo, Long warehouseId,
            Long locationId, List<String> epcCodes,
            RepositoryCallback<PdaInventoryBatchScanDto> callback) {
        List<String> codes = normalizeBatch(epcCodes);
        String checkedTaskNo = trim(taskNo);
        if (!check(callback, taskId != null && hasText(checkedTaskNo)
                && warehouseId != null && codes != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryBatchScan(taskId,
                new PdaInventoryBatchScanRequestDto(checkedTaskNo, warehouseId,
                        locationId, codes)), true, callback);
    }

    @Override
    public RequestHandle batchConfirm(Long taskId, String taskNo, Long warehouseId,
            Long locationId, List<String> epcCodes, String remark,
            RepositoryCallback<PdaInventoryBatchConfirmDto> callback) {
        List<String> codes = normalizeBatch(epcCodes);
        String checkedTaskNo = trim(taskNo);
        String checkedRemark = trim(remark);
        if (!check(callback, taskId != null && hasText(checkedTaskNo)
                && warehouseId != null && codes != null
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryBatchConfirm(taskId,
                new PdaInventoryBatchConfirmRequestDto(checkedTaskNo, warehouseId,
                        locationId, codes, checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle saveItemResult(Long taskId, Long itemId, String inventoryResult,
            Long warehouseId, Long locationId, String remark,
            RepositoryCallback<PdaInventoryItemDto> callback) {
        String result = trim(inventoryResult);
        String checkedRemark = trim(remark);
        boolean validResult = RESULT_NORMAL.equals(result) || RESULT_LOSS.equals(result);
        boolean validLocation = RESULT_NORMAL.equals(result)
                ? warehouseId != null : warehouseId == null && locationId == null;
        if (!check(callback, taskId != null && itemId != null && validResult && validLocation
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        // 正常结果保留现场仓位审计，盘亏表达未发现实物，因此不能携带现场仓位。
        return callExecutor.execute(apiService.inventoryItemResult(taskId, itemId,
                new PdaInventoryItemResultRequestDto(result, warehouseId, locationId,
                        checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle saveSurplus(Long taskId, String identifyMethod, String assetCode,
            String assetName, Long categoryId, String specModel, String brand,
            String epcCode, Long warehouseId, Long locationId, String remark,
            RepositoryCallback<PdaInventorySurplusDto> callback) {
        String method = trim(identifyMethod);
        String checkedAssetCode = trim(assetCode);
        String checkedAssetName = trim(assetName);
        String checkedEpc = normalizeEpc(epcCode);
        String checkedRemark = trim(remark);
        boolean identityValid = IDENTIFY_EPC.equals(method) ? checkedEpc != null
                : IDENTIFY_ASSET_CODE.equals(method) ? hasText(checkedAssetCode)
                : IDENTIFY_MANUAL.equals(method) && hasText(checkedAssetName)
                        && checkedEpc == null && !hasText(checkedAssetCode);
        if (!check(callback, taskId != null && identityValid && warehouseId != null
                && locationId != null && (checkedAssetCode == null || checkedAssetCode.length() <= 64)
                && (checkedAssetName == null || checkedAssetName.length() <= 128)
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventorySaveSurplus(taskId,
                new PdaInventorySurplusRequestDto(method, checkedAssetCode, checkedAssetName,
                        categoryId, trim(specModel), trim(brand), checkedEpc,
                        warehouseId, locationId, checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle removeSurplus(Long taskId, Long surplusId,
            RepositoryCallback<Void> callback) {
        if (!check(callback, taskId != null && surplusId != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryRemoveSurplus(taskId, surplusId), false,
                callback);
    }

    @Override
    public RequestHandle markPendingAsLoss(Long taskId, String taskNo, String remark,
            RepositoryCallback<PdaInventoryBatchLossDto> callback) {
        String checkedTaskNo = trim(taskNo);
        String checkedRemark = trim(remark);
        if (!check(callback, taskId != null && hasText(checkedTaskNo)
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventoryMarkPendingLoss(taskId,
                new PdaInventoryTaskActionRequestDto(checkedTaskNo, checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle submit(Long taskId, String taskNo, String remark,
            RepositoryCallback<PdaInventoryTaskDto> callback) {
        String checkedTaskNo = trim(taskNo);
        String checkedRemark = trim(remark);
        if (!check(callback, taskId != null && hasText(checkedTaskNo)
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inventorySubmit(taskId,
                new PdaInventoryTaskActionRequestDto(checkedTaskNo, checkedRemark)), true, callback);
    }

    private List<String> normalizeBatch(List<String> epcCodes) {
        if (epcCodes == null || epcCodes.isEmpty() || epcCodes.size() > MAX_BATCH_SIZE) {
            return null;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String epcCode : epcCodes) {
            String normalized = normalizeEpc(epcCode);
            if (normalized == null) {
                return null;
            }
            unique.add(normalized);
        }
        return unique.isEmpty() || unique.size() > MAX_BATCH_SIZE
                ? null : new ArrayList<>(unique);
    }

    private String normalizeEpc(String epcCode) {
        if (epcCode == null) {
            return null;
        }
        String normalized = epcCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > MAX_EPC_LENGTH) {
            return null;
        }
        try {
            return UhfTagReading.normalizeEpc(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private boolean check(RepositoryCallback<?> callback, boolean valid) {
        if (callback == null) {
            throw new IllegalArgumentException("盘点回调不能为空");
        }
        if (!valid) {
            callback.onError(callExecutor.protocolError());
        }
        return valid;
    }
}
