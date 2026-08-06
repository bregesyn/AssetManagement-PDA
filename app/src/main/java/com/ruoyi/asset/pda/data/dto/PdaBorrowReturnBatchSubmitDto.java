package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 归还审批提交回执；一条归还明细对应一个审批任务。 */
public final class PdaBorrowReturnBatchSubmitDto {
    @SerializedName("totalCount")
    private int totalCount;
    @SerializedName("successCount")
    private int successCount;
    @SerializedName("rows")
    private List<Row> rows;

    public int getTotalCount() { return totalCount; }
    public int getSuccessCount() { return successCount; }
    public List<Row> getRows() { return rows; }

    public static final class Row {
        @SerializedName("orderId")
        private Long orderId;
        @SerializedName("borrowNo")
        private String borrowNo;
        @SerializedName("itemId")
        private Long itemId;
        @SerializedName("assetId")
        private Long assetId;
        @SerializedName("assetCode")
        private String assetCode;
        @SerializedName("assetName")
        private String assetName;
        @SerializedName("targetWarehouseId")
        private Long targetWarehouseId;
        @SerializedName("targetWarehouseCode")
        private String targetWarehouseCode;
        @SerializedName("targetWarehouseName")
        private String targetWarehouseName;
        @SerializedName("targetLocationId")
        private Long targetLocationId;
        @SerializedName("targetLocationCode")
        private String targetLocationCode;
        @SerializedName("targetLocationName")
        private String targetLocationName;
        @SerializedName("returnStatus")
        private String returnStatus;
        @SerializedName("approvalTask")
        private PdaApprovalTaskSnapshotDto approvalTask;

        public Long getOrderId() { return orderId; }
        public String getBorrowNo() { return borrowNo; }
        public Long getItemId() { return itemId; }
        public Long getAssetId() { return assetId; }
        public String getAssetCode() { return assetCode; }
        public String getAssetName() { return assetName; }
        public Long getTargetWarehouseId() { return targetWarehouseId; }
        public String getTargetWarehouseCode() { return targetWarehouseCode; }
        public String getTargetWarehouseName() { return targetWarehouseName; }
        public Long getTargetLocationId() { return targetLocationId; }
        public String getTargetLocationCode() { return targetLocationCode; }
        public String getTargetLocationName() { return targetLocationName; }
        public String getReturnStatus() { return returnStatus; }
        public PdaApprovalTaskSnapshotDto getApprovalTask() { return approvalTask; }
    }
}
