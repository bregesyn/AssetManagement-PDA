package com.ruoyi.asset.pda.feature.repair;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.testing.FakeCommonRepository;

import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 权限组合与分页测试，证明只请求当前账号被允许读取的队列。 */
public class RepairWorkbenchViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void submitOnlyDoesNotRequestEitherOrderList() {
        FakeCommonRepository common = new FakeCommonRepository();
        CapturingRepairRepository repair = new CapturingRepairRepository();
        RepairWorkbenchViewModel viewModel = new RepairWorkbenchViewModel(repair, common,
                false, true, false, false);

        viewModel.initialize();
        common.completeBootstrap(bootstrap());

        assertEquals(0, repair.myCalls);
        assertEquals(0, repair.workCalls);
        assertEquals(RepairWorkbenchUiState.Tab.NONE, state(viewModel).getTab());
        assertTrue(state(viewModel).isCanSubmit());
    }

    @Test
    public void mineAndWorkQueuesUseTwentyItemPagesAndPermissionTabs() {
        FakeCommonRepository common = new FakeCommonRepository();
        CapturingRepairRepository repair = new CapturingRepairRepository();
        RepairWorkbenchViewModel viewModel = new RepairWorkbenchViewModel(repair, common,
                true, false, true, false);

        viewModel.initialize();
        common.completeBootstrap(bootstrap());

        assertEquals(1, repair.myCalls);
        assertEquals(20, repair.lastPageSize);
        assertEquals(RepairWorkbenchUiState.Tab.MINE, state(viewModel).getTab());
        repair.completeMine(page("WAIT_REPAIR"));
        assertEquals("BX-001", state(viewModel).getOrders().get(0).getRepairNo());

        viewModel.selectTab(RepairWorkbenchUiState.Tab.WORK);
        assertEquals(1, repair.workCalls);
        assertEquals(20, repair.lastPageSize);
        repair.completeWork(page("REPAIRING"));
        assertEquals(RepairWorkbenchUiState.Tab.WORK, state(viewModel).getTab());
        assertEquals("REPAIRING", state(viewModel).getOrders().get(0).getOrderStatus());
        assertTrue(state(viewModel).isCanStart());
        assertFalse(state(viewModel).isCanFinish());
    }

    private RepairWorkbenchUiState state(RepairWorkbenchViewModel viewModel) {
        return viewModel.getState().getValue();
    }

    private PdaBootstrapDto bootstrap() {
        Map<String, List<PdaDictItemDto>> dicts = new LinkedHashMap<>();
        dicts.put("ams_repair_status", Arrays.asList(
                new PdaDictItemDto("ams_repair_status", "待维修", "WAIT_REPAIR", null, null, "N"),
                new PdaDictItemDto("ams_repair_status", "维修中", "REPAIRING", null, null, "N")));
        return new PdaBootstrapDto("2026-08-06 08:00:00",
                new PdaUserDto(1L, "repair", "维修员", 2L, "设备部", Collections.emptyList()),
                dicts, Collections.emptyMap());
    }

    private PdaPageResultDto<PdaRepairOrderDto> page(String status) {
        Type type = new TypeToken<PdaPageResultDto<PdaRepairOrderDto>>() { }.getType();
        return new Gson().fromJson("{\"total\":1,\"pageNum\":1,\"pageSize\":20,\"rows\":[{"
                + "\"repairId\":1,\"repairNo\":\"BX-001\",\"orderStatus\":\"" + status + "\"}]}", type);
    }

    private static final class CapturingRepairRepository implements RepairRepository {
        private int myCalls;
        private int workCalls;
        private int lastPageSize;
        private RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> mineCallback;
        private RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> workCallback;

        @Override public RequestHandle loadMyOrders(String orderStatus, String keyword, int pageNum,
                int pageSize, RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) {
            myCalls++; lastPageSize = pageSize; mineCallback = callback; return RequestHandle.NONE;
        }
        @Override public RequestHandle loadWorkOrders(String keyword, int pageNum, int pageSize,
                RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) {
            workCalls++; lastPageSize = pageSize; workCallback = callback; return RequestHandle.NONE;
        }
        void completeMine(PdaPageResultDto<PdaRepairOrderDto> page) { mineCallback.onSuccess(page); }
        void completeWork(PdaPageResultDto<PdaRepairOrderDto> page) { workCallback.onSuccess(page); }
        @Override public RequestHandle loadOrder(Long repairId, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle searchRepairers(String keyword, RepositoryCallback<List<PdaRepairerDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle submit(PdaAssetIdentifyRequest identifier, String faultDesc, String expectedFinishTime, String remark, RepositoryCallback<PdaRepairSubmitResultDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle startRepair(Long repairId, String repairerType, Long repairUserId, String repairUserName, String repairOrgName, String repairContactPhone, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle finishRepair(Long repairId, String repairFinishTime, String repairResult, BigDecimal repairCost, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
    }
}
