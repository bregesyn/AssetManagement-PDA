package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaRfidBindRequest {
    @SerializedName("assetCode") private final String assetCode;
    @SerializedName("epcCode") private final String epcCode;

    public PdaRfidBindRequest(String assetCode, String epcCode) {
        this.assetCode = assetCode;
        this.epcCode = epcCode;
    }

    public String getAssetCode() { return assetCode; }
    public String getEpcCode() { return epcCode; }
}
