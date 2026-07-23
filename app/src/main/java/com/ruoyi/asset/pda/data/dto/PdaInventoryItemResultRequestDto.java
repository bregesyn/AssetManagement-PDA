package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaInventoryItemResultRequestDto {
    @SerializedName("inventoryResult") private final String inventoryResult;
    @SerializedName("inventoryWarehouseId") private final Long inventoryWarehouseId;
    @SerializedName("inventoryLocationId") private final Long inventoryLocationId;
    @SerializedName("remark") private final String remark;

    public PdaInventoryItemResultRequestDto(String inventoryResult, Long inventoryWarehouseId,
            Long inventoryLocationId, String remark) {
        this.inventoryResult = inventoryResult;
        this.inventoryWarehouseId = inventoryWarehouseId;
        this.inventoryLocationId = inventoryLocationId;
        this.remark = remark;
    }

    public String getInventoryResult() { return inventoryResult; }
    public Long getInventoryWarehouseId() { return inventoryWarehouseId; }
    public Long getInventoryLocationId() { return inventoryLocationId; }
    public String getRemark() { return remark; }
}
