package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;

/** 报修表单状态；资产身份只保存 EPC/资产编码，提交时不携带资产 ID。 */
public final class RepairSubmitUiState {
    public enum Mode {
        INITIALIZING,
        READY,
        IDENTIFYING,
        SUBMITTING,
        SUCCESS
    }

    private final Mode mode;
    private final String identifyType;
    private final String identifyValue;
    private final PdaAssetIdentifyDto asset;
    private final PdaUserDto currentUser;
    private final String serverDate;
    private final UhfScanState scanState;
    private final String message;
    private final PdaRepairSubmitResultDto result;

    private RepairSubmitUiState(Mode mode, String identifyType, String identifyValue,
            PdaAssetIdentifyDto asset, PdaUserDto currentUser, String serverDate,
            UhfScanState scanState, String message, PdaRepairSubmitResultDto result) {
        this.mode = mode;
        this.identifyType = identifyType;
        this.identifyValue = identifyValue;
        this.asset = asset;
        this.currentUser = currentUser;
        this.serverDate = serverDate;
        this.scanState = scanState;
        this.message = message;
        this.result = result;
    }

    static RepairSubmitUiState initial(String identifyType) {
        return new RepairSubmitUiState(Mode.INITIALIZING, identifyType, null, null,
                null, null, UhfScanState.IDLE, null, null);
    }

    RepairSubmitUiState ready(PdaUserDto user, String date) {
        return new RepairSubmitUiState(Mode.READY, identifyType, identifyValue, asset, user,
                date, scanState, null, null);
    }

    RepairSubmitUiState selectType(String type) {
        return new RepairSubmitUiState(Mode.READY, type, null, null, currentUser, serverDate,
                UhfScanState.IDLE, null, null);
    }

    RepairSubmitUiState scanning(UhfScanState state) {
        return new RepairSubmitUiState(Mode.READY, identifyType, identifyValue, asset,
                currentUser, serverDate, state, message, null);
    }

    /** 新一轮单标签识别不复用旧资产，防止现场误把上一件预览资产提交为本次报修。 */
    RepairSubmitUiState beginFreshEpcScan() {
        return new RepairSubmitUiState(Mode.READY, identifyType, null, null, currentUser,
                serverDate, UhfScanState.PROCESSING, null, null);
    }

    RepairSubmitUiState identifying(String type, String value) {
        return new RepairSubmitUiState(Mode.IDENTIFYING, type, value, null, currentUser,
                serverDate, UhfScanState.PROCESSING, null, null);
    }

    RepairSubmitUiState asset(PdaAssetIdentifyDto identified) {
        return new RepairSubmitUiState(Mode.READY, identifyType, identifyValue, identified,
                currentUser, serverDate, UhfScanState.IDLE, null, null);
    }

    RepairSubmitUiState submitting() {
        return new RepairSubmitUiState(Mode.SUBMITTING, identifyType, identifyValue, asset,
                currentUser, serverDate, UhfScanState.IDLE, null, null);
    }

    RepairSubmitUiState success(PdaRepairSubmitResultDto submitResult) {
        return new RepairSubmitUiState(Mode.SUCCESS, identifyType, identifyValue, asset,
                currentUser, serverDate, UhfScanState.IDLE, null, submitResult);
    }

    RepairSubmitUiState error(String error) {
        return new RepairSubmitUiState(Mode.READY, identifyType, identifyValue, asset,
                currentUser, serverDate, UhfScanState.IDLE, error, result);
    }

    public Mode getMode() {
        return mode;
    }

    public String getIdentifyType() {
        return identifyType;
    }

    public String getIdentifyValue() {
        return identifyValue;
    }

    public PdaAssetIdentifyDto getAsset() {
        return asset;
    }

    public PdaUserDto getCurrentUser() {
        return currentUser;
    }

    public String getServerDate() {
        return serverDate;
    }

    public UhfScanState getScanState() {
        return scanState;
    }

    public String getMessage() {
        return message;
    }

    public PdaRepairSubmitResultDto getResult() {
        return result;
    }

    public boolean isEpcMode() {
        return "EPC".equals(identifyType);
    }

    public boolean isBusy() {
        return mode == Mode.INITIALIZING || mode == Mode.IDENTIFYING || mode == Mode.SUBMITTING;
    }

    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING;
    }
}
