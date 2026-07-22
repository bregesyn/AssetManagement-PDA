package com.ruoyi.asset.pda.core.uhf;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScanKeyDispatcherTest {
    @Test
    public void acceptsBothVendorActionsAndC6200F6Only() {
        assertTrue(ScanKeyDispatcher.KeyState.isSupportedAction(
                ScanKeyDispatcher.ACTION_RFID_KEY));
        assertTrue(ScanKeyDispatcher.KeyState.isSupportedAction(
                ScanKeyDispatcher.ACTION_FUNCTION_KEY));
        assertTrue(ScanKeyDispatcher.KeyState.isSupportedKey(KeyEvent.KEYCODE_F6));
        assertFalse(ScanKeyDispatcher.KeyState.isSupportedKey(KeyEvent.KEYCODE_F1));
        assertFalse(ScanKeyDispatcher.KeyState.isSupportedKey(KeyEvent.KEYCODE_F5));
        assertFalse(ScanKeyDispatcher.KeyState.isSupportedKey(KeyEvent.KEYCODE_F7));
    }

    @Test
    public void emitsOneDownAndOneMatchingUpWhileFilteringRepeats() {
        ScanKeyDispatcher.KeyState state = new ScanKeyDispatcher.KeyState();

        assertEquals(ScanKeyDispatcher.KeyState.EVENT_DOWN, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F6, true));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F6, true));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F5, false));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_UP, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F6, false));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE, state.accept(
                ScanKeyDispatcher.ACTION_RFID_KEY, KeyEvent.KEYCODE_F6, false));
    }

    @Test
    public void unknownActionDoesNotChangePressedState() {
        ScanKeyDispatcher.KeyState state = new ScanKeyDispatcher.KeyState();

        assertEquals(ScanKeyDispatcher.KeyState.EVENT_NONE,
                state.accept("unknown.action", KeyEvent.KEYCODE_F6, true));
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_DOWN, state.accept(
                ScanKeyDispatcher.ACTION_FUNCTION_KEY, KeyEvent.KEYCODE_F6, true));
        state.reset();
        assertEquals(ScanKeyDispatcher.KeyState.EVENT_DOWN, state.accept(
                ScanKeyDispatcher.ACTION_FUNCTION_KEY, KeyEvent.KEYCODE_F6, true));
    }
}
