package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** PDA 借用审批任务的只读回执，客户端不据此推进资产状态。 */
public final class PdaApprovalTaskSnapshotDto {
    @SerializedName("taskId")
    private Long taskId;

    @SerializedName("approvalScene")
    private String approvalScene;

    @SerializedName("taskRound")
    private Integer taskRound;

    @SerializedName("businessId")
    private Long businessId;

    @SerializedName("businessNo")
    private String businessNo;

    @SerializedName("applicantUserId")
    private Long applicantUserId;

    @SerializedName("applicantUserName")
    private String applicantUserName;

    @SerializedName("taskStatus")
    private String taskStatus;

    public Long getTaskId() { return taskId; }
    public String getApprovalScene() { return approvalScene; }
    public Integer getTaskRound() { return taskRound; }
    public Long getBusinessId() { return businessId; }
    public String getBusinessNo() { return businessNo; }
    public Long getApplicantUserId() { return applicantUserId; }
    public String getApplicantUserName() { return applicantUserName; }
    public String getTaskStatus() { return taskStatus; }
}
