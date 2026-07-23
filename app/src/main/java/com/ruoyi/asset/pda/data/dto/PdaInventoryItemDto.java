package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 应盘明细；账面快照和当前台账分开保存，避免现场误把当前值当作盘点事实。 */
public final class PdaInventoryItemDto {
    @SerializedName("itemId") private Long itemId;
    @SerializedName("taskId") private Long taskId;
    @SerializedName("taskNo") private String taskNo;
    @SerializedName("assetId") private Long assetId;
    @SerializedName("assetCode") private String assetCode;
    @SerializedName("assetName") private String assetName;
    @SerializedName("categoryId") private Long categoryId;
    @SerializedName("categoryName") private String categoryName;
    @SerializedName("specModel") private String specModel;
    @SerializedName("brand") private String brand;
    @SerializedName("bookAssetStatus") private String bookAssetStatus;
    @SerializedName("bookWarehouseId") private Long bookWarehouseId;
    @SerializedName("bookWarehouseName") private String bookWarehouseName;
    @SerializedName("bookLocationId") private Long bookLocationId;
    @SerializedName("bookLocationName") private String bookLocationName;
    @SerializedName("currentAssetStatus") private String currentAssetStatus;
    @SerializedName("currentWarehouseId") private Long currentWarehouseId;
    @SerializedName("currentWarehouseName") private String currentWarehouseName;
    @SerializedName("currentLocationId") private Long currentLocationId;
    @SerializedName("currentLocationName") private String currentLocationName;
    @SerializedName("ledgerChanged") private Boolean ledgerChanged;
    @SerializedName("inventoryWarehouseId") private Long inventoryWarehouseId;
    @SerializedName("inventoryWarehouseName") private String inventoryWarehouseName;
    @SerializedName("inventoryLocationId") private Long inventoryLocationId;
    @SerializedName("inventoryLocationName") private String inventoryLocationName;
    @SerializedName("inventoryResult") private String inventoryResult;
    @SerializedName("inventoryUserId") private Long inventoryUserId;
    @SerializedName("inventoryUserName") private String inventoryUserName;
    @SerializedName("inventoryTime") private String inventoryTime;
    @SerializedName("remark") private String remark;

    public PdaInventoryItemDto() {
    }

    public Long getItemId() { return itemId; }
    public Long getTaskId() { return taskId; }
    public String getTaskNo() { return taskNo; }
    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getSpecModel() { return specModel; }
    public String getBrand() { return brand; }
    public String getBookAssetStatus() { return bookAssetStatus; }
    public Long getBookWarehouseId() { return bookWarehouseId; }
    public String getBookWarehouseName() { return bookWarehouseName; }
    public Long getBookLocationId() { return bookLocationId; }
    public String getBookLocationName() { return bookLocationName; }
    public String getCurrentAssetStatus() { return currentAssetStatus; }
    public Long getCurrentWarehouseId() { return currentWarehouseId; }
    public String getCurrentWarehouseName() { return currentWarehouseName; }
    public Long getCurrentLocationId() { return currentLocationId; }
    public String getCurrentLocationName() { return currentLocationName; }
    public Boolean getLedgerChanged() { return ledgerChanged; }
    public Long getInventoryWarehouseId() { return inventoryWarehouseId; }
    public String getInventoryWarehouseName() { return inventoryWarehouseName; }
    public Long getInventoryLocationId() { return inventoryLocationId; }
    public String getInventoryLocationName() { return inventoryLocationName; }
    public String getInventoryResult() { return inventoryResult; }
    public Long getInventoryUserId() { return inventoryUserId; }
    public String getInventoryUserName() { return inventoryUserName; }
    public String getInventoryTime() { return inventoryTime; }
    public String getRemark() { return remark; }
}
