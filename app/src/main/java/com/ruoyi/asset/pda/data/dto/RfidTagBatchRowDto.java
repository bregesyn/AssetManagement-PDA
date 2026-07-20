package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class RfidTagBatchRowDto {
    @SerializedName("rowNumber") private Integer rowNumber;
    @SerializedName("epcCode") private String epcCode;
    @SerializedName("success") private boolean success;
    @SerializedName("duplicate") private boolean duplicate;
    @SerializedName("tagId") private Long tagId;
    @SerializedName("tagCode") private String tagCode;
    @SerializedName("message") private String message;

    public RfidTagBatchRowDto() {
    }

    public RfidTagBatchRowDto(Integer rowNumber, String epcCode, boolean success,
            boolean duplicate, Long tagId, String tagCode, String message) {
        this.rowNumber = rowNumber;
        this.epcCode = epcCode;
        this.success = success;
        this.duplicate = duplicate;
        this.tagId = tagId;
        this.tagCode = tagCode;
        this.message = message;
    }

    public Integer getRowNumber() { return rowNumber; }
    public String getEpcCode() { return epcCode; }
    public boolean isSuccess() { return success; }
    public boolean isDuplicate() { return duplicate; }
    public Long getTagId() { return tagId; }
    public String getTagCode() { return tagCode; }
    public String getMessage() { return message; }
}
