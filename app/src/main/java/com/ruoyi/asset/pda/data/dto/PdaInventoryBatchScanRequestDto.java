package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PdaInventoryBatchScanRequestDto {
    @SerializedName("taskNo") private final String taskNo;
    @SerializedName("inventoryWarehouseId") private final Long inventoryWarehouseId;
    @SerializedName("inventoryLocationId") private final Long inventoryLocationId;
    @SerializedName("epcCodes") private final List<String> epcCodes;

    public PdaInventoryBatchScanRequestDto(String taskNo, Long inventoryWarehouseId,
            Long inventoryLocationId, List<String> epcCodes) {
        this.taskNo = taskNo;
        this.inventoryWarehouseId = inventoryWarehouseId;
        this.inventoryLocationId = inventoryLocationId;
        this.epcCodes = epcCodes;
    }

    public String getTaskNo() { return taskNo; }
    public Long getInventoryWarehouseId() { return inventoryWarehouseId; }
    public Long getInventoryLocationId() { return inventoryLocationId; }
    public List<String> getEpcCodes() { return epcCodes; }
}
