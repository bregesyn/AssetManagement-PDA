package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;

/** 完工登记状态，日期和费用校验在发请求前完成，服务端仍是最终裁决者。 */
public final class RepairFinishUiState {
    public enum Mode {
        LOADING,
        READY,
        SUBMITTING,
        SUCCESS,
        ERROR
    }

    private final Mode mode;
    private final PdaRepairOrderDto order;
    private final String message;

    private RepairFinishUiState(Mode mode, PdaRepairOrderDto order, String message) {
        this.mode = mode;
        this.order = order;
        this.message = message;
    }

    static RepairFinishUiState loading() { return new RepairFinishUiState(Mode.LOADING, null, null); }
    static RepairFinishUiState ready(PdaRepairOrderDto order) { return new RepairFinishUiState(Mode.READY, order, null); }
    RepairFinishUiState submitting() { return new RepairFinishUiState(Mode.SUBMITTING, order, null); }
    RepairFinishUiState success() { return new RepairFinishUiState(Mode.SUCCESS, order, null); }
    RepairFinishUiState error(String error) { return new RepairFinishUiState(Mode.ERROR, order, error); }

    public Mode getMode() { return mode; }
    public PdaRepairOrderDto getOrder() { return order; }
    public String getMessage() { return message; }
    public boolean isBusy() { return mode == Mode.LOADING || mode == Mode.SUBMITTING; }
}
