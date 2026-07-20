package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;

public interface AssetRepository {
    String IDENTIFY_TYPE_EPC = "EPC";
    String IDENTIFY_TYPE_ASSET_CODE = "ASSET_CODE";

    RequestHandle identify(String identifyType, String identifyValue,
            RepositoryCallback<PdaAssetIdentifyDto> callback);
}
