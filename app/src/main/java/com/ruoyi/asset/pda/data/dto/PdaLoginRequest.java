package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PDA 登录请求；当前没有真实设备编号时保持为空，禁止伪造示例值。
 */
public final class PdaLoginRequest {
    @SerializedName("username")
    private final String username;

    @SerializedName("password")
    private final String password;

    @SerializedName("deviceNo")
    private final String deviceNo;

    public PdaLoginRequest(String username, String password, String deviceNo) {
        this.username = username;
        this.password = password;
        this.deviceNo = deviceNo;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceNo() {
        return deviceNo;
    }
}
