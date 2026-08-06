package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** PDA领用审批提交请求。请求结构与预检一致，服务端只创建待审批任务。 */
public final class PdaReceiveBatchSubmitRequestDto extends PdaReceiveBatchCheckRequestDto {
    @SerializedName("remark")
    private final String remark;

    public PdaReceiveBatchSubmitRequestDto(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers, String remark) {
        super(receiveUserId, receiveDeptId, identifiers);
        this.remark = remark;
    }
}
