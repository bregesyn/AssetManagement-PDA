package com.ruoyi.asset.pda.feature.identify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.testing.FakeAssetRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AssetIdentifyViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeAssetRepository repository;
    private FakeUhfScanner scanner;
    private AssetIdentifyViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeAssetRepository();
        scanner = new FakeUhfScanner();
        viewModel = new AssetIdentifyViewModel(repository, scanner);
    }

    @Test
    public void assetCodeQueryTrimsInputAndRendersServerFact() {
        viewModel.selectIdentifyType(AssetRepository.IDENTIFY_TYPE_ASSET_CODE);
        viewModel.identifyAssetCode(" ASSET-001 ");

        assertEquals(AssetRepository.IDENTIFY_TYPE_ASSET_CODE, repository.getLastType());
        assertEquals("ASSET-001", repository.getLastValue());
        repository.completeLast(asset());

        assertEquals("测试资产", state().getAsset().getAssetName());
        assertFalse(state().isLoading());
    }

    @Test
    public void singleScanQueriesOnlyAfterScanWindowStops() {
        viewModel.toggleScan();
        scanner.emit("e20001", -42);

        assertEquals(0, repository.getRequestCount());
        viewModel.toggleScan();

        assertEquals(1, repository.getRequestCount());
        assertEquals(AssetRepository.IDENTIFY_TYPE_EPC, repository.getLastType());
        assertEquals("E20001", repository.getLastValue());
    }

    @Test
    public void multipleTagsAreRejectedWithoutNetworkRequest() {
        viewModel.toggleScan();
        scanner.emit("E20001", -42);
        scanner.emit("E20002", -45);

        assertEquals(0, repository.getRequestCount());
        assertTrue(state().getErrorMessage().contains("多个 RFID 标签"));
        assertNull(state().getAsset());
    }

    @Test
    public void missingRequiredAssetFieldsIsProtocolUiError() {
        viewModel.selectIdentifyType(AssetRepository.IDENTIFY_TYPE_ASSET_CODE);
        viewModel.identifyAssetCode("ASSET-001");
        repository.completeLast(new PdaAssetIdentifyDto());

        assertEquals(R.string.identify_invalid_asset_response,
                state().getErrorTextResId());
    }

    @Test
    public void blankAssetCodeDoesNotStartRequest() {
        viewModel.selectIdentifyType(AssetRepository.IDENTIFY_TYPE_ASSET_CODE);
        viewModel.identifyAssetCode("  ");

        assertEquals(0, repository.getRequestCount());
        assertEquals(R.string.identify_asset_code_required,
                state().getErrorTextResId());
    }

    @Test
    public void switchingTypeCancelsSingleWindowWithoutQueryingCollectedEpc() {
        viewModel.toggleScan();
        scanner.emit("E20001", -42);

        viewModel.selectIdentifyType(AssetRepository.IDENTIFY_TYPE_ASSET_CODE);

        assertEquals(0, repository.getRequestCount());
        assertEquals(AssetRepository.IDENTIFY_TYPE_ASSET_CODE, state().getIdentifyType());
        assertNull(state().getAsset());
        assertNull(state().getLastEpc());
    }

    @Test
    public void startingNewEpcScanClearsPreviousResult() {
        viewModel.toggleScan();
        scanner.emit("E20001", -42);
        viewModel.toggleScan();
        repository.completeLast(asset());
        assertEquals("E20001", state().getLastEpc());
        assertEquals("测试资产", state().getAsset().getAssetName());

        viewModel.toggleScan();

        assertNull(state().getLastEpc());
        assertNull(state().getAsset());
    }

    private AssetIdentifyUiState state() { return viewModel.getUiState().getValue(); }

    private PdaAssetIdentifyDto asset() {
        return new PdaAssetIdentifyDto(11L, "ASSET-001", "测试资产",
                3L, "电子设备", "M1", "品牌A", "IN_STOCK", "在库",
                5L, "一号仓", 6L, "A区", null, null, null,
                null, null, null, null, false);
    }
}
