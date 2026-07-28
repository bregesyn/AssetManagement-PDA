package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 提交时只传预检合格标识，确认人和确认时间由服务端会话及事务生成。 */
public final class PdaReceiveBatchConfirmRequestDto
        extends PdaReceiveBatchCheckRequestDto {
    @SerializedName("remark")
    private final String remark;

    public PdaReceiveBatchConfirmRequestDto(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers, String remark) {
        super(receiveUserId, receiveDeptId, identifiers);
        this.remark = remark;
    }
}
