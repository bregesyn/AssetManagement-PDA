package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 借出审批提交回执；资产状态仍由审批处理器负责变更。 */
public final class PdaBorrowIssueBatchSubmitDto {
    @SerializedName("orderId")
    private Long orderId;
    @SerializedName("borrowNo")
    private String borrowNo;
    @SerializedName("borrowerType")
    private String borrowerType;
    @SerializedName("borrowUserId")
    private Long borrowUserId;
    @SerializedName("borrowUserName")
    private String borrowUserName;
    @SerializedName("borrowDeptId")
    private Long borrowDeptId;
    @SerializedName("borrowDeptName")
    private String borrowDeptName;
    @SerializedName("orderStatus")
    private String orderStatus;
    @SerializedName("applicantUserId")
    private Long applicantUserId;
    @SerializedName("applicantUserName")
    private String applicantUserName;
    @SerializedName("approvalTask")
    private PdaApprovalTaskSnapshotDto approvalTask;
    @SerializedName("totalCount")
    private int totalCount;
    @SerializedName("successCount")
    private int successCount;
    @SerializedName("rows")
    private List<Row> rows;

    public Long getOrderId() { return orderId; }
    public String getBorrowNo() { return borrowNo; }
    public String getBorrowerType() { return borrowerType; }
    public Long getBorrowUserId() { return borrowUserId; }
    public String getBorrowUserName() { return borrowUserName; }
    public Long getBorrowDeptId() { return borrowDeptId; }
    public String getBorrowDeptName() { return borrowDeptName; }
    public String getOrderStatus() { return orderStatus; }
    public Long getApplicantUserId() { return applicantUserId; }
    public String getApplicantUserName() { return applicantUserName; }
    public PdaApprovalTaskSnapshotDto getApprovalTask() { return approvalTask; }
    public int getTotalCount() { return totalCount; }
    public int getSuccessCount() { return successCount; }
    public List<Row> getRows() { return rows; }

    public static final class Row {
        @SerializedName("assetId")
        private Long assetId;
        @SerializedName("assetCode")
        private String assetCode;
        @SerializedName("assetName")
        private String assetName;
        @SerializedName("status")
        private String status;

        public Long getAssetId() { return assetId; }
        public String getAssetCode() { return assetCode; }
        public String getAssetName() { return assetName; }
        public String getStatus() { return status; }
    }
}
