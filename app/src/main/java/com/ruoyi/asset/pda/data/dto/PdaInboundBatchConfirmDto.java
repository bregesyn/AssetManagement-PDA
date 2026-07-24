package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PdaInboundBatchConfirmDto {
    @SerializedName("orderId")
    private Long orderId;

    @SerializedName("inboundNo")
    private String inboundNo;

    @SerializedName("warehouseName")
    private String warehouseName;

    @SerializedName("locationName")
    private String locationName;

    @SerializedName("inboundUserName")
    private String inboundUserName;

    @SerializedName("inboundTime")
    private String inboundTime;

    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("successCount")
    private int successCount;

    @SerializedName("rows")
    private List<Row> rows;

    public PdaInboundBatchConfirmDto() {
    }

    public PdaInboundBatchConfirmDto(Long orderId, String inboundNo, String warehouseName,
            String locationName, String inboundUserName, String inboundTime,
            int totalCount, int successCount, List<Row> rows) {
        this.orderId = orderId;
        this.inboundNo = inboundNo;
        this.warehouseName = warehouseName;
        this.locationName = locationName;
        this.inboundUserName = inboundUserName;
        this.inboundTime = inboundTime;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.rows = rows;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getInboundNo() {
        return inboundNo;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getInboundUserName() {
        return inboundUserName;
    }

    public String getInboundTime() {
        return inboundTime;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public List<Row> getRows() {
        return rows;
    }

    public static final class Row {
        @SerializedName("assetId")
        private Long assetId;

        @SerializedName("assetCode")
        private String assetCode;

        @SerializedName("assetName")
        private String assetName;

        @SerializedName("status")
        private String status;

        public Row() {
        }

        public Row(Long assetId, String assetCode, String assetName, String status) {
            this.assetId = assetId;
            this.assetCode = assetCode;
            this.assetName = assetName;
            this.status = status;
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

        public String getStatus() {
            return status;
        }
    }
}
