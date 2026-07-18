package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页启动配置；服务时间按后端统一 JSON 日期格式保留为字符串。
 */
public final class PdaBootstrapDto {
    @SerializedName("serverTime")
    private String serverTime;

    @SerializedName("currentUser")
    private PdaUserDto currentUser;

    @SerializedName("dicts")
    private Map<String, List<PdaDictItemDto>> dicts = new LinkedHashMap<>();

    @SerializedName("features")
    private Map<String, Boolean> features = new LinkedHashMap<>();

    public PdaBootstrapDto() {
    }

    public PdaBootstrapDto(String serverTime, PdaUserDto currentUser,
            Map<String, List<PdaDictItemDto>> dicts, Map<String, Boolean> features) {
        this.serverTime = serverTime;
        this.currentUser = currentUser;
        this.dicts = dicts;
        this.features = features;
    }

    public String getServerTime() {
        return serverTime;
    }

    public PdaUserDto getCurrentUser() {
        return currentUser;
    }

    public Map<String, List<PdaDictItemDto>> getDicts() {
        return dicts;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }
}
