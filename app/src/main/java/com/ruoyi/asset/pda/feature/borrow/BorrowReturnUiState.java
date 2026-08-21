package com.ruoyi.asset.pda.feature.borrow;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 借出/归还页面的单一状态快照，扫描和网络回调均通过此状态驱动 UI。 */
public final class BorrowReturnUiState {
    public enum Mode {
        ISSUE,
        RETURN
    }

    public enum Operation {
        NONE,
        PRECHECK,
        ASSET_CODE,
        SUBMIT
    }

    private final boolean initialLoading;
    private final boolean initialReady;
    private final boolean initialLoadFailed;
    private final boolean canIssueScan;
    private final boolean canIssueSubmit;
    private final boolean canReturnScan;
    private final boolean canReturnSubmit;
    private final Mode mode;
    private final Operation operation;
    private final UhfScanState scanState;
    private final String operatorName;
    private final String serverTime;
    private final List<PdaDictItemDto> borrowerTypes;
    private final String borrowerType;
    private final PdaMasterDataDto selectedBorrower;
    private final String externalOrgName;
    private final String internalContactPhone;
    private final String externalContactName;
    private final String externalContactPhone;
    private final String expectedReturnDate;
    private final List<BorrowAssetItem> assets;
    private final List<BorrowIssueItem> issues;
    private final int rawEpcCount;
    private final int duplicateReadCount;
    private final String latestEpc;
    private final String infoMessage;
    private final String errorMessage;
    private final PdaBorrowIssueBatchSubmitDto lastIssueSubmission;
    private final PdaBorrowReturnBatchSubmitDto lastReturnSubmission;
    private final int assetCodeClearVersion;
    private final int batchResetVersion;

    public BorrowReturnUiState(boolean initialLoading, boolean initialReady,
            boolean initialLoadFailed, boolean canIssueScan, boolean canIssueSubmit,
            boolean canReturnScan, boolean canReturnSubmit, Mode mode,
            Operation operation, UhfScanState scanState, String operatorName,
            String serverTime, List<PdaDictItemDto> borrowerTypes, String borrowerType,
            PdaMasterDataDto selectedBorrower, String externalOrgName,
            String internalContactPhone, String externalContactName,
            String externalContactPhone, String expectedReturnDate,
            List<BorrowAssetItem> assets, List<BorrowIssueItem> issues, int rawEpcCount,
            int duplicateReadCount, String latestEpc, String infoMessage,
            String errorMessage, PdaBorrowIssueBatchSubmitDto lastIssueSubmission,
            PdaBorrowReturnBatchSubmitDto lastReturnSubmission, int assetCodeClearVersion,
            int batchResetVersion) {
        this.initialLoading = initialLoading;
        this.initialReady = initialReady;
        this.initialLoadFailed = initialLoadFailed;
        this.canIssueScan = canIssueScan;
        this.canIssueSubmit = canIssueSubmit;
        this.canReturnScan = canReturnScan;
        this.canReturnSubmit = canReturnSubmit;
        this.mode = mode == null ? Mode.ISSUE : mode;
        this.operation = operation == null ? Operation.NONE : operation;
        this.scanState = scanState == null ? UhfScanState.IDLE : scanState;
        this.operatorName = operatorName;
        this.serverTime = serverTime;
        this.borrowerTypes = immutable(borrowerTypes);
        this.borrowerType = borrowerType;
        this.selectedBorrower = selectedBorrower;
        this.externalOrgName = externalOrgName;
        this.internalContactPhone = internalContactPhone;
        this.externalContactName = externalContactName;
        this.externalContactPhone = externalContactPhone;
        this.expectedReturnDate = expectedReturnDate;
        this.assets = immutable(assets);
        this.issues = immutable(issues);
        this.rawEpcCount = rawEpcCount;
        this.duplicateReadCount = duplicateReadCount;
        this.latestEpc = latestEpc;
        this.infoMessage = infoMessage;
        this.errorMessage = errorMessage;
        this.lastIssueSubmission = lastIssueSubmission;
        this.lastReturnSubmission = lastReturnSubmission;
        this.assetCodeClearVersion = assetCodeClearVersion;
        this.batchResetVersion = batchResetVersion;
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public boolean isInitialLoading() { return initialLoading; }
    public boolean isInitialReady() { return initialReady; }
    public boolean isInitialLoadFailed() { return initialLoadFailed; }
    public boolean isCanIssueScan() { return canIssueScan; }
    public boolean isCanIssueSubmit() { return canIssueSubmit; }
    public boolean isCanReturnScan() { return canReturnScan; }
    public boolean isCanReturnSubmit() { return canReturnSubmit; }
    public Mode getMode() { return mode; }
    public Operation getOperation() { return operation; }
    public UhfScanState getScanState() { return scanState; }
    public String getOperatorName() { return operatorName; }
    public String getServerTime() { return serverTime; }
    public List<PdaDictItemDto> getBorrowerTypes() { return borrowerTypes; }
    public String getBorrowerType() { return borrowerType; }
    public PdaMasterDataDto getSelectedBorrower() { return selectedBorrower; }
    public String getExternalOrgName() { return externalOrgName; }
    public String getInternalContactPhone() { return internalContactPhone; }
    public String getExternalContactName() { return externalContactName; }
    public String getExternalContactPhone() { return externalContactPhone; }
    public String getExpectedReturnDate() { return expectedReturnDate; }
    public List<BorrowAssetItem> getAssets() { return assets; }
    public List<BorrowIssueItem> getIssues() { return issues; }
    public int getRawEpcCount() { return rawEpcCount; }
    public int getDuplicateReadCount() { return duplicateReadCount; }
    public String getLatestEpc() { return latestEpc; }
    public String getInfoMessage() { return infoMessage; }
    public String getErrorMessage() { return errorMessage; }
    public PdaBorrowIssueBatchSubmitDto getLastIssueSubmission() { return lastIssueSubmission; }
    public PdaBorrowReturnBatchSubmitDto getLastReturnSubmission() { return lastReturnSubmission; }
    public int getAssetCodeClearVersion() { return assetCodeClearVersion; }
    public int getBatchResetVersion() { return batchResetVersion; }

    public boolean isBusy() { return operation != Operation.NONE; }

    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING;
    }

    public boolean hasPendingWork() {
        return !assets.isEmpty() || !issues.isEmpty() || rawEpcCount > 0
                || selectedBorrower != null || hasText(externalOrgName)
                || hasText(internalContactPhone) || hasText(externalContactName)
                || hasText(externalContactPhone) || hasText(expectedReturnDate);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
