package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 内部维修人搜索项，仅供开始维修时选择。 */
public final class PdaRepairerDto {
    @SerializedName("id") private Long id;
    @SerializedName("code") private String code;
    @SerializedName("name") private String name;
    @SerializedName("parentId") private Long parentId;
    @SerializedName("parentName") private String parentName;
    @SerializedName("phonenumber") private String phonenumber;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Long getParentId() { return parentId; }
    public String getParentName() { return parentName; }
    public String getPhonenumber() { return phonenumber; }
}
