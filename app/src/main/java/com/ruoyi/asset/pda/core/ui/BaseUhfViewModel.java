package com.ruoyi.asset.pda.core.ui;

import androidx.lifecycle.ViewModel;

import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;

/**
 * 四个阶段 3 页面共享的最小硬件生命周期；业务状态仍由各自 ViewModel 维护。
 */
public abstract class BaseUhfViewModel extends ViewModel implements UhfScanner.Listener {
    private final UhfScanner scanner;
    private boolean ownsScanner;
    private UhfScanState scanState = UhfScanState.IDLE;

    protected BaseUhfViewModel(UhfScanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("UHF Scanner 不能为空");
        }
        this.scanner = scanner;
    }

    public final void startScanning(UhfScanMode mode) {
        if (scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING) {
            return;
        }
        try {
            ownsScanner = true;
            scanner.start(this, mode, this);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            ownsScanner = false;
            onScanError(exception.getMessage());
        }
    }

    public final void stopScanning() {
        if (ownsScanner) {
            scanner.stop(this);
        }
    }

    /** 切换条件或重置作业时丢弃扫描窗口，避免把旧 EPC 当成新操作结果。 */
    public final void cancelScanning() {
        if (ownsScanner) {
            scanner.cancel(this);
        }
    }

    /** 页面退到后台时释放硬件，但保留 ViewModel 中尚未提交的业务状态。 */
    public final void releaseScanner() {
        if (!ownsScanner) {
            return;
        }
        scanner.close(this);
        ownsScanner = false;
        scanState = UhfScanState.IDLE;
        onScannerStateChanged(UhfScanState.IDLE);
    }

    public final UhfScanState getCurrentScanState() {
        return scanState;
    }

    @Override
    public final void onStateChanged(UhfScanState state) {
        scanState = state == null ? UhfScanState.ERROR : state;
        onScannerStateChanged(scanState);
    }

    @Override
    public final void onTagRead(UhfTagReading reading) {
        if (reading != null) {
            onScannerTagRead(reading);
        }
    }

    @Override
    public final void onError(String message) {
        onScanError(message);
    }

    protected abstract void onScannerStateChanged(UhfScanState state);

    protected abstract void onScannerTagRead(UhfTagReading reading);

    protected abstract void onScanError(String message);

    protected void onViewModelCleared() {
    }

    @Override
    protected final void onCleared() {
        releaseScanner();
        onViewModelCleared();
    }
}
