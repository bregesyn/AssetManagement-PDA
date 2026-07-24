package com.ruoyi.asset.pda.feature.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.testing.FakeCommonRepository;
import com.ruoyi.asset.pda.testing.FakeInboundRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class InboundViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeInboundRepository inboundRepository;
    private FakeCommonRepository commonRepository;
    private FakeUhfScanner scanner;
    private InboundViewModel viewModel;

    @Before
    public void setUp() {
        inboundRepository = new FakeInboundRepository();
        commonRepository = new FakeCommonRepository();
        scanner = new FakeUhfScanner();
        viewModel = new InboundViewModel(inboundRepository,
                commonRepository, scanner, true);
    }

    @Test
    public void initializationLoadsOperatorWarehousesAndLocations() {
        initializeBase();

        assertEquals("测试用户", state().getOperatorName());
        assertEquals(1, state().getWarehouses().size());
        viewModel.changeWarehouse(5L);
        assertEquals(5L, commonRepository.getLastWarehouseId().longValue());

        commonRepository.completeLocations(locations());

        assertEquals(2, state().getLocations().size());
        assertFalse(state().isBusy());
    }

    @Test
    public void scanRequiresWarehouseAndLocation() {
        initializeBase();

        viewModel.toggleScan();

        assertNull(scanner.getMode());
        assertTrue(state().getErrorMessage().contains("仓库和位置"));
    }

    @Test
    public void bootstrapFailureNeverMarksPageReadyEvenIfWarehousesLoaded() {
        viewModel.initialize();
        commonRepository.completeWarehouses(Collections.singletonList(
                new PdaMasterDataDto(5L, "WH-01", "一号仓",
                        null, null)));
        commonRepository.failBootstrap(TestErrors.network());

        assertTrue(state().isInitialLoadFailed());
        assertFalse(state().isInitialReady());

        viewModel.retryInitialization();

        assertEquals(2, commonRepository.getBootstrapCount());
        assertEquals(2, commonRepository.getWarehousesCount());
    }

    @Test
    public void batchScanAddsOnlyEligibleAssetsAndKeepsIssuesSeparate() {
        readyAtDestination();

        viewModel.toggleScan();
        assertEquals(UhfScanMode.BATCH, scanner.getMode());
        scanner.emitRound("E20001", "E20002");
        viewModel.toggleScan();

        assertEquals(Arrays.asList("E20001", "E20002"),
                inboundRepository.getLastBatchEpcs());
        inboundRepository.completeBatchCheck(batchCheck());

        assertEquals(1, state().getAssets().size());
        assertEquals("ZC-001", state().getAssets().get(0).getAssetCode());
        assertEquals(1, state().getIssues().size());
        assertEquals(0, state().getRawEpcCount());
    }

    @Test
    public void assetCodeCanAddEligibleAssetWithoutRfidBinding() {
        readyAtDestination();

        viewModel.addByAssetCode(" ZC-002 ");
        assertEquals("ZC-002", inboundRepository.getLastAssetCode());
        inboundRepository.completeEligibility(eligibility(12L, "ZC-002", true, null));

        assertEquals(1, state().getAssets().size());
        assertEquals(InboundAssetItem.Source.ASSET_CODE,
                state().getAssets().get(0).getSource());
        assertNull(state().getAssets().get(0).getTagCode());
        assertEquals(1, state().getAssetCodeClearVersion());
    }

    @Test
    public void ineligibleAssetCodeDoesNotEnterConfirmationList() {
        readyAtDestination();
        viewModel.addByAssetCode("ZC-003");

        inboundRepository.completeEligibility(eligibility(
                13L, "ZC-003", false, "资产已在库"));

        assertTrue(state().getAssets().isEmpty());
        assertEquals("资产已在库", state().getErrorMessage());
    }

    @Test
    public void changingLocationClearsUnsubmittedBatch() {
        readyAtDestination();
        addAsset(12L, "ZC-002");
        int previousReset = state().getBatchResetVersion();

        viewModel.changeLocation(7L);

        assertTrue(state().getAssets().isEmpty());
        assertEquals(7L, state().getSelectedLocationId().longValue());
        assertTrue(state().getBatchResetVersion() > previousReset);
    }

    @Test
    public void successfulConfirmationUsesServerFactsAndClearsBatch() {
        readyAtDestination();
        addAsset(12L, "ZC-002");

        viewModel.confirm(" 现场接收 ");

        assertEquals(5L, inboundRepository.getLastWarehouseId().longValue());
        assertEquals(6L, inboundRepository.getLastLocationId().longValue());
        assertEquals(Collections.singletonList(12L),
                inboundRepository.getLastAssetIds());
        assertEquals("现场接收", inboundRepository.getLastRemark());
        inboundRepository.completeConfirm(confirmation(12L));

        assertTrue(state().getAssets().isEmpty());
        assertNotNull(state().getLastConfirmation());
        assertEquals("RK-001", state().getLastConfirmation().getInboundNo());
        assertEquals("管理员", state().getOperatorName());
        assertEquals("2026-07-24 10:20:30", state().getServerTime());
    }

    @Test
    public void unknownWriteResultPreservesAssetsAndWarnsAgainstBlindRetry() {
        readyAtDestination();
        addAsset(12L, "ZC-002");

        viewModel.confirm(null);
        inboundRepository.failConfirm(TestErrors.network());

        assertEquals(1, state().getAssets().size());
        assertTrue(state().getErrorMessage().contains("勿直接重复提交"));
        assertNull(state().getLastConfirmation());
    }

    @Test
    public void physicalKeyUsesSameBatchTogglePath() {
        readyAtDestination();

        viewModel.onScanKeyPressed();
        assertEquals(UhfScanMode.BATCH, scanner.getMode());
        viewModel.onScanKeyPressed();

        assertNull(scanner.getMode());
        assertEquals(0, inboundRepository.getBatchCheckCount());
    }

    private void initializeBase() {
        viewModel.initialize();
        PdaUserDto user = new PdaUserDto(7L, "tester", "测试用户",
                9L, "资产部", Collections.emptyList());
        commonRepository.completeBootstrap(new PdaBootstrapDto(
                "2026-07-24 09:00:00", user,
                Collections.emptyMap(), Collections.emptyMap()));
        commonRepository.completeWarehouses(Collections.singletonList(
                new PdaMasterDataDto(5L, "WH-01", "一号仓",
                        null, null)));
    }

    private void readyAtDestination() {
        initializeBase();
        viewModel.changeWarehouse(5L);
        commonRepository.completeLocations(locations());
        viewModel.changeLocation(6L);
    }

    private java.util.List<PdaMasterDataDto> locations() {
        return Arrays.asList(
                new PdaMasterDataDto(6L, "A-01", "A区01位",
                        5L, "一号仓"),
                new PdaMasterDataDto(7L, "A-02", "A区02位",
                        5L, "一号仓"));
    }

    private void addAsset(Long id, String code) {
        viewModel.addByAssetCode(code);
        inboundRepository.completeEligibility(eligibility(id, code, true, null));
    }

    private PdaInboundEligibilityDto eligibility(Long id, String code,
            boolean eligible, String reason) {
        return new PdaInboundEligibilityDto(id, code, "手持终端",
                "电子设备", "C6200", "测试品牌", "PENDING_INBOUND",
                "待入库", null, eligible, reason);
    }

    private PdaInboundBatchCheckDto batchCheck() {
        PdaInboundBatchCheckDto.Row eligible = new PdaInboundBatchCheckDto.Row(
                "E20001", 11L, "ZC-001", "手持终端", "电子设备",
                "PENDING_INBOUND", "待入库", "ELIGIBLE", null);
        PdaInboundBatchCheckDto.Row unknown = new PdaInboundBatchCheckDto.Row(
                "E20002", null, null, null, null,
                null, null, "UNKNOWN", "EPC 未绑定资产");
        return new PdaInboundBatchCheckDto(2, 1, 0, 1,
                Arrays.asList(eligible, unknown));
    }

    private PdaInboundBatchConfirmDto confirmation(Long assetId) {
        PdaInboundBatchConfirmDto.Row row =
                new PdaInboundBatchConfirmDto.Row(assetId,
                        "ZC-002", "手持终端", "SUCCESS");
        return new PdaInboundBatchConfirmDto(91L, "RK-001",
                "一号仓", "A区01位", "管理员",
                "2026-07-24 10:20:30", 1, 1,
                Collections.singletonList(row));
    }

    private InboundUiState state() {
        return viewModel.getUiState().getValue();
    }
}
