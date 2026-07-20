package com.ruoyi.asset.pda.feature.rfid;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.testing.FakeAssetRepository;
import com.ruoyi.asset.pda.testing.FakeRfidRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RfidBindingViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeAssetRepository assetRepository;
    private FakeRfidRepository rfidRepository;
    private FakeUhfScanner scanner;

    @Before
    public void setUp() {
        assetRepository = new FakeAssetRepository();
        rfidRepository = new FakeRfidRepository();
        scanner = new FakeUhfScanner();
    }

    @Test
    public void bindTimeoutQueriesTagAndAcceptsOnlyConfirmedTargetRelation() {
        RfidBindViewModel viewModel = readyToBind();

        viewModel.bind();
        viewModel.bind();
        assertEquals(1, rfidRepository.getBindCount());
        assertEquals("ASSET-001", rfidRepository.getLastBindAssetCode());
        assertEquals("E20001", rfidRepository.getLastBindEpc());

        rfidRepository.failLastBind(TestErrors.network());
        assertTrue(bindState(viewModel).isVerifying());
        assertEquals(2, rfidRepository.getQueryCount());
        rfidRepository.completeLastQuery(boundTag("ASSET-001"));

        assertTrue(bindState(viewModel).isSuccess());
        assertEquals("ASSET-001", bindState(viewModel).getTag().getAssetCode());
    }

    @Test
    public void bindVerificationRejectsDifferentAsset() {
        RfidBindViewModel viewModel = readyToBind();
        viewModel.bind();
        rfidRepository.failLastBind(TestErrors.network());
        rfidRepository.completeLastQuery(boundTag("ASSET-OTHER"));

        assertFalse(bindState(viewModel).isSuccess());
        assertEquals(R.string.bind_result_not_confirmed,
                bindState(viewModel).getErrorTextResId());
        assertEquals("ASSET-OTHER", bindState(viewModel).getTag().getAssetCode());
        assertFalse(bindState(viewModel).canBind());
    }

    @Test
    public void bindUnknownResultRequiresFreshTagScan() {
        RfidBindViewModel viewModel = readyToBind();
        viewModel.bind();
        rfidRepository.failLastBind(TestErrors.network());
        rfidRepository.failLastQuery(TestErrors.network());

        assertEquals(R.string.bind_result_unknown,
                bindState(viewModel).getErrorTextResId());
        assertFalse(bindState(viewModel).canBind());
        assertNull(bindState(viewModel).getTag());
    }

    @Test
    public void boundAssetPreventsScanningAndBinding() {
        RfidBindViewModel viewModel = new RfidBindViewModel(
                assetRepository, rfidRepository, scanner);
        viewModel.queryAsset("ASSET-001");
        assetRepository.completeLast(boundAsset());

        viewModel.toggleScan();
        viewModel.onScanKeyDown();

        assertFalse(bindState(viewModel).isScanning());
        assertEquals(0, rfidRepository.getQueryCount());
        assertFalse(bindState(viewModel).canBind());
    }

    @Test
    public void unbindTimeoutQueriesTagAndAcceptsOnlyUnboundState() {
        RfidUnbindViewModel viewModel = readyToUnbind();

        viewModel.unbind();
        viewModel.unbind();
        assertEquals(1, rfidRepository.getUnbindCount());
        assertEquals(Long.valueOf(21L), rfidRepository.getLastUnbindTagId());

        rfidRepository.failLastUnbind(TestErrors.network());
        assertTrue(unbindState(viewModel).isVerifying());
        assertEquals(2, rfidRepository.getQueryCount());
        rfidRepository.completeLastQuery(unboundTag());

        assertTrue(unbindState(viewModel).isSuccess());
        assertFalse(unbindState(viewModel).getTag().isRfidBound());
    }

    @Test
    public void unboundTagClearlyBlocksUnbindRequest() {
        RfidUnbindViewModel viewModel = new RfidUnbindViewModel(rfidRepository, scanner);
        scanOne(viewModel, "E20001");
        rfidRepository.completeLastQuery(unboundTag());

        assertFalse(unbindState(viewModel).canUnbind());
        viewModel.unbind();
        assertEquals(0, rfidRepository.getUnbindCount());
    }

    @Test
    public void unbindUnknownResultRequiresFreshTagScan() {
        RfidUnbindViewModel viewModel = readyToUnbind();
        viewModel.unbind();
        rfidRepository.failLastUnbind(TestErrors.network());
        rfidRepository.failLastQuery(TestErrors.network());

        assertEquals(R.string.unbind_result_unknown,
                unbindState(viewModel).getErrorTextResId());
        assertFalse(unbindState(viewModel).canUnbind());
        assertNull(unbindState(viewModel).getTag());
    }

    @Test
    public void directWriteResponseMustContainExpectedFinalState() {
        RfidBindViewModel bindViewModel = readyToBind();
        bindViewModel.bind();
        rfidRepository.completeLastBind(unboundTag());
        assertEquals(R.string.rfid_invalid_tag_response,
                bindState(bindViewModel).getErrorTextResId());

        rfidRepository = new FakeRfidRepository();
        scanner = new FakeUhfScanner();
        RfidUnbindViewModel unbindViewModel = readyToUnbind();
        unbindViewModel.unbind();
        rfidRepository.completeLastUnbind(boundTag("ASSET-001"));
        assertEquals(R.string.rfid_invalid_tag_response,
                unbindState(unbindViewModel).getErrorTextResId());
    }

    @Test
    public void startingNewBindScanImmediatelyDiscardsPreviousTag() {
        RfidBindViewModel viewModel = readyToBind();

        viewModel.toggleScan();

        assertNull(bindState(viewModel).getTag());
        assertFalse(bindState(viewModel).canBind());
        scanner.emitEmptyRound();
        viewModel.toggleScan();
        assertNull(bindState(viewModel).getTag());
        assertFalse(bindState(viewModel).canBind());
    }

    @Test
    public void startingNewUnbindScanImmediatelyDiscardsPreviousTag() {
        RfidUnbindViewModel viewModel = readyToUnbind();

        viewModel.toggleScan();

        assertNull(unbindState(viewModel).getTag());
        assertFalse(unbindState(viewModel).canUnbind());
        scanner.emitEmptyRound();
        viewModel.toggleScan();
        assertNull(unbindState(viewModel).getTag());
        assertFalse(unbindState(viewModel).canUnbind());
    }

    @Test
    public void changingAssetInputInvalidatesQueriedAssetAndTag() {
        RfidBindViewModel viewModel = readyToBind();

        viewModel.onAssetCodeChanged("ASSET-002");

        assertNull(bindState(viewModel).getAsset());
        assertNull(bindState(viewModel).getTag());
        assertFalse(bindState(viewModel).canBind());
    }

    private RfidBindViewModel readyToBind() {
        RfidBindViewModel viewModel = new RfidBindViewModel(
                assetRepository, rfidRepository, scanner);
        viewModel.queryAsset("ASSET-001");
        assetRepository.completeLast(asset());
        viewModel.toggleScan();
        scanner.emit("E20001", -40);
        viewModel.toggleScan();
        rfidRepository.completeLastQuery(unboundTag());
        assertTrue(bindState(viewModel).canBind());
        return viewModel;
    }

    private RfidUnbindViewModel readyToUnbind() {
        RfidUnbindViewModel viewModel = new RfidUnbindViewModel(rfidRepository, scanner);
        scanOne(viewModel, "E20001");
        rfidRepository.completeLastQuery(boundTag("ASSET-001"));
        assertTrue(unbindState(viewModel).canUnbind());
        return viewModel;
    }

    private void scanOne(RfidUnbindViewModel viewModel, String epc) {
        viewModel.toggleScan();
        scanner.emit(epc, -40);
        viewModel.toggleScan();
    }

    private RfidBindUiState bindState(RfidBindViewModel viewModel) {
        return viewModel.getUiState().getValue();
    }

    private RfidUnbindUiState unbindState(RfidUnbindViewModel viewModel) {
        return viewModel.getUiState().getValue();
    }

    private PdaAssetIdentifyDto asset() {
        return new PdaAssetIdentifyDto(11L, "ASSET-001", "测试资产",
                3L, "电子设备", "M1", "品牌A", "IN_STOCK", "在库",
                5L, "一号仓", 6L, "A区", null, null, null,
                null, null, null, null, false);
    }

    private PdaAssetIdentifyDto boundAsset() {
        return new PdaAssetIdentifyDto(11L, "ASSET-001", "测试资产",
                3L, "电子设备", "M1", "品牌A", "IN_STOCK", "在库",
                5L, "一号仓", 6L, "A区", 21L, "TAG021", "E20001",
                "NORMAL", "正常", "BOUND", "已绑定", true);
    }

    private PdaRfidTagDto unboundTag() {
        return new PdaRfidTagDto(21L, "TAG021", "E20001", "NORMAL", "正常",
                "UNBOUND", "未绑定", null, null, null, false);
    }

    private PdaRfidTagDto boundTag(String assetCode) {
        return new PdaRfidTagDto(21L, "TAG021", "E20001", "NORMAL", "正常",
                "BOUND", "已绑定", 11L, assetCode, "测试资产", true);
    }
}
