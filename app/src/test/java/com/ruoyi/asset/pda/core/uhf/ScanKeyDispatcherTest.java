package com.ruoyi.asset.pda.core.uhf;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScanKeyDispatcherTest {
    @Test
    public void acceptsBothVendorActionsAndF1ThroughF5() {
        assertTrue(ScanKeyDispatcher.KeyState.isSupportedAction(
                ScanKeyDispatcher.ACTION_RFID_KEY));
        assertTrue(ScanKeyDispatcher.KeyState.isSupportedAction(
                ScanKeyDispatcher.ACTION_FUNCTION_KEY));
        for (int key = KeyEvent.KEYCODE_F1; key <= KeyEvent.KEYCODE_F5; key++) {
            assertTrue(ScanKeyDispatcher.KeyState.isSupportedKey(key));
        }
        assertFalse(ScanKeyDispatcher.KeyState.isSupportedKey(KeyEvent.KEYCODE_F1 - 1));
        assertFalse(ScanKeyDispatcher.KeyState.isSupportedKey(KeyEvent.KEYCODE_F5 + 1));
    }

    @Test
    public void emitsOneDownAndOneMatchingUpWhileFilteringRepeats() {
        ScanKeyDispatcher.KeyState state = new ScanKeyDispatcher.KeyState();

        assertEquals(ScanKeyDispatcher.KeyState.EVENT_DOWN, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F2, true));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F2, true));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F3, false));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_UP, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F2, false));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F2, false));
    }

    @Test
    public void unknownActionDoesNotChangePressedState() {
        ScanKeyDispatcher.KeyState state = new ScanKeyDispatcher.KeyState();

        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE,
                state.accept("unknown.action", KeyEvent.KEYCODE_F1, true));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_DOWN, state.accept(
                ScanKeyDispatcher.ACTION_FUNCTION_KEY, KeyEvent.KEYCODE_F1, true));
        state.reset();
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_DOWN, state.accept(
                ScanKeyDispatcher.ACTION_FUNCTION_KEY, KeyEvent.KEYCODE_F5, true));
    }
}
