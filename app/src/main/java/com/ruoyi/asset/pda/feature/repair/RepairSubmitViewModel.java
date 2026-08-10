package com.ruoyi.asset.pda.feature.repair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.BaseUhfViewModel;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

/**
 * 单资产报修输入。识别仅用于预览，最终提交仍由服务端按照 identifier 加锁校验资产状态。
 */
public final class RepairSubmitViewModel extends BaseUhfViewModel {
    private static final int MAX_EPC_LENGTH = 128;

    private final AssetRepository assetRepository;
    private final RepairRepository repairRepository;
    private final CommonRepository commonRepository;
    private final MutableLiveData<RepairSubmitUiState> state = new MutableLiveData<>(
            RepairSubmitUiState.initial(AssetRepository.IDENTIFY_TYPE_EPC));
    private RequestHandle request = RequestHandle.NONE;
    private int operationVersion;
    private boolean initialized;
    private boolean bootstrapRequesting;

    public RepairSubmitViewModel(AssetRepository assetRepository, RepairRepository repairRepository,
            CommonRepository commonRepository, UhfScanner scanner) {
        super(scanner);
        if (assetRepository == null || repairRepository == null || commonRepository == null) {
            throw new IllegalArgumentException("报修提交依赖不能为空");
        }
        this.assetRepository = assetRepository;
        this.repairRepository = repairRepository;
        this.commonRepository = commonRepository;
    }

    public LiveData<RepairSubmitUiState> getState() {
        return state;
    }

    public void initialize() {
        if (initialized || bootstrapRequesting) {
            return;
        }
        bootstrapRequesting = true;
        int version = ++operationVersion;
        state.setValue(RepairSubmitUiState.initial(current().getIdentifyType()));
        request = commonRepository.bootstrap(new RepositoryCallback<PdaBootstrapDto>() {
            @Override
            public void onSuccess(PdaBootstrapDto data) {
                if (version != operationVersion) {
                    return;
                }
                bootstrapRequesting = false;
                if (data == null || data.getCurrentUser() == null) {
                    state.setValue(current().error("启动配置缺少当前用户信息"));
                    return;
                }
                initialized = true;
                state.setValue(current().ready(data.getCurrentUser(), RepairUi.datePart(data.getServerTime())));
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                if (version == operationVersion) {
                    bootstrapRequesting = false;
                    state.setValue(current().error(errorMessage(error)));
                }
            }
        });
    }

    public void selectIdentifyType(String type) {
        RepairSubmitUiState current = current();
        if (current.isBusy() || current.getIdentifyType().equals(type)
                || (!AssetRepository.IDENTIFY_TYPE_EPC.equals(type)
                && !AssetRepository.IDENTIFY_TYPE_ASSET_CODE.equals(type))) {
            return;
        }
        cancelScanning();
        state.setValue(current.selectType(type));
    }

    public void toggleScan() {
        RepairSubmitUiState current = current();
        if (!initialized || !current.isEpcMode() || current.isBusy()) {
            return;
        }
        if (current.isScanning()) {
            stopScanning();
        } else {
            state.setValue(current.beginFreshEpcScan());
            startScanning(UhfScanMode.SINGLE);
        }
    }

    public void onScanKeyPressed() {
        toggleScan();
    }

    public void identifyAssetCode(String assetCode) {
        if (!initialized || current().isBusy()) {
            return;
        }
        String code = trim(assetCode);
        if (code == null) {
            state.setValue(current().error("请输入资产编码"));
            return;
        }
        identify(AssetRepository.IDENTIFY_TYPE_ASSET_CODE, code);
    }

    public void submit(String faultDesc, String expectedFinishTime, String remark) {
        RepairSubmitUiState current = current();
        if (!initialized || current.isBusy() || current.getAsset() == null
                || !RepairUi.hasText(current.getIdentifyValue())) {
            state.setValue(current.error("请先识别一项资产"));
            return;
        }
        if (!RepairUi.hasText(faultDesc)) {
            state.setValue(current.error("请填写故障描述"));
            return;
        }
        String date = trim(expectedFinishTime);
        if (date != null && RepairUi.hasText(current.getServerDate())
                && date.compareTo(current.getServerDate()) < 0) {
            state.setValue(current.error("期望完成日期不能早于服务器日期"));
            return;
        }
        int version = ++operationVersion;
        state.setValue(current.submitting());
        request = repairRepository.submit(new PdaAssetIdentifyRequest(current.getIdentifyType(),
                        current.getIdentifyValue()), faultDesc, date, remark,
                new RepositoryCallback<PdaRepairSubmitResultDto>() {
                    @Override
                    public void onSuccess(PdaRepairSubmitResultDto data) {
                        if (version != operationVersion) {
                            return;
                        }
                        if (data == null || data.getOrder() == null
                                || data.getOrder().getRepairId() == null) {
                            state.setValue(current().error("提交结果缺少报修单信息"));
                            return;
                        }
                        cancelScanning();
                        state.setValue(current().success(data));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            state.setValue(current().error(errorMessage(error)));
                        }
                    }
                });
    }

    public boolean hasUnsavedWork(String faultDesc, String expectedFinishTime, String remark) {
        RepairSubmitUiState current = current();
        return current.getResult() == null && (current.getAsset() != null || RepairUi.hasText(faultDesc)
                || RepairUi.hasText(expectedFinishTime) || RepairUi.hasText(remark));
    }

    private void identify(String type, String value) {
        cancelRequest();
        int version = ++operationVersion;
        state.setValue(current().identifying(type, value));
        request = assetRepository.identify(type, value, new RepositoryCallback<PdaAssetIdentifyDto>() {
            @Override
            public void onSuccess(PdaAssetIdentifyDto data) {
                if (version != operationVersion) {
                    return;
                }
                if (data == null || data.getAssetId() == null || !RepairUi.hasText(data.getAssetCode())) {
                    cancelScanning();
                    state.setValue(current().error("资产识别结果不完整，请重新识别"));
                    return;
                }
                state.setValue(current().asset(data));
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                if (version == operationVersion) {
                    cancelScanning();
                    state.setValue(current().error(errorMessage(error)));
                }
            }
        });
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        if (!current().isBusy() && current().isEpcMode()) {
            state.setValue(current().scanning(scanState));
        }
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        String epc = reading.getEpc();
        if (epc.length() > MAX_EPC_LENGTH) {
            state.setValue(current().error("EPC 长度超过限制"));
            return;
        }
        identify(AssetRepository.IDENTIFY_TYPE_EPC, epc);
    }

    @Override
    protected void onScanError(String message) {
        state.setValue(current().error(RepairUi.hasText(message) ? message : "RFID 读取失败"));
    }

    private RepairSubmitUiState current() {
        RepairSubmitUiState value = state.getValue();
        return value == null ? RepairSubmitUiState.initial(AssetRepository.IDENTIFY_TYPE_EPC) : value;
    }

    private void cancelRequest() {
        request.cancel();
        request = RequestHandle.NONE;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String errorMessage(ApiErrorMapper.ApiError error) {
        return error == null ? "请求失败，请重试" : error.getMessage();
    }

    @Override
    protected void onViewModelCleared() {
        bootstrapRequesting = false;
        operationVersion++;
        cancelRequest();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final AssetRepository assetRepository;
        private final RepairRepository repairRepository;
        private final CommonRepository commonRepository;
        private final UhfScanner scanner;

        public Factory(AssetRepository assetRepository, RepairRepository repairRepository,
                CommonRepository commonRepository, UhfScanner scanner) {
            this.assetRepository = assetRepository;
            this.repairRepository = repairRepository;
            this.commonRepository = commonRepository;
            this.scanner = scanner;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RepairSubmitViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RepairSubmitViewModel(assetRepository, repairRepository,
                    commonRepository, scanner);
        }
    }
}
