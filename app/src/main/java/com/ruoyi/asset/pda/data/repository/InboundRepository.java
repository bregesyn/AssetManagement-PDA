package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;

import java.util.List;

public interface InboundRepository {
    RequestHandle queryByEpc(String epcCode,
            RepositoryCallback<PdaInboundEligibilityDto> callback);

    RequestHandle queryByAssetCode(String assetCode,
            RepositoryCallback<PdaInboundEligibilityDto> callback);

    RequestHandle batchCheck(List<String> epcCodes,
            RepositoryCallback<PdaInboundBatchCheckDto> callback);

    RequestHandle batchConfirm(Long warehouseId, Long locationId, List<Long> assetIds,
            String remark, RepositoryCallback<PdaInboundBatchConfirmDto> callback);
}
