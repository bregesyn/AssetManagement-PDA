package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 与后端 PdaReceiveBatchCheckResult 一一对应，保留行顺序供现场异常核对。 */
public final class PdaReceiveBatchCheckDto {
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

    public PdaReceiveBatchCheckDto() {
    }

    public PdaReceiveBatchCheckDto(int totalCount, int eligibleCount,
            int ineligibleCount, int unknownCount, int duplicateCount,
            List<Row> rows) {
        this.totalCount = totalCount;
        this.eligibleCount = eligibleCount;
        this.ineligibleCount = ineligibleCount;
        this.unknownCount = unknownCount;
        this.duplicateCount = duplicateCount;
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

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public List<Row> getRows() {
        return rows;
    }

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

        public Row() {
        }

        public Row(String identifyType, String identifyValue, Long assetId,
                String assetCode, String assetName, String categoryName,
                String specModel, String brand, String assetStatus,
                String assetStatusLabel, String status, String message) {
            this.identifyType = identifyType;
            this.identifyValue = identifyValue;
            this.assetId = assetId;
            this.assetCode = assetCode;
            this.assetName = assetName;
            this.categoryName = categoryName;
            this.specModel = specModel;
            this.brand = brand;
            this.assetStatus = assetStatus;
            this.assetStatusLabel = assetStatusLabel;
            this.status = status;
            this.message = message;
        }

        public String getIdentifyType() {
            return identifyType;
        }

        public String getIdentifyValue() {
            return identifyValue;
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

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
