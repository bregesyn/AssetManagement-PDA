package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class PdaInventoryBatchConfirmRequestDto extends PdaInventoryBatchScanRequestDto {
    @SerializedName("remark") private final String remark;

    public PdaInventoryBatchConfirmRequestDto(String taskNo, Long inventoryWarehouseId,
            Long inventoryLocationId, List<String> epcCodes, String remark) {
        super(taskNo, inventoryWarehouseId, inventoryLocationId, epcCodes);
        this.remark = remark;
    }

    public String getRemark() { return remark; }
}
