package com.ruoyi.asset.pda.feature.repair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

/** 维修人候选始终由服务端按启用用户过滤，客户端不缓存人员真实性。 */
public final class RepairerPickerViewModel extends ViewModel {
    private final RepairRepository repairRepository;
    private final MutableLiveData<RepairerPickerUiState> state = new MutableLiveData<>(
            RepairerPickerUiState.initial());
    private RequestHandle request = RequestHandle.NONE;
    private int operationVersion;

    public RepairerPickerViewModel(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public LiveData<RepairerPickerUiState> getState() {
        return state;
    }

    public void search(String keyword) {
        String checked = keyword == null ? "" : keyword.trim();
        RepairerPickerUiState current = current();
        if (checked.isEmpty()) {
            state.setValue(RepairerPickerUiState.error(current.getRepairers(), "请输入搜索关键词"));
            return;
        }
        request.cancel();
        int version = ++operationVersion;
        state.setValue(RepairerPickerUiState.searching(current.getRepairers()));
        request = repairRepository.searchRepairers(checked, new RepositoryCallback<List<PdaRepairerDto>>() {
            @Override
            public void onSuccess(List<PdaRepairerDto> data) {
                if (version == operationVersion) {
                    state.setValue(RepairerPickerUiState.content(data));
                }
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                if (version == operationVersion) {
                    state.setValue(RepairerPickerUiState.error(current().getRepairers(),
                            error == null ? "请求失败，请重试" : error.getMessage()));
                }
            }
        });
    }

    private RepairerPickerUiState current() {
        RepairerPickerUiState value = state.getValue();
        return value == null ? RepairerPickerUiState.initial() : value;
    }

    @Override
    protected void onCleared() {
        operationVersion++;
        request.cancel();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final RepairRepository repairRepository;

        public Factory(RepairRepository repairRepository) {
            this.repairRepository = repairRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RepairerPickerViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RepairerPickerViewModel(repairRepository);
        }
    }
}
