package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 盘盈行既用于展示，也用于删除时携带稳定的 surplusId。 */
public final class PdaInventorySurplusDto {
    @SerializedName("surplusId") private Long surplusId;
    @SerializedName("taskId") private Long taskId;
    @SerializedName("taskNo") private String taskNo;
    @SerializedName("surplusType") private String surplusType;
    @SerializedName("identifyMethod") private String identifyMethod;
    @SerializedName("assetId") private Long assetId;
    @SerializedName("assetCode") private String assetCode;
    @SerializedName("assetName") private String assetName;
    @SerializedName("categoryId") private Long categoryId;
    @SerializedName("categoryName") private String categoryName;
    @SerializedName("specModel") private String specModel;
    @SerializedName("brand") private String brand;
    @SerializedName("tagId") private Long tagId;
    @SerializedName("tagCode") private String tagCode;
    @SerializedName("epcCode") private String epcCode;
    @SerializedName("bookAssetStatus") private String bookAssetStatus;
    @SerializedName("bookWarehouseId") private Long bookWarehouseId;
    @SerializedName("bookWarehouseName") private String bookWarehouseName;
    @SerializedName("bookLocationId") private Long bookLocationId;
    @SerializedName("bookLocationName") private String bookLocationName;
    @SerializedName("inventoryWarehouseId") private Long inventoryWarehouseId;
    @SerializedName("inventoryWarehouseName") private String inventoryWarehouseName;
    @SerializedName("inventoryLocationId") private Long inventoryLocationId;
    @SerializedName("inventoryLocationName") private String inventoryLocationName;
    @SerializedName("inventoryUserId") private Long inventoryUserId;
    @SerializedName("inventoryUserName") private String inventoryUserName;
    @SerializedName("inventoryTime") private String inventoryTime;
    @SerializedName("confirmStatus") private String confirmStatus;
    @SerializedName("remark") private String remark;
    @SerializedName("created") private boolean created;

    public PdaInventorySurplusDto() {
    }

    public Long getSurplusId() { return surplusId; }
    public Long getTaskId() { return taskId; }
    public String getTaskNo() { return taskNo; }
    public String getSurplusType() { return surplusType; }
    public String getIdentifyMethod() { return identifyMethod; }
    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getSpecModel() { return specModel; }
    public String getBrand() { return brand; }
    public Long getTagId() { return tagId; }
    public String getTagCode() { return tagCode; }
    public String getEpcCode() { return epcCode; }
    public String getBookAssetStatus() { return bookAssetStatus; }
    public Long getBookWarehouseId() { return bookWarehouseId; }
    public String getBookWarehouseName() { return bookWarehouseName; }
    public Long getBookLocationId() { return bookLocationId; }
    public String getBookLocationName() { return bookLocationName; }
    public Long getInventoryWarehouseId() { return inventoryWarehouseId; }
    public String getInventoryWarehouseName() { return inventoryWarehouseName; }
    public Long getInventoryLocationId() { return inventoryLocationId; }
    public String getInventoryLocationName() { return inventoryLocationName; }
    public Long getInventoryUserId() { return inventoryUserId; }
    public String getInventoryUserName() { return inventoryUserName; }
    public String getInventoryTime() { return inventoryTime; }
    public String getConfirmStatus() { return confirmStatus; }
    public String getRemark() { return remark; }
    public boolean isCreated() { return created; }
}
