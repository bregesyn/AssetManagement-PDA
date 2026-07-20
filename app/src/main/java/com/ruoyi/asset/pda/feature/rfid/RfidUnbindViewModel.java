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
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.data.repository.RfidRepository;

public final class RfidUnbindViewModel extends BaseUhfViewModel {
    private static final int MAX_EPC_LENGTH = 128;

    private final RfidRepository rfidRepository;
    private final MutableLiveData<RfidUnbindUiState> uiState =
            new MutableLiveData<>(RfidUnbindUiState.initial());
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int operationVersion;

    public RfidUnbindViewModel(RfidRepository rfidRepository, UhfScanner scanner) {
        super(scanner);
        if (rfidRepository == null) {
            throw new IllegalArgumentException("RfidRepository 不能为空");
        }
        this.rfidRepository = rfidRepository;
    }

    public LiveData<RfidUnbindUiState> getUiState() { return uiState; }

    public void toggleScan() {
        RfidUnbindUiState current = state();
        if (current.isBusy() || current.isSuccess()) return;
        if (current.isScanning()) stopScanning();
        else startFreshTagScan(current);
    }

    public void onScanKeyDown() {
        RfidUnbindUiState current = state();
        if (!current.isBusy() && !current.isSuccess() && !current.isScanning()) {
            startFreshTagScan(current);
        }
    }

    public void onScanKeyUp() { stopScanning(); }

    private void startFreshTagScan(RfidUnbindUiState current) {
        uiState.setValue(current.beginTagScan());
        startScanning(UhfScanMode.SINGLE);
    }

    public void unbind() {
        RfidUnbindUiState current = state();
        if (!current.canUnbind()) return;
        PdaRfidTagDto tag = current.getTag();
        cancelRequest();
        int version = ++operationVersion;
        uiState.setValue(current.submitting());
        RequestHandle request = rfidRepository.unbind(tag.getTagId(),
                new RepositoryCallback<PdaRfidTagDto>() {
                    @Override
                    public void onSuccess(PdaRfidTagDto data) {
                        if (version != operationVersion) return;
                        if (!validTag(data) || data.isRfidBound()) {
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
                            verifyUnbinding(tag.getEpcCode());
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

    private void verifyUnbinding(String epc) {
        int version = ++operationVersion;
        uiState.setValue(state().verifying());
        RequestHandle request = rfidRepository.queryTag(epc,
                new RepositoryCallback<PdaRfidTagDto>() {
                    @Override
                    public void onSuccess(PdaRfidTagDto data) {
                        if (version != operationVersion) return;
                        if (validTag(data) && !data.isRfidBound()) {
                            uiState.setValue(state().success(data));
                        } else if (validTag(data)) {
                            uiState.setValue(state().tag(data).error(
                                    R.string.unbind_result_not_confirmed));
                        } else {
                            uiState.setValue(state().unknown(
                                    R.string.unbind_result_not_confirmed));
                        }
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            uiState.setValue(state().unknown(R.string.unbind_result_unknown));
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
        uiState.setValue(RfidUnbindUiState.initial());
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

    private RfidUnbindUiState state() {
        RfidUnbindUiState current = uiState.getValue();
        return current == null ? RfidUnbindUiState.initial() : current;
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
        private final RfidRepository rfidRepository;
        private final UhfScanner scanner;

        public Factory(RfidRepository rfidRepository, UhfScanner scanner) {
            this.rfidRepository = rfidRepository;
            this.scanner = scanner;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RfidUnbindViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RfidUnbindViewModel(rfidRepository, scanner);
        }
    }
}
