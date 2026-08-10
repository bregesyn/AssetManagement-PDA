package com.ruoyi.asset.pda.feature.repair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;

import java.util.Collections;
import java.util.List;

/** 每次进入详情均重新从服务端读取，防止 PDA 以旧状态放行维修动作。 */
public final class RepairDetailViewModel extends ViewModel {
    private final Long repairId;
    private final RepairRepository repairRepository;
    private final CommonRepository commonRepository;
    private final boolean canStart;
    private final boolean canFinish;
    private final MutableLiveData<RepairDetailUiState> state;

    public RepairDetailViewModel(Long repairId, RepairRepository repairRepository,
            CommonRepository commonRepository, boolean canStart, boolean canFinish) {
        this.repairId = repairId;
        this.repairRepository = repairRepository;
        this.commonRepository = commonRepository;
        this.canStart = canStart;
        this.canFinish = canFinish;
        this.state = new MutableLiveData<>(RepairDetailUiState.loading(canStart, canFinish));
    }

    public LiveData<RepairDetailUiState> getState() {
        return state;
    }

    public void load() {
        state.setValue(RepairDetailUiState.loading(canStart, canFinish));
        commonRepository.bootstrap(new RepositoryCallback<PdaBootstrapDto>() {
            @Override
            public void onSuccess(PdaBootstrapDto data) {
                List<PdaDictItemDto> statuses = data == null || data.getDicts() == null
                        ? Collections.emptyList()
                        : safeStatuses(data.getDicts().get("ams_repair_status"));
                loadOrder(statuses);
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                state.setValue(RepairDetailUiState.error(canStart, canFinish, errorMessage(error)));
            }
        });
    }

    private void loadOrder(List<PdaDictItemDto> statuses) {
        repairRepository.loadOrder(repairId, new RepositoryCallback<PdaRepairOrderDto>() {
            @Override
            public void onSuccess(PdaRepairOrderDto data) {
                if (data == null || data.getRepairId() == null) {
                    state.setValue(RepairDetailUiState.error(canStart, canFinish,
                            "工单详情返回不完整"));
                    return;
                }
                state.setValue(RepairDetailUiState.content(data, statuses, canStart, canFinish));
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                state.setValue(RepairDetailUiState.error(canStart, canFinish, errorMessage(error)));
            }
        });
    }

    private String errorMessage(ApiErrorMapper.ApiError error) {
        return error == null ? "请求失败，请重试" : error.getMessage();
    }

    private List<PdaDictItemDto> safeStatuses(List<PdaDictItemDto> options) {
        return options == null ? Collections.emptyList() : options;
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final Long repairId;
        private final RepairRepository repairRepository;
        private final CommonRepository commonRepository;
        private final boolean canStart;
        private final boolean canFinish;

        public Factory(Long repairId, RepairRepository repairRepository,
                CommonRepository commonRepository, boolean canStart, boolean canFinish) {
            this.repairId = repairId;
            this.repairRepository = repairRepository;
            this.commonRepository = commonRepository;
            this.canStart = canStart;
            this.canFinish = canFinish;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RepairDetailViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RepairDetailViewModel(repairId, repairRepository,
                    commonRepository, canStart, canFinish);
        }
    }
}
