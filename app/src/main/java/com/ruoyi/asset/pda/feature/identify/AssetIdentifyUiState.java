package com.ruoyi.asset.pda.feature.identify;

import androidx.annotation.StringRes;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;

public final class AssetIdentifyUiState {
    private final String identifyType;
    private final UhfScanState scanState;
    private final boolean loading;
    private final String lastEpc;
    private final PdaAssetIdentifyDto asset;
    private final int errorTextResId;
    private final String errorMessage;

    private AssetIdentifyUiState(String identifyType, UhfScanState scanState,
            boolean loading, String lastEpc, PdaAssetIdentifyDto asset,
            int errorTextResId, String errorMessage) {
        this.identifyType = identifyType;
        this.scanState = scanState;
        this.loading = loading;
        this.lastEpc = lastEpc;
        this.asset = asset;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static AssetIdentifyUiState initial() {
        return new AssetIdentifyUiState(AssetRepository.IDENTIFY_TYPE_EPC,
                UhfScanState.IDLE, false, null, null, 0, null);
    }

    public AssetIdentifyUiState selectType(String type) {
        return new AssetIdentifyUiState(type, UhfScanState.IDLE,
                false, null, null, 0, null);
    }

    public AssetIdentifyUiState beginScan() {
        return new AssetIdentifyUiState(identifyType, UhfScanState.PROCESSING,
                false, null, null, 0, null);
    }

    public AssetIdentifyUiState scanning(UhfScanState state) {
        return new AssetIdentifyUiState(identifyType, state, false,
                lastEpc, asset, 0, null);
    }

    public AssetIdentifyUiState loading(String value) {
        return new AssetIdentifyUiState(identifyType, UhfScanState.IDLE,
                true, AssetRepository.IDENTIFY_TYPE_EPC.equals(identifyType) ? value : lastEpc,
                null, 0, null);
    }

    public AssetIdentifyUiState content(PdaAssetIdentifyDto value) {
        return new AssetIdentifyUiState(identifyType, UhfScanState.IDLE,
                false, lastEpc, value, 0, null);
    }

    public AssetIdentifyUiState error(String message) {
        return new AssetIdentifyUiState(identifyType, UhfScanState.IDLE,
                false, lastEpc, asset, 0, message);
    }

    public AssetIdentifyUiState error(@StringRes int messageResId) {
        return new AssetIdentifyUiState(identifyType, UhfScanState.IDLE,
                false, lastEpc, asset, messageResId, null);
    }

    public String getIdentifyType() { return identifyType; }
    public UhfScanState getScanState() { return scanState; }
    public boolean isLoading() { return loading; }
    public String getLastEpc() { return lastEpc; }
    public PdaAssetIdentifyDto getAsset() { return asset; }
    public int getErrorTextResId() { return errorTextResId; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isEpcMode() {
        return AssetRepository.IDENTIFY_TYPE_EPC.equals(identifyType);
    }
    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING;
    }
}
