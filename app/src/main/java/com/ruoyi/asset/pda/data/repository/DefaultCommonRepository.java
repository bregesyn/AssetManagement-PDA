package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.List;

/**
 * 公共数据只在线按需读取，不建立会漂移的本地业务缓存。
 */
public final class DefaultCommonRepository implements CommonRepository {
    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultCommonRepository(PdaApiService apiService, ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("公共 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle bootstrap(RepositoryCallback<PdaBootstrapDto> callback) {
        return callExecutor.execute(apiService.bootstrap(), true, callback);
    }

    @Override
    public RequestHandle dict(String dictType,
            RepositoryCallback<List<PdaDictItemDto>> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("字典回调不能为空");
        }
        String checkedType = dictType == null ? "" : dictType.trim();
        if (checkedType.isEmpty()) {
            callback.onError(callExecutor.protocolError());
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.dict(checkedType), true, callback);
    }

    @Override
    public RequestHandle warehouses(RepositoryCallback<List<PdaMasterDataDto>> callback) {
        return callExecutor.execute(apiService.warehouses(), true, callback);
    }

    @Override
    public RequestHandle locations(Long warehouseId,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
        return callExecutor.execute(apiService.locations(warehouseId), true, callback);
    }

    @Override
    public RequestHandle categories(RepositoryCallback<List<PdaMasterDataDto>> callback) {
        return callExecutor.execute(apiService.categories(), true, callback);
    }
}
