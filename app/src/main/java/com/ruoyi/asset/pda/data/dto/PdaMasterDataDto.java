package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 仓库、位置和资产类别共用的轻量主数据项。
 */
public final class PdaMasterDataDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("code")
    private String code;

    @SerializedName("name")
    private String name;

    @SerializedName("parentId")
    private Long parentId;

    @SerializedName("parentName")
    private String parentName;

    /** 外部借用选择内部联系人时回填的系统用户联系电话。 */
    @SerializedName("phonenumber")
    private String phoneNumber;

    public PdaMasterDataDto() {
    }

    public PdaMasterDataDto(Long id, String code, String name, Long parentId,
            String parentName) {
        this(id, code, name, parentId, parentName, null);
    }

    public PdaMasterDataDto(Long id, String code, String name, Long parentId,
            String parentName, String phoneNumber) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
