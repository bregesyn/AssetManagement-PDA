package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;

import java.util.Collections;
import java.util.List;

/** 工单详情状态；动作可见性同时受本地权限和当前服务端状态裁剪。 */
public final class RepairDetailUiState {
    public enum Mode {
        LOADING,
        CONTENT,
        ERROR
    }

    private final Mode mode;
    private final PdaRepairOrderDto order;
    private final List<PdaDictItemDto> statuses;
    private final boolean canStart;
    private final boolean canFinish;
    private final String message;

    private RepairDetailUiState(Mode mode, PdaRepairOrderDto order,
            List<PdaDictItemDto> statuses, boolean canStart, boolean canFinish, String message) {
        this.mode = mode;
        this.order = order;
        this.statuses = statuses == null ? Collections.emptyList() : statuses;
        this.canStart = canStart;
        this.canFinish = canFinish;
        this.message = message;
    }

    static RepairDetailUiState loading(boolean canStart, boolean canFinish) {
        return new RepairDetailUiState(Mode.LOADING, null, Collections.emptyList(),
                canStart, canFinish, null);
    }

    static RepairDetailUiState content(PdaRepairOrderDto order, List<PdaDictItemDto> statuses,
            boolean canStart, boolean canFinish) {
        return new RepairDetailUiState(Mode.CONTENT, order, statuses, canStart, canFinish, null);
    }

    static RepairDetailUiState error(boolean canStart, boolean canFinish, String message) {
        return new RepairDetailUiState(Mode.ERROR, null, Collections.emptyList(),
                canStart, canFinish, message);
    }

    public Mode getMode() { return mode; }
    public PdaRepairOrderDto getOrder() { return order; }
    public List<PdaDictItemDto> getStatuses() { return statuses; }
    public String getMessage() { return message; }

    public boolean canStartAction() {
        return canStart && order != null && RepairUi.STATUS_WAIT_REPAIR.equals(order.getOrderStatus());
    }

    public boolean canFinishAction() {
        return canFinish && order != null && RepairUi.STATUS_REPAIRING.equals(order.getOrderStatus());
    }
}
