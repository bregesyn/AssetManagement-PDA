package com.ruoyi.asset.pda.feature.repair;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 开始维修 ViewModel 表单校验单元测试。 */
public class RepairStartViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void unselectedInternalRepairerDoesNotSubmit() {
        FakeRepairRepository repair = new FakeRepairRepository();
        RepairStartViewModel viewModel = new RepairStartViewModel(101L, repair);
        viewModel.load();
        repair.completeLoadOrder(order(101L, RepairUi.STATUS_WAIT_REPAIR));

        viewModel.submit(null, null, null);

        assertEquals(0, repair.startRepairCount);
        assertTrue(state(viewModel).getMessage().contains("请选择内部维修人"));
    }

    @Test
    public void emptyExternalOrgDoesNotSubmit() {
        FakeRepairRepository repair = new FakeRepairRepository();
        RepairStartViewModel viewModel = new RepairStartViewModel(101L, repair);
        viewModel.load();
        repair.completeLoadOrder(order(101L, RepairUi.STATUS_WAIT_REPAIR));
        viewModel.selectRepairerType(RepairUi.REPAIRER_EXTERNAL);

        viewModel.submit("   ", null, null);

        assertEquals(0, repair.startRepairCount);
        assertTrue(state(viewModel).getMessage().contains("请填写外部维修单位"));
    }

    private RepairStartUiState state(RepairStartViewModel viewModel) {
        return viewModel.getState().getValue();
    }

    private PdaRepairOrderDto order(Long repairId, String status) {
        return new Gson().fromJson("{\"repairId\":" + repairId + ",\"repairNo\":\"BX202608070001\",\"orderStatus\":\"" + status + "\"}", PdaRepairOrderDto.class);
    }

    private static final class FakeRepairRepository implements RepairRepository {
        private RepositoryCallback<PdaRepairOrderDto> loadOrderCallback;
        private int startRepairCount;

        void completeLoadOrder(PdaRepairOrderDto dto) {
            if (loadOrderCallback != null) {
                loadOrderCallback.onSuccess(dto);
            }
        }

        @Override public RequestHandle loadMyOrders(String orderStatus, String keyword, int pageNum, int pageSize, RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle loadWorkOrders(String keyword, int pageNum, int pageSize, RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle loadOrder(Long repairId, RepositoryCallback<PdaRepairOrderDto> callback) { this.loadOrderCallback = callback; return RequestHandle.NONE; }
        @Override public RequestHandle searchRepairers(String keyword, RepositoryCallback<List<PdaRepairerDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle submit(PdaAssetIdentifyRequest identifier, String faultDesc, String expectedFinishTime, String remark, RepositoryCallback<PdaRepairSubmitResultDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle startRepair(Long repairId, String repairerType, Long repairUserId, String repairUserName, String repairOrgName, String repairContactPhone, RepositoryCallback<PdaRepairOrderDto> callback) { startRepairCount++; return RequestHandle.NONE; }
        @Override public RequestHandle finishRepair(Long repairId, String repairFinishTime, String repairResult, BigDecimal repairCost, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
    }
}
