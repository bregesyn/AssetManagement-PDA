package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaRfidUnbindRequest {
    @SerializedName("tagId") private final Long tagId;

    public PdaRfidUnbindRequest(Long tagId) {
        this.tagId = tagId;
    }

    public Long getTagId() { return tagId; }
}
