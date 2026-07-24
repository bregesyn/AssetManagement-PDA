package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 服务端单资产入库资格；Android 只展示结果，不复制资格规则。 */
public final class PdaInboundEligibilityDto {
    @SerializedName("assetId")
    private Long assetId;

    @SerializedName("assetCode")
    private String assetCode;

    @SerializedName("assetName")
    private String assetName;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("specModel")
    private String specModel;

    @SerializedName("brand")
    private String brand;

    @SerializedName("assetStatus")
    private String assetStatus;

    @SerializedName("assetStatusLabel")
    private String assetStatusLabel;

    @SerializedName("tagCode")
    private String tagCode;

    @SerializedName("eligible")
    private boolean eligible;

    @SerializedName("ineligibleReason")
    private String ineligibleReason;

    public PdaInboundEligibilityDto() {
    }

    public PdaInboundEligibilityDto(Long assetId, String assetCode, String assetName,
            String categoryName, String specModel, String brand, String assetStatus,
            String assetStatusLabel, String tagCode, boolean eligible,
            String ineligibleReason) {
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.categoryName = categoryName;
        this.specModel = specModel;
        this.brand = brand;
        this.assetStatus = assetStatus;
        this.assetStatusLabel = assetStatusLabel;
        this.tagCode = tagCode;
        this.eligible = eligible;
        this.ineligibleReason = ineligibleReason;
    }

    public Long getAssetId() {
        return assetId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getSpecModel() {
        return specModel;
    }

    public String getBrand() {
        return brand;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public String getAssetStatusLabel() {
        return assetStatusLabel;
    }

    public String getTagCode() {
        return tagCode;
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getIneligibleReason() {
        return ineligibleReason;
    }
}
