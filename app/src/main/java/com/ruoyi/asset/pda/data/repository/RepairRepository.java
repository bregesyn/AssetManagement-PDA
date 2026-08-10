package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;

import java.math.BigDecimal;
import java.util.List;

/** PDA 报修维修协议边界，页面不直接依赖 Retrofit。 */
public interface RepairRepository {
    RequestHandle loadMyOrders(String orderStatus, String keyword, int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback);

    RequestHandle loadWorkOrders(String keyword, int pageNum, int pageSize,
            RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback);

    RequestHandle loadOrder(Long repairId, RepositoryCallback<PdaRepairOrderDto> callback);

    RequestHandle searchRepairers(String keyword,
            RepositoryCallback<List<PdaRepairerDto>> callback);

    RequestHandle submit(PdaAssetIdentifyRequest identifier, String faultDesc,
            String expectedFinishTime, String remark,
            RepositoryCallback<PdaRepairSubmitResultDto> callback);

    RequestHandle startRepair(Long repairId, String repairerType, Long repairUserId,
            String repairUserName, String repairOrgName, String repairContactPhone,
            RepositoryCallback<PdaRepairOrderDto> callback);

    RequestHandle finishRepair(Long repairId, String repairFinishTime, String repairResult,
            BigDecimal repairCost, RepositoryCallback<PdaRepairOrderDto> callback);
}
