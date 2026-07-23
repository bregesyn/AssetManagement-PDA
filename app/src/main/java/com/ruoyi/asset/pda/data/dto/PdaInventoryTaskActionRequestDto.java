package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaInventoryTaskActionRequestDto {
    @SerializedName("taskNo") private final String taskNo;
    @SerializedName("remark") private final String remark;

    public PdaInventoryTaskActionRequestDto(String taskNo, String remark) {
        this.taskNo = taskNo;
        this.remark = remark;
    }

    public String getTaskNo() { return taskNo; }
    public String getRemark() { return remark; }
}
