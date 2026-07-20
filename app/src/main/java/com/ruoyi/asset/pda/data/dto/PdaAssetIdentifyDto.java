package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 资产识别接口返回的服务端事实，不在 Android 端补算业务状态。 */
public final class PdaAssetIdentifyDto {
    @SerializedName("assetId") private Long assetId;
    @SerializedName("assetCode") private String assetCode;
    @SerializedName("assetName") private String assetName;
    @SerializedName("categoryId") private Long categoryId;
    @SerializedName("categoryName") private String categoryName;
    @SerializedName("specModel") private String specModel;
    @SerializedName("brand") private String brand;
    @SerializedName("assetStatus") private String assetStatus;
    @SerializedName("assetStatusName") private String assetStatusName;
    @SerializedName("warehouseId") private Long warehouseId;
    @SerializedName("warehouseName") private String warehouseName;
    @SerializedName("locationId") private Long locationId;
    @SerializedName("locationName") private String locationName;
    @SerializedName("tagId") private Long tagId;
    @SerializedName("tagCode") private String tagCode;
    @SerializedName("epcCode") private String epcCode;
    @SerializedName("tagStatus") private String tagStatus;
    @SerializedName("tagStatusName") private String tagStatusName;
    @SerializedName("bindStatus") private String bindStatus;
    @SerializedName("bindStatusName") private String bindStatusName;
    @SerializedName("rfidBound") private boolean rfidBound;

    public PdaAssetIdentifyDto() {
    }

    public PdaAssetIdentifyDto(Long assetId, String assetCode, String assetName,
            Long categoryId, String categoryName, String specModel, String brand,
            String assetStatus, String assetStatusName, Long warehouseId,
            String warehouseName, Long locationId, String locationName, Long tagId,
            String tagCode, String epcCode, String tagStatus, String tagStatusName,
            String bindStatus, String bindStatusName, boolean rfidBound) {
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.specModel = specModel;
        this.brand = brand;
        this.assetStatus = assetStatus;
        this.assetStatusName = assetStatusName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.locationId = locationId;
        this.locationName = locationName;
        this.tagId = tagId;
        this.tagCode = tagCode;
        this.epcCode = epcCode;
        this.tagStatus = tagStatus;
        this.tagStatusName = tagStatusName;
        this.bindStatus = bindStatus;
        this.bindStatusName = bindStatusName;
        this.rfidBound = rfidBound;
    }

    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getSpecModel() { return specModel; }
    public String getBrand() { return brand; }
    public String getAssetStatus() { return assetStatus; }
    public String getAssetStatusName() { return assetStatusName; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public Long getLocationId() { return locationId; }
    public String getLocationName() { return locationName; }
    public Long getTagId() { return tagId; }
    public String getTagCode() { return tagCode; }
    public String getEpcCode() { return epcCode; }
    public String getTagStatus() { return tagStatus; }
    public String getTagStatusName() { return tagStatusName; }
    public String getBindStatus() { return bindStatus; }
    public String getBindStatusName() { return bindStatusName; }
    public boolean isRfidBound() { return rfidBound; }
}
