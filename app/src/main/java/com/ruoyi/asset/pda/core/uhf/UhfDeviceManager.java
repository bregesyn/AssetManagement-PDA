package com.ruoyi.asset.pda.core.uhf;

import android.os.Handler;
import android.os.Looper;

import com.android.hdhe.uhf.reader.UhfReader;
import com.android.hdhe.uhf.readerInterface.TagModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 厂商 UhfReader 的唯一生产包装层。
 */
public final class UhfDeviceManager implements UhfScanner {
    private static final int UNCONFIGURED = -1;
    private static final int LEGACY_DEVELOPMENT_WORK_AREA = 0;
    private static final long VENDOR_READER_READY_DELAY_MS = 1000L;
    private static final long POWER_CONFIGURATION_RETRY_DELAY_MS = 200L;
    private static final long EMPTY_ROUND_BACKOFF_MS = 30L;

    private final Object lock = new Object();
    private final Object callbackLock = new Object();
    private final int outputPower;
    private final int workArea;
    private final ReaderFactory readerFactory;
    private final ScheduledExecutorService executor;
    private final Executor callbackExecutor;
    private final Map<String, UhfTagReading> readings = new LinkedHashMap<>();

    private volatile UhfScanState state = UhfScanState.IDLE;
    private Reader reader;
    private ScheduledFuture<?> scanFuture;
    private Listener activeListener;
    private UhfScanMode activeMode;
    private Object activeOwner;
    private long generation;
    private boolean closing;

    public UhfDeviceManager(int outputPower, int workArea) {
        this(outputPower, workArea, new VendorReaderFactory(), createMainThreadExecutor());
    }

    UhfDeviceManager(int outputPower, int workArea, ReaderFactory readerFactory) {
        this(outputPower, workArea, readerFactory, Runnable::run);
    }

    UhfDeviceManager(int outputPower, int workArea, ReaderFactory readerFactory,
            Executor callbackExecutor) {
        if (readerFactory == null || callbackExecutor == null) {
            throw new IllegalArgumentException("UHF 依赖组件不能为空");
        }
        this.outputPower = outputPower;
        this.workArea = workArea;
        this.readerFactory = readerFactory;
        this.callbackExecutor = callbackExecutor;
        executor = Executors.newSingleThreadScheduledExecutor(new UhfThreadFactory());
    }

    @Override
    public void start(Object owner, UhfScanMode mode, Listener listener) {
        if (owner == null || mode == null || listener == null) {
            throw new IllegalArgumentException("扫描所有者、模式和监听器不能为空");
        }
        long currentGeneration;
        boolean recoverReader;
        synchronized (lock) {
            if (closing) {
                throw new IllegalStateException("UHF 设备正在释放，请稍后重试");
            }
            if (state == UhfScanState.PROCESSING || state == UhfScanState.SCANNING) {
                if (activeOwner == owner && activeMode == mode && activeListener == listener) {
                    return;
                }
                throw new IllegalStateException("UHF 设备正在被其他扫描任务使用");
            }
            recoverReader = state == UhfScanState.ERROR && reader != null;
            generation++;
            currentGeneration = generation;
            activeOwner = owner;
            activeMode = mode;
            activeListener = listener;
            readings.clear();
            state = UhfScanState.PROCESSING;
        }
        dispatchCallback(currentGeneration, owner,
                () -> listener.onStateChanged(UhfScanState.PROCESSING));
        executor.execute(() -> initializeAndScan(currentGeneration, recoverReader));
    }

    @Override
    public void stop(Object owner) {
        finishScan(owner, true);
    }

    @Override
    public void cancel(Object owner) {
        finishScan(owner, false);
    }

    private void finishScan(Object owner, boolean deliverSingleReading) {
        if (owner == null) {
            throw new IllegalArgumentException("扫描所有者不能为空");
        }
        Listener listener;
        UhfTagReading singleReading = null;
        long stoppedGeneration;
        synchronized (lock) {
            if (activeOwner != owner || closing || state == UhfScanState.IDLE) {
                return;
            }
            generation++;
            stoppedGeneration = generation;
            cancelScanFuture();
            listener = activeListener;
            if (deliverSingleReading && state == UhfScanState.SCANNING
                    && activeMode == UhfScanMode.SINGLE
                    && readings.size() == 1) {
                singleReading = readings.values().iterator().next();
            }
            activeListener = null;
            activeMode = null;
            readings.clear();
            state = UhfScanState.IDLE;
        }
        awaitCallbacks();
        Listener stoppedListener = listener;
        UhfTagReading completedReading = singleReading;
        if (stoppedListener != null) {
            // 只有正常结束才交付 SINGLE 结果；切模式或重置使用 cancel 丢弃旧窗口。
            dispatchCallback(stoppedGeneration, owner, () -> {
                if (completedReading != null) {
                    stoppedListener.onTagRead(completedReading);
                    if (!isCallbackCurrent(stoppedGeneration, owner)) {
                        return;
                    }
                }
                stoppedListener.onStateChanged(UhfScanState.IDLE);
            });
        }
    }

    @Override
    public void close(Object owner) {
        if (owner == null) {
            throw new IllegalArgumentException("扫描所有者不能为空");
        }
        long closeGeneration;
        synchronized (lock) {
            if (activeOwner != owner || closing) {
                return;
            }
            closing = true;
            generation++;
            closeGeneration = generation;
            cancelScanFuture();
            activeListener = null;
            activeMode = null;
            readings.clear();
            state = UhfScanState.PROCESSING;
        }
        // 已开始执行的旧页面回调必须结束后 close 才能返回；排队但未执行的回调会被代次校验丢弃。
        awaitCallbacks();
        executor.execute(() -> {
            Reader readerToClose;
            synchronized (lock) {
                readerToClose = reader;
            }
            if (!releaseReader(readerToClose)) {
                synchronized (lock) {
                    if (generation == closeGeneration && activeOwner == owner) {
                        closing = false;
                        state = UhfScanState.ERROR;
                    }
                }
                return;
            }
            synchronized (lock) {
                if (generation == closeGeneration && activeOwner == owner) {
                    closing = false;
                    activeOwner = null;
                    state = UhfScanState.IDLE;
                }
            }
        });
    }

    @Override
    public UhfScanState getState() {
        return state;
    }

    private void initializeAndScan(long currentGeneration, boolean recoverReader) {
        try {
            validateConfiguration();
            if (recoverReader) {
                Reader readerToRecover;
                synchronized (lock) {
                    if (!isCurrent(currentGeneration, UhfScanState.PROCESSING)) {
                        return;
                    }
                    readerToRecover = reader;
                }
                // ERROR 状态下的 Reader 可能已经处于未知状态，必须先释放再重新初始化。
                if (!releaseReader(readerToRecover)) {
                    fail(currentGeneration, "UHF 设备恢复失败，请退出后重试");
                    return;
                }
            }

            Reader readyReader;
            synchronized (lock) {
                if (!isCurrent(currentGeneration, UhfScanState.PROCESSING)) {
                    return;
                }
                readyReader = reader;
            }
            if (readyReader == null) {
                readyReader = readerFactory.open();
                if (readyReader == null) {
                    throw new UhfOperationException("UHF 设备初始化失败");
                }
                synchronized (lock) {
                    // 打开后立即保存句柄；后续参数设置或清理失败时仍可由页面退出重试释放。
                    reader = readyReader;
                }
                if (!setOutputPowerWithRetry(readyReader)) {
                    releaseReader(readyReader);
                    throw new UhfOperationException("UHF 功率设置失败");
                }
                if (readyReader.setWorkArea(workArea) != 0) {
                    releaseReader(readyReader);
                    throw new UhfOperationException("UHF 工作区设置失败");
                }
                synchronized (lock) {
                    if (!isCurrent(currentGeneration, UhfScanState.PROCESSING)) {
                        return;
                    }
                }
            }

            Listener listener;
            Object owner;
            synchronized (lock) {
                if (!isCurrent(currentGeneration, UhfScanState.PROCESSING)) {
                    return;
                }
                state = UhfScanState.SCANNING;
                listener = activeListener;
                owner = activeOwner;
                scanFuture = executor.scheduleWithFixedDelay(
                        () -> scanRound(currentGeneration), 0L, EMPTY_ROUND_BACKOFF_MS,
                        TimeUnit.MILLISECONDS);
            }
            dispatchCallback(currentGeneration, owner,
                    () -> listener.onStateChanged(UhfScanState.SCANNING));
        } catch (UhfOperationException exception) {
            fail(currentGeneration, exception.getMessage());
        } catch (RuntimeException | LinkageError exception) {
            fail(currentGeneration, "UHF 设备初始化失败");
        }
    }

    private void scanRound(long currentGeneration) {
        Reader activeReader;
        synchronized (lock) {
            if (!isCurrent(currentGeneration, UhfScanState.SCANNING)) {
                return;
            }
            activeReader = reader;
        }
        try {
            List<RawTag> rawTags = activeReader.inventoryRealTime();
            if (rawTags == null || rawTags.isEmpty()) {
                return;
            }
            List<UhfTagReading> changedReadings = new ArrayList<>();
            boolean ambiguousSingle;
            Listener listener;
            UhfScanMode mode;
            Object owner;
            synchronized (lock) {
                if (!isCurrent(currentGeneration, UhfScanState.SCANNING)) {
                    return;
                }
                long now = System.currentTimeMillis();
                for (RawTag rawTag : rawTags) {
                    if (rawTag == null) {
                        continue;
                    }
                    String epc = UhfTagReading.normalizeEpc(bytesToHex(rawTag.epcBytes));
                    UhfTagReading previous = readings.get(epc);
                    UhfTagReading updated = previous == null
                            ? new UhfTagReading(epc, rawTag.rssi, 1, now, now)
                            : previous.next(rawTag.rssi, now);
                    readings.put(epc, updated);
                    changedReadings.add(updated);
                }
                ambiguousSingle = activeMode == UhfScanMode.SINGLE && readings.size() > 1;
                listener = activeListener;
                mode = activeMode;
                owner = activeOwner;
            }
            if (changedReadings.isEmpty()) {
                return;
            }
            if (ambiguousSingle) {
                fail(currentGeneration, "检测到多个 RFID 标签，请靠近目标标签后重试");
                return;
            }
            if (mode == UhfScanMode.BATCH && listener != null) {
                dispatchCallback(currentGeneration, owner, () -> {
                    for (UhfTagReading reading : changedReadings) {
                        listener.onTagRead(reading);
                        if (!isCallbackCurrent(currentGeneration, owner)) {
                            return;
                        }
                    }
                });
            }
        } catch (IllegalArgumentException exception) {
            fail(currentGeneration, "读取到无效的 EPC 数据");
        } catch (RuntimeException | LinkageError exception) {
            fail(currentGeneration, "UHF 扫描失败，请停止后重试");
        }
    }

    private void validateConfiguration() throws UhfOperationException {
        if (outputPower == UNCONFIGURED || workArea == UNCONFIGURED) {
            throw new UhfOperationException("UHF 参数尚未完成真机标定");
        }
        if (outputPower < 16 || outputPower > 26) {
            throw new UhfOperationException("UHF 功率配置不合法");
        }
        // 历史项目 SZXBGJ 使用 0；厂商文档未定义该值，仅允许用于当前受控真机冒烟。
        if (workArea != LEGACY_DEVELOPMENT_WORK_AREA
                && workArea != 1 && workArea != 2 && workArea != 3
                && workArea != 4 && workArea != 6) {
            throw new UhfOperationException("UHF 工作区配置不合法");
        }
    }

    private boolean setOutputPowerWithRetry(Reader readyReader)
            throws UhfOperationException {
        if (readyReader.setOutputPower(outputPower)) {
            return true;
        }
        try {
            // C6200 真机偶发丢失首次配置确认；功率设置幂等，因此只允许一次短间隔重试。
            Thread.sleep(POWER_CONFIGURATION_RETRY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UhfOperationException("UHF 设备初始化失败");
        }
        return readyReader.setOutputPower(outputPower);
    }

    private void fail(long currentGeneration, String message) {
        Listener listener;
        Object owner;
        long errorGeneration;
        synchronized (lock) {
            if (generation != currentGeneration) {
                return;
            }
            generation++;
            errorGeneration = generation;
            cancelScanFuture();
            state = UhfScanState.ERROR;
            listener = activeListener;
            owner = activeOwner;
            activeListener = null;
            activeMode = null;
            readings.clear();
        }
        awaitCallbacks();
        if (listener != null) {
            dispatchCallback(errorGeneration, owner, () -> {
                listener.onStateChanged(UhfScanState.ERROR);
                if (isCallbackCurrent(errorGeneration, owner)) {
                    listener.onError(message);
                }
            });
        }
    }

    private boolean isCurrent(long currentGeneration, UhfScanState expectedState) {
        return generation == currentGeneration && state == expectedState && !closing;
    }

    private boolean isCallbackCurrent(long callbackGeneration, Object owner) {
        synchronized (lock) {
            return generation == callbackGeneration && activeOwner == owner;
        }
    }

    private void dispatchCallback(long callbackGeneration, Object owner, Runnable callback) {
        callbackExecutor.execute(() -> {
            synchronized (callbackLock) {
                if (isCallbackCurrent(callbackGeneration, owner)) {
                    callback.run();
                }
            }
        });
    }

    private void awaitCallbacks() {
        synchronized (callbackLock) {
            // 该同步点用于等待已经开始执行的页面回调退出。
        }
    }

    private void cancelScanFuture() {
        if (scanFuture != null) {
            scanFuture.cancel(false);
            scanFuture = null;
        }
    }

    private boolean releaseReader(Reader readerToClose) {
        if (readerToClose == null) {
            return true;
        }
        try {
            readerToClose.close();
        } catch (RuntimeException | LinkageError exception) {
            // 保留失败句柄，允许原页面退出或下一位所有者再次执行释放。
            return false;
        }
        synchronized (lock) {
            if (reader == readerToClose) {
                reader = null;
            }
        }
        return true;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] digits = "0123456789ABCDEF".toCharArray();
        char[] output = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xFF;
            output[index * 2] = digits[value >>> 4];
            output[index * 2 + 1] = digits[value & 0x0F];
        }
        return new String(output);
    }

    private static Executor createMainThreadExecutor() {
        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);
        return command -> {
            if (Looper.myLooper() == mainLooper) {
                command.run();
            } else if (!mainHandler.post(command)) {
                throw new IllegalStateException("无法向 Android 主线程派发 UHF 回调");
            }
        };
    }

    interface ReaderFactory {
        Reader open();
    }

    interface Reader {
        boolean setOutputPower(int value);

        int setWorkArea(int area);

        List<RawTag> inventoryRealTime();

        void close();
    }

    static final class RawTag {
        private final byte[] epcBytes;
        private final int rssi;

        RawTag(byte[] epcBytes, int rssi) {
            this.epcBytes = epcBytes == null ? null : epcBytes.clone();
            this.rssi = rssi;
        }
    }

    private static final class VendorReaderFactory implements ReaderFactory {
        @Override
        public Reader open() {
            UhfReader vendorReader = UhfReader.getInstance();
            if (vendorReader == null) {
                return null;
            }
            try {
                // 厂商 Demo 在上电取得实例后保留 1 秒稳定窗口；真机若立即发配置命令会无响应。
                Thread.sleep(VENDOR_READER_READY_DELAY_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                vendorReader.close();
                return null;
            }
            return new VendorReader(vendorReader);
        }
    }

    private static final class VendorReader implements Reader {
        private final UhfReader vendorReader;

        private VendorReader(UhfReader vendorReader) {
            this.vendorReader = vendorReader;
        }

        @Override
        public boolean setOutputPower(int value) {
            return vendorReader.setOutputPower(value);
        }

        @Override
        public int setWorkArea(int area) {
            return vendorReader.setWorkArea(area);
        }

        @Override
        public List<RawTag> inventoryRealTime() {
            List<TagModel> tags = vendorReader.inventoryRealTime();
            if (tags == null || tags.isEmpty()) {
                return Collections.emptyList();
            }
            List<RawTag> readings = new ArrayList<>(tags.size());
            for (TagModel tag : tags) {
                if (tag != null) {
                    readings.add(new RawTag(tag.getmEpcBytes(), tag.getmRssi()));
                }
            }
            return readings;
        }

        @Override
        public void close() {
            vendorReader.close();
        }
    }

    private static final class UhfOperationException extends Exception {
        private UhfOperationException(String message) {
            super(message);
        }
    }

    private static final class UhfThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "asset-pda-uhf");
            thread.setDaemon(true);
            return thread;
        }
    }
}
