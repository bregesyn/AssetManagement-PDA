package com.ruoyi.asset.pda.feature.rfid;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.BaseUhfViewModel;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchRowDto;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.data.repository.RfidRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RfidTagBatchViewModel extends BaseUhfViewModel {
    static final int MAX_UNIQUE_EPCS = 5000;
    static final int MAX_REMARK_LENGTH = 500;
    static final long UI_UPDATE_INTERVAL_MS = 150L;

    interface UiUpdateScheduler {
        long nowMillis();

        void postDelayed(Runnable task, long delayMillis);

        void removeCallbacks(Runnable task);
    }

    private final RfidRepository rfidRepository;
    private final UiUpdateScheduler uiUpdateScheduler;
    private final Map<String, UhfTagReading> readingsByEpc = new LinkedHashMap<>();
    private final MutableLiveData<RfidTagBatchUiState> uiState =
            new MutableLiveData<>(RfidTagBatchUiState.initial());
    private RequestHandle currentRequest = RequestHandle.NONE;
    private int duplicateReadCount;
    private int operationVersion;
    private boolean readingsPublished;
    private boolean uiUpdateScheduled;
    private long lastUiUpdateAt;
    private UhfTagReading pendingLastReading;
    private final Runnable scheduledUiUpdate = this::flushPendingUiUpdate;

    public RfidTagBatchViewModel(RfidRepository rfidRepository, UhfScanner scanner) {
        this(rfidRepository, scanner, new MainThreadUiUpdateScheduler());
    }

    RfidTagBatchViewModel(RfidRepository rfidRepository, UhfScanner scanner,
            UiUpdateScheduler uiUpdateScheduler) {
        super(scanner);
        if (rfidRepository == null || uiUpdateScheduler == null) {
            throw new IllegalArgumentException("批量建档依赖不能为空");
        }
        this.rfidRepository = rfidRepository;
        this.uiUpdateScheduler = uiUpdateScheduler;
    }

    public LiveData<RfidTagBatchUiState> getUiState() {
        return uiState;
    }

    public void setRemark(String remark) {
        RfidTagBatchUiState current = state();
        if (!current.isSubmitting() && !current.isResultMode()) {
            uiState.setValue(current.withRemark(remark == null ? "" : remark));
        }
    }

    public void toggleScan() {
        RfidTagBatchUiState current = state();
        if (current.isSubmitting() || current.isResultMode()) {
            return;
        }
        if (current.isScanning()) {
            stopScanning();
        } else {
            startScanning(UhfScanMode.BATCH);
        }
    }

    public void onScanKeyDown() {
        RfidTagBatchUiState current = state();
        if (!current.isSubmitting() && !current.isResultMode() && !current.isScanning()) {
            startScanning(UhfScanMode.BATCH);
        }
    }

    public void onScanKeyUp() {
        stopScanning();
    }

    public void submit() {
        RfidTagBatchUiState current = state();
        if (current.isSubmitting() || current.isResultMode() || current.isScanning()) {
            return;
        }
        if (readingsByEpc.isEmpty()) {
            uiState.setValue(current.error(R.string.batch_epc_required));
            return;
        }
        if (current.getRemark().length() > MAX_REMARK_LENGTH) {
            uiState.setValue(current.error(R.string.batch_remark_too_long));
            return;
        }
        cancelRequest();
        int requestVersion = ++operationVersion;
        uiState.setValue(current.submitting());
        List<String> epcCodes = new ArrayList<>(readingsByEpc.keySet());
        RequestHandle request = rfidRepository.batchCreate(
                epcCodes, current.getRemark(),
                new RepositoryCallback<RfidTagBatchResultDto>() {
                    @Override
                    public void onSuccess(RfidTagBatchResultDto data) {
                        if (requestVersion != operationVersion) {
                            return;
                        }
                        if (!isValidBatchResult(data, epcCodes)) {
                            uiState.setValue(state().error(R.string.batch_invalid_response));
                            return;
                        }
                        uiState.setValue(state().result(data));
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (requestVersion == operationVersion) {
                            // 请求级失败时保留 EPC 与备注，避免现场重新采集。
                            uiState.setValue(state().error(error.getMessage()));
                        }
                    }
                });
        if (requestVersion == operationVersion) {
            currentRequest = request;
        }
    }

    public void startNewBatch() {
        if (state().isSubmitting()) {
            return;
        }
        stopScanning();
        cancelRequest();
        operationVersion++;
        clearScheduledUiUpdate();
        readingsByEpc.clear();
        duplicateReadCount = 0;
        readingsPublished = false;
        pendingLastReading = null;
        uiState.setValue(RfidTagBatchUiState.initial());
    }

    public boolean hasUnsubmittedReadings() {
        RfidTagBatchUiState current = state();
        return !current.isResultMode() && !readingsByEpc.isEmpty();
    }

    public boolean isSubmitting() {
        return state().isSubmitting();
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        if (scanState != UhfScanState.PROCESSING && scanState != UhfScanState.SCANNING) {
            flushPendingUiUpdate();
        }
        RfidTagBatchUiState current = state();
        if (!current.isSubmitting() && !current.isResultMode()) {
            uiState.setValue(current.scanning(scanState));
        }
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        RfidTagBatchUiState current = state();
        if (current.isSubmitting() || current.isResultMode()) {
            return;
        }
        UhfTagReading previous = readingsByEpc.get(reading.getEpc());
        if (previous == null && readingsByEpc.size() >= MAX_UNIQUE_EPCS) {
            stopScanning();
            uiState.setValue(state().error(R.string.batch_limit_reached));
            return;
        }
        if (previous != null) {
            duplicateReadCount++;
        }
        readingsByEpc.put(reading.getEpc(), reading);
        pendingLastReading = reading;
        scheduleUiUpdate();
    }

    @Override
    protected void onScanError(String message) {
        flushPendingUiUpdate();
        uiState.setValue(hasText(message)
                ? state().error(message) : state().error(R.string.identify_scan_failed));
    }

    private void scheduleUiUpdate() {
        long now = uiUpdateScheduler.nowMillis();
        if (!readingsPublished || now - lastUiUpdateAt >= UI_UPDATE_INTERVAL_MS) {
            flushPendingUiUpdate();
            return;
        }
        if (!uiUpdateScheduled) {
            uiUpdateScheduled = true;
            uiUpdateScheduler.postDelayed(scheduledUiUpdate,
                    UI_UPDATE_INTERVAL_MS - (now - lastUiUpdateAt));
        }
    }

    private void flushPendingUiUpdate() {
        if (uiUpdateScheduled) {
            uiUpdateScheduler.removeCallbacks(scheduledUiUpdate);
            uiUpdateScheduled = false;
        }
        if (pendingLastReading == null) {
            return;
        }
        UhfTagReading latest = pendingLastReading;
        pendingLastReading = null;
        readingsPublished = true;
        lastUiUpdateAt = uiUpdateScheduler.nowMillis();
        uiState.setValue(state().collected(
                new ArrayList<>(readingsByEpc.values()), duplicateReadCount, latest));
    }

    private void clearScheduledUiUpdate() {
        if (uiUpdateScheduled) {
            uiUpdateScheduler.removeCallbacks(scheduledUiUpdate);
            uiUpdateScheduled = false;
        }
    }

    private boolean isValidBatchResult(RfidTagBatchResultDto data,
            List<String> submittedEpcs) {
        if (data == null || data.getRows() == null
                || data.getRows().size() != submittedEpcs.size()) {
            return false;
        }
        Set<Integer> seenRows = new HashSet<>();
        int successCount = 0;
        int duplicateCount = 0;
        int failureCount = 0;
        for (RfidTagBatchRowDto row : data.getRows()) {
            if (row == null || row.getRowNumber() == null) {
                return false;
            }
            int rowNumber = row.getRowNumber();
            if (rowNumber < 1 || rowNumber > submittedEpcs.size()
                    || !seenRows.add(rowNumber)) {
                return false;
            }
            String responseEpc;
            try {
                responseEpc = UhfTagReading.normalizeEpc(row.getEpcCode());
            } catch (IllegalArgumentException exception) {
                return false;
            }
            if (!submittedEpcs.get(rowNumber - 1).equals(responseEpc)
                    || (row.isSuccess() && row.isDuplicate())) {
                return false;
            }
            if (row.isSuccess()) {
                successCount++;
            } else if (row.isDuplicate()) {
                duplicateCount++;
            } else {
                failureCount++;
            }
        }
        return data.getSuccessCount() == successCount
                && data.getDuplicateCount() == duplicateCount
                && data.getFailureCount() == failureCount;
    }

    private RfidTagBatchUiState state() {
        RfidTagBatchUiState current = uiState.getValue();
        return current == null ? RfidTagBatchUiState.initial() : current;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void cancelRequest() {
        currentRequest.cancel();
        currentRequest = RequestHandle.NONE;
    }

    @Override
    protected void onViewModelCleared() {
        clearScheduledUiUpdate();
        cancelRequest();
        operationVersion++;
    }

    private static final class MainThreadUiUpdateScheduler implements UiUpdateScheduler {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public long nowMillis() {
            return SystemClock.uptimeMillis();
        }

        @Override
        public void postDelayed(Runnable task, long delayMillis) {
            handler.postDelayed(task, delayMillis);
        }

        @Override
        public void removeCallbacks(Runnable task) {
            handler.removeCallbacks(task);
        }
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final RfidRepository rfidRepository;
        private final UhfScanner scanner;

        public Factory(RfidRepository rfidRepository, UhfScanner scanner) {
            this.rfidRepository = rfidRepository;
            this.scanner = scanner;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RfidTagBatchViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new RfidTagBatchViewModel(rfidRepository, scanner);
        }
    }
}
