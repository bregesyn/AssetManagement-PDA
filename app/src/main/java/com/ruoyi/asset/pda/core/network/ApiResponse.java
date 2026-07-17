package com.ruoyi.asset.pda.core.network;

import com.google.gson.annotations.SerializedName;

/**
 * 与后端真实 AjaxResult 对齐的最小响应外壳。
 */
public final class ApiResponse<T> {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_SESSION_EXPIRED = 401;

    @SerializedName("code")
    private Integer code;

    @SerializedName("msg")
    private String message;

    @SerializedName("data")
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return code != null && code == CODE_SUCCESS;
    }

    public boolean isSessionExpired() {
        return code != null && code == CODE_SESSION_EXPIRED;
    }
}
