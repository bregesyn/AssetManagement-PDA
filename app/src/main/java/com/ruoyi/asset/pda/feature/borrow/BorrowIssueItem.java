package com.ruoyi.asset.pda.feature.borrow;

import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;

/** 预检失败项必须留在页面上，避免现场误以为整批资产均已进入申请。 */
public final class BorrowIssueItem {
    private final String identifyType;
    private final String identifyValue;
    private final String assetCode;
    private final String assetName;
    private final String status;
    private final String message;

    public BorrowIssueItem(PdaBorrowBatchCheckDto.Row row) {
        this(row.getIdentifyType(), row.getIdentifyValue(), row.getAssetCode(),
                row.getAssetName(), row.getStatus(), row.getMessage());
    }

    public BorrowIssueItem(String identifyType, String identifyValue, String assetCode,
            String assetName, String status, String message) {
        this.identifyType = identifyType;
        this.identifyValue = identifyValue;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.status = status;
        this.message = message;
    }

    public String getIdentifyType() { return identifyType; }
    public String getIdentifyValue() { return identifyValue; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
