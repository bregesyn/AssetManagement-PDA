package com.ruoyi.asset.pda.feature.receive;

import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;

/** 现场必须可追溯的预检异常；不把异常标识带入最终确认请求。 */
public final class ReceiveIssueItem {
    private final String identifyType;
    private final String identifyValue;
    private final String assetCode;
    private final String assetName;
    private final String status;
    private final String message;

    public ReceiveIssueItem(PdaReceiveBatchCheckDto.Row row) {
        this(row.getIdentifyType(), row.getIdentifyValue(), row.getAssetCode(),
                row.getAssetName(), row.getStatus(), row.getMessage());
    }

    public ReceiveIssueItem(String identifyType, String identifyValue,
            String assetCode, String assetName, String status, String message) {
        this.identifyType = identifyType;
        this.identifyValue = identifyValue;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.status = status;
        this.message = message;
    }

    public String getIdentifyType() {
        return identifyType;
    }

    public String getIdentifyValue() {
        return identifyValue;
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
