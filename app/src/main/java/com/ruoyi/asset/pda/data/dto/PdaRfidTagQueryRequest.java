package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaRfidTagQueryRequest {
    @SerializedName("epcCode")
    private final String epcCode;

    public PdaRfidTagQueryRequest(String epcCode) {
        this.epcCode = epcCode;
    }

    public String getEpcCode() { return epcCode; }
}
