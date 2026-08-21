package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchCheckRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchCheckRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 借还请求的输入边界集中在 Repository，避免页面直接把客户端残留字段发给后端。
 */
public final class DefaultBorrowRepository implements BorrowRepository {
    public static final int MAX_BATCH_SIZE = 100;
    public static final int MAX_EPC_LENGTH = 128;
    public static final int MAX_ASSET_CODE_LENGTH = 64;
    public static final int MAX_REMARK_LENGTH = 500;
    public static final int MAX_KEYWORD_LENGTH = 30;
    public static final int MAX_ORG_NAME_LENGTH = 100;
    public static final int MAX_CONTACT_NAME_LENGTH = 100;
    public static final int MAX_PHONE_LENGTH = 30;

    public static final String BORROWER_TYPE_INTERNAL = "INTERNAL";
    public static final String BORROWER_TYPE_EXTERNAL = "EXTERNAL";
    public static final String IDENTIFY_TYPE_EPC = "EPC";
    public static final String IDENTIFY_TYPE_ASSET_CODE = "ASSET_CODE";

    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultBorrowRepository(PdaApiService apiService, ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("借还 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle searchBorrowers(String keyword,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
        String checkedKeyword = trim(keyword);
        if (!check(callback, checkedKeyword != null
                && checkedKeyword.length() <= MAX_KEYWORD_LENGTH)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.borrowBorrowers(checkedKeyword), true, callback);
    }

    @Override
    public RequestHandle batchCheckIssue(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowBatchCheckDto> callback) {
        List<PdaAssetIdentifyRequest> checked = normalizeIdentifiers(identifiers);
        IssueInput input = normalizeIssueInput(borrowerType, borrowUserId, borrowDeptId,
                borrowOrgName, borrowContactPhone, borrowExternalContactName,
                borrowExternalContactPhone, expectedReturnDate);
        if (!check(callback, input != null && checked != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.borrowIssueBatchCheck(
                new PdaBorrowIssueBatchCheckRequestDto(input.borrowerType,
                        input.borrowUserId, input.borrowDeptId, input.borrowOrgName,
                        input.borrowContactPhone, input.borrowExternalContactName,
                        input.borrowExternalContactPhone, input.expectedReturnDate, checked)),
                true, callback);
    }

    @Override
    public RequestHandle batchSubmitIssue(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            String remark, RepositoryCallback<PdaBorrowIssueBatchSubmitDto> callback) {
        List<PdaAssetIdentifyRequest> checked = normalizeIdentifiers(identifiers);
        IssueInput input = normalizeIssueInput(borrowerType, borrowUserId, borrowDeptId,
                borrowOrgName, borrowContactPhone, borrowExternalContactName,
                borrowExternalContactPhone, expectedReturnDate);
        String checkedRemark = trim(remark);
        if (!check(callback, input != null && checked != null
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.borrowIssueBatchSubmit(
                new PdaBorrowIssueBatchSubmitRequestDto(input.borrowerType,
                        input.borrowUserId, input.borrowDeptId, input.borrowOrgName,
                        input.borrowContactPhone, input.borrowExternalContactName,
                        input.borrowExternalContactPhone, input.expectedReturnDate,
                        checked, checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle batchCheckReturn(List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowBatchCheckDto> callback) {
        List<PdaAssetIdentifyRequest> checked = normalizeIdentifiers(identifiers);
        if (!check(callback, checked != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.borrowReturnBatchCheck(
                new PdaBorrowReturnBatchCheckRequestDto(checked)), true, callback);
    }

    @Override
    public RequestHandle batchSubmitReturn(List<PdaAssetIdentifyRequest> identifiers,
            RepositoryCallback<PdaBorrowReturnBatchSubmitDto> callback) {
        List<PdaAssetIdentifyRequest> checked = normalizeIdentifiers(identifiers);
        if (!check(callback, checked != null)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.borrowReturnBatchSubmit(
                new PdaBorrowReturnBatchSubmitRequestDto(checked)), true, callback);
    }

    private IssueInput normalizeIssueInput(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate) {
        String type = trim(borrowerType);
        if (type == null) {
            return null;
        }
        type = type.toUpperCase(Locale.ROOT);
        if ((!BORROWER_TYPE_INTERNAL.equals(type)
                && !BORROWER_TYPE_EXTERNAL.equals(type))
                || borrowUserId == null || borrowUserId < 1L
                || borrowDeptId == null || borrowDeptId < 1L
                || !validDate(expectedReturnDate)) {
            return null;
        }
        String org = trim(borrowOrgName);
        String internalContactPhone = trim(borrowContactPhone);
        String externalContactName = trim(borrowExternalContactName);
        String externalContactPhone = trim(borrowExternalContactPhone);
        if (BORROWER_TYPE_EXTERNAL.equals(type)) {
            if (org == null || internalContactPhone == null || externalContactName == null
                    || externalContactPhone == null
                    || org.length() > MAX_ORG_NAME_LENGTH
                    || internalContactPhone.length() > MAX_PHONE_LENGTH
                    || externalContactName.length() > MAX_CONTACT_NAME_LENGTH
                    || externalContactPhone.length() > MAX_PHONE_LENGTH) {
                return null;
            }
        } else {
            // 内部借用不携带外部联系资料，避免切换借用类型后混入旧表单值。
            org = null;
            internalContactPhone = null;
            externalContactName = null;
            externalContactPhone = null;
        }
        return new IssueInput(type, borrowUserId, borrowDeptId, org, internalContactPhone,
                externalContactName, externalContactPhone, expectedReturnDate.trim());
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

    private boolean validDate(String value) {
        if (value == null || value.trim().length() != 10) {
            return false;
        }
        String checked = value.trim();
        return checked.charAt(4) == '-' && checked.charAt(7) == '-';
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String checked = value.trim();
        return checked.isEmpty() ? null : checked;
    }

    private boolean check(RepositoryCallback<?> callback, boolean valid) {
        if (callback == null) {
            throw new IllegalArgumentException("借还回调不能为空");
        }
        if (!valid) {
            callback.onError(callExecutor.protocolError());
        }
        return valid;
    }

    private static final class IssueInput {
        private final String borrowerType;
        private final Long borrowUserId;
        private final Long borrowDeptId;
        private final String borrowOrgName;
        private final String borrowContactPhone;
        private final String borrowExternalContactName;
        private final String borrowExternalContactPhone;
        private final String expectedReturnDate;

        private IssueInput(String borrowerType, Long borrowUserId, Long borrowDeptId,
                String borrowOrgName, String borrowContactPhone,
                String borrowExternalContactName, String borrowExternalContactPhone,
                String expectedReturnDate) {
            this.borrowerType = borrowerType;
            this.borrowUserId = borrowUserId;
            this.borrowDeptId = borrowDeptId;
            this.borrowOrgName = borrowOrgName;
            this.borrowContactPhone = borrowContactPhone;
            this.borrowExternalContactName = borrowExternalContactName;
            this.borrowExternalContactPhone = borrowExternalContactPhone;
            this.expectedReturnDate = expectedReturnDate;
        }
    }
}
