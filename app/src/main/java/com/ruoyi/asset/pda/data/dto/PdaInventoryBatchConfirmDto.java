package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** 批量确认结果保留逐行失败信息，现场可以只重做失败项。 */
public final class PdaInventoryBatchConfirmDto {
    @SerializedName("totalCount") private int totalCount;
    @SerializedName("successCount") private int successCount;
    @SerializedName("failureCount") private int failureCount;
    @SerializedName("duplicateCount") private int duplicateCount;
    @SerializedName("itemUpdatedCount") private int itemUpdatedCount;
    @SerializedName("surplusCreatedCount") private int surplusCreatedCount;
    @SerializedName("surplusExistingCount") private int surplusExistingCount;
    @SerializedName("normalCount") private int normalCount;
    @SerializedName("rows") private List<PdaInventoryScanDto> rows = new ArrayList<>();
    @SerializedName("task") private PdaInventoryTaskDto task;

    public PdaInventoryBatchConfirmDto() {
    }

    public int getTotalCount() { return totalCount; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public int getItemUpdatedCount() { return itemUpdatedCount; }
    public int getSurplusCreatedCount() { return surplusCreatedCount; }
    public int getSurplusExistingCount() { return surplusExistingCount; }
    public int getNormalCount() { return normalCount; }
    public List<PdaInventoryScanDto> getRows() { return rows; }
    public PdaInventoryTaskDto getTask() { return task; }
}
