package com.ruoyi.asset.pda.core.uhf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;

import androidx.core.content.ContextCompat;

/** 仅在扫描页面可见期间接收厂商功能键广播。 */
public final class ScanKeyDispatcher {
    public static final String ACTION_RFID_KEY = "android.rfid.FUN_KEY";
    public static final String ACTION_FUNCTION_KEY = "android.intent.action.FUN_KEY";

    private final Context context;
    private final Listener listener;
    private final KeyState keyState = new KeyState();
    private boolean registered;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ignored, Intent intent) {
            if (intent == null || !intent.hasExtra("keydown")) {
                return;
            }
            int keyCode = intent.hasExtra("keyCode")
                    ? intent.getIntExtra("keyCode", KeyEvent.KEYCODE_UNKNOWN)
                    : intent.getIntExtra("keycode", KeyEvent.KEYCODE_UNKNOWN);
            int event = keyState.accept(intent.getAction(), keyCode,
                    intent.getBooleanExtra("keydown", false));
            if (event == KeyState.EVENT_DOWN) {
                listener.onScanKeyDown();
            } else if (event == KeyState.EVENT_UP) {
                listener.onScanKeyUp();
            }
        }
    };

    public ScanKeyDispatcher(Context context, Listener listener) {
        if (context == null || listener == null) {
            throw new IllegalArgumentException("物理键分发依赖不能为空");
        }
        this.context = context;
        this.listener = listener;
    }

    public void start() {
        if (registered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RFID_KEY);
        filter.addAction(ACTION_FUNCTION_KEY);
        // 厂商系统进程发送广播，必须允许外部来源；前台生命周期和键值白名单限制作用范围。
        ContextCompat.registerReceiver(context, receiver, filter,
                ContextCompat.RECEIVER_EXPORTED);
        registered = true;
    }

    public void stop() {
        if (!registered) {
            return;
        }
        context.unregisterReceiver(receiver);
        registered = false;
        keyState.reset();
    }

    public interface Listener {
        void onScanKeyDown();

        void onScanKeyUp();
    }

    static final class KeyState {
        static final int EVENT_NONE = 0;
        static final int EVENT_DOWN = 1;
        static final int EVENT_UP = 2;

        private int pressedKey = KeyEvent.KEYCODE_UNKNOWN;

        int accept(String action, int keyCode, boolean down) {
            if (!isSupportedAction(action) || !isSupportedKey(keyCode)) {
                return EVENT_NONE;
            }
            if (down) {
                if (pressedKey != KeyEvent.KEYCODE_UNKNOWN) {
                    return EVENT_NONE;
                }
                pressedKey = keyCode;
                return EVENT_DOWN;
            }
            if (pressedKey != keyCode) {
                return EVENT_NONE;
            }
            pressedKey = KeyEvent.KEYCODE_UNKNOWN;
            return EVENT_UP;
        }

        void reset() {
            pressedKey = KeyEvent.KEYCODE_UNKNOWN;
        }

        static boolean isSupportedAction(String action) {
            return ACTION_RFID_KEY.equals(action) || ACTION_FUNCTION_KEY.equals(action);
        }

        static boolean isSupportedKey(int keyCode) {
            return keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F5;
        }
    }
}
