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

    public PdaMasterDataDto() {
    }

    public PdaMasterDataDto(Long id, String code, String name, Long parentId,
            String parentName) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
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
}
