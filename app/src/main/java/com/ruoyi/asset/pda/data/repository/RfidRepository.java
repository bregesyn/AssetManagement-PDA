package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;

import java.util.List;

public interface RfidRepository {
    RequestHandle queryTag(String epcCode, RepositoryCallback<PdaRfidTagDto> callback);

    RequestHandle batchCreate(List<String> epcCodes, String remark,
            RepositoryCallback<RfidTagBatchResultDto> callback);

    RequestHandle bind(String assetCode, String epcCode,
            RepositoryCallback<PdaRfidTagDto> callback);

    RequestHandle unbind(Long tagId, RepositoryCallback<PdaRfidTagDto> callback);
}
