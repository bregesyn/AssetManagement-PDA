package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.api.PdaApiService;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairFinishRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairStartRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 报修接口的输入边界。这里裁剪客户端字段，避免内部维修姓名或资产身份残留进入请求。
 */
public final class DefaultRepairRepository implements RepairRepository {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_EPC_LENGTH = 128;
    public static final int MAX_ASSET_CODE_LENGTH = 64;
    public static final int MAX_ORDER_KEYWORD_LENGTH = 64;
    public static final int MAX_REPAIRER_KEYWORD_LENGTH = 30;
    public static final int MAX_FAULT_DESC_LENGTH = 500;
    public static final int MAX_REMARK_LENGTH = 500;
    public static final int MAX_REPAIR_RESULT_LENGTH = 500;
    public static final int MAX_REPAIRER_NAME_LENGTH = 100;
    public static final int MAX_REPAIR_ORG_LENGTH = 100;
    public static final int MAX_PHONE_LENGTH = 30;

    public static final String IDENTIFY_TYPE_EPC = "EPC";
    public static final String IDENTIFY_TYPE_ASSET_CODE = "ASSET_CODE";
    public static final String REPAIRER_TYPE_INTERNAL = "INTERNAL";
    public static final String REPAIRER_TYPE_EXTERNAL = "EXTERNAL";

    private static final BigDecimal MAX_REPAIR_COST = new BigDecimal("9999999999999999.99");

    private final PdaApiService apiService;
    private final ApiCallExecutor callExecutor;

    public DefaultRepairRepository(PdaApiService apiService, ApiCallExecutor callExecutor) {
        if (apiService == null || callExecutor == null) {
            throw new IllegalArgumentException("报修 Repository 依赖不能为空");
        }
        this.apiService = apiService;
        this.callExecutor = callExecutor;
    }

    @Override
    public RequestHandle loadMyOrders(String orderStatus, String keyword, int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) {
        String checkedStatus = trim(orderStatus);
        String checkedKeyword = trim(keyword);
        if (!check(callback, validPage(pageNum, pageSize)
                && (checkedKeyword == null || checkedKeyword.length() <= MAX_ORDER_KEYWORD_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairMyOrders(checkedStatus, checkedKeyword,
                pageNum, pageSize), true, callback);
    }

    @Override
    public RequestHandle loadWorkOrders(String keyword, int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) {
        String checkedKeyword = trim(keyword);
        if (!check(callback, validPage(pageNum, pageSize)
                && (checkedKeyword == null || checkedKeyword.length() <= MAX_ORDER_KEYWORD_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairWorkOrders(checkedKeyword, pageNum, pageSize),
                true, callback);
    }

    @Override
    public RequestHandle loadOrder(Long repairId, RepositoryCallback<PdaRepairOrderDto> callback) {
        if (!check(callback, positiveId(repairId))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairOrder(repairId), true, callback);
    }

    @Override
    public RequestHandle searchRepairers(String keyword,
            RepositoryCallback<List<PdaRepairerDto>> callback) {
        String checkedKeyword = trim(keyword);
        if (!check(callback, checkedKeyword != null
                && checkedKeyword.length() <= MAX_REPAIRER_KEYWORD_LENGTH)) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairers(checkedKeyword), true, callback);
    }

    @Override
    public RequestHandle submit(PdaAssetIdentifyRequest identifier, String faultDesc,
            String expectedFinishTime, String remark,
            RepositoryCallback<PdaRepairSubmitResultDto> callback) {
        PdaAssetIdentifyRequest checkedIdentifier = normalizeIdentifier(identifier);
        String checkedFaultDesc = trim(faultDesc);
        String checkedDate = trim(expectedFinishTime);
        String checkedRemark = trim(remark);
        if (!check(callback, checkedIdentifier != null && checkedFaultDesc != null
                && checkedFaultDesc.length() <= MAX_FAULT_DESC_LENGTH
                && (checkedDate == null || validDate(checkedDate))
                && (checkedRemark == null || checkedRemark.length() <= MAX_REMARK_LENGTH))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairSubmit(new PdaRepairSubmitRequestDto(
                checkedIdentifier, checkedFaultDesc, checkedDate, checkedRemark)), true, callback);
    }

    @Override
    public RequestHandle startRepair(Long repairId, String repairerType, Long repairUserId,
            String repairUserName, String repairOrgName, String repairContactPhone,
            RepositoryCallback<PdaRepairOrderDto> callback) {
        String type = trim(repairerType);
        if (type != null) {
            type = type.toUpperCase(Locale.ROOT);
        }
        PdaRepairStartRequestDto request;
        if (REPAIRER_TYPE_INTERNAL.equals(type)) {
            if (!check(callback, positiveId(repairId) && positiveId(repairUserId))) {
                return RequestHandle.NONE;
            }
            // 服务器会重新查询内部用户，客户端不上传可能过期的姓名、电话或外部单位。
            request = new PdaRepairStartRequestDto(type, repairUserId, null, null, null);
        } else if (REPAIRER_TYPE_EXTERNAL.equals(type)) {
            String checkedName = trim(repairUserName);
            String checkedOrg = trim(repairOrgName);
            String checkedPhone = trim(repairContactPhone);
            if (!check(callback, positiveId(repairId) && checkedOrg != null
                    && checkedOrg.length() <= MAX_REPAIR_ORG_LENGTH
                    && (checkedName == null || checkedName.length() <= MAX_REPAIRER_NAME_LENGTH)
                    && (checkedPhone == null || checkedPhone.length() <= MAX_PHONE_LENGTH))) {
                return RequestHandle.NONE;
            }
            request = new PdaRepairStartRequestDto(type, null, checkedName, checkedOrg, checkedPhone);
        } else {
            check(callback, false);
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairStart(repairId, request), true, callback);
    }

    @Override
    public RequestHandle finishRepair(Long repairId, String repairFinishTime, String repairResult,
            BigDecimal repairCost, RepositoryCallback<PdaRepairOrderDto> callback) {
        String checkedDate = trim(repairFinishTime);
        String checkedResult = trim(repairResult);
        if (!check(callback, positiveId(repairId) && validDate(checkedDate)
                && checkedResult != null && checkedResult.length() <= MAX_REPAIR_RESULT_LENGTH
                && validCost(repairCost))) {
            return RequestHandle.NONE;
        }
        return callExecutor.execute(apiService.repairFinish(repairId,
                new PdaRepairFinishRequestDto(checkedDate, checkedResult, repairCost)), true, callback);
    }

    private PdaAssetIdentifyRequest normalizeIdentifier(PdaAssetIdentifyRequest identifier) {
        if (identifier == null) {
            return null;
        }
        String type = trim(identifier.getIdentifyType());
        String value = trim(identifier.getIdentifyValue());
        if (IDENTIFY_TYPE_EPC.equals(type)) {
            if (value == null || value.length() > MAX_EPC_LENGTH) {
                return null;
            }
            try {
                value = UhfTagReading.normalizeEpc(value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        } else if (IDENTIFY_TYPE_ASSET_CODE.equals(type)) {
            if (value == null || value.length() > MAX_ASSET_CODE_LENGTH) {
                return null;
            }
        } else {
            return null;
        }
        return new PdaAssetIdentifyRequest(type, value);
    }

    private boolean validPage(int pageNum, int pageSize) {
        return pageNum >= 1 && pageSize >= 1 && pageSize <= MAX_PAGE_SIZE;
    }

    private boolean positiveId(Long value) {
        return value != null && value > 0L;
    }

    private boolean validCost(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0
                && value.scale() <= 2 && value.compareTo(MAX_REPAIR_COST) <= 0;
    }

    private boolean validDate(String value) {
        if (value == null || value.length() != 10) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setLenient(false);
        try {
            Date parsed = format.parse(value);
            return parsed != null && format.format(parsed).equals(value);
        } catch (ParseException exception) {
            return false;
        }
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
            throw new IllegalArgumentException("报修回调不能为空");
        }
        if (!valid) {
            callback.onError(callExecutor.protocolError());
        }
        return valid;
    }
}
