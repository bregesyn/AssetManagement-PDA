package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PdaInboundBatchCheckDto {
    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("eligibleCount")
    private int eligibleCount;

    @SerializedName("ineligibleCount")
    private int ineligibleCount;

    @SerializedName("unknownCount")
    private int unknownCount;

    @SerializedName("rows")
    private List<Row> rows;

    public PdaInboundBatchCheckDto() {
    }

    public PdaInboundBatchCheckDto(int totalCount, int eligibleCount, int ineligibleCount,
            int unknownCount, List<Row> rows) {
        this.totalCount = totalCount;
        this.eligibleCount = eligibleCount;
        this.ineligibleCount = ineligibleCount;
        this.unknownCount = unknownCount;
        this.rows = rows;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getEligibleCount() {
        return eligibleCount;
    }

    public int getIneligibleCount() {
        return ineligibleCount;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public List<Row> getRows() {
        return rows;
    }

    public static final class Row {
        @SerializedName("epcCode")
        private String epcCode;

        @SerializedName("assetId")
        private Long assetId;

        @SerializedName("assetCode")
        private String assetCode;

        @SerializedName("assetName")
        private String assetName;

        @SerializedName("categoryName")
        private String categoryName;

        @SerializedName("assetStatus")
        private String assetStatus;

        @SerializedName("assetStatusLabel")
        private String assetStatusLabel;

        @SerializedName("status")
        private String status;

        @SerializedName("message")
        private String message;

        public Row() {
        }

        public Row(String epcCode, Long assetId, String assetCode, String assetName,
                String categoryName, String assetStatus, String assetStatusLabel,
                String status, String message) {
            this.epcCode = epcCode;
            this.assetId = assetId;
            this.assetCode = assetCode;
            this.assetName = assetName;
            this.categoryName = categoryName;
            this.assetStatus = assetStatus;
            this.assetStatusLabel = assetStatusLabel;
            this.status = status;
            this.message = message;
        }

        public String getEpcCode() {
            return epcCode;
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

        public String getAssetStatus() {
            return assetStatus;
        }

        public String getAssetStatusLabel() {
            return assetStatusLabel;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
