package com.ruoyi.asset.pda.feature.inbound;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InboundUiState {
    public enum Operation {
        NONE,
        LOCATION,
        PRECHECK,
        ASSET_QUERY,
        CONFIRM
    }

    private final boolean initialLoading;
    private final boolean initialReady;
    private final boolean initialLoadFailed;
    private final boolean canConfirm;
    private final Operation operation;
    private final UhfScanState scanState;
    private final String operatorName;
    private final String serverTime;
    private final List<PdaMasterDataDto> warehouses;
    private final List<PdaMasterDataDto> locations;
    private final Long selectedWarehouseId;
    private final Long selectedLocationId;
    private final List<InboundAssetItem> assets;
    private final List<InboundIssueItem> issues;
    private final int rawEpcCount;
    private final int duplicateReadCount;
    private final String latestEpc;
    private final String infoMessage;
    private final String errorMessage;
    private final PdaInboundBatchConfirmDto lastConfirmation;
    private final int assetCodeClearVersion;
    private final int batchResetVersion;

    public InboundUiState(boolean initialLoading, boolean initialReady,
            boolean initialLoadFailed, boolean canConfirm,
            Operation operation, UhfScanState scanState, String operatorName,
            String serverTime, List<PdaMasterDataDto> warehouses,
            List<PdaMasterDataDto> locations, Long selectedWarehouseId,
            Long selectedLocationId, List<InboundAssetItem> assets,
            List<InboundIssueItem> issues, int rawEpcCount,
            int duplicateReadCount, String latestEpc, String infoMessage,
            String errorMessage, PdaInboundBatchConfirmDto lastConfirmation,
            int assetCodeClearVersion, int batchResetVersion) {
        this.initialLoading = initialLoading;
        this.initialReady = initialReady;
        this.initialLoadFailed = initialLoadFailed;
        this.canConfirm = canConfirm;
        this.operation = operation == null ? Operation.NONE : operation;
        this.scanState = scanState == null ? UhfScanState.IDLE : scanState;
        this.operatorName = operatorName;
        this.serverTime = serverTime;
        this.warehouses = immutable(warehouses);
        this.locations = immutable(locations);
        this.selectedWarehouseId = selectedWarehouseId;
        this.selectedLocationId = selectedLocationId;
        this.assets = immutable(assets);
        this.issues = immutable(issues);
        this.rawEpcCount = rawEpcCount;
        this.duplicateReadCount = duplicateReadCount;
        this.latestEpc = latestEpc;
        this.infoMessage = infoMessage;
        this.errorMessage = errorMessage;
        this.lastConfirmation = lastConfirmation;
        this.assetCodeClearVersion = assetCodeClearVersion;
        this.batchResetVersion = batchResetVersion;
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public boolean isInitialLoading() {
        return initialLoading;
    }

    public boolean isInitialReady() {
        return initialReady;
    }

    public boolean isInitialLoadFailed() {
        return initialLoadFailed;
    }

    public boolean isCanConfirm() {
        return canConfirm;
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isBusy() {
        return operation != Operation.NONE;
    }

    public UhfScanState getScanState() {
        return scanState;
    }

    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING
                || scanState == UhfScanState.PROCESSING;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getServerTime() {
        return serverTime;
    }

    public List<PdaMasterDataDto> getWarehouses() {
        return warehouses;
    }

    public List<PdaMasterDataDto> getLocations() {
        return locations;
    }

    public Long getSelectedWarehouseId() {
        return selectedWarehouseId;
    }

    public Long getSelectedLocationId() {
        return selectedLocationId;
    }

    public List<InboundAssetItem> getAssets() {
        return assets;
    }

    public List<InboundIssueItem> getIssues() {
        return issues;
    }

    public int getRawEpcCount() {
        return rawEpcCount;
    }

    public int getDuplicateReadCount() {
        return duplicateReadCount;
    }

    public String getLatestEpc() {
        return latestEpc;
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public PdaInboundBatchConfirmDto getLastConfirmation() {
        return lastConfirmation;
    }

    public int getAssetCodeClearVersion() {
        return assetCodeClearVersion;
    }

    public int getBatchResetVersion() {
        return batchResetVersion;
    }

    public boolean hasPendingWork() {
        return !assets.isEmpty() || !issues.isEmpty() || rawEpcCount > 0;
    }
}
