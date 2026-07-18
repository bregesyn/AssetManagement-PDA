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
    private final boolean showInventory;
    private final int errorTextResId;
    private final String errorMessage;

    private HomeUiState(Mode mode, String loginName, String userName, String deptName,
            String serverTime, boolean showTagCreate, boolean showRfidBind,
            boolean showRfidUnbind, boolean showInventory, int errorTextResId,
            String errorMessage) {
        this.mode = mode;
        this.loginName = loginName;
        this.userName = userName;
        this.deptName = deptName;
        this.serverTime = serverTime;
        this.showTagCreate = showTagCreate;
        this.showRfidBind = showRfidBind;
        this.showRfidUnbind = showRfidUnbind;
        this.showInventory = showInventory;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static HomeUiState loading() {
        return new HomeUiState(Mode.LOADING, null, null, null, null,
                false, false, false, false, 0, null);
    }

    public static HomeUiState content(String loginName, String userName, String deptName,
            String serverTime, boolean showTagCreate, boolean showRfidBind,
            boolean showRfidUnbind, boolean showInventory) {
        return new HomeUiState(Mode.CONTENT, loginName, userName, deptName, serverTime,
                showTagCreate, showRfidBind, showRfidUnbind, showInventory, 0, null);
    }

    public static HomeUiState error(@StringRes int errorTextResId) {
        return new HomeUiState(Mode.ERROR, null, null, null, null,
                false, false, false, false, errorTextResId, null);
    }

    public static HomeUiState error(String errorMessage) {
        return new HomeUiState(Mode.ERROR, null, null, null, null,
                false, false, false, false, 0, errorMessage);
    }

    public HomeUiState asLoggingOut() {
        return new HomeUiState(Mode.LOGGING_OUT, loginName, userName, deptName, serverTime,
                showTagCreate, showRfidBind, showRfidUnbind, showInventory, 0, null);
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

    public boolean isShowInventory() {
        return showInventory;
    }

    public int getErrorTextResId() {
        return errorTextResId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
