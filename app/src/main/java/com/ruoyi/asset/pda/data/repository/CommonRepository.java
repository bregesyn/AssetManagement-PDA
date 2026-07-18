package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.List;

public interface CommonRepository {
    RequestHandle bootstrap(RepositoryCallback<PdaBootstrapDto> callback);

    RequestHandle dict(String dictType, RepositoryCallback<List<PdaDictItemDto>> callback);

    RequestHandle warehouses(RepositoryCallback<List<PdaMasterDataDto>> callback);

    RequestHandle locations(Long warehouseId,
            RepositoryCallback<List<PdaMasterDataDto>> callback);

    RequestHandle categories(RepositoryCallback<List<PdaMasterDataDto>> callback);
}
