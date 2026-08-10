package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 开始维修请求；内部维修只上传用户 ID。 */
public final class PdaRepairStartRequestDto {
    @SerializedName("repairerType") private final String repairerType;
    @SerializedName("repairUserId") private final Long repairUserId;
    @SerializedName("repairUserName") private final String repairUserName;
    @SerializedName("repairOrgName") private final String repairOrgName;
    @SerializedName("repairContactPhone") private final String repairContactPhone;

    public PdaRepairStartRequestDto(String repairerType, Long repairUserId,
            String repairUserName, String repairOrgName, String repairContactPhone) {
        this.repairerType = repairerType;
        this.repairUserId = repairUserId;
        this.repairUserName = repairUserName;
        this.repairOrgName = repairOrgName;
        this.repairContactPhone = repairContactPhone;
    }
}
