package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaInventoryScanRequestDto {
    @SerializedName("identifyType") private final String identifyType;
    @SerializedName("identifyValue") private final String identifyValue;

    public PdaInventoryScanRequestDto(String identifyType, String identifyValue) {
        this.identifyType = identifyType;
        this.identifyValue = identifyValue;
    }

    public String getIdentifyType() { return identifyType; }
    public String getIdentifyValue() { return identifyValue; }
}
