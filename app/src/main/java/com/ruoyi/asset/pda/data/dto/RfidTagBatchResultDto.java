package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public final class RfidTagBatchResultDto {
    @SerializedName("successCount") private int successCount;
    @SerializedName("duplicateCount") private int duplicateCount;
    @SerializedName("failureCount") private int failureCount;
    @SerializedName("rows") private List<RfidTagBatchRowDto> rows = new ArrayList<>();

    public RfidTagBatchResultDto() {
    }

    public RfidTagBatchResultDto(int successCount, int duplicateCount,
            int failureCount, List<RfidTagBatchRowDto> rows) {
        this.successCount = successCount;
        this.duplicateCount = duplicateCount;
        this.failureCount = failureCount;
        this.rows = rows;
    }

    public int getSuccessCount() { return successCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public int getFailureCount() { return failureCount; }
    public List<RfidTagBatchRowDto> getRows() { return rows; }
}
