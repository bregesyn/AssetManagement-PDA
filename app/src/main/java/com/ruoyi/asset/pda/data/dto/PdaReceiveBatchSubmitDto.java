package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * PDA领用申请回执。任务状态是 PENDING 时资产仍在库，不能展示为领用完成。
 */
public final class PdaReceiveBatchSubmitDto {
    @SerializedName("orderId") private Long orderId;
    @SerializedName("receiveNo") private String receiveNo;
    @SerializedName("receiveUserId") private Long receiveUserId;
    @SerializedName("receiveUserName") private String receiveUserName;
    @SerializedName("receiveDeptId") private Long receiveDeptId;
    @SerializedName("receiveDeptName") private String receiveDeptName;
    @SerializedName("applicantUserId") private Long applicantUserId;
    @SerializedName("applicantUserName") private String applicantUserName;
    @SerializedName("submitTime") private String submitTime;
    @SerializedName("orderStatus") private String orderStatus;
    @SerializedName("taskId") private Long taskId;
    @SerializedName("taskRound") private Integer taskRound;
    @SerializedName("taskStatus") private String taskStatus;
    @SerializedName("totalCount") private int totalCount;
    @SerializedName("successCount") private int successCount;
    @SerializedName("rows") private List<Row> rows;

    public PdaReceiveBatchSubmitDto() { }
    public Long getOrderId() { return orderId; }
    public String getReceiveNo() { return receiveNo; }
    public Long getReceiveUserId() { return receiveUserId; }
    public String getReceiveUserName() { return receiveUserName; }
    public Long getReceiveDeptId() { return receiveDeptId; }
    public String getReceiveDeptName() { return receiveDeptName; }
    public Long getApplicantUserId() { return applicantUserId; }
    public String getApplicantUserName() { return applicantUserName; }
    public String getSubmitTime() { return submitTime; }
    public String getOrderStatus() { return orderStatus; }
    public Long getTaskId() { return taskId; }
    public Integer getTaskRound() { return taskRound; }
    public String getTaskStatus() { return taskStatus; }
    public int getTotalCount() { return totalCount; }
    public int getSuccessCount() { return successCount; }
    public List<Row> getRows() { return rows; }

    public static final class Row {
        @SerializedName("assetId") private Long assetId;
        @SerializedName("assetCode") private String assetCode;
        @SerializedName("assetName") private String assetName;
        @SerializedName("status") private String status;
        public Row() { }
        public Long getAssetId() { return assetId; }
        public String getAssetCode() { return assetCode; }
        public String getAssetName() { return assetName; }
        public String getStatus() { return status; }
    }
}
