package com.ruoyi.asset.pda.feature.rfid;

import androidx.annotation.StringRes;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RfidTagBatchUiState {
    private final UhfScanState scanState;
    private final boolean submitting;
    private final List<UhfTagReading> readings;
    private final int duplicateReadCount;
    private final UhfTagReading lastReading;
    private final String remark;
    private final RfidTagBatchResultDto result;
    private final int errorTextResId;
    private final String errorMessage;

    private RfidTagBatchUiState(UhfScanState scanState, boolean submitting,
            List<UhfTagReading> readings, int duplicateReadCount, String remark,
            UhfTagReading lastReading, RfidTagBatchResultDto result,
            int errorTextResId, String errorMessage) {
        this.scanState = scanState;
        this.submitting = submitting;
        this.readings = Collections.unmodifiableList(new ArrayList<>(readings));
        this.duplicateReadCount = duplicateReadCount;
        this.lastReading = lastReading;
        this.remark = remark;
        this.result = result;
        this.errorTextResId = errorTextResId;
        this.errorMessage = errorMessage;
    }

    public static RfidTagBatchUiState initial() {
        return new RfidTagBatchUiState(UhfScanState.IDLE, false,
                Collections.emptyList(), 0, "", null, null, 0, null);
    }

    public RfidTagBatchUiState scanning(UhfScanState state) {
        return copy(state, false, readings, duplicateReadCount, remark,
                lastReading, result, 0, null);
    }

    public RfidTagBatchUiState collected(List<UhfTagReading> values, int duplicates,
            UhfTagReading latest) {
        return copy(scanState, false, values, duplicates, remark, latest, null, 0, null);
    }

    public RfidTagBatchUiState withRemark(String value) {
        return copy(scanState, submitting, readings, duplicateReadCount,
                value, lastReading, result, errorTextResId, errorMessage);
    }

    public RfidTagBatchUiState submitting() {
        return copy(UhfScanState.IDLE, true, readings, duplicateReadCount,
                remark, lastReading, null, 0, null);
    }

    public RfidTagBatchUiState result(RfidTagBatchResultDto value) {
        return copy(UhfScanState.IDLE, false, readings, duplicateReadCount,
                remark, lastReading, value, 0, null);
    }

    public RfidTagBatchUiState error(@StringRes int messageResId) {
        return copy(UhfScanState.IDLE, false, readings, duplicateReadCount,
                remark, lastReading, result, messageResId, null);
    }

    public RfidTagBatchUiState error(String message) {
        return copy(UhfScanState.IDLE, false, readings, duplicateReadCount,
                remark, lastReading, result, 0, message);
    }

    private RfidTagBatchUiState copy(UhfScanState state, boolean isSubmitting,
            List<UhfTagReading> values, int duplicates, String currentRemark,
            UhfTagReading latest, RfidTagBatchResultDto currentResult,
            int errorResId, String error) {
        return new RfidTagBatchUiState(state, isSubmitting, values, duplicates,
                currentRemark, latest, currentResult, errorResId, error);
    }

    public UhfScanState getScanState() { return scanState; }
    public boolean isSubmitting() { return submitting; }
    public List<UhfTagReading> getReadings() { return readings; }
    public int getDuplicateReadCount() { return duplicateReadCount; }
    public String getRemark() { return remark; }
    public RfidTagBatchResultDto getResult() { return result; }
    public int getErrorTextResId() { return errorTextResId; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isResultMode() { return result != null; }
    public boolean isScanning() {
        return scanState == UhfScanState.SCANNING || scanState == UhfScanState.PROCESSING;
    }
    public UhfTagReading getLastReading() {
        return lastReading;
    }
}
