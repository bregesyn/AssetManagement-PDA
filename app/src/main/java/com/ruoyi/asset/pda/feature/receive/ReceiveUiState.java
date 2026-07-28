package com.ruoyi.asset.pda.feature.receive;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 领用屏幕的单一状态快照，避免扫描回调和网络回调各自修改 View。 */
public final class ReceiveUiState {
    public enum Operation {
        NONE,
        PRECHECK,
        ASSET_CODE,
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
    private final PdaMasterDataDto selectedRecipient;
    private final List<ReceiveAssetItem> assets;
    private final List<ReceiveIssueItem> issues;
    private final int rawEpcCount;
    private final int duplicateReadCount;
    private final String latestEpc;
    private final String infoMessage;
    private final String errorMessage;
    private final PdaReceiveBatchConfirmDto lastConfirmation;
    private final int assetCodeClearVersion;
    private final int batchResetVersion;

    public ReceiveUiState(boolean initialLoading, boolean initialReady,
            boolean initialLoadFailed, boolean canConfirm, Operation operation,
            UhfScanState scanState, String operatorName, String serverTime,
            PdaMasterDataDto selectedRecipient, List<ReceiveAssetItem> assets,
            List<ReceiveIssueItem> issues, int rawEpcCount,
            int duplicateReadCount, String latestEpc, String infoMessage,
            String errorMessage, PdaReceiveBatchConfirmDto lastConfirmation,
            int assetCodeClearVersion, int batchResetVersion) {
        this.initialLoading = initialLoading;
        this.initialReady = initialReady;
        this.initialLoadFailed = initialLoadFailed;
        this.canConfirm = canConfirm;
        this.operation = operation == null ? Operation.NONE : operation;
        this.scanState = scanState == null ? UhfScanState.IDLE : scanState;
        this.operatorName = operatorName;
        this.serverTime = serverTime;
        this.selectedRecipient = selectedRecipient;
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

    public PdaMasterDataDto getSelectedRecipient() {
        return selectedRecipient;
    }

    public List<ReceiveAssetItem> getAssets() {
        return assets;
    }

    public List<ReceiveIssueItem> getIssues() {
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

    public PdaReceiveBatchConfirmDto getLastConfirmation() {
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
