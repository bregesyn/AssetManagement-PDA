package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;

/** 开始维修登记状态。内部维修人仅作为 ID 保存，姓名仅用于现场确认展示。 */
public final class RepairStartUiState {
    public enum Mode {
        LOADING,
        READY,
        SUBMITTING,
        SUCCESS,
        ERROR
    }

    private final Mode mode;
    private final PdaRepairOrderDto order;
    private final String repairerType;
    private final Long repairerId;
    private final String repairerName;
    private final String repairerCode;
    private final String message;

    private RepairStartUiState(Mode mode, PdaRepairOrderDto order, String repairerType,
            Long repairerId, String repairerName, String repairerCode, String message) {
        this.mode = mode;
        this.order = order;
        this.repairerType = repairerType;
        this.repairerId = repairerId;
        this.repairerName = repairerName;
        this.repairerCode = repairerCode;
        this.message = message;
    }

    static RepairStartUiState loading() {
        return new RepairStartUiState(Mode.LOADING, null, RepairUi.REPAIRER_INTERNAL,
                null, null, null, null);
    }

    static RepairStartUiState ready(PdaRepairOrderDto order) {
        return new RepairStartUiState(Mode.READY, order, RepairUi.REPAIRER_INTERNAL,
                null, null, null, null);
    }

    RepairStartUiState selectType(String type) {
        return new RepairStartUiState(Mode.READY, order, type, null, null, null, null);
    }

    RepairStartUiState selectRepairer(Long id, String name, String code) {
        return new RepairStartUiState(Mode.READY, order, repairerType, id, name, code, null);
    }

    RepairStartUiState submitting() {
        return new RepairStartUiState(Mode.SUBMITTING, order, repairerType,
                repairerId, repairerName, repairerCode, null);
    }

    RepairStartUiState success() {
        return new RepairStartUiState(Mode.SUCCESS, order, repairerType,
                repairerId, repairerName, repairerCode, null);
    }

    RepairStartUiState error(String error) {
        return new RepairStartUiState(Mode.ERROR, order, repairerType,
                repairerId, repairerName, repairerCode, error);
    }

    public Mode getMode() { return mode; }
    public PdaRepairOrderDto getOrder() { return order; }
    public String getRepairerType() { return repairerType; }
    public Long getRepairerId() { return repairerId; }
    public String getRepairerName() { return repairerName; }
    public String getRepairerCode() { return repairerCode; }
    public String getMessage() { return message; }
    public boolean isInternal() { return RepairUi.REPAIRER_INTERNAL.equals(repairerType); }
    public boolean isBusy() { return mode == Mode.LOADING || mode == Mode.SUBMITTING; }
}
