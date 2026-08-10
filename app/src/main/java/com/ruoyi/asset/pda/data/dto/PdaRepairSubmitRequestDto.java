package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 报修提交只携带现场输入，身份和资产 ID 由服务端重新解析。 */
public final class PdaRepairSubmitRequestDto {
    @SerializedName("identifier") private final PdaAssetIdentifyRequest identifier;
    @SerializedName("faultDesc") private final String faultDesc;
    @SerializedName("expectedFinishTime") private final String expectedFinishTime;
    @SerializedName("remark") private final String remark;

    public PdaRepairSubmitRequestDto(PdaAssetIdentifyRequest identifier, String faultDesc,
            String expectedFinishTime, String remark) {
        this.identifier = identifier;
        this.faultDesc = faultDesc;
        this.expectedFinishTime = expectedFinishTime;
        this.remark = remark;
    }
}
