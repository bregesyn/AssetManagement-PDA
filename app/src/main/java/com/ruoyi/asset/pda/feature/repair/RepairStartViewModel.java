package com.ruoyi.asset.pda.feature.repair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

/** 开工前先重读工单，避免 Web 审批结果变化后仍以缓存状态开始维修。 */
public final class RepairStartViewModel extends ViewModel {
    private final Long repairId;
    private final RepairRepository repairRepository;
    private final MutableLiveData<RepairStartUiState> state = new MutableLiveData<>(
            RepairStartUiState.loading());
    private RequestHandle request = RequestHandle.NONE;
    private int operationVersion;

    public RepairStartViewModel(Long repairId, RepairRepository repairRepository) {
        this.repairId = repairId;
        this.repairRepository = repairRepository;
    }

    public LiveData<RepairStartUiState> getState() {
        return state;
    }

    public void load() {
        request.cancel();
        int version = ++operationVersion;
        state.setValue(RepairStartUiState.loading());
        request = repairRepository.loadOrder(repairId, new RepositoryCallback<PdaRepairOrderDto>() {
            @Override
            public void onSuccess(PdaRepairOrderDto data) {
                if (version != operationVersion) {
                    return;
                }
                if (data == null || !RepairUi.STATUS_WAIT_REPAIR.equals(data.getOrderStatus())) {
                    state.setValue(RepairStartUiState.loading().error("当前工单不处于待维修状态"));
                    return;
                }
                state.setValue(RepairStartUiState.ready(data));
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                if (version == operationVersion) {
                    state.setValue(RepairStartUiState.loading().error(errorMessage(error)));
                }
            }
        });
    }

    public void selectRepairerType(String type) {
        RepairStartUiState current = current();
        if (current.isBusy() || current.getMode() == RepairStartUiState.Mode.SUCCESS
                || current.getRepairerType().equals(type)
                || (!RepairUi.REPAIRER_INTERNAL.equals(type) && !RepairUi.REPAIRER_EXTERNAL.equals(type))) {
            return;
        }
        state.setValue(current.selectType(type));
    }

    public void selectRepairer(Long id, String name, String code) {
        RepairStartUiState current = current();
        if (current.isBusy() || !current.isInternal() || id == null || id < 1L) {
            return;
        }
        state.setValue(current.selectRepairer(id, name, code));
    }

    public void submit(String externalOrg, String externalContact, String externalPhone) {
        RepairStartUiState current = current();
        if (current.isBusy() || current.getMode() == RepairStartUiState.Mode.SUCCESS
                || current.getOrder() == null) {
            return;
        }
        if (current.isInternal() && (current.getRepairerId() == null || current.getRepairerId() < 1L)) {
            state.setValue(current.error("请选择内部维修人"));
            return;
        }
        if (!current.isInternal() && !RepairUi.hasText(externalOrg)) {
            state.setValue(current.error("请填写外部维修单位"));
            return;
        }
        int version = ++operationVersion;
        state.setValue(current.submitting());
        request = repairRepository.startRepair(repairId, current.getRepairerType(),
                current.getRepairerId(), current.isInternal() ? null : externalContact,
                current.isInternal() ? null : externalOrg,
                current.isInternal() ? null : externalPhone,
                new RepositoryCallback<PdaRepairOrderDto>() {
                    @Override
                    public void onSuccess(PdaRepairOrderDto data) {
                        if (version == operationVersion) {
                            state.setValue(current().success());
                        }
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            state.setValue(current().error(errorMessage(error)));
                        }
                    }
                });
    }

    private RepairStartUiState current() {
        RepairStartUiState value = state.getValue();
        return value == null ? RepairStartUiState.loading() : value;
    }

    private String errorMessage(ApiErrorMapper.ApiError error) {
        return error == null ? "请求失败，请重试" : error.getMessage();
    }

    @Override
    protected void onCleared() {
        operationVersion++;
        request.cancel();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final Long repairId;
        private final RepairRepository repairRepository;

        public Factory(Long repairId, RepairRepository repairRepository) {
            this.repairId = repairId;
            this.repairRepository = repairRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RepairStartViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RepairStartViewModel(repairId, repairRepository);
        }
    }
}
