package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

/** 单条识别和批量预判共用的逐行协议结果。 */
public final class PdaInventoryScanDto {
    @SerializedName("matchType") private String matchType;
    @SerializedName("identifyType") private String identifyType;
    @SerializedName("identifyValue") private String identifyValue;
    @SerializedName("item") private PdaInventoryItemDto item;
    @SerializedName("assetId") private Long assetId;
    @SerializedName("assetCode") private String assetCode;
    @SerializedName("assetName") private String assetName;
    @SerializedName("assetStatus") private String assetStatus;
    @SerializedName("warehouseId") private Long warehouseId;
    @SerializedName("warehouseName") private String warehouseName;
    @SerializedName("locationId") private Long locationId;
    @SerializedName("locationName") private String locationName;
    @SerializedName("tagId") private Long tagId;
    @SerializedName("tagCode") private String tagCode;
    @SerializedName("epcCode") private String epcCode;
    @SerializedName("tagStatus") private String tagStatus;
    @SerializedName("bindStatus") private String bindStatus;
    @SerializedName("rowNumber") private Integer rowNumber;
    @SerializedName("firstRowNumber") private Integer firstRowNumber;
    @SerializedName("confirmable") private Boolean confirmable;
    @SerializedName("success") private Boolean success;
    @SerializedName("reasonCode") private String reasonCode;
    @SerializedName("message") private String message;
    @SerializedName("proposedResult") private String proposedResult;
    @SerializedName("action") private String action;
    @SerializedName("overwritten") private Boolean overwritten;

    public PdaInventoryScanDto() {
    }

    public String getMatchType() { return matchType; }
    public String getIdentifyType() { return identifyType; }
    public String getIdentifyValue() { return identifyValue; }
    public PdaInventoryItemDto getItem() { return item; }
    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public String getAssetStatus() { return assetStatus; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public Long getLocationId() { return locationId; }
    public String getLocationName() { return locationName; }
    public Long getTagId() { return tagId; }
    public String getTagCode() { return tagCode; }
    public String getEpcCode() { return epcCode; }
    public String getTagStatus() { return tagStatus; }
    public String getBindStatus() { return bindStatus; }
    public Integer getRowNumber() { return rowNumber; }
    public Integer getFirstRowNumber() { return firstRowNumber; }
    public Boolean getConfirmable() { return confirmable; }
    public Boolean getSuccess() { return success; }
    public String getReasonCode() { return reasonCode; }
    public String getMessage() { return message; }
    public String getProposedResult() { return proposedResult; }
    public String getAction() { return action; }
    public Boolean getOverwritten() { return overwritten; }
}
