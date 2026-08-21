package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.List;

/** PDA 借出/归还协议边界，页面不直接依赖 Retrofit。 */
public interface BorrowRepository {
    RequestHandle searchBorrowers(String keyword,
            RepositoryCallback<List<PdaMasterDataDto>> callback);

    RequestHandle batchCheckIssue(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowBatchCheckDto> callback);

    RequestHandle batchSubmitIssue(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            String remark, RepositoryCallback<PdaBorrowIssueBatchSubmitDto> callback);

    RequestHandle batchCheckReturn(List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowBatchCheckDto> callback);

    RequestHandle batchSubmitReturn(List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowReturnBatchSubmitDto> callback);
}
