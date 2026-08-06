package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchSubmitDto;
import com.ruoyi.asset.pda.data.repository.ReceiveRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

/** 领用 ViewModel 的同步假实现，暴露提交事实以验证页面没有伪造审计字段。 */
public final class FakeReceiveRepository implements ReceiveRepository {
    private RepositoryCallback<List<PdaMasterDataDto>> recipientsCallback;
    private RepositoryCallback<PdaReceiveBatchCheckDto> batchCheckCallback;
    private RepositoryCallback<PdaReceiveBatchSubmitDto> submitCallback;
    private String lastKeyword;
    private Long lastReceiveUserId;
    private Long lastReceiveDeptId;
    private List<PdaAssetIdentifyRequest> lastIdentifiers;
    private String lastRemark;
    private int recipientsCount;
    private int batchCheckCount;
    private int submitCount;

    @Override
    public RequestHandle searchRecipients(String keyword,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
        recipientsCount++;
        lastKeyword = keyword;
        recipientsCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchCheck(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaReceiveBatchCheckDto> callback) {
        batchCheckCount++;
        lastReceiveUserId = receiveUserId;
        lastReceiveDeptId = receiveDeptId;
        lastIdentifiers = copy(identifiers);
        batchCheckCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle batchSubmit(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers, String remark,
            RepositoryCallback<PdaReceiveBatchSubmitDto> callback) {
        submitCount++;
        lastReceiveUserId = receiveUserId;
        lastReceiveDeptId = receiveDeptId;
        lastIdentifiers = copy(identifiers);
        lastRemark = remark;
        submitCallback = callback;
        return RequestHandle.NONE;
    }

    public void completeRecipients(List<PdaMasterDataDto> values) {
        recipientsCallback.onSuccess(values);
    }

    public void failRecipients(ApiErrorMapper.ApiError error) {
        recipientsCallback.onError(error);
    }

    public void completeBatchCheck(PdaReceiveBatchCheckDto value) {
        batchCheckCallback.onSuccess(value);
    }

    public void failBatchCheck(ApiErrorMapper.ApiError error) {
        batchCheckCallback.onError(error);
    }

    public void completeSubmit(PdaReceiveBatchSubmitDto value) {
        submitCallback.onSuccess(value);
    }

    public void failSubmit(ApiErrorMapper.ApiError error) {
        submitCallback.onError(error);
    }

    public String getLastKeyword() {
        return lastKeyword;
    }

    public Long getLastReceiveUserId() {
        return lastReceiveUserId;
    }

    public Long getLastReceiveDeptId() {
        return lastReceiveDeptId;
    }

    public List<PdaAssetIdentifyRequest> getLastIdentifiers() {
        return lastIdentifiers;
    }

    public String getLastRemark() {
        return lastRemark;
    }

    public int getRecipientsCount() {
        return recipientsCount;
    }

    public int getBatchCheckCount() {
        return batchCheckCount;
    }

    public int getSubmitCount() {
        return submitCount;
    }

    private List<PdaAssetIdentifyRequest> copy(
            List<PdaAssetIdentifyRequest> identifiers) {
        return identifiers == null ? null : new ArrayList<>(identifiers);
    }
}
