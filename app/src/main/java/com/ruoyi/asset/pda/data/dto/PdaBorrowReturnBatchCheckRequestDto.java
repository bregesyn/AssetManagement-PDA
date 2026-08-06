package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 归还预检请求只携带扫描到的资产标识，借用单和仓位由服务端定位。 */
public class PdaBorrowReturnBatchCheckRequestDto {
    @SerializedName("identifiers")
    private final List<PdaAssetIdentifyRequest> identifiers;

    public PdaBorrowReturnBatchCheckRequestDto(List<PdaAssetIdentifyRequest> identifiers) {
        this.identifiers = identifiers;
    }
}
