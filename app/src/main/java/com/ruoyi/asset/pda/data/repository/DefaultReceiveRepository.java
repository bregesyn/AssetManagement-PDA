package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmRequestDto;

import java.util.ArrayList;
import java.util.List;

/**
 * 领用请求的输入校验集中在此处；重复标识保留给后端预检逐行返回，不能在此静默丢失。
 */
public final class DefaultReceiveRepository implements ReceiveRepository {
    public static final int MAX_BATCH_SIZE = 100;
    public static final int MAX_EPC_LENGTH = 128;
    public static final int MAX_ASSET_CODE_LENGTH = 64;
    public static final int MAX_REMARK_LENGTH = 500;
    public static final int MAX_RECIPIENT_KEYWORD_LENGTH = 30;

    private static final String IDENTIFY_TYPE_EPC = "EPC";
    private static final String IDENTIFY_TYPE_ASSET_CODE = "ASSET_CODE";

    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultReceiveRepository(PdaApiService apiService,
            ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("领用 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle searchRecipients(String keyword,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
        String checkedKeyword = trim(keyword);
        if (!check(callback, checkedKeyword != null
                && checkedKeyword.length() <= MAX_RECIPIENT_KEYWORD_LENGTH)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.receiveRecipients(checkedKeyword),
                true, callback);
    }

    @Override
    public RequestHandle batchCheck(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaReceiveBatchCheckDto> callback) {
        List<PdaAssetIdentifyRequest> checkedIdentifiers = normalizeIdentifiers(identifiers);
        if (!check(callback, validRecipient(receiveUserId, receiveDeptId)
                && checkedIdentifiers != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.receiveBatchCheck(
                new PdaReceiveBatchCheckRequestDto(receiveUserId, receiveDeptId,
                        checkedIdentifiers)), true, callback);
    }

    @Override
    public RequestHandle batchConfirm(Long receiveUserId, Long receiveDeptId,
            List<PdaAssetIdentifyRequest> identifiers, String remark,
            RepositoryCallback<PdaReceiveBatchConfirmDto> callback) {
        List<PdaAssetIdentifyRequest> checkedIdentifiers = normalizeIdentifiers(identifiers);
        String checkedRemark = trim(remark);
        if (!check(callback, validRecipient(receiveUserId, receiveDeptId)
                && checkedIdentifiers != null
                && (checkedRemark == null
                || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.receiveBatchConfirm(
                new PdaReceiveBatchConfirmRequestDto(receiveUserId, receiveDeptId,
                        checkedIdentifiers, checkedRemark)), true, callback);
    }

    private List<PdaAssetIdentifyRequest> normalizeIdentifiers(
            List<PdaAssetIdentifyRequest> identifiers) {
        if (identifiers == null || identifiers.isEmpty()
                || identifiers.size() > MAX_BATCH_SIZE) {
            return null;
        }
        List<PdaAssetIdentifyRequest> result = new ArrayList<>(identifiers.size());
        for (PdaAssetIdentifyRequest identifier : identifiers) {
            if (identifier == null) {
                return null;
            }
            String type = trim(identifier.getIdentifyType());
            String value = trim(identifier.getIdentifyValue());
            if (IDENTIFY_TYPE_EPC.equals(type)) {
                value = normalizeEpc(value);
            } else if (IDENTIFY_TYPE_ASSET_CODE.equals(type)) {
                if (value == null || value.length() > MAX_ASSET_CODE_LENGTH) {
                    return null;
                }
            } else {
                return null;
            }
            if (value == null) {
                return null;
            }
            result.add(new PdaAssetIdentifyRequest(type, value));
        }
        return result;
    }

    private String normalizeEpc(String value) {
        if (value == null || value.length() > MAX_EPC_LENGTH) {
            return null;
        }
        try {
            return UhfTagReading.normalizeEpc(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean validRecipient(Long receiveUserId, Long receiveDeptId) {
        return receiveUserId != null && receiveUserId > 0L
                && receiveDeptId != null && receiveDeptId > 0L;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean check(RepositoryCallback<?> callback, boolean valid) {
        if (callback == null) {
            throw new IllegalArgumentException("领用回调不能为空");
        }
        if (!valid) {
            callback.onError(callExecutor.protocolError());
        }
        return valid;
    }
}
