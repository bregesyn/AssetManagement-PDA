package com.ruoyi.asset.pda.core.uhf;

/**
 * 后续 ViewModel 使用的最小扫描接口，厂商类不会越过此边界。
 */
public interface UhfScanner {
    void start(Object owner, UhfScanMode mode, Listener listener);

    void stop(Object owner);

    void close(Object owner);

    UhfScanState getState();

    interface Listener {
        void onStateChanged(UhfScanState state);

        void onTagRead(UhfTagReading reading);

        void onError(String message);
    }
}
