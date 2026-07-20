package com.ruoyi.asset.pda.feature.rfid;

import androidx.annotation.StringRes;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;

public final class RfidUnbindUiState {
    private final UhfScanState scanState;
    private final PdaRfidTagDto tag;
    private final boolean loadingTag;
    private final boolean submitting;
    private final boolean verifying;
    private final boolean success;
    private final int errorTextResId;
    private final String errorMessage;

    private RfidUnbindUiState(UhfScanState scanState, PdaRfidTagDto tag,
            boolean loadingTag, boolean submitting, boolean verifying,
            boolean success, int errorTextResId, String errorMessage) {
        this.scanState = scanState;
        this.tag = tag;
        this.loadingTag = loadingTag;
        this.submitting = submitting;
        this.verifying = verifying;
        this.success = success;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static RfidUnbindUiState initial() {
        return new RfidUnbindUiState(UhfScanState.IDLE, null,
                false, false, false, false, 0, null);
    }

    public RfidUnbindUiState scanning(UhfScanState state) {
        return copy(state, tag, false, false, false, false, 0, null);
    }

    public RfidUnbindUiState beginTagScan() {
        return copy(UhfScanState.PROCESSING, null, false, false,
                false, false, 0, null);
    }

    public RfidUnbindUiState loadingTag() {
        return copy(UhfScanState.IDLE, null, true, false, false, false, 0, null);
    }

    public RfidUnbindUiState tag(PdaRfidTagDto value) {
        return copy(UhfScanState.IDLE, value, false, false, false, false, 0, null);
    }

    public RfidUnbindUiState submitting() {
        return copy(UhfScanState.IDLE, tag, false, true, false, false, 0, null);
    }

    public RfidUnbindUiState verifying() {
        return copy(UhfScanState.IDLE, tag, false, false, true, false, 0, null);
    }

    public RfidUnbindUiState success(PdaRfidTagDto value) {
        return copy(UhfScanState.IDLE, value, false, false, false, true, 0, null);
    }

    public RfidUnbindUiState error(@StringRes int messageResId) {
        return copy(UhfScanState.IDLE, tag, false, false, false, false,
                messageResId, null);
    }

    public RfidUnbindUiState error(String message) {
        return copy(UhfScanState.IDLE, tag, false, false, false, false, 0, message);
    }

    /** 结果无法查询确认时丢弃旧标签事实，强制重新扫描，避免重复写入。 */
    public RfidUnbindUiState unknown(@StringRes int messageResId) {
        return copy(UhfScanState.IDLE, null, false, false, false, false,
                messageResId, null);
    }

    private RfidUnbindUiState copy(UhfScanState state, PdaRfidTagDto value,
            boolean loading, boolean isSubmitting, boolean isVerifying,
            boolean isSuccess, int errorResId, String error) {
        return new RfidUnbindUiState(state, value, loading, isSubmitting,
                isVerifying, isSuccess, errorResId, error);
    }

    public UhfScanState getScanState() { return scanState; }
    public PdaRfidTagDto getTag() { return tag; }
    public boolean isLoadingTag() { return loadingTag; }
    public boolean isSubmitting() { return submitting; }
    public boolean isVerifying() { return verifying; }
    public boolean isSuccess() { return success; }
    public int getErrorTextResId() { return errorTextResId; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isBusy() { return loadingTag || submitting || verifying; }
    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING;
    }
    public boolean canUnbind() {
        return !isBusy() && !isScanning() && !success && tag != null
                && tag.getTagId() != null && tag.isRfidBound();
    }
}
