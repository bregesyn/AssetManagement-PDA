package com.ruoyi.asset.pda.feature.inventory;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.repository.InventoryRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

public final class InventoryTaskListViewModel extends ViewModel {
    private final InventoryRepository inventoryRepository;
    private final MutableLiveData<InventoryTaskListUiState> uiState =
            new MutableLiveData<>(InventoryTaskListUiState.loading());
    private final List<PdaInventoryTaskDto> tasks = new ArrayList<>();
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int pageNum;
    private long total;
    private int operationVersion;
    private boolean loading;

    public InventoryTaskListViewModel(InventoryRepository inventoryRepository) {
        if (inventoryRepository == null) {
            throw new IllegalArgumentException("盘点任务 Repository 不能为空");
        }
        this.inventoryRepository = inventoryRepository;
    }

    public LiveData<InventoryTaskListUiState> getUiState() {
        return uiState;
    }

    public void initialize() {
        if (pageNum == 0 && !loading) {
            refresh();
        }
    }

    public void refresh() {
        cancelRequest();
        operationVersion++;
        tasks.clear();
        pageNum = 0;
        total = 0;
        loadPage(1, false);
    }

    public void loadMore() {
        if (loading || pageNum == 0 || tasks.size() >= total) {
            return;
        }
        loadPage(pageNum + 1, true);
    }

    private void loadPage(int requestedPage, boolean loadingMore) {
        loading = true;
        int requestVersion = ++operationVersion;
        InventoryTaskListUiState current = uiState.getValue();
        if (!loadingMore || current == null) {
            uiState.setValue(InventoryTaskListUiState.loading());
        } else {
            uiState.setValue(InventoryTaskListUiState.content(
                    current.getActionableTasks(), current.getReadonlyTasks(), true,
                    current.isHasMore()));
        }
        currentRequest = inventoryRepository.loadTasks(requestedPage, 100,
                new RepositoryCallback<PdaPageResultDto<PdaInventoryTaskDto>>() {
                    @Override
                    public void onSuccess(PdaPageResultDto<PdaInventoryTaskDto> data) {
                        if (requestVersion != operationVersion) {
                            return;
                        }
                        loading = false;
                        if (data == null || data.getRows() == null) {
                            uiState.setValue(InventoryTaskListUiState.error("任务列表响应缺少 rows"));
                            return;
                        }
                        pageNum = requestedPage;
                        total = data.getTotal();
                        tasks.addAll(data.getRows());
                        publish(false, tasks.size() < total);
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (requestVersion != operationVersion) {
                            return;
                        }
                        loading = false;
                        uiState.setValue(InventoryTaskListUiState.error(
                                error == null ? "任务列表加载失败" : error.getMessage()));
                    }
                });
    }

    private void publish(boolean loadingMore, boolean hasMore) {
        List<PdaInventoryTaskDto> actionable = new ArrayList<>();
        List<PdaInventoryTaskDto> readonly = new ArrayList<>();
        for (PdaInventoryTaskDto task : tasks) {
            if (task == null) {
                continue;
            }
            if ("PENDING_RESULT_CONFIRM".equals(task.getTaskStatus())) {
                readonly.add(task);
            } else if ("ISSUED".equals(task.getTaskStatus())
                    || "INVENTORYING".equals(task.getTaskStatus())) {
                actionable.add(task);
            } else {
                // 未知状态只能进入只读分段，保留服务端原始编码，避免客户端擅自放行写操作。
                readonly.add(task);
            }
        }
        uiState.setValue(InventoryTaskListUiState.content(actionable, readonly,
                loadingMore, hasMore));
    }

    private void cancelRequest() {
        currentRequest.cancel();
        currentRequest = RequestHandle.NONE;
        loading = false;
    }

    @Override
    protected void onCleared() {
        cancelRequest();
        operationVersion++;
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final InventoryRepository inventoryRepository;

        public Factory(InventoryRepository inventoryRepository) {
            this.inventoryRepository = inventoryRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(InventoryTaskListViewModel.class)) {
                throw new IllegalArgumentException("不支持的盘点任务 ViewModel 类型");
            }
            return (T) new InventoryTaskListViewModel(inventoryRepository);
        }
    }
}
