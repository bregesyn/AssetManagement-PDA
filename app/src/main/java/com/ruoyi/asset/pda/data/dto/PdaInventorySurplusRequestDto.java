package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

public final class PdaInventorySurplusRequestDto {
    @SerializedName("identifyMethod") private final String identifyMethod;
    @SerializedName("assetCode") private final String assetCode;
    @SerializedName("assetName") private final String assetName;
    @SerializedName("categoryId") private final Long categoryId;
    @SerializedName("specModel") private final String specModel;
    @SerializedName("brand") private final String brand;
    @SerializedName("epcCode") private final String epcCode;
    @SerializedName("inventoryWarehouseId") private final Long inventoryWarehouseId;
    @SerializedName("inventoryLocationId") private final Long inventoryLocationId;
    @SerializedName("remark") private final String remark;

    public PdaInventorySurplusRequestDto(String identifyMethod, String assetCode,
            String assetName, Long categoryId, String specModel, String brand,
            String epcCode, Long inventoryWarehouseId, Long inventoryLocationId,
            String remark) {
        this.identifyMethod = identifyMethod;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.categoryId = categoryId;
        this.specModel = specModel;
        this.brand = brand;
        this.epcCode = epcCode;
        this.inventoryWarehouseId = inventoryWarehouseId;
        this.inventoryLocationId = inventoryLocationId;
        this.remark = remark;
    }

    public String getIdentifyMethod() { return identifyMethod; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public Long getCategoryId() { return categoryId; }
    public String getSpecModel() { return specModel; }
    public String getBrand() { return brand; }
    public String getEpcCode() { return epcCode; }
    public Long getInventoryWarehouseId() { return inventoryWarehouseId; }
    public Long getInventoryLocationId() { return inventoryLocationId; }
    public String getRemark() { return remark; }
}
