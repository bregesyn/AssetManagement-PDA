package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 只包含客户端可提交字段；操作人始终由后端当前 Session 决定。 */
public final class RfidTagBatchCreateRequest {
    @SerializedName("epcCodes") private final List<String> epcCodes;
    @SerializedName("remark") private final String remark;

    public RfidTagBatchCreateRequest(List<String> epcCodes, String remark) {
        this.epcCodes = epcCodes;
        this.remark = remark;
    }

    public List<String> getEpcCodes() { return epcCodes; }
    public String getRemark() { return remark; }
}
