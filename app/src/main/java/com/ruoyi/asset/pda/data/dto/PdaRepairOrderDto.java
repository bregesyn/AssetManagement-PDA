package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/** PDA 报修单只读快照；状态和资产归属始终以后端回执为准。 */
public final class PdaRepairOrderDto {
    @SerializedName("repairId") private Long repairId;
    @SerializedName("repairNo") private String repairNo;
    @SerializedName("assetId") private Long assetId;
    @SerializedName("assetCode") private String assetCode;
    @SerializedName("assetName") private String assetName;
    @SerializedName("categoryId") private Long categoryId;
    @SerializedName("categoryCode") private String categoryCode;
    @SerializedName("categoryName") private String categoryName;
    @SerializedName("specModel") private String specModel;
    @SerializedName("brand") private String brand;
    @SerializedName("reportUserId") private Long reportUserId;
    @SerializedName("reportUserName") private String reportUserName;
    @SerializedName("faultDesc") private String faultDesc;
    @SerializedName("reportTime") private String reportTime;
    @SerializedName("expectedFinishTime") private String expectedFinishTime;
    @SerializedName("repairerType") private String repairerType;
    @SerializedName("repairUserId") private Long repairUserId;
    @SerializedName("repairUserName") private String repairUserName;
    @SerializedName("repairOrgName") private String repairOrgName;
    @SerializedName("repairContactPhone") private String repairContactPhone;
    @SerializedName("repairStartTime") private String repairStartTime;
    @SerializedName("repairFinishTime") private String repairFinishTime;
    @SerializedName("repairResult") private String repairResult;
    @SerializedName("repairCost") private BigDecimal repairCost;
    @SerializedName("beforeAssetStatus") private String beforeAssetStatus;
    @SerializedName("rejectReason") private String rejectReason;
    @SerializedName("orderStatus") private String orderStatus;
    @SerializedName("approvalTaskId") private Long approvalTaskId;
    @SerializedName("approvalTaskStatus") private String approvalTaskStatus;
    @SerializedName("approvalTaskRound") private Integer approvalTaskRound;
    @SerializedName("remark") private String remark;

    public Long getRepairId() { return repairId; }
    public String getRepairNo() { return repairNo; }
    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public String getSpecModel() { return specModel; }
    public String getBrand() { return brand; }
    public Long getReportUserId() { return reportUserId; }
    public String getReportUserName() { return reportUserName; }
    public String getFaultDesc() { return faultDesc; }
    public String getReportTime() { return reportTime; }
    public String getExpectedFinishTime() { return expectedFinishTime; }
    public String getRepairerType() { return repairerType; }
    public Long getRepairUserId() { return repairUserId; }
    public String getRepairUserName() { return repairUserName; }
    public String getRepairOrgName() { return repairOrgName; }
    public String getRepairContactPhone() { return repairContactPhone; }
    public String getRepairStartTime() { return repairStartTime; }
    public String getRepairFinishTime() { return repairFinishTime; }
    public String getRepairResult() { return repairResult; }
    public BigDecimal getRepairCost() { return repairCost; }
    public String getBeforeAssetStatus() { return beforeAssetStatus; }
    public String getRejectReason() { return rejectReason; }
    public String getOrderStatus() { return orderStatus; }
    public Long getApprovalTaskId() { return approvalTaskId; }
    public String getApprovalTaskStatus() { return approvalTaskStatus; }
    public Integer getApprovalTaskRound() { return approvalTaskRound; }
    public String getRemark() { return remark; }
}
