package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaInventoryBatchLossDto {
    @SerializedName("affectedRows") private int affectedRows;
    @SerializedName("task") private PdaInventoryTaskDto task;

    public PdaInventoryBatchLossDto() {
    }

    public int getAffectedRows() { return affectedRows; }
    public PdaInventoryTaskDto getTask() { return task; }
}
