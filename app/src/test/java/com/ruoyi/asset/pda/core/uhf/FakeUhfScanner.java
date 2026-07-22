package com.ruoyi.asset.pda.core.uhf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 纯 JVM 测试扫描器，不加载厂商 JAR/JNI。
 */
public final class FakeUhfScanner implements UhfScanner {
    private final Object callbackLock = new Object();
    private final Map<String, UhfTagReading> readings = new LinkedHashMap<>();
    private final Executor callbackExecutor;

    private UhfScanState state = UhfScanState.IDLE;
    private UhfScanMode mode;
    private Listener listener;
    private Object owner;
    private long generation;
    private int stopCallCount;
    private int closeCallCount;

    public FakeUhfScanner() {
        this(Runnable::run);
    }

    FakeUhfScanner(Executor callbackExecutor) {
        if (callbackExecutor == null) {
            throw new IllegalArgumentException("回调执行器不能为空");
        }
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public void start(Object owner, UhfScanMode mode, Listener listener) {
        if (owner == null || mode == null || listener == null) {
            throw new IllegalArgumentException("扫描所有者、模式和监听器不能为空");
        }
        long currentGeneration;
        synchronized (this) {
            if (state == UhfScanState.SCANNING) {
                if (this.owner == owner && this.mode == mode && this.listener == listener) {
                    return;
                }
                throw new IllegalStateException("扫描任务已被占用");
            }
            generation++;
            currentGeneration = generation;
            this.owner = owner;
            this.mode = mode;
            this.listener = listener;
            readings.clear();
            state = UhfScanState.SCANNING;
        }
        dispatch(currentGeneration, owner, () -> listener.onStateChanged(UhfScanState.SCANNING));
    }

    @Override
    public void stop(Object owner) {
        synchronized (this) {
            stopCallCount++;
        }
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
        Listener stoppedListener;
        UhfTagReading completedReading = null;
        long stoppedGeneration;
        synchronized (this) {
            if (this.owner != owner || state == UhfScanState.IDLE) {
                return;
            }
            generation++;
            stoppedGeneration = generation;
            stoppedListener = listener;
            if (deliverSingleReading && state == UhfScanState.SCANNING
                    && mode == UhfScanMode.SINGLE
                    && readings.size() == 1 && stoppedListener != null) {
                completedReading = readings.values().iterator().next();
            }
            state = UhfScanState.IDLE;
            mode = null;
            listener = null;
            readings.clear();
        }
        awaitCallbacks();
        if (stoppedListener != null) {
            UhfTagReading reading = completedReading;
            dispatch(stoppedGeneration, owner, () -> {
                if (reading != null) {
                    stoppedListener.onTagRead(reading);
                    if (!isCurrent(stoppedGeneration, owner)) {
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
        synchronized (this) {
            closeCallCount++;
            if (this.owner != owner) {
                return;
            }
            generation++;
            state = UhfScanState.IDLE;
            mode = null;
            listener = null;
            this.owner = null;
            readings.clear();
        }
        awaitCallbacks();
    }

    @Override
    public synchronized UhfScanState getState() {
        return state;
    }

    public void emit(String epc, int rssi) {
        emitRound(new TagInput(epc, rssi));
    }

    /**
     * 模拟一次硬件 inventory 回合；同一回合可同时返回多个 EPC，用于验证 SINGLE 歧义边界。
     */
    public void emitRound(String... epcs) {
        TagInput[] inputs = new TagInput[epcs == null ? 0 : epcs.length];
        for (int index = 0; index < inputs.length; index++) {
            inputs[index] = new TagInput(epcs[index], -40);
        }
        emitRound(inputs);
    }

    private void emitRound(TagInput... tags) {
        Listener callbackListener;
        List<UhfTagReading> updatedReadings = new ArrayList<>();
        Object callbackOwner;
        long callbackGeneration;
        boolean ambiguous;
        UhfScanMode currentMode;
        synchronized (this) {
            requireScanning();
            long now = System.currentTimeMillis();
            for (TagInput tag : tags) {
                if (tag == null) {
                    continue;
                }
                String normalized = UhfTagReading.normalizeEpc(tag.epc);
                UhfTagReading previous = readings.get(normalized);
                UhfTagReading updated = previous == null
                        ? new UhfTagReading(normalized, tag.rssi, 1, now, now)
                        : previous.next(tag.rssi, now);
                readings.put(normalized, updated);
                updatedReadings.add(updated);
            }
            callbackListener = listener;
            callbackOwner = owner;
            ambiguous = mode == UhfScanMode.SINGLE && readings.size() > 1;
            currentMode = mode;
            if (ambiguous) {
                generation++;
                state = UhfScanState.ERROR;
                listener = null;
                mode = null;
                readings.clear();
            }
            callbackGeneration = generation;
        }
        if (ambiguous) {
            dispatch(callbackGeneration, callbackOwner, () -> {
                callbackListener.onStateChanged(UhfScanState.ERROR);
                if (isCurrent(callbackGeneration, callbackOwner)) {
                    callbackListener.onError("检测到多个 RFID 标签，请靠近目标标签后重试");
                }
            });
        } else if (currentMode == UhfScanMode.BATCH) {
            dispatch(callbackGeneration, callbackOwner, () -> {
                for (UhfTagReading reading : updatedReadings) {
                    callbackListener.onTagRead(reading);
                    if (!isCurrent(callbackGeneration, callbackOwner)) {
                        return;
                    }
                }
            });
        } else if (currentMode == UhfScanMode.SINGLE && !updatedReadings.isEmpty()) {
            // 与真实 UhfDeviceManager 保持一致：一个唯一 EPC 到达后自动结束 SINGLE 扫描。
            finishScan(callbackOwner, true);
        }
    }

    public synchronized void emitEmptyRound() {
        requireScanning();
    }

    public void emitError(String message) {
        Listener failedListener;
        Object failedOwner;
        long errorGeneration;
        synchronized (this) {
            requireScanning();
            generation++;
            errorGeneration = generation;
            failedListener = listener;
            failedOwner = owner;
            state = UhfScanState.ERROR;
            listener = null;
            mode = null;
            readings.clear();
        }
        awaitCallbacks();
        dispatch(errorGeneration, failedOwner, () -> {
            failedListener.onStateChanged(UhfScanState.ERROR);
            if (isCurrent(errorGeneration, failedOwner)) {
                failedListener.onError(message);
            }
        });
    }

    public synchronized List<UhfTagReading> snapshot() {
        return new ArrayList<>(readings.values());
    }

    public synchronized UhfScanMode getMode() {
        return mode;
    }

    public synchronized int getStopCallCount() {
        return stopCallCount;
    }

    public synchronized int getCloseCallCount() {
        return closeCallCount;
    }

    private void requireScanning() {
        if (state != UhfScanState.SCANNING) {
            throw new IllegalStateException("当前没有活动扫描任务");
        }
    }

    private synchronized boolean isCurrent(long callbackGeneration, Object callbackOwner) {
        return generation == callbackGeneration && owner == callbackOwner;
    }

    private void dispatch(long callbackGeneration, Object callbackOwner, Runnable callback) {
        callbackExecutor.execute(() -> {
            synchronized (callbackLock) {
                if (isCurrent(callbackGeneration, callbackOwner)) {
                    callback.run();
                }
            }
        });
    }

    private void awaitCallbacks() {
        synchronized (callbackLock) {
            // 与生产包装层保持相同的关闭语义：不越过已经开始执行的旧页面回调。
        }
    }

    private static final class TagInput {
        private final String epc;
        private final int rssi;

        private TagInput(String epc, int rssi) {
            this.epc = epc;
            this.rssi = rssi;
        }
    }
}
