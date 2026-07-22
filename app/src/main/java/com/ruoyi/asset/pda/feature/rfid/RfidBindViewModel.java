package com.ruoyi.asset.pda.feature.rfid;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.BaseUhfViewModel;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.data.repository.RfidRepository;

public final class RfidBindViewModel extends BaseUhfViewModel {
    private static final int MAX_ASSET_CODE_LENGTH = 64;
    private static final int MAX_EPC_LENGTH = 128;

    private final AssetRepository assetRepository;
    private final RfidRepository rfidRepository;
    private final MutableLiveData<RfidBindUiState> uiState =
            new MutableLiveData<>(RfidBindUiState.initial());
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int operationVersion;
    private String acceptedAssetInput;

    public RfidBindViewModel(AssetRepository assetRepository,
            RfidRepository rfidRepository, UhfScanner scanner) {
        super(scanner);
        if (assetRepository == null || rfidRepository == null) {
            throw new IllegalArgumentException("绑定 Repository 不能为空");
        }
        this.assetRepository = assetRepository;
        this.rfidRepository = rfidRepository;
    }

    public LiveData<RfidBindUiState> getUiState() { return uiState; }

    public void queryAsset(String assetCode) {
        RfidBindUiState current = state();
        if (current.isBusy() || current.isScanning() || current.isSuccess()) return;
        String checkedCode = assetCode == null ? "" : assetCode.trim();
        if (checkedCode.isEmpty()) {
            uiState.setValue(current.error(R.string.identify_asset_code_required));
            return;
        }
        if (checkedCode.length() > MAX_ASSET_CODE_LENGTH) {
            uiState.setValue(current.error(R.string.bind_asset_code_too_long));
            return;
        }
        cancelRequest();
        acceptedAssetInput = null;
        int version = ++operationVersion;
        uiState.setValue(current.loadingAsset());
        RequestHandle request = assetRepository.identify(
                AssetRepository.IDENTIFY_TYPE_ASSET_CODE, checkedCode,
                new RepositoryCallback<PdaAssetIdentifyDto>() {
                    @Override
                    public void onSuccess(PdaAssetIdentifyDto data) {
                        if (version != operationVersion) return;
                        if (data == null || data.getAssetId() == null
                                || !hasText(data.getAssetCode())) {
                            uiState.setValue(state().error(
                                    R.string.identify_invalid_asset_response));
                            return;
                        }
                        acceptedAssetInput = checkedCode;
                        uiState.setValue(state().asset(data));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            uiState.setValue(state().error(error.getMessage()));
                        }
                    }
                });
        setRequest(version, request);
    }

    public void toggleScan() {
        RfidBindUiState current = state();
        if (current.isBusy() || current.isSuccess() || !canScanTag(current)) return;
        if (current.isScanning()) stopScanning();
        else startFreshTagScan(current);
    }

    public void onScanKeyPressed() {
        RfidBindUiState current = state();
        if (current.isBusy() || current.isSuccess() || !canScanTag(current)) {
            return;
        }
        if (current.isScanning()) {
            stopScanning();
        } else {
            startFreshTagScan(current);
        }
    }

    public void onAssetCodeChanged(String assetCode) {
        RfidBindUiState current = state();
        if (current.isBusy() || current.isSuccess() || current.getAsset() == null) {
            return;
        }
        String checkedCode = assetCode == null ? "" : assetCode.trim();
        if (checkedCode.equals(acceptedAssetInput)) {
            return;
        }
        cancelScanning();
        cancelRequest();
        operationVersion++;
        acceptedAssetInput = null;
        uiState.setValue(current.clearSelection());
    }

    private void startFreshTagScan(RfidBindUiState current) {
        uiState.setValue(current.beginTagScan());
        startScanning(UhfScanMode.SINGLE);
    }

    public void bind() {
        RfidBindUiState current = state();
        if (!current.canBind()) return;
        PdaAssetIdentifyDto asset = current.getAsset();
        PdaRfidTagDto tag = current.getTag();
        cancelRequest();
        int version = ++operationVersion;
        uiState.setValue(current.submitting());
        RequestHandle request = rfidRepository.bind(asset.getAssetCode(), tag.getEpcCode(),
                new RepositoryCallback<PdaRfidTagDto>() {
                    @Override
                    public void onSuccess(PdaRfidTagDto data) {
                        if (version != operationVersion) return;
                        if (!isBoundTo(data, asset.getAssetCode())) {
                            uiState.setValue(validTag(data)
                                    ? state().tag(data).error(R.string.rfid_invalid_tag_response)
                                    : state().unknown(R.string.rfid_invalid_tag_response));
                            return;
                        }
                        uiState.setValue(state().success(data));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version != operationVersion) return;
                        if (error.getKind() == ApiErrorMapper.Kind.NETWORK
                                || error.getKind() == ApiErrorMapper.Kind.TIMEOUT) {
                            verifyBinding(asset.getAssetCode(), tag.getEpcCode());
                        } else {
                            uiState.setValue(state().error(error.getMessage()));
                        }
                    }
                });
        setRequest(version, request);
    }

    private void queryTag(String epc) {
        if (epc.length() > MAX_EPC_LENGTH) {
            uiState.setValue(state().error(R.string.identify_epc_too_long));
            return;
        }
        cancelRequest();
        int version = ++operationVersion;
        uiState.setValue(state().loadingTag());
        RequestHandle request = rfidRepository.queryTag(epc,
                new RepositoryCallback<PdaRfidTagDto>() {
                    @Override
                    public void onSuccess(PdaRfidTagDto data) {
                        if (version != operationVersion) return;
                        if (!validTag(data)) {
                            uiState.setValue(state().error(R.string.rfid_invalid_tag_response));
                            return;
                        }
                        uiState.setValue(state().tag(data));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            uiState.setValue(state().error(error.getMessage()));
                        }
                    }
                });
        setRequest(version, request);
    }

    private void verifyBinding(String assetCode, String epc) {
        int version = ++operationVersion;
        uiState.setValue(state().verifying());
        RequestHandle request = rfidRepository.queryTag(epc,
                new RepositoryCallback<PdaRfidTagDto>() {
                    @Override
                    public void onSuccess(PdaRfidTagDto data) {
                        if (version != operationVersion) return;
                        if (isBoundTo(data, assetCode)) {
                            uiState.setValue(state().success(data));
                        } else if (validTag(data)) {
                            uiState.setValue(state().tag(data).error(
                                    R.string.bind_result_not_confirmed));
                        } else {
                            uiState.setValue(state().unknown(
                                    R.string.bind_result_not_confirmed));
                        }
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            uiState.setValue(state().unknown(R.string.bind_result_unknown));
                        }
                    }
                });
        setRequest(version, request);
    }

    public void reset() {
        if (state().isBusy()) return;
        cancelRequest();
        operationVersion++;
        cancelScanning();
        acceptedAssetInput = null;
        uiState.setValue(RfidBindUiState.initial());
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        if (!state().isBusy() && !state().isSuccess()) {
            uiState.setValue(state().scanning(scanState));
        }
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        queryTag(reading.getEpc());
    }

    @Override
    protected void onScanError(String message) {
        uiState.setValue(hasText(message)
                ? state().error(message) : state().error(R.string.identify_scan_failed));
    }

    private boolean validTag(PdaRfidTagDto tag) {
        return tag != null && tag.getTagId() != null && hasText(tag.getEpcCode());
    }

    private boolean isBoundTo(PdaRfidTagDto tag, String assetCode) {
        return validTag(tag) && tag.isRfidBound() && assetCode.equals(tag.getAssetCode());
    }

    private boolean canScanTag(RfidBindUiState current) {
        return current.getAsset() != null && !current.getAsset().isRfidBound();
    }

    private RfidBindUiState state() {
        RfidBindUiState current = uiState.getValue();
        return current == null ? RfidBindUiState.initial() : current;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setRequest(int version, RequestHandle request) {
        if (version == operationVersion) currentRequest = request;
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
        private final RfidRepository rfidRepository;
        private final UhfScanner scanner;

        public Factory(AssetRepository assetRepository,
                RfidRepository rfidRepository, UhfScanner scanner) {
            this.assetRepository = assetRepository;
            this.rfidRepository = rfidRepository;
            this.scanner = scanner;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RfidBindViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RfidBindViewModel(assetRepository, rfidRepository, scanner);
        }
    }
}
