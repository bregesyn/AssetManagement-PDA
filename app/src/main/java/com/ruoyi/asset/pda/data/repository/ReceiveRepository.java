package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmDto;

import java.util.List;

/** 领用协议边界，ViewModel 只表达批次业务动作。 */
public interface ReceiveRepository {
    RequestHandle searchRecipients(String keyword,
            RepositoryCallback<List<PdaMasterDataDto>> callback);

    RequestHandle batchCheck(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaReceiveBatchCheckDto> callback);

    RequestHandle batchConfirm(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers, String remark,
            RepositoryCallback<PdaReceiveBatchConfirmDto> callback);
}
