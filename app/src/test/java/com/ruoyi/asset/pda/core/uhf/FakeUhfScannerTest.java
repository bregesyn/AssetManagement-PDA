package com.ruoyi.asset.pda.core.uhf;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FakeUhfScannerTest {
    @Test
    public void batchKeepsFirstSeenOrderAndUpdatesDuplicateCount() {
        FakeUhfScanner scanner = new FakeUhfScanner();
        RecordingListener listener = new RecordingListener();
        scanner.start(listener, UhfScanMode.BATCH, listener);

        scanner.emit(" aa ", -60);
        scanner.emit("AA", -55);
        scanner.emitEmptyRound();
        scanner.emit("BB", -50);

        List<UhfTagReading> snapshot = scanner.snapshot();
        assertEquals(2, snapshot.size());
        assertEquals("AA", snapshot.get(0).getEpc());
        assertEquals(2, snapshot.get(0).getReadCount());
        assertEquals(-55, snapshot.get(0).getRssi());
        assertEquals("BB", snapshot.get(1).getEpc());
        assertEquals(3, listener.readings.size());
        assertEquals(UhfScanState.SCANNING, scanner.getState());
    }

    @Test
    public void singleRejectsMultipleDistinctTagsBeforeDeliveringResult() {
        FakeUhfScanner scanner = new FakeUhfScanner();
        RecordingListener listener = new RecordingListener();
        scanner.start(listener, UhfScanMode.SINGLE, listener);

        scanner.emit("AA", -60);
        scanner.emit("BB", -55);

        assertEquals(UhfScanState.ERROR, scanner.getState());
        assertTrue(listener.readings.isEmpty());
        assertEquals(1, listener.errors.size());
    }

    @Test
    public void singleDeliversUniqueTagOnlyWhenStopped() {
        FakeUhfScanner scanner = new FakeUhfScanner();
        RecordingListener listener = new RecordingListener();
        scanner.start(listener, UhfScanMode.SINGLE, listener);
        scanner.emit("AA", -60);
        scanner.emit("AA", -55);

        scanner.stop(listener);
        scanner.stop(listener);

        assertEquals(1, listener.readings.size());
        assertEquals(2, listener.readings.get(0).getReadCount());
        assertEquals(UhfScanState.IDLE, scanner.getState());
        assertEquals(2, scanner.getStopCallCount());
    }

    @Test
    public void cancellingSingleScanDiscardsUniqueTag() {
        FakeUhfScanner scanner = new FakeUhfScanner();
        RecordingListener listener = new RecordingListener();
        scanner.start(listener, UhfScanMode.SINGLE, listener);
        scanner.emit("AA", -60);

        scanner.cancel(listener);

        assertTrue(listener.readings.isEmpty());
        assertEquals(UhfScanState.IDLE, scanner.getState());
    }

    @Test
    public void closeIsIdempotent() {
        FakeUhfScanner scanner = new FakeUhfScanner();
        RecordingListener listener = new RecordingListener();
        scanner.start(listener, UhfScanMode.BATCH, listener);

        scanner.close(listener);
        scanner.close(listener);

        assertEquals(UhfScanState.IDLE, scanner.getState());
        assertEquals(2, scanner.getCloseCallCount());
    }

    @Test
    public void staleOwnerCannotStopNewScan() {
        FakeUhfScanner scanner = new FakeUhfScanner();
        Object oldOwner = new Object();
        Object newOwner = new Object();
        RecordingListener oldListener = new RecordingListener();
        RecordingListener newListener = new RecordingListener();
        scanner.start(oldOwner, UhfScanMode.BATCH, oldListener);
        scanner.stop(oldOwner);
        scanner.start(newOwner, UhfScanMode.BATCH, newListener);

        scanner.stop(oldOwner);
        scanner.close(oldOwner);
        scanner.emit("EE", -35);

        assertEquals(UhfScanState.SCANNING, scanner.getState());
        assertEquals(1, newListener.readings.size());
        assertEquals("EE", newListener.readings.get(0).getEpc());
    }

    private static final class RecordingListener implements UhfScanner.Listener {
        private final List<UhfTagReading> readings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

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
