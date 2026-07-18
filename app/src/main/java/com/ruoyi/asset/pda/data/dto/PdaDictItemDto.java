package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PDA 白名单字典轻量项。
 */
public final class PdaDictItemDto {
    @SerializedName("dictType")
    private String dictType;

    @SerializedName("label")
    private String label;

    @SerializedName("value")
    private String value;

    @SerializedName("listClass")
    private String listClass;

    @SerializedName("cssClass")
    private String cssClass;

    @SerializedName("isDefault")
    private String isDefault;

    public PdaDictItemDto() {
    }

    public PdaDictItemDto(String dictType, String label, String value, String listClass,
            String cssClass, String isDefault) {
        this.dictType = dictType;
        this.label = label;
        this.value = value;
        this.listClass = listClass;
        this.cssClass = cssClass;
        this.isDefault = isDefault;
    }

    public String getDictType() {
        return dictType;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getListClass() {
        return listClass;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getIsDefault() {
        return isDefault;
    }
}
