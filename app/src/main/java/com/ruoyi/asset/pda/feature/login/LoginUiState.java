package com.ruoyi.asset.pda.feature.login;

import androidx.annotation.StringRes;

public final class LoginUiState {
    public enum Mode {
        FORM,
        SUBMITTING,
        RECOVERING,
        RECOVERY_ERROR
    }

    private final Mode mode;
    private final int errorTextResId;
    private final String errorMessage;

    private LoginUiState(Mode mode, int errorTextResId, String errorMessage) {
        this.mode = mode;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static LoginUiState form() {
        return new LoginUiState(Mode.FORM, 0, null);
    }

    public static LoginUiState formError(@StringRes int errorTextResId) {
        return new LoginUiState(Mode.FORM, errorTextResId, null);
    }

    public static LoginUiState formError(String errorMessage) {
        return new LoginUiState(Mode.FORM, 0, errorMessage);
    }

    public static LoginUiState submitting() {
        return new LoginUiState(Mode.SUBMITTING, 0, null);
    }

    public static LoginUiState recovering() {
        return new LoginUiState(Mode.RECOVERING, 0, null);
    }

    public static LoginUiState recoveryError(String errorMessage) {
        return new LoginUiState(Mode.RECOVERY_ERROR, 0, errorMessage);
    }

    public Mode getMode() {
        return mode;
    }

    public int getErrorTextResId() {
        return errorTextResId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
