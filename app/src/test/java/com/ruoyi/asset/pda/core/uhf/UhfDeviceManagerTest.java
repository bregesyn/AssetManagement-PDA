package com.ruoyi.asset.pda.core.uhf;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UhfDeviceManagerTest {
    @Test
    public void constructionDoesNotOpenHardware() {
        FakeReaderFactory factory = new FakeReaderFactory(new FakeReader());

        UhfDeviceManager manager = new UhfDeviceManager(20, 1, factory);

        assertEquals(0, factory.openCount.get());
        assertEquals(UhfScanState.IDLE, manager.getState());
    }

    @Test
    public void unconfiguredParametersFailBeforeOpeningHardware() throws Exception {
        FakeReaderFactory factory = new FakeReaderFactory(new FakeReader());
        UhfDeviceManager manager = new UhfDeviceManager(-1, -1, factory);
        RecordingListener listener = new RecordingListener();

        manager.start(listener, UhfScanMode.BATCH, listener);
        await(() -> !listener.errors.isEmpty());

        assertEquals("UHF 参数尚未完成真机标定", listener.errors.get(0));
        assertEquals(0, factory.openCount.get());
        assertEquals(UhfScanState.ERROR, manager.getState());
        manager.close(listener);
        await(() -> manager.getState() == UhfScanState.IDLE);
    }

    @Test
    public void nullReaderReportsInitializationFailure() throws Exception {
        FakeReaderFactory factory = new FakeReaderFactory(null);
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, factory);
        RecordingListener listener = new RecordingListener();

        manager.start(listener, UhfScanMode.BATCH, listener);
        await(() -> !listener.errors.isEmpty());

        assertEquals("UHF 设备初始化失败", listener.errors.get(0));
        assertEquals(1, factory.openCount.get());
        manager.close(listener);
        await(() -> manager.getState() == UhfScanState.IDLE);
    }

    @Test
    public void parameterFailureClosesPartiallyInitializedReader() throws Exception {
        FakeReader reader = new FakeReader();
        reader.outputPowerAccepted = false;
        FakeReaderFactory factory = new FakeReaderFactory(reader);
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, factory);
        RecordingListener listener = new RecordingListener();

        manager.start(listener, UhfScanMode.BATCH, listener);
        await(() -> !listener.errors.isEmpty());

        assertEquals("UHF 功率设置失败", listener.errors.get(0));
        assertEquals(1, reader.closeCount.get());
        manager.close(listener);
        await(() -> manager.getState() == UhfScanState.IDLE);
        assertEquals(1, reader.closeCount.get());
    }

    @Test
    public void parameterFailureRetainsReaderWhenCleanupFailsAndAllowsRetry() throws Exception {
        FakeReader reader = new FakeReader();
        reader.outputPowerAccepted = false;
        reader.closeFailuresRemaining = 1;
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, new FakeReaderFactory(reader));
        Object owner = new Object();
        RecordingListener listener = new RecordingListener();

        manager.start(owner, UhfScanMode.BATCH, listener);
        await(() -> manager.getState() == UhfScanState.ERROR);

        assertEquals("UHF 功率设置失败", listener.errors.get(0));
        assertEquals(1, reader.closeCount.get());
        manager.close(owner);
        await(() -> manager.getState() == UhfScanState.IDLE);
        assertEquals("初始化清理失败后必须保留 Reader 供再次释放", 2, reader.closeCount.get());
    }

    @Test
    public void batchCountsDuplicatesAndKeepsFirstSeenOrder() throws Exception {
        FakeReader reader = new FakeReader();
        reader.rounds.add(Collections.emptyList());
        reader.rounds.add(Arrays.asList(
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xAA}, -60),
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xAA}, -55),
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xBB}, -50)));
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, new FakeReaderFactory(reader));
        RecordingListener listener = new RecordingListener();

        manager.start(listener, UhfScanMode.BATCH, listener);
        await(() -> listener.readings.size() >= 3);

        assertEquals("AA", listener.readings.get(0).getEpc());
        assertEquals(1, listener.readings.get(0).getReadCount());
        assertEquals("AA", listener.readings.get(1).getEpc());
        assertEquals(2, listener.readings.get(1).getReadCount());
        assertEquals("BB", listener.readings.get(2).getEpc());
        assertEquals(UhfScanState.SCANNING, manager.getState());
        manager.stop(listener);
        assertEquals(UhfScanState.IDLE, manager.getState());
        manager.close(listener);
        await(() -> manager.getState() == UhfScanState.IDLE);
    }

    @Test
    public void singleModeRejectsMultipleTagsWithoutReturningFirst() throws Exception {
        FakeReader reader = new FakeReader();
        reader.rounds.add(Arrays.asList(
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xAA}, -60),
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xBB}, -50)));
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, new FakeReaderFactory(reader));
        RecordingListener listener = new RecordingListener();

        manager.start(listener, UhfScanMode.SINGLE, listener);
        await(() -> !listener.errors.isEmpty());

        assertTrue(listener.readings.isEmpty());
        assertEquals("检测到多个 RFID 标签，请靠近目标标签后重试", listener.errors.get(0));
        assertEquals(UhfScanState.ERROR, manager.getState());
        manager.close(listener);
        await(() -> manager.getState() == UhfScanState.IDLE);
    }

    @Test
    public void repeatedStartAndCloseDoNotCreateSecondLoopOrDoubleClose() throws Exception {
        FakeReader reader = new FakeReader();
        FakeReaderFactory factory = new FakeReaderFactory(reader);
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, factory);
        RecordingListener listener = new RecordingListener();

        manager.start(listener, UhfScanMode.BATCH, listener);
        manager.start(listener, UhfScanMode.BATCH, listener);
        await(() -> manager.getState() == UhfScanState.SCANNING);
        assertEquals(1, factory.openCount.get());

        try {
            manager.start(new Object(), UhfScanMode.SINGLE, new RecordingListener());
            fail("其他扫描所有者应被拒绝");
        } catch (IllegalStateException expected) {
            // 预期行为
        }
        manager.close(listener);
        manager.close(listener);
        await(() -> manager.getState() == UhfScanState.IDLE);
        assertEquals(1, reader.closeCount.get());
    }

    @Test
    public void closeFailureKeepsReaderInErrorStateAndAllowsOwnerToRetry() throws Exception {
        FakeReader reader = new FakeReader();
        reader.closeFailuresRemaining = 1;
        FakeReaderFactory factory = new FakeReaderFactory(reader);
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, factory);
        Object owner = new Object();

        manager.start(owner, UhfScanMode.BATCH, new RecordingListener());
        await(() -> manager.getState() == UhfScanState.SCANNING);
        manager.close(owner);
        await(() -> manager.getState() == UhfScanState.ERROR);

        assertEquals(1, reader.closeCount.get());
        manager.close(owner);
        await(() -> manager.getState() == UhfScanState.IDLE);
        assertEquals(2, reader.closeCount.get());
        assertEquals(1, factory.openCount.get());
    }

    @Test
    public void newOwnerRecoversRetainedReaderBeforeStartingNewScan() throws Exception {
        FakeReader reader = new FakeReader();
        reader.closeFailuresRemaining = 1;
        FakeReaderFactory factory = new FakeReaderFactory(reader);
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, factory);
        Object oldOwner = new Object();
        Object newOwner = new Object();

        manager.start(oldOwner, UhfScanMode.BATCH, new RecordingListener());
        await(() -> manager.getState() == UhfScanState.SCANNING);
        manager.close(oldOwner);
        await(() -> manager.getState() == UhfScanState.ERROR);

        manager.start(newOwner, UhfScanMode.BATCH, new RecordingListener());
        await(() -> manager.getState() == UhfScanState.SCANNING);

        assertEquals("新页面扫描前必须先成功释放旧 Reader", 2, reader.closeCount.get());
        assertEquals("恢复后必须重新获取厂商 Reader", 2, factory.openCount.get());
        manager.close(newOwner);
        await(() -> manager.getState() == UhfScanState.IDLE);
        assertEquals(3, reader.closeCount.get());
    }

    @Test
    public void closeReturnsOnlyAfterInFlightCallbacksFinish() throws Exception {
        FakeReader reader = new FakeReader();
        reader.rounds.add(Arrays.asList(
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xAA}, -60),
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xBB}, -50)));
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, new FakeReaderFactory(reader));
        CountDownLatch firstCallbackEntered = new CountDownLatch(1);
        CountDownLatch allowCallbackToFinish = new CountDownLatch(1);
        CountDownLatch closeReturned = new CountDownLatch(1);
        AtomicInteger callbackCount = new AtomicInteger();
        UhfScanner.Listener listener = new UhfScanner.Listener() {
            @Override
            public void onStateChanged(UhfScanState state) {
            }

            @Override
            public void onTagRead(UhfTagReading reading) {
                if (callbackCount.incrementAndGet() == 1) {
                    firstCallbackEntered.countDown();
                    try {
                        allowCallbackToFinish.await(3L, TimeUnit.SECONDS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            @Override
            public void onError(String message) {
            }
        };
        manager.start(listener, UhfScanMode.BATCH, listener);
        assertTrue(firstCallbackEntered.await(3L, TimeUnit.SECONDS));

        Thread closeThread = new Thread(() -> {
            manager.close(listener);
            closeReturned.countDown();
        });
        closeThread.start();

        assertFalse("close 不应越过正在执行的旧页面回调", closeReturned.await(200L, TimeUnit.MILLISECONDS));
        allowCallbackToFinish.countDown();
        assertTrue(closeReturned.await(3L, TimeUnit.SECONDS));
        int callbacksWhenCloseReturned = callbackCount.get();
        await(() -> manager.getState() == UhfScanState.IDLE);
        assertEquals(callbacksWhenCloseReturned, callbackCount.get());
    }

    @Test
    public void staleOwnerCannotStopOrCloseNewScan() throws Exception {
        FakeReader reader = new FakeReader();
        UhfDeviceManager manager = new UhfDeviceManager(20, 1, new FakeReaderFactory(reader));
        Object oldOwner = new Object();
        Object newOwner = new Object();
        RecordingListener oldListener = new RecordingListener();
        RecordingListener newListener = new RecordingListener();

        manager.start(oldOwner, UhfScanMode.BATCH, oldListener);
        await(() -> manager.getState() == UhfScanState.SCANNING);
        manager.stop(oldOwner);
        manager.start(newOwner, UhfScanMode.BATCH, newListener);
        await(() -> manager.getState() == UhfScanState.SCANNING);

        manager.stop(oldOwner);
        manager.close(oldOwner);
        reader.rounds.add(Collections.singletonList(
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xCC}, -45)));

        await(() -> !newListener.readings.isEmpty());
        assertEquals(UhfScanState.SCANNING, manager.getState());
        assertEquals("CC", newListener.readings.get(0).getEpc());
        manager.stop(newOwner);
        manager.close(newOwner);
        await(() -> manager.getState() == UhfScanState.IDLE);
    }

    @Test
    public void allCallbacksUseConfiguredCallbackExecutor() throws Exception {
        FakeReader reader = new FakeReader();
        reader.rounds.add(Collections.singletonList(
                new UhfDeviceManager.RawTag(new byte[] {(byte) 0xDD}, -40)));
        reader.failWhenRoundsEmpty = true;
        ExecutorService callbackExecutor = Executors.newSingleThreadExecutor(runnable ->
                new Thread(runnable, "test-uhf-callback"));
        UhfDeviceManager manager = new UhfDeviceManager(
                20, 1, new FakeReaderFactory(reader), callbackExecutor);
        Object owner = new Object();
        List<String> callbackThreads = new CopyOnWriteArrayList<>();
        CountDownLatch errorDelivered = new CountDownLatch(1);
        UhfScanner.Listener listener = new UhfScanner.Listener() {
            @Override
            public void onStateChanged(UhfScanState state) {
                callbackThreads.add(Thread.currentThread().getName());
            }

            @Override
            public void onTagRead(UhfTagReading reading) {
                callbackThreads.add(Thread.currentThread().getName());
            }

            @Override
            public void onError(String message) {
                callbackThreads.add(Thread.currentThread().getName());
                errorDelivered.countDown();
            }
        };

        try {
            manager.start(owner, UhfScanMode.BATCH, listener);
            assertTrue(errorDelivered.await(3L, TimeUnit.SECONDS));
            assertTrue("应覆盖状态、标签和错误回调", callbackThreads.size() >= 5);
            for (String callbackThread : callbackThreads) {
                assertEquals("test-uhf-callback", callbackThread);
            }
            manager.close(owner);
            await(() -> manager.getState() == UhfScanState.IDLE);
        } finally {
            callbackExecutor.shutdownNow();
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + 3000L;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        assertTrue("异步 UHF 操作超时", condition.getAsBoolean());
    }

    private static final class FakeReaderFactory implements UhfDeviceManager.ReaderFactory {
        private final FakeReader reader;
        private final AtomicInteger openCount = new AtomicInteger();

        private FakeReaderFactory(FakeReader reader) {
            this.reader = reader;
        }

        @Override
        public UhfDeviceManager.Reader open() {
            openCount.incrementAndGet();
            return reader;
        }
    }

    private static final class FakeReader implements UhfDeviceManager.Reader {
        private final ConcurrentLinkedQueue<List<UhfDeviceManager.RawTag>> rounds =
                new ConcurrentLinkedQueue<>();
        private final AtomicInteger closeCount = new AtomicInteger();
        private boolean outputPowerAccepted = true;
        private int workAreaResult;
        private boolean failWhenRoundsEmpty;
        private int closeFailuresRemaining;

        @Override
        public boolean setOutputPower(int value) {
            return outputPowerAccepted;
        }

        @Override
        public int setWorkArea(int area) {
            return workAreaResult;
        }

        @Override
        public List<UhfDeviceManager.RawTag> inventoryRealTime() {
            List<UhfDeviceManager.RawTag> round = rounds.poll();
            if (round == null && failWhenRoundsEmpty) {
                throw new IllegalStateException("simulated reader failure");
            }
            return round == null ? Collections.emptyList() : round;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (closeFailuresRemaining > 0) {
                closeFailuresRemaining--;
                throw new IllegalStateException("simulated close failure");
            }
        }
    }

    private static final class RecordingListener implements UhfScanner.Listener {
        private final List<UhfTagReading> readings = new CopyOnWriteArrayList<>();
        private final List<String> errors = new CopyOnWriteArrayList<>();

        @Override
        public void onStateChanged(UhfScanState state) {
        }

        @Override
        public void onTagRead(UhfTagReading reading) {
            readings.add(reading);
        }

        @Override
        public void onError(String message) {
            errors.add(message);
        }
    }
}
