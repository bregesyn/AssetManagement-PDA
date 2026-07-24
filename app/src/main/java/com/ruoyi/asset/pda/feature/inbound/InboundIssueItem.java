package com.ruoyi.asset.pda.feature.inbound;

import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;

public final class InboundIssueItem {
    private final String epcCode;
    private final String assetCode;
    private final String assetName;
    private final String status;
    private final String message;

    public InboundIssueItem(PdaInboundBatchCheckDto.Row row) {
        epcCode = row.getEpcCode();
        assetCode = row.getAssetCode();
        assetName = row.getAssetName();
        status = row.getStatus();
        message = row.getMessage();
    }

    public String getEpcCode() {
        return epcCode;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
