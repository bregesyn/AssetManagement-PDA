package com.ruoyi.asset.pda.feature.repair;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairSubmitResultDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.RepairRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.testing.FakeAssetRepository;
import com.ruoyi.asset.pda.testing.FakeCommonRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 报修提交的 SINGLE 扫描和服务端身份输入边界。 */
public class RepairSubmitViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void singleEpcScanPreviewsAssetAndSubmitsOnlyIdentifier() {
        FakeCommonRepository common = new FakeCommonRepository();
        FakeAssetRepository asset = new FakeAssetRepository();
        CapturingRepairRepository repair = new CapturingRepairRepository();
        FakeUhfScanner scanner = new FakeUhfScanner();
        RepairSubmitViewModel viewModel = new RepairSubmitViewModel(asset, repair, common, scanner);

        viewModel.initialize();
        common.completeBootstrap(bootstrap());
        viewModel.toggleScan();
        scanner.emit("e20001", -42);
        assertEquals("EPC", asset.getLastType());
        assertEquals("E20001", asset.getLastValue());
        asset.completeLast(asset());

        viewModel.submit("按键无响应", "2026-08-07", "现场报修");

        assertEquals(1, repair.submitCount);
        assertEquals("EPC", repair.identifier.getIdentifyType());
        assertEquals("E20001", repair.identifier.getIdentifyValue());
        assertEquals("按键无响应", repair.faultDesc);
        assertEquals("2026-08-07", repair.expectedFinishTime);
        assertEquals("现场报修", repair.remark);
        assertEquals(Long.valueOf(11L), state(viewModel).getAsset().getAssetId());
    }

    @Test
    public void multipleTagsAreRejectedBeforeAnyAssetRequest() {
        FakeCommonRepository common = new FakeCommonRepository();
        FakeAssetRepository asset = new FakeAssetRepository();
        FakeUhfScanner scanner = new FakeUhfScanner();
        RepairSubmitViewModel viewModel = new RepairSubmitViewModel(asset,
                new CapturingRepairRepository(), common, scanner);
        viewModel.initialize();
        common.completeBootstrap(bootstrap());

        viewModel.toggleScan();
        scanner.emitRound("E20001", "E20002");

        assertEquals(0, asset.getRequestCount());
        assertTrue(state(viewModel).getMessage().contains("多个 RFID 标签"));
    }

    @Test
    public void expectedDateBeforeServerDateDoesNotSubmit() {
        FakeCommonRepository common = new FakeCommonRepository();
        FakeAssetRepository asset = new FakeAssetRepository();
        CapturingRepairRepository repair = new CapturingRepairRepository();
        RepairSubmitViewModel viewModel = new RepairSubmitViewModel(asset, repair, common,
                new FakeUhfScanner());
        viewModel.initialize();
        common.completeBootstrap(bootstrap());
        viewModel.selectIdentifyType("ASSET_CODE");
        viewModel.identifyAssetCode("ZC-001");
        asset.completeLast(asset());

        viewModel.submit("按键无响应", "2026-08-05", null);

        assertEquals(0, repair.submitCount);
        assertTrue(state(viewModel).getMessage().contains("服务器日期"));
    }

    @Test
    public void emptyFaultDescDoesNotSubmit() {
        FakeCommonRepository common = new FakeCommonRepository();
        FakeAssetRepository asset = new FakeAssetRepository();
        CapturingRepairRepository repair = new CapturingRepairRepository();
        RepairSubmitViewModel viewModel = new RepairSubmitViewModel(asset, repair, common,
                new FakeUhfScanner());
        viewModel.initialize();
        common.completeBootstrap(bootstrap());
        viewModel.selectIdentifyType("ASSET_CODE");
        viewModel.identifyAssetCode("ZC-001");
        asset.completeLast(asset());

        viewModel.submit("   ", "2026-08-07", null);

        assertEquals(0, repair.submitCount);
        assertTrue(state(viewModel).getMessage().contains("请填写故障描述"));
    }

    @Test
    public void startingFreshEpcScanClearsPreviousAssetPreview() {
        FakeCommonRepository common = new FakeCommonRepository();
        FakeAssetRepository asset = new FakeAssetRepository();
        RepairSubmitViewModel viewModel = new RepairSubmitViewModel(asset,
                new CapturingRepairRepository(), common, new FakeUhfScanner());
        viewModel.initialize();
        common.completeBootstrap(bootstrap());
        viewModel.selectIdentifyType("ASSET_CODE");
        viewModel.identifyAssetCode("ZC-001");
        asset.completeLast(asset());
        assertEquals("测试资产", state(viewModel).getAsset().getAssetName());

        viewModel.selectIdentifyType("EPC");
        viewModel.toggleScan();

        assertNull(state(viewModel).getAsset());
        assertNull(state(viewModel).getIdentifyValue());
    }

    @Test
    public void unboundEpcResetsScanStateToIdle() {
        FakeCommonRepository common = new FakeCommonRepository();
        FakeAssetRepository asset = new FakeAssetRepository();
        FakeUhfScanner scanner = new FakeUhfScanner();
        RepairSubmitViewModel viewModel = new RepairSubmitViewModel(asset,
                new CapturingRepairRepository(), common, scanner);
        viewModel.initialize();
        common.completeBootstrap(bootstrap());

        viewModel.toggleScan();
        scanner.emit("e20000112233", -50);
        asset.failLast(TestErrors.business("EPC 未绑定资产，请先完成 RFID 绑定"));

        assertFalse(state(viewModel).isScanning());
        assertTrue(state(viewModel).getMessage().contains("未绑定资产"));
    }

    private RepairSubmitUiState state(RepairSubmitViewModel viewModel) {
        return viewModel.getState().getValue();
    }

    private PdaBootstrapDto bootstrap() {
        return new PdaBootstrapDto("2026-08-06 08:00:00",
                new PdaUserDto(1L, "reporter", "报修人", 2L, "设备部", Collections.emptyList()),
                Collections.emptyMap(), Collections.emptyMap());
    }

    private PdaAssetIdentifyDto asset() {
        return new PdaAssetIdentifyDto(11L, "ZC-001", "测试资产", 3L, "电子设备",
                "M1", "品牌", "IN_STOCK", "在库", null, null, null, null,
                null, null, "E20001", null, null, null, null, true);
    }

    private static final class CapturingRepairRepository implements RepairRepository {
        private int submitCount;
        private PdaAssetIdentifyRequest identifier;
        private String faultDesc;
        private String expectedFinishTime;
        private String remark;

        @Override public RequestHandle loadMyOrders(String orderStatus, String keyword, int pageNum, int pageSize, RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle loadWorkOrders(String keyword, int pageNum, int pageSize, RepositoryCallback<PdaPageResultDto<PdaRepairOrderDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle loadOrder(Long repairId, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle searchRepairers(String keyword, RepositoryCallback<List<PdaRepairerDto>> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle submit(PdaAssetIdentifyRequest identifier, String faultDesc, String expectedFinishTime, String remark, RepositoryCallback<PdaRepairSubmitResultDto> callback) {
            submitCount++; this.identifier = identifier; this.faultDesc = faultDesc;
            this.expectedFinishTime = expectedFinishTime; this.remark = remark; return RequestHandle.NONE;
        }
        @Override public RequestHandle startRepair(Long repairId, String repairerType, Long repairUserId, String repairUserName, String repairOrgName, String repairContactPhone, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
        @Override public RequestHandle finishRepair(Long repairId, String repairFinishTime, String repairResult, BigDecimal repairCost, RepositoryCallback<PdaRepairOrderDto> callback) { return RequestHandle.NONE; }
    }
}
