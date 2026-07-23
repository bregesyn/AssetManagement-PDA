package com.ruoyi.asset.pda.feature.inventory;

import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InventoryTaskListUiState {
    public enum Mode { LOADING, CONTENT, ERROR }

    private final Mode mode;
    private final List<PdaInventoryTaskDto> actionableTasks;
    private final List<PdaInventoryTaskDto> readonlyTasks;
    private final boolean loadingMore;
    private final boolean hasMore;
    private final String errorMessage;

    private InventoryTaskListUiState(Mode mode, List<PdaInventoryTaskDto> actionableTasks,
            List<PdaInventoryTaskDto> readonlyTasks, boolean loadingMore,
            boolean hasMore, String errorMessage) {
        this.mode = mode;
        this.actionableTasks = Collections.unmodifiableList(new ArrayList<>(actionableTasks));
        this.readonlyTasks = Collections.unmodifiableList(new ArrayList<>(readonlyTasks));
        this.loadingMore = loadingMore;
        this.hasMore = hasMore;
        this.errorMessage = errorMessage;
    }

    public static InventoryTaskListUiState loading() {
        return new InventoryTaskListUiState(Mode.LOADING, Collections.emptyList(),
                Collections.emptyList(), false, false, null);
    }

    public static InventoryTaskListUiState content(List<PdaInventoryTaskDto> actionable,
            List<PdaInventoryTaskDto> readonly, boolean loadingMore, boolean hasMore) {
        return new InventoryTaskListUiState(Mode.CONTENT, actionable, readonly,
                loadingMore, hasMore, null);
    }

    public static InventoryTaskListUiState error(String message) {
        return new InventoryTaskListUiState(Mode.ERROR, Collections.emptyList(),
                Collections.emptyList(), false, false, message);
    }

    public Mode getMode() { return mode; }
    public List<PdaInventoryTaskDto> getActionableTasks() { return actionableTasks; }
    public List<PdaInventoryTaskDto> getReadonlyTasks() { return readonlyTasks; }
    public boolean isLoadingMore() { return loadingMore; }
    public boolean isHasMore() { return hasMore; }
    public String getErrorMessage() { return errorMessage; }
}
