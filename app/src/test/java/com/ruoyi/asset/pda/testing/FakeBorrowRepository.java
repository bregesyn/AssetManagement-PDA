package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.repository.BorrowRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

/** 借还 ViewModel 的同步假实现，记录请求以验证没有逐 EPC HTTP 或客户端伪造字段。 */
public final class FakeBorrowRepository implements BorrowRepository {
    private RepositoryCallback<List<PdaMasterDataDto>> borrowersCallback;
    private RepositoryCallback<PdaBorrowBatchCheckDto> issueCheckCallback;
    private RepositoryCallback<PdaBorrowBatchCheckDto> returnCheckCallback;
    private RepositoryCallback<PdaBorrowIssueBatchSubmitDto> issueSubmitCallback;
    private RepositoryCallback<PdaBorrowReturnBatchSubmitDto> returnSubmitCallback;
    private String lastKeyword;
    private String lastBorrowerType;
    private Long lastBorrowUserId;
    private Long lastBorrowDeptId;
    private String lastBorrowOrgName;
    private String lastBorrowContactPhone;
    private String lastBorrowExternalContactName;
    private String lastBorrowExternalContactPhone;
    private String lastExpectedReturnDate;
    private String lastRemark;
    private List<PdaAssetIdentifyRequest> lastIdentifiers;
    private int borrowerSearchCount;
    private int issueCheckCount;
    private int returnCheckCount;
    private int issueSubmitCount;
    private int returnSubmitCount;

    @Override
    public RequestHandle searchBorrowers(String keyword,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
        borrowerSearchCount++;
        lastKeyword = keyword;
        borrowersCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchCheckIssue(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowBatchCheckDto> callback) {
        issueCheckCount++;
        recordIssue(borrowerType, borrowUserId, borrowDeptId, borrowOrgName,
                borrowContactPhone, borrowExternalContactName, borrowExternalContactPhone,
                expectedReturnDate, identifiers);
        issueCheckCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchSubmitIssue(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            String remark, RepositoryCallback<PdaBorrowIssueBatchSubmitDto> callback) {
        issueSubmitCount++;
        recordIssue(borrowerType, borrowUserId, borrowDeptId, borrowOrgName,
                borrowContactPhone, borrowExternalContactName, borrowExternalContactPhone,
                expectedReturnDate, identifiers);
        lastRemark = remark;
        issueSubmitCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchCheckReturn(List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowBatchCheckDto> callback) {
        returnCheckCount++;
        lastIdentifiers = copy(identifiers);
        returnCheckCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchSubmitReturn(List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowReturnBatchSubmitDto> callback) {
        returnSubmitCount++;
        lastIdentifiers = copy(identifiers);
        returnSubmitCallback = callback;
        return RequestHandle.NONE;
    }

    public void completeBorrowers(List<PdaMasterDataDto> values) {
        borrowersCallback.onSuccess(values);
    }

    public void completeIssueCheck(PdaBorrowBatchCheckDto value) {
        issueCheckCallback.onSuccess(value);
    }

    public void completeReturnCheck(PdaBorrowBatchCheckDto value) {
        returnCheckCallback.onSuccess(value);
    }

    public void completeIssueSubmit(PdaBorrowIssueBatchSubmitDto value) {
        issueSubmitCallback.onSuccess(value);
    }

    public void completeReturnSubmit(PdaBorrowReturnBatchSubmitDto value) {
        returnSubmitCallback.onSuccess(value);
    }

    public void failIssueSubmit(ApiErrorMapper.ApiError error) {
        issueSubmitCallback.onError(error);
    }

    public String getLastKeyword() { return lastKeyword; }
    public String getLastBorrowerType() { return lastBorrowerType; }
    public Long getLastBorrowUserId() { return lastBorrowUserId; }
    public Long getLastBorrowDeptId() { return lastBorrowDeptId; }
    public String getLastBorrowOrgName() { return lastBorrowOrgName; }
    public String getLastBorrowContactPhone() { return lastBorrowContactPhone; }
    public String getLastBorrowExternalContactName() { return lastBorrowExternalContactName; }
    public String getLastBorrowExternalContactPhone() { return lastBorrowExternalContactPhone; }
    public String getLastExpectedReturnDate() { return lastExpectedReturnDate; }
    public String getLastRemark() { return lastRemark; }
    public List<PdaAssetIdentifyRequest> getLastIdentifiers() { return lastIdentifiers; }
    public int getBorrowerSearchCount() { return borrowerSearchCount; }
    public int getIssueCheckCount() { return issueCheckCount; }
    public int getReturnCheckCount() { return returnCheckCount; }
    public int getIssueSubmitCount() { return issueSubmitCount; }
    public int getReturnSubmitCount() { return returnSubmitCount; }

    private void recordIssue(String borrowerType, Long borrowUserId, Long borrowDeptId,
            String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate,
            List<PdaAssetIdentifyRequest> identifiers) {
        lastBorrowerType = borrowerType;
        lastBorrowUserId = borrowUserId;
        lastBorrowDeptId = borrowDeptId;
        lastBorrowOrgName = borrowOrgName;
        lastBorrowContactPhone = borrowContactPhone;
        lastBorrowExternalContactName = borrowExternalContactName;
        lastBorrowExternalContactPhone = borrowExternalContactPhone;
        lastExpectedReturnDate = expectedReturnDate;
        lastIdentifiers = copy(identifiers);
    }

    private List<PdaAssetIdentifyRequest> copy(List<PdaAssetIdentifyRequest> identifiers) {
        return identifiers == null ? null : new ArrayList<>(identifiers);
    }
}
