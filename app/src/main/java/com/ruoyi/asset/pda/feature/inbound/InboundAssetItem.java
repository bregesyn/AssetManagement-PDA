package com.ruoyi.asset.pda.feature.inbound;

import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;

/** 入库页统一展示模型，屏蔽单条查询与批量预检响应结构差异。 */
public final class InboundAssetItem {
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
    private final String tagCode;
    private final String epcCode;
    private final Source source;

    private InboundAssetItem(Long assetId, String assetCode, String assetName,
            String categoryName, String specModel, String brand, String assetStatus,
            String assetStatusLabel, String tagCode, String epcCode, Source source) {
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.categoryName = categoryName;
        this.specModel = specModel;
        this.brand = brand;
        this.assetStatus = assetStatus;
        this.assetStatusLabel = assetStatusLabel;
        this.tagCode = tagCode;
        this.epcCode = epcCode;
        this.source = source;
    }

    public static InboundAssetItem fromEligibility(PdaInboundEligibilityDto value) {
        return new InboundAssetItem(value.getAssetId(), value.getAssetCode(),
                value.getAssetName(), value.getCategoryName(), value.getSpecModel(),
                value.getBrand(), value.getAssetStatus(), value.getAssetStatusLabel(),
                value.getTagCode(), null, Source.ASSET_CODE);
    }

    public static InboundAssetItem fromBatchRow(PdaInboundBatchCheckDto.Row value) {
        return new InboundAssetItem(value.getAssetId(), value.getAssetCode(),
                value.getAssetName(), value.getCategoryName(), null, null,
                value.getAssetStatus(), value.getAssetStatusLabel(), null,
                value.getEpcCode(), Source.RFID);
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

    public String getTagCode() {
        return tagCode;
    }

    public String getEpcCode() {
        return epcCode;
    }

    public Source getSource() {
        return source;
    }
}
