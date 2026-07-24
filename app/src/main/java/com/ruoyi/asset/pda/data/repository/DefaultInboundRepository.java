package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 入库协议边界集中校验，页面只传业务输入，不直接拼 Retrofit 请求。 */
public final class DefaultInboundRepository implements InboundRepository {
    public static final int MAX_BATCH_SIZE = 100;
    public static final int MAX_EPC_LENGTH = 128;
    public static final int MAX_ASSET_CODE_LENGTH = 64;
    public static final int MAX_REMARK_LENGTH = 500;

    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultInboundRepository(PdaApiService apiService,
            ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("入库 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle queryByEpc(String epcCode,
            RepositoryCallback<PdaInboundEligibilityDto> callback) {
        String checkedEpc = normalizeEpc(epcCode);
        if (!check(callback, checkedEpc != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inboundAsset(checkedEpc, null),
                true, callback);
    }

    @Override
    public RequestHandle queryByAssetCode(String assetCode,
            RepositoryCallback<PdaInboundEligibilityDto> callback) {
        String checkedCode = trim(assetCode);
        if (!check(callback, checkedCode != null
                && checkedCode.length() <= MAX_ASSET_CODE_LENGTH)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inboundAsset(null, checkedCode),
                true, callback);
    }

    @Override
    public RequestHandle batchCheck(List<String> epcCodes,
            RepositoryCallback<PdaInboundBatchCheckDto> callback) {
        List<String> checkedCodes = normalizeBatch(epcCodes);
        if (!check(callback, checkedCodes != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inboundBatchCheck(
                new PdaInboundBatchCheckRequestDto(checkedCodes)), true, callback);
    }

    @Override
    public RequestHandle batchConfirm(Long warehouseId, Long locationId,
            List<Long> assetIds, String remark,
            RepositoryCallback<PdaInboundBatchConfirmDto> callback) {
        List<Long> checkedAssetIds = normalizeAssetIds(assetIds);
        String checkedRemark = trim(remark);
        if (!check(callback, warehouseId != null && locationId != null
                && checkedAssetIds != null
                && (checkedRemark == null
                || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.inboundBatchConfirm(
                new PdaInboundBatchConfirmRequestDto(warehouseId, locationId,
                        checkedAssetIds, checkedRemark)), true, callback);
    }

    private List<String> normalizeBatch(List<String> epcCodes) {
        if (epcCodes == null || epcCodes.isEmpty()
                || epcCodes.size() > MAX_BATCH_SIZE) {
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

    private List<Long> normalizeAssetIds(List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()
                || assetIds.size() > MAX_BATCH_SIZE) {
            return null;
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long assetId : assetIds) {
            if (assetId == null || assetId < 1L || !unique.add(assetId)) {
                return null;
            }
        }
        return new ArrayList<>(unique);
    }

    private String normalizeEpc(String epcCode) {
        String checked = trim(epcCode);
        if (checked == null || checked.length() > MAX_EPC_LENGTH) {
            return null;
        }
        try {
            return UhfTagReading.normalizeEpc(checked);
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

    private boolean check(RepositoryCallback<?> callback, boolean valid) {
        if (callback == null) {
            throw new IllegalArgumentException("入库回调不能为空");
        }
        if (!valid) {
            callback.onError(callExecutor.protocolError());
        }
        return valid;
    }
}
