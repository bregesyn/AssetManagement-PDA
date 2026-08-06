package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 借出/归还共用的逐标识预检结果。 */
public final class PdaBorrowBatchCheckDto {
    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("eligibleCount")
    private int eligibleCount;

    @SerializedName("ineligibleCount")
    private int ineligibleCount;

    @SerializedName("unknownCount")
    private int unknownCount;

    @SerializedName("duplicateCount")
    private int duplicateCount;

    @SerializedName("rows")
    private List<Row> rows;

    public int getTotalCount() { return totalCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getIneligibleCount() { return ineligibleCount; }
    public int getUnknownCount() { return unknownCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public List<Row> getRows() { return rows; }

    public static final class Row {
        @SerializedName("identifyType")
        private String identifyType;
        @SerializedName("identifyValue")
        private String identifyValue;
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
        @SerializedName("status")
        private String status;
        @SerializedName("message")
        private String message;
        @SerializedName("orderId")
        private Long orderId;
        @SerializedName("borrowNo")
        private String borrowNo;
        @SerializedName("orderStatus")
        private String orderStatus;
        @SerializedName("itemId")
        private Long itemId;
        @SerializedName("returnStatus")
        private String returnStatus;
        @SerializedName("beforeWarehouseId")
        private Long beforeWarehouseId;
        @SerializedName("beforeWarehouseCode")
        private String beforeWarehouseCode;
        @SerializedName("beforeWarehouseName")
        private String beforeWarehouseName;
        @SerializedName("beforeLocationId")
        private Long beforeLocationId;
        @SerializedName("beforeLocationCode")
        private String beforeLocationCode;
        @SerializedName("beforeLocationName")
        private String beforeLocationName;

        public String getIdentifyType() { return identifyType; }
        public String getIdentifyValue() { return identifyValue; }
        public Long getAssetId() { return assetId; }
        public String getAssetCode() { return assetCode; }
        public String getAssetName() { return assetName; }
        public String getCategoryName() { return categoryName; }
        public String getSpecModel() { return specModel; }
        public String getBrand() { return brand; }
        public String getAssetStatus() { return assetStatus; }
        public String getAssetStatusLabel() { return assetStatusLabel; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public Long getOrderId() { return orderId; }
        public String getBorrowNo() { return borrowNo; }
        public String getOrderStatus() { return orderStatus; }
        public Long getItemId() { return itemId; }
        public String getReturnStatus() { return returnStatus; }
        public Long getBeforeWarehouseId() { return beforeWarehouseId; }
        public String getBeforeWarehouseCode() { return beforeWarehouseCode; }
        public String getBeforeWarehouseName() { return beforeWarehouseName; }
        public Long getBeforeLocationId() { return beforeLocationId; }
        public String getBeforeLocationCode() { return beforeLocationCode; }
        public String getBeforeLocationName() { return beforeLocationName; }
    }
}
