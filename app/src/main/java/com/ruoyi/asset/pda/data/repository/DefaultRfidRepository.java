package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaRfidBindRequest;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagQueryRequest;
import com.ruoyi.asset.pda.data.dto.PdaRfidUnbindRequest;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchCreateRequest;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DefaultRfidRepository implements RfidRepository {
    private static final int MAX_BATCH_SIZE = 5000;
    private static final int MAX_EPC_LENGTH = 128;
    private static final int MAX_ASSET_CODE_LENGTH = 64;
    private static final int MAX_REMARK_LENGTH = 500;

    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultRfidRepository(PdaApiService apiService, ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("RFID Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle queryTag(String epcCode,
            RepositoryCallback<PdaRfidTagDto> callback) {
        String checkedEpc = normalizeEpc(epcCode);
        if (!check(callback, checkedEpc != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.queryRfidTag(
                new PdaRfidTagQueryRequest(checkedEpc)), true, callback);
    }

    @Override
    public RequestHandle batchCreate(List<String> epcCodes, String remark,
            RepositoryCallback<RfidTagBatchResultDto> callback) {
        if (!check(callback, epcCodes != null && !epcCodes.isEmpty()
                && epcCodes.size() <= MAX_BATCH_SIZE
                && (remark == null || remark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        List<String> copiedCodes = new ArrayList<>(epcCodes.size());
        for (String epcCode : epcCodes) {
            String checkedEpc = normalizeEpc(epcCode);
            if (checkedEpc == null) {
                callback.onError(callExecutor.protocolError());
                return RequestHandle.NONE;
            }
            copiedCodes.add(checkedEpc);
        }
        String checkedRemark = remark == null || remark.isEmpty() ? null : remark;
        return callExecutor.execute(apiService.batchCreateRfidTags(
                new RfidTagBatchCreateRequest(copiedCodes, checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle bind(String assetCode, String epcCode,
            RepositoryCallback<PdaRfidTagDto> callback) {
        String checkedAssetCode = assetCode == null ? null : assetCode.trim();
        String checkedEpc = normalizeEpc(epcCode);
        if (!check(callback, checkedAssetCode != null && !checkedAssetCode.isEmpty()
                && checkedAssetCode.length() <= MAX_ASSET_CODE_LENGTH
                && checkedEpc != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.bindRfid(
                new PdaRfidBindRequest(checkedAssetCode, checkedEpc)), true, callback);
    }

    @Override
    public RequestHandle unbind(Long tagId, RepositoryCallback<PdaRfidTagDto> callback) {
        if (!check(callback, tagId != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.unbindRfid(
                new PdaRfidUnbindRequest(tagId)), true, callback);
    }

    private boolean check(RepositoryCallback<?> callback, boolean valid) {
        if (callback == null) {
            throw new IllegalArgumentException("RFID 回调不能为空");
        }
        if (!valid) {
            callback.onError(callExecutor.protocolError());
        }
        return valid;
    }

    private String normalizeEpc(String epcCode) {
        if (epcCode == null) {
            return null;
        }
        String normalized = epcCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.length() > MAX_EPC_LENGTH
                ? null : normalized;
    }
}
