package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 盘点任务列表和执行页顶部统计使用的服务端事实。 */
public final class PdaInventoryTaskDto {
    @SerializedName("taskId") private Long taskId;
    @SerializedName("taskNo") private String taskNo;
    @SerializedName("scopeType") private String scopeType;
    @SerializedName("warehouseId") private Long warehouseId;
    @SerializedName("warehouseCode") private String warehouseCode;
    @SerializedName("warehouseName") private String warehouseName;
    @SerializedName("locationId") private Long locationId;
    @SerializedName("locationCode") private String locationCode;
    @SerializedName("locationName") private String locationName;
    @SerializedName("categoryId") private Long categoryId;
    @SerializedName("categoryCode") private String categoryCode;
    @SerializedName("categoryName") private String categoryName;
    @SerializedName("ownerUserId") private Long ownerUserId;
    @SerializedName("ownerUserName") private String ownerUserName;
    @SerializedName("taskStatus") private String taskStatus;
    @SerializedName("issueTime") private String issueTime;
    @SerializedName("remark") private String remark;
    @SerializedName("totalCount") private long totalCount;
    @SerializedName("inventoriedCount") private long inventoriedCount;
    @SerializedName("pendingCount") private long pendingCount;
    @SerializedName("normalCount") private long normalCount;
    @SerializedName("lossCount") private long lossCount;
    @SerializedName("surplusCount") private long surplusCount;
    @SerializedName("editable") private boolean editable;

    public PdaInventoryTaskDto() {
    }

    public Long getTaskId() { return taskId; }
    public String getTaskNo() { return taskNo; }
    public String getScopeType() { return scopeType; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public Long getLocationId() { return locationId; }
    public String getLocationCode() { return locationCode; }
    public String getLocationName() { return locationName; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public Long getOwnerUserId() { return ownerUserId; }
    public String getOwnerUserName() { return ownerUserName; }
    public String getTaskStatus() { return taskStatus; }
    public String getIssueTime() { return issueTime; }
    public String getRemark() { return remark; }
    public long getTotalCount() { return totalCount; }
    public long getInventoriedCount() { return inventoriedCount; }
    public long getPendingCount() { return pendingCount; }
    public long getNormalCount() { return normalCount; }
    public long getLossCount() { return lossCount; }
    public long getSurplusCount() { return surplusCount; }
    public boolean isEditable() { return editable; }
}
