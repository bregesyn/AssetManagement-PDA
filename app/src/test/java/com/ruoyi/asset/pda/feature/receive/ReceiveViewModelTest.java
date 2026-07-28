package com.ruoyi.asset.pda.feature.receive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.testing.FakeCommonRepository;
import com.ruoyi.asset.pda.testing.FakeReceiveRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReceiveViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeReceiveRepository receiveRepository;
    private FakeCommonRepository commonRepository;
    private FakeUhfScanner scanner;
    private ReceiveViewModel viewModel;

    @Before
    public void setUp() {
        receiveRepository = new FakeReceiveRepository();
        commonRepository = new FakeCommonRepository();
        scanner = new FakeUhfScanner();
        viewModel = new ReceiveViewModel(receiveRepository,
                commonRepository, scanner, true);
    }

    @Test
    public void scanRequiresRecipientBeforeStartingHardware() {
        initialize();

        viewModel.toggleScan();

        assertNull(scanner.getMode());
        assertTrue(state().getErrorMessage().contains("领用人"));
    }

    @Test
    public void physicalKeyBatchScansDeduplicatesAndSeparatesIssues() {
        readyWithRecipient();

        viewModel.onScanKeyPressed();
        assertEquals(UhfScanMode.BATCH, scanner.getMode());
        scanner.emitRound("E20001", "E20002", "e20001");
        viewModel.onScanKeyPressed();

        assertEquals(2, receiveRepository.getLastIdentifiers().size());
        assertEquals("E20001", receiveRepository.getLastIdentifiers()
                .get(0).getIdentifyValue());
        assertEquals("E20002", receiveRepository.getLastIdentifiers()
                .get(1).getIdentifyValue());
        receiveRepository.completeBatchCheck(batchCheck());

        assertEquals(1, state().getAssets().size());
        assertEquals("ZC-001", state().getAssets().get(0).getAssetCode());
        assertEquals(1, state().getIssues().size());
        assertEquals(0, state().getRawEpcCount());
        assertEquals(1, state().getDuplicateReadCount());
    }

    @Test
    public void assetCodeUsesSamePrecheckProtocol() {
        readyWithRecipient();

        viewModel.addByAssetCode(" ZC-002 ");

        PdaAssetIdentifyRequest identifier = receiveRepository.getLastIdentifiers().get(0);
        assertEquals("ASSET_CODE", identifier.getIdentifyType());
        assertEquals("ZC-002", identifier.getIdentifyValue());
        receiveRepository.completeBatchCheck(eligibleCheck("ASSET_CODE", "ZC-002",
                12L, "ZC-002"));

        assertEquals(1, state().getAssets().size());
        assertEquals(ReceiveAssetItem.Source.ASSET_CODE,
                state().getAssets().get(0).getSource());
        assertEquals(1, state().getAssetCodeClearVersion());
    }

    @Test
    public void changingRecipientClearsUnsubmittedAssetsAndIssues() {
        readyWithRecipient();
        addAsset(12L, "ZC-002");
        int resetVersion = state().getBatchResetVersion();

        viewModel.selectRecipient(new PdaMasterDataDto(8L, "lisi", "李四",
                10L, "生产部"));

        assertTrue(state().getAssets().isEmpty());
        assertTrue(state().getIssues().isEmpty());
        assertEquals(8L, state().getSelectedRecipient().getId().longValue());
        assertTrue(state().getBatchResetVersion() > resetVersion);
    }

    @Test
    public void confirmPermissionIsCheckedBeforeNetwork() {
        ReceiveViewModel noPermissionViewModel = new ReceiveViewModel(receiveRepository,
                commonRepository, scanner, false);
        viewModel = noPermissionViewModel;
        readyWithRecipient();
        addAsset(12L, "ZC-002");

        viewModel.confirm(null);

        assertEquals(0, receiveRepository.getConfirmCount());
        assertTrue(state().getErrorMessage().contains("确认权限"));
    }

    @Test
    public void confirmationUsesServerTimeAndClearsWorkingBatch() {
        readyWithRecipient();
        addAsset(12L, "ZC-002");

        viewModel.confirm(" 现场交接 ");

        assertEquals(7L, receiveRepository.getLastReceiveUserId().longValue());
        assertEquals(9L, receiveRepository.getLastReceiveDeptId().longValue());
        assertEquals("现场交接", receiveRepository.getLastRemark());
        assertEquals(1, receiveRepository.getLastIdentifiers().size());
        receiveRepository.completeConfirm(confirmation(12L));

        assertTrue(state().getAssets().isEmpty());
        assertNotNull(state().getLastConfirmation());
        assertEquals("LY-001", state().getLastConfirmation().getReceiveNo());
        assertEquals("管理员", state().getOperatorName());
        assertEquals("2026-07-24 10:20:30", state().getServerTime());
    }

    @Test
    public void unknownConfirmationResultPreservesBatchAndWarnsAgainstRetry() {
        readyWithRecipient();
        addAsset(12L, "ZC-002");

        viewModel.confirm(null);
        receiveRepository.failConfirm(TestErrors.network());

        assertEquals(1, state().getAssets().size());
        assertTrue(state().getErrorMessage().contains("勿直接重复提交"));
        assertNull(state().getLastConfirmation());
    }

    @Test
    public void invalidBatchStatisticsAreRejectedWithoutLosingReadings() {
        readyWithRecipient();
        viewModel.toggleScan();
        scanner.emitRound("E20001");
        viewModel.toggleScan();
        PdaReceiveBatchCheckDto bad = new PdaReceiveBatchCheckDto(1, 1,
                0, 0, 1, Collections.singletonList(
                        new PdaReceiveBatchCheckDto.Row("EPC", "E20001", 11L,
                                "ZC-001", "手持终端", null, null, null, null,
                                "在库", "ELIGIBLE", null)));

        receiveRepository.completeBatchCheck(bad);

        assertEquals(1, state().getRawEpcCount());
        assertTrue(state().getErrorMessage().contains("预检响应"));
        assertFalse(state().isBusy());
    }

    private void initialize() {
        viewModel.initialize();
        PdaUserDto user = new PdaUserDto(7L, "tester", "测试用户",
                9L, "资产部", Collections.emptyList());
        commonRepository.completeBootstrap(new PdaBootstrapDto(
                "2026-07-24 09:00:00", user,
                Collections.emptyMap(), Collections.emptyMap()));
    }

    private void readyWithRecipient() {
        initialize();
        viewModel.selectRecipient(new PdaMasterDataDto(7L, "zhangsan", "张三",
                9L, "资产部"));
    }

    private void addAsset(Long id, String code) {
        viewModel.addByAssetCode(code);
        receiveRepository.completeBatchCheck(eligibleCheck("ASSET_CODE", code, id, code));
    }

    private PdaReceiveBatchCheckDto batchCheck() {
        PdaReceiveBatchCheckDto.Row eligible = new PdaReceiveBatchCheckDto.Row(
                "EPC", "E20001", 11L, "ZC-001", "手持终端", "电子设备",
                "C6200", "测试品牌", "IN_STOCK", "在库", "ELIGIBLE", null);
        PdaReceiveBatchCheckDto.Row unknown = new PdaReceiveBatchCheckDto.Row(
                "EPC", "E20002", null, null, null, null, null, null,
                null, null, "UNKNOWN", "EPC 未绑定资产");
        return new PdaReceiveBatchCheckDto(2, 1, 0, 1, 0,
                Arrays.asList(eligible, unknown));
    }

    private PdaReceiveBatchCheckDto eligibleCheck(String type, String value,
            Long id, String code) {
        return new PdaReceiveBatchCheckDto(1, 1, 0, 0, 0,
                Collections.singletonList(new PdaReceiveBatchCheckDto.Row(type, value,
                        id, code, "手持终端", "电子设备", "C6200", "测试品牌",
                        "IN_STOCK", "在库", "ELIGIBLE", null)));
    }

    private PdaReceiveBatchConfirmDto confirmation(Long assetId) {
        PdaReceiveBatchConfirmDto.Row row = new PdaReceiveBatchConfirmDto.Row(assetId,
                "ZC-002", "手持终端", "SUCCESS");
        return new PdaReceiveBatchConfirmDto(91L, "LY-001", 7L, "张三", 9L,
                "资产部", "管理员", "2026-07-24 10:20:30", "COMPLETED",
                1, 1, Collections.singletonList(row));
    }

    private ReceiveUiState state() {
        return viewModel.getUiState().getValue();
    }
}
