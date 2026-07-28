package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** PDA 领用预检请求：同一批次只允许一个领用归属。 */
public class PdaReceiveBatchCheckRequestDto {
    @SerializedName("receiveUserId")
    private final Long receiveUserId;

    @SerializedName("receiveDeptId")
    private final Long receiveDeptId;

    @SerializedName("identifiers")
    private final List<PdaAssetIdentifyRequest> identifiers;

    public PdaReceiveBatchCheckRequestDto(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers) {
        this.receiveUserId = receiveUserId;
        this.receiveDeptId = receiveDeptId;
        this.identifiers = identifiers;
    }
}
