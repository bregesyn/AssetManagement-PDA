package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;

import java.util.Locale;

public final class DefaultAssetRepository implements AssetRepository {
    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultAssetRepository(PdaApiService apiService, ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("资产 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle identify(String identifyType, String identifyValue,
            RepositoryCallback<PdaAssetIdentifyDto> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("资产识别回调不能为空");
        }
        String checkedType = identifyType == null ? "" : identifyType.trim();
        String checkedValue = identifyValue == null ? "" : identifyValue.trim();
        if ((!IDENTIFY_TYPE_EPC.equals(checkedType)
                && !IDENTIFY_TYPE_ASSET_CODE.equals(checkedType)) || checkedValue.isEmpty()) {
            callback.onError(callExecutor.protocolError());
            return RequestHandle.NONE;
        }
        if (IDENTIFY_TYPE_EPC.equals(checkedType)) {
            checkedValue = checkedValue.toUpperCase(Locale.ROOT);
        }
        return callExecutor.execute(apiService.identifyAsset(
                new PdaAssetIdentifyRequest(checkedType, checkedValue)), true, callback);
    }
}
