package com.ruoyi.asset.pda.feature.borrow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.testing.FakeBorrowRepository;
import com.ruoyi.asset.pda.testing.FakeCommonRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 借还状态机测试：扫描批量化、跨单归还和未知提交结果均不绕过后端。 */
public class BorrowReturnViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private final Gson gson = new Gson();
    private FakeBorrowRepository borrowRepository;
    private FakeCommonRepository commonRepository;
    private FakeUhfScanner scanner;
    private BorrowReturnViewModel viewModel;

    @Before
    public void setUp() {
        borrowRepository = new FakeBorrowRepository();
        commonRepository = new FakeCommonRepository();
        scanner = new FakeUhfScanner();
        viewModel = new BorrowReturnViewModel(borrowRepository, commonRepository, scanner,
                true, true, true, true);
    }

    @Test
    public void issueRequiresBorrowerAndDateBeforeStartingHardware() {
        initialize();

        viewModel.toggleScan();

        assertNull(scanner.getMode());
        assertTrue(state().getErrorMessage().contains("借用人"));
    }

    @Test
    public void issueRejectsDateBeforeServerDateBeforeStartingHardware() {
        initialize();
        viewModel.selectBorrower(new PdaMasterDataDto(7L, "zhangsan", "张三",
                9L, "资产部"));
        viewModel.setExpectedReturnDate("2026-08-04");

        viewModel.toggleScan();

        assertNull(scanner.getMode());
        assertTrue(state().getErrorMessage().contains("不能早于服务器日期"));
    }

    @Test
    public void batchScanUsesOnePrecheckAndKeepsDuplicateFeedback() {
        readyIssue();

        viewModel.onScanKeyPressed();
        assertEquals(UhfScanMode.BATCH, scanner.getMode());
        scanner.emitRound("e20001", "E20001");
        viewModel.onScanKeyPressed();

        assertEquals(1, borrowRepository.getIssueCheckCount());
        assertEquals(1, borrowRepository.getLastIdentifiers().size());
        assertEquals("E20001", borrowRepository.getLastIdentifiers().get(0).getIdentifyValue());
        borrowRepository.completeIssueCheck(singleIssueCheck("EPC", "E20001", 11L));

        assertEquals(1, state().getAssets().size());
        assertEquals(1, state().getDuplicateReadCount());
        assertEquals(0, state().getRawEpcCount());
    }

    @Test
    public void switchingInternalClearsExternalFieldsBeforePrecheck() {
        readyIssue();
        viewModel.setBorrowerType("EXTERNAL");
        viewModel.setExternalOrgName("外部公司");
        viewModel.setExternalContactName("王经理");
        viewModel.setExternalContactPhone("13800000000");
        viewModel.setBorrowerType("INTERNAL");
        viewModel.addByAssetCode("ZC-001");

        assertEquals("INTERNAL", borrowRepository.getLastBorrowerType());
        assertNull(borrowRepository.getLastBorrowOrgName());
        assertNull(borrowRepository.getLastBorrowContactPhone());
        assertNull(borrowRepository.getLastBorrowExternalContactName());
        assertNull(borrowRepository.getLastBorrowExternalContactPhone());
    }

    @Test
    public void selectingInternalContactAutofillsInternalPhone() {
        initialize();
        viewModel.setBorrowerType("EXTERNAL");

        viewModel.selectBorrower(new PdaMasterDataDto(7L, "zhangsan", "张三",
                9L, "资产部", "13800000000"));

        assertEquals("13800000000", state().getInternalContactPhone());
        assertNull(state().getExternalContactPhone());
    }

    @Test
    public void externalIssueRequiresManualExternalContactFields() {
        initialize();
        viewModel.setBorrowerType("EXTERNAL");
        viewModel.selectBorrower(new PdaMasterDataDto(7L, "zhangsan", "张三",
                9L, "资产部", "13800000000"));
        viewModel.setExternalOrgName("外部公司");
        viewModel.setExpectedReturnDate("2026-08-06");

        viewModel.addByAssetCode("ZC-001");

        assertEquals(0, borrowRepository.getIssueCheckCount());
        assertTrue(state().getErrorMessage().contains("外部联系人"));

        viewModel.setExternalContactName("王经理");
        viewModel.setExternalContactPhone("13900000000");
        viewModel.addByAssetCode("ZC-001");

        assertEquals(1, borrowRepository.getIssueCheckCount());
        assertEquals("王经理", borrowRepository.getLastBorrowExternalContactName());
        assertEquals("13900000000", borrowRepository.getLastBorrowExternalContactPhone());
    }

    @Test
    public void switchingModeClearsPendingBatchAndBorrowContext() {
        readyIssue();
        viewModel.addByAssetCode("ZC-001");
        borrowRepository.completeIssueCheck(singleIssueCheck("ASSET_CODE", "ZC-001", 11L));

        assertTrue(state().hasPendingWork());
        viewModel.setMode(BorrowReturnUiState.Mode.RETURN);

        assertTrue(state().getAssets().isEmpty());
        assertNull(state().getSelectedBorrower());
        assertNull(state().getExpectedReturnDate());
        assertNull(scanner.getMode());
    }

    @Test
    public void returnCanResolveTwoAssetsFromDifferentBorrowOrders() {
        initialize();
        viewModel.setMode(BorrowReturnUiState.Mode.RETURN);

        viewModel.onScanKeyPressed();
        scanner.emitRound("E20001", "E20002");
        viewModel.onScanKeyPressed();
        borrowRepository.completeReturnCheck(returnCheck());

        assertEquals(2, state().getAssets().size());
        assertEquals("JY-001", state().getAssets().get(0).getBorrowNo());
        assertEquals("JY-002", state().getAssets().get(1).getBorrowNo());
        viewModel.submit(null);
        borrowRepository.completeReturnSubmit(returnSubmission());

        assertTrue(state().getAssets().isEmpty());
        assertNotNull(state().getLastReturnSubmission());
        assertEquals(2, state().getLastReturnSubmission().getSuccessCount());
    }

    @Test
    public void issueSubmitClearsBatchButPreservesApprovalReceipt() {
        readyIssue();
        viewModel.addByAssetCode("ZC-001");
        borrowRepository.completeIssueCheck(singleIssueCheck("ASSET_CODE", "ZC-001", 11L));

        viewModel.submit("现场临时借用");
        borrowRepository.completeIssueSubmit(issueSubmission());

        assertTrue(state().getAssets().isEmpty());
        assertNotNull(state().getLastIssueSubmission());
        assertEquals("JY-001", state().getLastIssueSubmission().getBorrowNo());
        assertEquals("现场临时借用", borrowRepository.getLastRemark());
    }

    @Test
    public void unknownSubmitKeepsBatchAndWarnsAgainstRetry() {
        readyIssue();
        viewModel.addByAssetCode("ZC-001");
        borrowRepository.completeIssueCheck(singleIssueCheck("ASSET_CODE", "ZC-001", 11L));

        viewModel.submit(null);
        borrowRepository.failIssueSubmit(TestErrors.network());

        assertEquals(1, state().getAssets().size());
        assertTrue(state().getErrorMessage().contains("勿直接重复提交"));
        assertNull(state().getLastIssueSubmission());
    }

    private void initialize() {
        viewModel.initialize();
        PdaUserDto user = new PdaUserDto(99L, "operator", "现场操作员",
                9L, "资产部", Collections.emptyList());
        Map<String, List<PdaDictItemDto>> dicts = new HashMap<>();
        dicts.put(BorrowReturnViewModel.DICT_BORROWER_TYPE, Arrays.asList(
                new PdaDictItemDto("ams_borrower_type", "内部借用", "INTERNAL",
                        null, null, "Y"),
                new PdaDictItemDto("ams_borrower_type", "外部借用", "EXTERNAL",
                        null, null, "N")));
        commonRepository.completeBootstrap(new PdaBootstrapDto(
                "2026-08-05 09:00:00", user, dicts, Collections.emptyMap()));
    }

    private void readyIssue() {
        initialize();
        viewModel.selectBorrower(new PdaMasterDataDto(7L, "zhangsan", "张三",
                9L, "资产部"));
        viewModel.setExpectedReturnDate("2026-08-06");
    }

    private com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto singleIssueCheck(
            String type, String value, long assetId) {
        return gson.fromJson("{\"totalCount\":1,\"eligibleCount\":1,"
                + "\"ineligibleCount\":0,\"unknownCount\":0,\"duplicateCount\":0,"
                + "\"rows\":["
                + "{\"identifyType\":\"" + type + "\",\"identifyValue\":\"" + value + "\","
                + "\"assetId\":" + assetId + ",\"assetCode\":\"ZC-001\","
                + "\"assetName\":\"手持终端\",\"assetStatusLabel\":\"在库\","
                + "\"status\":\"ELIGIBLE\"}]}",
                com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto.class);
    }

    private com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto returnCheck() {
        return gson.fromJson("{\"totalCount\":2,\"eligibleCount\":2,"
                + "\"ineligibleCount\":0,\"unknownCount\":0,\"duplicateCount\":0,"
                + "\"rows\":["
                + "{\"identifyType\":\"EPC\",\"identifyValue\":\"E20001\",\"assetId\":11,"
                + "\"assetCode\":\"ZC-001\",\"assetName\":\"手持终端\",\"orderId\":91,"
                + "\"borrowNo\":\"JY-001\",\"itemId\":101,\"returnStatus\":\"BORROWING\","
                + "\"beforeWarehouseId\":1,\"beforeLocationId\":11,"
                + "\"beforeWarehouseName\":\"一号仓\",\"beforeLocationName\":\"A-01\","
                + "\"status\":\"ELIGIBLE\"},"
                + "{\"identifyType\":\"EPC\",\"identifyValue\":\"E20002\",\"assetId\":12,"
                + "\"assetCode\":\"ZC-002\",\"assetName\":\"扫码枪\",\"orderId\":92,"
                + "\"borrowNo\":\"JY-002\",\"itemId\":102,\"returnStatus\":\"BORROWING\","
                + "\"beforeWarehouseId\":1,\"beforeLocationId\":12,"
                + "\"beforeWarehouseName\":\"一号仓\",\"beforeLocationName\":\"A-02\","
                + "\"status\":\"ELIGIBLE\"}]}",
                com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto.class);
    }

    private com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto issueSubmission() {
        return gson.fromJson("{\"orderId\":91,\"borrowNo\":\"JY-001\","
                + "\"borrowerType\":\"INTERNAL\",\"borrowUserId\":7,"
                + "\"borrowUserName\":\"张三\",\"borrowDeptId\":9,"
                + "\"borrowDeptName\":\"资产部\",\"orderStatus\":\"PENDING_CONFIRM\","
                + "\"approvalTask\":{\"taskId\":7001,\"taskStatus\":\"PENDING\"},"
                + "\"totalCount\":1,\"successCount\":1,"
                + "\"rows\":["
                + "{\"assetId\":11,\"status\":\"SUCCESS\"}]}",
                com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto.class);
    }

    private com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto returnSubmission() {
        return gson.fromJson("{\"totalCount\":2,\"successCount\":2,"
                + "\"rows\":["
                + "{\"orderId\":91,\"borrowNo\":\"JY-001\",\"itemId\":101,"
                + "\"assetId\":11,\"targetWarehouseId\":1,\"targetLocationId\":11,"
                + "\"returnStatus\":\"PENDING_RETURN_CONFIRM\",\"approvalTask\":{"
                + "\"taskId\":8001,\"taskStatus\":\"PENDING\"}},"
                + "{\"orderId\":92,\"borrowNo\":\"JY-002\",\"itemId\":102,"
                + "\"assetId\":12,\"targetWarehouseId\":1,\"targetLocationId\":12,"
                + "\"returnStatus\":\"PENDING_RETURN_CONFIRM\",\"approvalTask\":{"
                + "\"taskId\":8002,\"taskStatus\":\"PENDING\"}}]}",
                com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto.class);
    }

    private BorrowReturnUiState state() {
        return viewModel.getUiState().getValue();
    }
}
