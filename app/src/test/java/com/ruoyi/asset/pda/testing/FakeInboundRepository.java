package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;
import com.ruoyi.asset.pda.data.repository.InboundRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

public final class FakeInboundRepository implements InboundRepository {
    private RepositoryCallback<PdaInboundEligibilityDto> eligibilityCallback;
    private RepositoryCallback<PdaInboundBatchCheckDto> batchCheckCallback;
    private RepositoryCallback<PdaInboundBatchConfirmDto> confirmCallback;
    private String lastEpcCode;
    private String lastAssetCode;
    private List<String> lastBatchEpcs;
    private Long lastWarehouseId;
    private Long lastLocationId;
    private List<Long> lastAssetIds;
    private String lastRemark;
    private int eligibilityCount;
    private int batchCheckCount;
    private int confirmCount;

    @Override
    public RequestHandle queryByEpc(String epcCode,
            RepositoryCallback<PdaInboundEligibilityDto> callback) {
        eligibilityCount++;
        lastEpcCode = epcCode;
        eligibilityCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle queryByAssetCode(String assetCode,
            RepositoryCallback<PdaInboundEligibilityDto> callback) {
        eligibilityCount++;
        lastAssetCode = assetCode;
        eligibilityCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchCheck(List<String> epcCodes,
            RepositoryCallback<PdaInboundBatchCheckDto> callback) {
        batchCheckCount++;
        lastBatchEpcs = new ArrayList<>(epcCodes);
        batchCheckCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchConfirm(Long warehouseId, Long locationId,
            List<Long> assetIds, String remark,
            RepositoryCallback<PdaInboundBatchConfirmDto> callback) {
        confirmCount++;
        lastWarehouseId = warehouseId;
        lastLocationId = locationId;
        lastAssetIds = new ArrayList<>(assetIds);
        lastRemark = remark;
        confirmCallback = callback;
        return RequestHandle.NONE;
    }

    public void completeEligibility(PdaInboundEligibilityDto value) {
        eligibilityCallback.onSuccess(value);
    }

    public void failEligibility(ApiErrorMapper.ApiError error) {
        eligibilityCallback.onError(error);
    }

    public void completeBatchCheck(PdaInboundBatchCheckDto value) {
        batchCheckCallback.onSuccess(value);
    }

    public void failBatchCheck(ApiErrorMapper.ApiError error) {
        batchCheckCallback.onError(error);
    }

    public void completeConfirm(PdaInboundBatchConfirmDto value) {
        confirmCallback.onSuccess(value);
    }

    public void failConfirm(ApiErrorMapper.ApiError error) {
        confirmCallback.onError(error);
    }

    public String getLastEpcCode() {
        return lastEpcCode;
    }

    public String getLastAssetCode() {
        return lastAssetCode;
    }

    public List<String> getLastBatchEpcs() {
        return lastBatchEpcs;
    }

    public Long getLastWarehouseId() {
        return lastWarehouseId;
    }

    public Long getLastLocationId() {
        return lastLocationId;
    }

    public List<Long> getLastAssetIds() {
        return lastAssetIds;
    }

    public String getLastRemark() {
        return lastRemark;
    }

    public int getEligibilityCount() {
        return eligibilityCount;
    }

    public int getBatchCheckCount() {
        return batchCheckCount;
    }

    public int getConfirmCount() {
        return confirmCount;
    }
}
