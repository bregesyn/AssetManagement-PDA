package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaRfidTagDto {
    @SerializedName("tagId") private Long tagId;
    @SerializedName("tagCode") private String tagCode;
    @SerializedName("epcCode") private String epcCode;
    @SerializedName("tagStatus") private String tagStatus;
    @SerializedName("tagStatusName") private String tagStatusName;
    @SerializedName("bindStatus") private String bindStatus;
    @SerializedName("bindStatusName") private String bindStatusName;
    @SerializedName("assetId") private Long assetId;
    @SerializedName("assetCode") private String assetCode;
    @SerializedName("assetName") private String assetName;
    @SerializedName("rfidBound") private boolean rfidBound;

    public PdaRfidTagDto() {
    }

    public PdaRfidTagDto(Long tagId, String tagCode, String epcCode,
            String tagStatus, String tagStatusName, String bindStatus,
            String bindStatusName, Long assetId, String assetCode,
            String assetName, boolean rfidBound) {
        this.tagId = tagId;
        this.tagCode = tagCode;
        this.epcCode = epcCode;
        this.tagStatus = tagStatus;
        this.tagStatusName = tagStatusName;
        this.bindStatus = bindStatus;
        this.bindStatusName = bindStatusName;
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.rfidBound = rfidBound;
    }

    public Long getTagId() { return tagId; }
    public String getTagCode() { return tagCode; }
    public String getEpcCode() { return epcCode; }
    public String getTagStatus() { return tagStatus; }
    public String getTagStatusName() { return tagStatusName; }
    public String getBindStatus() { return bindStatus; }
    public String getBindStatusName() { return bindStatusName; }
    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public boolean isRfidBound() { return rfidBound; }

    public boolean isNormalAndUnbound() {
        return "NORMAL".equals(tagStatus) && "UNBOUND".equals(bindStatus) && !rfidBound;
    }
}
