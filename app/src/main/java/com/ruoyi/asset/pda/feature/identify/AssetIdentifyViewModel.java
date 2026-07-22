package com.ruoyi.asset.pda.feature.identify;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.BaseUhfViewModel;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

public final class AssetIdentifyViewModel extends BaseUhfViewModel {
    private static final int MAX_EPC_LENGTH = 128;

    private final AssetRepository assetRepository;
    private final MutableLiveData<AssetIdentifyUiState> uiState =
            new MutableLiveData<>(AssetIdentifyUiState.initial());
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int operationVersion;

    public AssetIdentifyViewModel(AssetRepository assetRepository, UhfScanner scanner) {
        super(scanner);
        if (assetRepository == null) {
            throw new IllegalArgumentException("AssetRepository 不能为空");
        }
        this.assetRepository = assetRepository;
    }

    public LiveData<AssetIdentifyUiState> getUiState() {
        return uiState;
    }

    public void selectIdentifyType(String type) {
        AssetIdentifyUiState current = state();
        if (current.isLoading() || current.getIdentifyType().equals(type)) {
            return;
        }
        if (!AssetRepository.IDENTIFY_TYPE_EPC.equals(type)
                && !AssetRepository.IDENTIFY_TYPE_ASSET_CODE.equals(type)) {
            return;
        }
        cancelScanning();
        uiState.setValue(current.selectType(type));
    }

    public void toggleScan() {
        AssetIdentifyUiState current = state();
        if (!current.isEpcMode() || current.isLoading()) {
            return;
        }
        if (current.isScanning()) {
            stopScanning();
        } else {
            startFreshEpcScan(current);
        }
    }

    public void onScanKeyPressed() {
        AssetIdentifyUiState current = state();
        if (!current.isEpcMode() || current.isLoading()) {
            return;
        }
        if (current.isScanning()) {
            stopScanning();
        } else {
            startFreshEpcScan(current);
        }
    }

    private void startFreshEpcScan(AssetIdentifyUiState current) {
        uiState.setValue(current.beginScan());
        startScanning(UhfScanMode.SINGLE);
    }

    public void identifyAssetCode(String assetCode) {
        AssetIdentifyUiState current = state();
        if (current.isLoading()) {
            return;
        }
        String checkedCode = assetCode == null ? "" : assetCode.trim();
        if (checkedCode.isEmpty()) {
            uiState.setValue(current.error(R.string.identify_asset_code_required));
            return;
        }
        identify(AssetRepository.IDENTIFY_TYPE_ASSET_CODE, checkedCode);
    }

    private void identify(String type, String value) {
        cancelRequest();
        int requestVersion = ++operationVersion;
        uiState.setValue(state().loading(value));
        RequestHandle request = assetRepository.identify(type, value,
                new RepositoryCallback<PdaAssetIdentifyDto>() {
                    @Override
                    public void onSuccess(PdaAssetIdentifyDto data) {
                        if (requestVersion != operationVersion) {
                            return;
                        }
                        if (data == null || data.getAssetId() == null
                                || !hasText(data.getAssetCode())) {
                            uiState.setValue(state().error(
                                    R.string.identify_invalid_asset_response));
                            return;
                        }
                        uiState.setValue(state().content(data));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (requestVersion == operationVersion) {
                            uiState.setValue(state().error(error.getMessage()));
                        }
                    }
                });
        if (requestVersion == operationVersion) {
            currentRequest = request;
        }
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        AssetIdentifyUiState current = state();
        if (!current.isLoading()) {
            uiState.setValue(current.scanning(scanState));
        }
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        String epc = reading.getEpc();
        if (epc.length() > MAX_EPC_LENGTH) {
            uiState.setValue(state().error(R.string.identify_epc_too_long));
            return;
        }
        identify(AssetRepository.IDENTIFY_TYPE_EPC, epc);
    }

    @Override
    protected void onScanError(String message) {
        uiState.setValue(hasText(message)
                ? state().error(message) : state().error(R.string.identify_scan_failed));
    }

    private AssetIdentifyUiState state() {
        AssetIdentifyUiState current = uiState.getValue();
        return current == null ? AssetIdentifyUiState.initial() : current;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void cancelRequest() {
        currentRequest.cancel();
        currentRequest = RequestHandle.NONE;
    }

    @Override
    protected void onViewModelCleared() {
        cancelRequest();
        operationVersion++;
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final AssetRepository assetRepository;
        private final UhfScanner scanner;

        public Factory(AssetRepository assetRepository, UhfScanner scanner) {
            this.assetRepository = assetRepository;
            this.scanner = scanner;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(AssetIdentifyViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new AssetIdentifyViewModel(assetRepository, scanner);
        }
    }
}
