package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 报修审批提交回执；审批通过前不代表资产已经进入维修。 */
public final class PdaRepairSubmitResultDto {
    @SerializedName("order") private PdaRepairOrderDto order;
    @SerializedName("approvalTask") private PdaApprovalTaskSnapshotDto approvalTask;

    public PdaRepairOrderDto getOrder() { return order; }
    public PdaApprovalTaskSnapshotDto getApprovalTask() { return approvalTask; }
}
