package com.ruoyi.asset.pda.feature.rfid;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.uhf.FakeUhfScanner;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchRowDto;
import com.ruoyi.asset.pda.testing.FakeRfidRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RfidTagBatchViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeRfidRepository repository;
    private FakeUhfScanner scanner;
    private RfidTagBatchViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeRfidRepository();
        scanner = new FakeUhfScanner();
        viewModel = new RfidTagBatchViewModel(repository, scanner,
                new ImmediateUiUpdateScheduler());
    }

    @Test
    public void batchScanNormalizesDeduplicatesAndPreservesFirstOrder() {
        viewModel.toggleScan();
        scanner.emit(" e20002 ", -45);
        scanner.emit("e20001", -50);
        scanner.emit("E20002", -38);

        assertEquals(2, state().getReadings().size());
        assertEquals("E20002", state().getReadings().get(0).getEpc());
        assertEquals("E20001", state().getReadings().get(1).getEpc());
        assertEquals(1, state().getDuplicateReadCount());
        assertEquals(2, state().getLastReading().getReadCount());
        assertEquals(-38, state().getLastReading().getRssi());
    }

    @Test
    public void requestFailureKeepsCollectedBatchForRetry() {
        collectTwoAndStop();
        viewModel.setRemark("现场批次");
        viewModel.submit();

        assertEquals(Arrays.asList("E20001", "E20002"), repository.getLastBatchEpcs());
        assertEquals("现场批次", repository.getLastBatchRemark());
        viewModel.submit();
        assertEquals(1, repository.getBatchCount());

        repository.failLastBatch(TestErrors.business("无权执行当前操作"));
        assertTrue(viewModel.hasUnsubmittedReadings());
        assertEquals(2, state().getReadings().size());
        assertFalse(state().isResultMode());
        assertEquals("无权执行当前操作", state().getErrorMessage());
    }

    @Test
    public void rowResultsRemainVisibleIncludingDuplicateAndFailure() {
        collectTwoAndStop();
        viewModel.submit();
        RfidTagBatchRowDto success = new RfidTagBatchRowDto(
                1, "E20001", true, false, 21L, "TAG021", "建档成功");
        RfidTagBatchRowDto duplicate = new RfidTagBatchRowDto(
                2, "E20002", false, true, null, null, "EPC已存在");
        repository.completeLastBatch(new RfidTagBatchResultDto(
                1, 1, 0, Arrays.asList(success, duplicate)));

        assertTrue(state().isResultMode());
        assertEquals(2, state().getResult().getRows().size());
        assertTrue(state().getResult().getRows().get(1).isDuplicate());

        viewModel.startNewBatch();
        assertFalse(state().isResultMode());
        assertTrue(state().getReadings().isEmpty());
    }

    @Test
    public void remarkOverLimitBlocksRequestAndKeepsReadings() {
        viewModel.toggleScan();
        scanner.emit("E20001", -40);
        viewModel.toggleScan();
        viewModel.setRemark(repeat('R', 501));

        viewModel.submit();

        assertEquals(0, repository.getBatchCount());
        assertEquals(R.string.batch_remark_too_long, state().getErrorTextResId());
        assertTrue(viewModel.hasUnsubmittedReadings());
    }

    @Test
    public void uniqueLimitStopsScanBeforeAcceptingExtraTag() {
        viewModel.toggleScan();
        for (int index = 0; index < RfidTagBatchViewModel.MAX_UNIQUE_EPCS; index++) {
            scanner.emit(String.format("E2%06d", index), -40);
        }
        scanner.emit("E2FFFFFF", -40);

        assertEquals(RfidTagBatchViewModel.MAX_UNIQUE_EPCS,
                state().getReadings().size());
        assertEquals(R.string.batch_limit_reached, state().getErrorTextResId());
        assertFalse(state().isScanning());
    }

    @Test
    public void missingRowsIsProtocolUiErrorAndPreservesBatch() {
        collectTwoAndStop();
        viewModel.submit();
        repository.completeLastBatch(new RfidTagBatchResultDto(0, 0, 0, null));

        assertEquals(R.string.batch_invalid_response, state().getErrorTextResId());
        assertTrue(viewModel.hasUnsubmittedReadings());
    }

    @Test
    public void incompleteRowsAreProtocolErrorAndPreserveBatch() {
        collectTwoAndStop();
        viewModel.submit();
        RfidTagBatchRowDto onlyFirstRow = new RfidTagBatchRowDto(
                1, "E20001", true, false, 21L, "TAG021", "建档成功");

        repository.completeLastBatch(new RfidTagBatchResultDto(
                1, 0, 0, Collections.singletonList(onlyFirstRow)));

        assertEquals(R.string.batch_invalid_response, state().getErrorTextResId());
        assertFalse(state().isResultMode());
        assertTrue(viewModel.hasUnsubmittedReadings());
    }

    @Test
    public void mismatchedRowIdentityIsProtocolError() {
        collectTwoAndStop();
        viewModel.submit();
        RfidTagBatchRowDto wrongFirstRow = new RfidTagBatchRowDto(
                2, "E20001", true, false, 21L, "TAG021", "建档成功");
        RfidTagBatchRowDto duplicateSecondRow = new RfidTagBatchRowDto(
                2, "E20002", false, true, null, null, "EPC已存在");

        repository.completeLastBatch(new RfidTagBatchResultDto(
                2, 0, 0, Arrays.asList(wrongFirstRow, duplicateSecondRow)));

        assertEquals(R.string.batch_invalid_response, state().getErrorTextResId());
        assertFalse(state().isResultMode());
        assertTrue(viewModel.hasUnsubmittedReadings());
    }

    @Test
    public void mismatchedSummaryCountsAreProtocolError() {
        collectTwoAndStop();
        viewModel.submit();
        RfidTagBatchRowDto success = new RfidTagBatchRowDto(
                1, "E20001", true, false, 21L, "TAG021", "建档成功");
        RfidTagBatchRowDto duplicate = new RfidTagBatchRowDto(
                2, "E20002", false, true, null, null, "EPC已存在");

        repository.completeLastBatch(new RfidTagBatchResultDto(
                2, 0, 0, Arrays.asList(success, duplicate)));

        assertEquals(R.string.batch_invalid_response, state().getErrorTextResId());
        assertFalse(state().isResultMode());
        assertTrue(viewModel.hasUnsubmittedReadings());
    }

    @Test
    public void denseReadsAreCoalescedBeforePublishingFullSnapshot() {
        ManualUiUpdateScheduler scheduler = new ManualUiUpdateScheduler();
        viewModel = new RfidTagBatchViewModel(repository, scanner, scheduler);
        viewModel.toggleScan();

        scanner.emit("E20001", -40);
        scanner.emit("E20002", -41);
        scanner.emit("E20003", -42);

        assertEquals(1, state().getReadings().size());
        assertTrue(scheduler.hasPendingTask());
        scheduler.runPendingTask();
        assertEquals(3, state().getReadings().size());
        assertEquals("E20003", state().getLastReading().getEpc());
    }

    private void collectTwoAndStop() {
        viewModel.toggleScan();
        scanner.emit("E20001", -40);
        scanner.emit("E20002", -42);
        viewModel.toggleScan();
    }

    private RfidTagBatchUiState state() { return viewModel.getUiState().getValue(); }

    private String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private static final class ImmediateUiUpdateScheduler
            implements RfidTagBatchViewModel.UiUpdateScheduler {
        @Override public long nowMillis() { return 1_000L; }
        @Override public void postDelayed(Runnable task, long delayMillis) { task.run(); }
        @Override public void removeCallbacks(Runnable task) { }
    }

    private static final class ManualUiUpdateScheduler
            implements RfidTagBatchViewModel.UiUpdateScheduler {
        private long now = 1_000L;
        private long pendingDelay;
        private Runnable pendingTask;

        @Override public long nowMillis() { return now; }

        @Override
        public void postDelayed(Runnable task, long delayMillis) {
            pendingTask = task;
            pendingDelay = delayMillis;
        }

        @Override
        public void removeCallbacks(Runnable task) {
            if (pendingTask == task) pendingTask = null;
        }

        boolean hasPendingTask() { return pendingTask != null; }

        void runPendingTask() {
            Runnable task = pendingTask;
            pendingTask = null;
            now += pendingDelay;
            task.run();
        }
    }
}
