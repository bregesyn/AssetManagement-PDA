package com.ruoyi.asset.pda.feature.receive;

import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;

/** 领用页的已预检资产；保留原标识，确认时不依赖客户端推断的资产主键。 */
public final class ReceiveAssetItem {
    public enum Source {
        RFID,
        ASSET_CODE
    }

    private final Long assetId;
    private final String assetCode;
    private final String assetName;
    private final String categoryName;
    private final String specModel;
    private final String brand;
    private final String assetStatus;
    private final String assetStatusLabel;
    private final PdaAssetIdentifyRequest identifier;
    private final Source source;

    private ReceiveAssetItem(Long assetId, String assetCode, String assetName,
            String categoryName, String specModel, String brand, String assetStatus,
            String assetStatusLabel, PdaAssetIdentifyRequest identifier,
            Source source) {
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.categoryName = categoryName;
        this.specModel = specModel;
        this.brand = brand;
        this.assetStatus = assetStatus;
        this.assetStatusLabel = assetStatusLabel;
        this.identifier = identifier;
        this.source = source;
    }

    public static ReceiveAssetItem fromBatchRow(PdaReceiveBatchCheckDto.Row row) {
        Source source = "EPC".equals(row.getIdentifyType())
                ? Source.RFID : Source.ASSET_CODE;
        return new ReceiveAssetItem(row.getAssetId(), row.getAssetCode(),
                row.getAssetName(), row.getCategoryName(), row.getSpecModel(),
                row.getBrand(), row.getAssetStatus(), row.getAssetStatusLabel(),
                new PdaAssetIdentifyRequest(row.getIdentifyType(),
                        row.getIdentifyValue()), source);
    }

    public Long getAssetId() {
        return assetId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getSpecModel() {
        return specModel;
    }

    public String getBrand() {
        return brand;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public String getAssetStatusLabel() {
        return assetStatusLabel;
    }

    public PdaAssetIdentifyRequest getIdentifier() {
        return identifier;
    }

    public Source getSource() {
        return source;
    }
}
