package com.ruoyi.asset.pda.feature.rfid;

import androidx.annotation.StringRes;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;

public final class RfidBindUiState {
    private final UhfScanState scanState;
    private final PdaAssetIdentifyDto asset;
    private final PdaRfidTagDto tag;
    private final boolean loadingAsset;
    private final boolean loadingTag;
    private final boolean submitting;
    private final boolean verifying;
    private final boolean success;
    private final int errorTextResId;
    private final String errorMessage;

    private RfidBindUiState(UhfScanState scanState, PdaAssetIdentifyDto asset,
            PdaRfidTagDto tag, boolean loadingAsset, boolean loadingTag,
            boolean submitting, boolean verifying, boolean success,
            int errorTextResId, String errorMessage) {
        this.scanState = scanState;
        this.asset = asset;
        this.tag = tag;
        this.loadingAsset = loadingAsset;
        this.loadingTag = loadingTag;
        this.submitting = submitting;
        this.verifying = verifying;
        this.success = success;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static RfidBindUiState initial() {
        return new RfidBindUiState(UhfScanState.IDLE, null, null,
                false, false, false, false, false, 0, null);
    }

    public RfidBindUiState scanning(UhfScanState state) {
        return copy(state, asset, tag, false, false, false, false,
                false, 0, null);
    }

    public RfidBindUiState beginTagScan() {
        return copy(UhfScanState.PROCESSING, asset, null, false, false,
                false, false, false, 0, null);
    }

    public RfidBindUiState loadingAsset() {
        return copy(UhfScanState.IDLE, null, null, true, false,
                false, false, false, 0, null);
    }

    public RfidBindUiState clearSelection() {
        return copy(UhfScanState.IDLE, null, null, false, false,
                false, false, false, 0, null);
    }

    public RfidBindUiState asset(PdaAssetIdentifyDto value) {
        return copy(UhfScanState.IDLE, value, tag, false, false,
                false, false, false, 0, null);
    }

    public RfidBindUiState loadingTag() {
        return copy(UhfScanState.IDLE, asset, null, false, true,
                false, false, false, 0, null);
    }

    public RfidBindUiState tag(PdaRfidTagDto value) {
        return copy(UhfScanState.IDLE, asset, value, false, false,
                false, false, false, 0, null);
    }

    public RfidBindUiState submitting() {
        return copy(UhfScanState.IDLE, asset, tag, false, false,
                true, false, false, 0, null);
    }

    public RfidBindUiState verifying() {
        return copy(UhfScanState.IDLE, asset, tag, false, false,
                false, true, false, 0, null);
    }

    public RfidBindUiState success(PdaRfidTagDto value) {
        return copy(UhfScanState.IDLE, asset, value, false, false,
                false, false, true, 0, null);
    }

    public RfidBindUiState error(@StringRes int messageResId) {
        return copy(UhfScanState.IDLE, asset, tag, false, false,
                false, false, false, messageResId, null);
    }

    public RfidBindUiState error(String message) {
        return copy(UhfScanState.IDLE, asset, tag, false, false,
                false, false, false, 0, message);
    }

    /** 结果无法查询确认时丢弃旧标签事实，强制重新扫描，避免重复写入。 */
    public RfidBindUiState unknown(@StringRes int messageResId) {
        return copy(UhfScanState.IDLE, asset, null, false, false,
                false, false, false, messageResId, null);
    }

    private RfidBindUiState copy(UhfScanState state, PdaAssetIdentifyDto currentAsset,
            PdaRfidTagDto currentTag, boolean isLoadingAsset, boolean isLoadingTag,
            boolean isSubmitting, boolean isVerifying, boolean isSuccess,
            int errorResId, String error) {
        return new RfidBindUiState(state, currentAsset, currentTag,
                isLoadingAsset, isLoadingTag, isSubmitting, isVerifying,
                isSuccess, errorResId, error);
    }

    public UhfScanState getScanState() { return scanState; }
    public PdaAssetIdentifyDto getAsset() { return asset; }
    public PdaRfidTagDto getTag() { return tag; }
    public boolean isLoadingAsset() { return loadingAsset; }
    public boolean isLoadingTag() { return loadingTag; }
    public boolean isSubmitting() { return submitting; }
    public boolean isVerifying() { return verifying; }
    public boolean isSuccess() { return success; }
    public int getErrorTextResId() { return errorTextResId; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isBusy() {
        return loadingAsset || loadingTag || submitting || verifying;
    }
    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING;
    }
    public boolean canBind() {
        return !isBusy() && !isScanning() && !success && asset != null && tag != null
                && !asset.isRfidBound() && tag.isNormalAndUnbound();
    }
}
