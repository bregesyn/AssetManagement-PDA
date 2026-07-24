package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PdaInboundBatchCheckRequestDto {
    @SerializedName("epcCodes")
    private final List<String> epcCodes;

    public PdaInboundBatchCheckRequestDto(List<String> epcCodes) {
        this.epcCodes = epcCodes;
    }
}
