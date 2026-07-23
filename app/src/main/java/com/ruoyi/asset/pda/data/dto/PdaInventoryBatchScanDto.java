package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** 批量预判只读结果；客户端会再次校验行号、EPC 顺序和汇总，防止错误协议被确认。 */
public final class PdaInventoryBatchScanDto {
    @SerializedName("totalCount") private int totalCount;
    @SerializedName("confirmableCount") private int confirmableCount;
    @SerializedName("unresolvedCount") private int unresolvedCount;
    @SerializedName("duplicateCount") private int duplicateCount;
    @SerializedName("expectedCount") private int expectedCount;
    @SerializedName("surplusCount") private int surplusCount;
    @SerializedName("normalCount") private int normalCount;
    @SerializedName("rows") private List<PdaInventoryScanDto> rows = new ArrayList<>();

    public PdaInventoryBatchScanDto() {
    }

    public int getTotalCount() { return totalCount; }
    public int getConfirmableCount() { return confirmableCount; }
    public int getUnresolvedCount() { return unresolvedCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public int getExpectedCount() { return expectedCount; }
    public int getSurplusCount() { return surplusCount; }
    public int getNormalCount() { return normalCount; }
    public List<PdaInventoryScanDto> getRows() { return rows; }
}
