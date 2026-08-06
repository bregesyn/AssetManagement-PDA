package com.ruoyi.asset.pda.feature.borrow;

import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;

/** 借出/归还清单中的服务端事实；归还仓位只读来自借用前快照。 */
public final class BorrowAssetItem {
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
    private final String assetStatusLabel;
    private final PdaAssetIdentifyRequest identifier;
    private final Source source;
    private final Long orderId;
    private final Long itemId;
    private final String borrowNo;
    private final String returnStatus;
    private final String beforeWarehouseName;
    private final String beforeLocationName;

    private BorrowAssetItem(Long assetId, String assetCode, String assetName,
            String categoryName, String specModel, String brand, String assetStatusLabel,
            PdaAssetIdentifyRequest identifier, Source source, Long orderId, Long itemId,
            String borrowNo, String returnStatus, String beforeWarehouseName,
            String beforeLocationName) {
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.categoryName = categoryName;
        this.specModel = specModel;
        this.brand = brand;
        this.assetStatusLabel = assetStatusLabel;
        this.identifier = identifier;
        this.source = source;
        this.orderId = orderId;
        this.itemId = itemId;
        this.borrowNo = borrowNo;
        this.returnStatus = returnStatus;
        this.beforeWarehouseName = beforeWarehouseName;
        this.beforeLocationName = beforeLocationName;
    }

    public static BorrowAssetItem fromRow(PdaBorrowBatchCheckDto.Row row) {
        Source source = "EPC".equals(row.getIdentifyType())
                ? Source.RFID : Source.ASSET_CODE;
        return new BorrowAssetItem(row.getAssetId(), row.getAssetCode(), row.getAssetName(),
                row.getCategoryName(), row.getSpecModel(), row.getBrand(),
                row.getAssetStatusLabel(),
                new PdaAssetIdentifyRequest(row.getIdentifyType(), row.getIdentifyValue()),
                source, row.getOrderId(), row.getItemId(), row.getBorrowNo(), row.getReturnStatus(),
                row.getBeforeWarehouseName(), row.getBeforeLocationName());
    }

    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public String getCategoryName() { return categoryName; }
    public String getSpecModel() { return specModel; }
    public String getBrand() { return brand; }
    public String getAssetStatusLabel() { return assetStatusLabel; }
    public PdaAssetIdentifyRequest getIdentifier() { return identifier; }
    public Source getSource() { return source; }
    public Long getOrderId() { return orderId; }
    public Long getItemId() { return itemId; }
    public String getBorrowNo() { return borrowNo; }
    public String getReturnStatus() { return returnStatus; }
    public String getBeforeWarehouseName() { return beforeWarehouseName; }
    public String getBeforeLocationName() { return beforeLocationName; }
}
