package com.ruoyi.asset.pda.feature.home;

import androidx.annotation.StringRes;

public final class HomeUiState {
    public enum Mode {
        LOADING,
        CONTENT,
        ERROR,
        LOGGING_OUT
    }

    private final Mode mode;
    private final String loginName;
    private final String userName;
    private final String deptName;
    private final String serverTime;
    private final boolean showTagCreate;
    private final boolean showRfidBind;
    private final boolean showRfidUnbind;
    private final boolean showInbound;
    private final boolean showInboundConfirm;
    private final boolean showInventory;
    private final boolean showInventorySubmit;
    private final int errorTextResId;
    private final String errorMessage;

    private HomeUiState(Mode mode, String loginName, String userName, String deptName,
            String serverTime, boolean showTagCreate, boolean showRfidBind,
            boolean showRfidUnbind, boolean showInbound, boolean showInboundConfirm,
            boolean showInventory, boolean showInventorySubmit, int errorTextResId,
            String errorMessage) {
        this.mode = mode;
        this.loginName = loginName;
        this.userName = userName;
        this.deptName = deptName;
        this.serverTime = serverTime;
        this.showTagCreate = showTagCreate;
        this.showRfidBind = showRfidBind;
        this.showRfidUnbind = showRfidUnbind;
        this.showInbound = showInbound;
        this.showInboundConfirm = showInboundConfirm;
        this.showInventory = showInventory;
        this.showInventorySubmit = showInventorySubmit;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static HomeUiState loading() {
        return new HomeUiState(Mode.LOADING, null, null, null, null,
                false, false, false, false, false, false, false, 0, null);
    }

    public static HomeUiState content(String loginName, String userName, String deptName,
            String serverTime, boolean showTagCreate, boolean showRfidBind,
            boolean showRfidUnbind, boolean showInventory) {
        return content(loginName, userName, deptName, serverTime, showTagCreate,
                showRfidBind, showRfidUnbind, false, false, showInventory, false);
    }

    public static HomeUiState content(String loginName, String userName, String deptName,
            String serverTime, boolean showTagCreate, boolean showRfidBind,
            boolean showRfidUnbind, boolean showInventory, boolean showInventorySubmit) {
        return new HomeUiState(Mode.CONTENT, loginName, userName, deptName, serverTime,
                showTagCreate, showRfidBind, showRfidUnbind, false, false,
                showInventory, showInventorySubmit, 0, null);
    }

    public static HomeUiState content(String loginName, String userName, String deptName,
            String serverTime, boolean showTagCreate, boolean showRfidBind,
            boolean showRfidUnbind, boolean showInbound, boolean showInboundConfirm,
            boolean showInventory, boolean showInventorySubmit) {
        return new HomeUiState(Mode.CONTENT, loginName, userName, deptName, serverTime,
                showTagCreate, showRfidBind, showRfidUnbind, showInbound,
                showInboundConfirm, showInventory, showInventorySubmit, 0, null);
    }

    public static HomeUiState error(@StringRes int errorTextResId) {
        return new HomeUiState(Mode.ERROR, null, null, null, null,
                false, false, false, false, false, false, false,
                errorTextResId, null);
    }

    public static HomeUiState error(String errorMessage) {
        return new HomeUiState(Mode.ERROR, null, null, null, null,
                false, false, false, false, false, false, false, 0, errorMessage);
    }

    public HomeUiState asLoggingOut() {
        return new HomeUiState(Mode.LOGGING_OUT, loginName, userName, deptName, serverTime,
                showTagCreate, showRfidBind, showRfidUnbind, showInbound,
                showInboundConfirm, showInventory, showInventorySubmit, 0, null);
    }

    public Mode getMode() {
        return mode;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getUserName() {
        return userName;
    }

    public String getDeptName() {
        return deptName;
    }

    public String getServerTime() {
        return serverTime;
    }

    public boolean isShowTagCreate() {
        return showTagCreate;
    }

    public boolean isShowRfidBind() {
        return showRfidBind;
    }

    public boolean isShowRfidUnbind() {
        return showRfidUnbind;
    }

    public boolean isShowInbound() {
        return showInbound;
    }

    public boolean isShowInboundConfirm() {
        return showInboundConfirm;
    }

    public boolean isShowInventory() {
        return showInventory;
    }

    public boolean isShowInventorySubmit() {
        return showInventorySubmit;
    }

    public int getErrorTextResId() {
        return errorTextResId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
