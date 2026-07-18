package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录与 profile 共用的当前用户事实。
 */
public final class PdaUserDto {
    @SerializedName("userId")
    private Long userId;

    @SerializedName("loginName")
    private String loginName;

    @SerializedName("userName")
    private String userName;

    @SerializedName("deptId")
    private Long deptId;

    @SerializedName("deptName")
    private String deptName;

    @SerializedName("permissions")
    private List<String> permissions = new ArrayList<>();

    public PdaUserDto() {
    }

    public PdaUserDto(Long userId, String loginName, String userName, Long deptId,
            String deptName, List<String> permissions) {
        this.userId = userId;
        this.loginName = loginName;
        this.userName = userName;
        this.deptId = deptId;
        this.deptName = deptName;
        this.permissions = permissions;
    }

    public Long getUserId() {
        return userId;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getUserName() {
        return userName;
    }

    public Long getDeptId() {
        return deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
