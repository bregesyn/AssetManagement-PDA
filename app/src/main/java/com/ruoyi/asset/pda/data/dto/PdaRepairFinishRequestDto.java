package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/** 维修完成请求。 */
public final class PdaRepairFinishRequestDto {
    @SerializedName("repairFinishTime") private final String repairFinishTime;
    @SerializedName("repairResult") private final String repairResult;
    @SerializedName("repairCost") private final BigDecimal repairCost;

    public PdaRepairFinishRequestDto(String repairFinishTime, String repairResult,
            BigDecimal repairCost) {
        this.repairFinishTime = repairFinishTime;
        this.repairResult = repairResult;
        this.repairCost = repairCost;
    }
}
