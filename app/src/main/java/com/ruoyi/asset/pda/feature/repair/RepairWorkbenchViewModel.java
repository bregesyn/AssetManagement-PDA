package com.ruoyi.asset.pda.feature.repair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 只请求当前用户有权限读取的报修列表，避免把 403 当作空数据处理。 */
public final class RepairWorkbenchViewModel extends ViewModel {
    private static final int PAGE_SIZE = 20;

    private final RepairRepository repairRepository;
    private final CommonRepository commonRepository;
    private final boolean canList;
    private final boolean canSubmit;
    private final boolean canStart;
    private final boolean canFinish;
    private final MutableLiveData<RepairWorkbenchUiState> state;
    private final List<PdaRepairOrderDto> orders = new ArrayList<>();

    private List<PdaDictItemDto> statuses = Collections.emptyList();
    private RepairWorkbenchUiState.Tab tab;
    private String status;
    private String keyword;
    private int pageNum;
    private long total;
    private boolean initialized;
    private boolean requesting;

    public RepairWorkbenchViewModel(RepairRepository repairRepository,
            CommonRepository commonRepository, boolean canList, boolean canSubmit,
            boolean canStart, boolean canFinish) {
        this.repairRepository = repairRepository;
        this.commonRepository = commonRepository;
        this.canList = canList;
        this.canSubmit = canSubmit;
        this.canStart = canStart;
        this.canFinish = canFinish;
        this.tab = canList ? RepairWorkbenchUiState.Tab.MINE
                : (canStart || canFinish ? RepairWorkbenchUiState.Tab.WORK
                : RepairWorkbenchUiState.Tab.NONE);
        this.state = new MutableLiveData<>(RepairWorkbenchUiState.initializing(
                canList, canSubmit, canStart, canFinish));
    }

    public LiveData<RepairWorkbenchUiState> getState() {
        return state;
    }

    public void initialize() {
        if (initialized || requesting) {
            return;
        }
        requesting = true;
        commonRepository.bootstrap(new RepositoryCallback<PdaBootstrapDto>() {
            @Override
            public void onSuccess(PdaBootstrapDto data) {
                requesting = false;
                initialized = true;
                statuses = data == null || data.getDicts() == null
                        ? Collections.emptyList()
                        : safeStatuses(data.getDicts().get("ams_repair_status"));
                refresh();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                requesting = false;
                state.setValue(RepairWorkbenchUiState.error(tab, orders, statuses, status,
                        canList, canSubmit, canStart, canFinish, errorMessage(error)));
            }
        });
    }

    public void retry() {
        if (!initialized) {
            initialize();
            return;
        }
        refresh();
    }

    public void selectTab(RepairWorkbenchUiState.Tab nextTab) {
        if (nextTab == null || nextTab == tab || !isAllowed(nextTab)) {
            return;
        }
        tab = nextTab;
        status = null;
        keyword = null;
        refresh();
    }

    public void setStatus(String nextStatus) {
        if (tab != RepairWorkbenchUiState.Tab.MINE) {
            return;
        }
        status = nextStatus;
        refresh();
    }

    public void search(String nextKeyword) {
        keyword = nextKeyword == null ? null : nextKeyword.trim();
        refresh();
    }

    public void refresh() {
        if (!initialized || requesting) {
            return;
        }
        orders.clear();
        pageNum = 0;
        total = 0;
        if (tab == RepairWorkbenchUiState.Tab.NONE) {
            publish(false, false);
            return;
        }
        loadPage(1, false);
    }

    public void loadMore() {
        if (!initialized || requesting || pageNum * PAGE_SIZE >= total
                || tab == RepairWorkbenchUiState.Tab.NONE) {
            return;
        }
        loadPage(pageNum + 1, true);
    }

    private void loadPage(final int targetPage, final boolean append) {
        requesting = true;
        publish(!append, append);
        RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback =
                new RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>>() {
                    @Override
                    public void onSuccess(PdaPageResultDto<PdaRepairOrderDto> data) {
                        requesting = false;
                        pageNum = targetPage;
                        total = data == null ? 0 : data.getTotal();
                        if (data != null && data.getRows() != null) {
                            orders.addAll(data.getRows());
                        }
                        publish(false, false);
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        requesting = false;
                        state.setValue(RepairWorkbenchUiState.error(tab, orders, statuses, status,
                                canList, canSubmit, canStart, canFinish, errorMessage(error)));
                    }
        };
        if (tab == RepairWorkbenchUiState.Tab.MINE) {
            repairRepository.loadMyOrders(status, keyword, targetPage, PAGE_SIZE, callback);
        } else {
            repairRepository.loadWorkOrders(keyword, targetPage, PAGE_SIZE, callback);
        }
    }

    private void publish(boolean loading, boolean loadingMore) {
        boolean hasMore = pageNum * PAGE_SIZE < total;
        state.setValue(RepairWorkbenchUiState.content(tab, new ArrayList<>(orders), statuses,
                status, canList, canSubmit, canStart, canFinish, loading, loadingMore, hasMore));
    }

    private boolean isAllowed(RepairWorkbenchUiState.Tab target) {
        return target == RepairWorkbenchUiState.Tab.MINE ? canList
                : target == RepairWorkbenchUiState.Tab.WORK && (canStart || canFinish);
    }

    private String errorMessage(ApiErrorMapper.ApiError error) {
        return error == null ? "请求失败，请重试" : error.getMessage();
    }

    private List<PdaDictItemDto> safeStatuses(List<PdaDictItemDto> options) {
        return options == null ? Collections.emptyList() : options;
    }
}
