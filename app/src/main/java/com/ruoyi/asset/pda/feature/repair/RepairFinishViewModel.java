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

import java.math.BigDecimal;

/** 完工前重新读取 REPAIRING 状态；日期、结果、费用再交由后端完成最终业务校验。 */
public final class RepairFinishViewModel extends ViewModel {
    private final Long repairId;
    private final RepairRepository repairRepository;
    private final MutableLiveData<RepairFinishUiState> state = new MutableLiveData<>(
            RepairFinishUiState.loading());
    private RequestHandle request = RequestHandle.NONE;
    private int operationVersion;

    public RepairFinishViewModel(Long repairId, RepairRepository repairRepository) {
        this.repairId = repairId;
        this.repairRepository = repairRepository;
    }

    public LiveData<RepairFinishUiState> getState() { return state; }

    public void load() {
        request.cancel();
        int version = ++operationVersion;
        state.setValue(RepairFinishUiState.loading());
        request = repairRepository.loadOrder(repairId, new RepositoryCallback<PdaRepairOrderDto>() {
            @Override
            public void onSuccess(PdaRepairOrderDto data) {
                if (version != operationVersion) {
                    return;
                }
                if (data == null || !RepairUi.STATUS_REPAIRING.equals(data.getOrderStatus())) {
                    state.setValue(RepairFinishUiState.loading().error("当前工单不处于维修中状态"));
                    return;
                }
                state.setValue(RepairFinishUiState.ready(data));
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                if (version == operationVersion) {
                    state.setValue(RepairFinishUiState.loading().error(errorMessage(error)));
                }
            }
        });
    }

    public void submit(String finishDate, String result, String costText) {
        RepairFinishUiState current = current();
        if (current.isBusy() || current.getOrder() == null
                || current.getMode() == RepairFinishUiState.Mode.SUCCESS) {
            return;
        }
        String date = trim(finishDate);
        String repairResult = trim(result);
        if (date == null) {
            state.setValue(current.error("请选择维修完成日期"));
            return;
        }
        String startDate = RepairUi.datePart(current.getOrder().getRepairStartTime());
        if (RepairUi.hasText(startDate) && date.compareTo(startDate) < 0) {
            state.setValue(current.error("维修完成日期不能早于开始维修日期"));
            return;
        }
        if (repairResult == null) {
            state.setValue(current.error("请输入维修结果"));
            return;
        }
        BigDecimal cost;
        try {
            cost = new BigDecimal(trim(costText) == null ? "0.00" : trim(costText));
        } catch (NumberFormatException exception) {
            state.setValue(current.error("维修费用格式不正确"));
            return;
        }
        if (cost.compareTo(BigDecimal.ZERO) < 0 || cost.scale() > 2) {
            state.setValue(current.error("维修费用必须为非负且最多两位小数"));
            return;
        }
        int version = ++operationVersion;
        state.setValue(current.submitting());
        request = repairRepository.finishRepair(repairId, date, repairResult, cost,
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

    private RepairFinishUiState current() {
        RepairFinishUiState value = state.getValue();
        return value == null ? RepairFinishUiState.loading() : value;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String checked = value.trim();
        return checked.isEmpty() ? null : checked;
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
            if (!modelClass.isAssignableFrom(RepairFinishViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RepairFinishViewModel(repairId, repairRepository);
        }
    }
}
