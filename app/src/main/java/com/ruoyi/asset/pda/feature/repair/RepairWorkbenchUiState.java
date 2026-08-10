package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;

import java.util.Collections;
import java.util.List;

/** 报修维修工作台的单一渲染状态。 */
public final class RepairWorkbenchUiState {
    public enum Mode {
        INITIALIZING,
        CONTENT,
        ERROR
    }

    public enum Tab {
        MINE,
        WORK,
        NONE
    }

    private final Mode mode;
    private final Tab tab;
    private final List<PdaRepairOrderDto> orders;
    private final List<PdaDictItemDto> statuses;
    private final String status;
    private final boolean canList;
    private final boolean canSubmit;
    private final boolean canStart;
    private final boolean canFinish;
    private final boolean loading;
    private final boolean loadingMore;
    private final boolean hasMore;
    private final String message;

    private RepairWorkbenchUiState(Mode mode, Tab tab, List<PdaRepairOrderDto> orders,
            List<PdaDictItemDto> statuses, String status, boolean canList, boolean canSubmit,
            boolean canStart, boolean canFinish, boolean loading, boolean loadingMore,
            boolean hasMore, String message) {
        this.mode = mode;
        this.tab = tab;
        this.orders = orders == null ? Collections.emptyList() : orders;
        this.statuses = statuses == null ? Collections.emptyList() : statuses;
        this.status = status;
        this.canList = canList;
        this.canSubmit = canSubmit;
        this.canStart = canStart;
        this.canFinish = canFinish;
        this.loading = loading;
        this.loadingMore = loadingMore;
        this.hasMore = hasMore;
        this.message = message;
    }

    static RepairWorkbenchUiState initializing(boolean canList, boolean canSubmit,
            boolean canStart, boolean canFinish) {
        return new RepairWorkbenchUiState(Mode.INITIALIZING, Tab.NONE, Collections.emptyList(),
                Collections.emptyList(), null, canList, canSubmit, canStart, canFinish,
                true, false, false, null);
    }

    static RepairWorkbenchUiState content(Tab tab, List<PdaRepairOrderDto> orders,
            List<PdaDictItemDto> statuses, String status, boolean canList, boolean canSubmit,
            boolean canStart, boolean canFinish, boolean loading, boolean loadingMore,
            boolean hasMore) {
        return new RepairWorkbenchUiState(Mode.CONTENT, tab, orders, statuses, status,
                canList, canSubmit, canStart, canFinish, loading, loadingMore, hasMore, null);
    }

    static RepairWorkbenchUiState error(Tab tab, List<PdaRepairOrderDto> orders,
            List<PdaDictItemDto> statuses, String status, boolean canList, boolean canSubmit,
            boolean canStart, boolean canFinish, String message) {
        return new RepairWorkbenchUiState(Mode.ERROR, tab, orders, statuses, status,
                canList, canSubmit, canStart, canFinish, false, false, false, message);
    }

    public Mode getMode() {
        return mode;
    }

    public Tab getTab() {
        return tab;
    }

    public List<PdaRepairOrderDto> getOrders() {
        return orders;
    }

    public List<PdaDictItemDto> getStatuses() {
        return statuses;
    }

    public String getStatus() {
        return status;
    }

    public boolean isCanList() {
        return canList;
    }

    public boolean isCanSubmit() {
        return canSubmit;
    }

    public boolean isCanStart() {
        return canStart;
    }

    public boolean isCanFinish() {
        return canFinish;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isLoadingMore() {
        return loadingMore;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public String getMessage() {
        return message;
    }
}
