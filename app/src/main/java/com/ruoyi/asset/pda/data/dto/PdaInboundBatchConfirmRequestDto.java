package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PdaInboundBatchConfirmRequestDto {
    @SerializedName("warehouseId")
    private final Long warehouseId;

    @SerializedName("locationId")
    private final Long locationId;

    @SerializedName("assetIds")
    private final List<Long> assetIds;

    @SerializedName("remark")
    private final String remark;

    public PdaInboundBatchConfirmRequestDto(Long warehouseId, Long locationId,
            List<Long> assetIds, String remark) {
        this.warehouseId = warehouseId;
        this.locationId = locationId;
        this.assetIds = assetIds;
        this.remark = remark;
    }
}
