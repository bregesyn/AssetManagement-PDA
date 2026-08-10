package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;

import java.util.Collections;
import java.util.List;

/** 内部维修人搜索状态。 */
public final class RepairerPickerUiState {
    public enum Mode {
        INITIAL,
        SEARCHING,
        CONTENT,
        ERROR
    }

    private final Mode mode;
    private final List<PdaRepairerDto> repairers;
    private final String message;

    private RepairerPickerUiState(Mode mode, List<PdaRepairerDto> repairers, String message) {
        this.mode = mode;
        this.repairers = repairers == null ? Collections.emptyList() : repairers;
        this.message = message;
    }

    static RepairerPickerUiState initial() {
        return new RepairerPickerUiState(Mode.INITIAL, Collections.emptyList(), null);
    }

    static RepairerPickerUiState searching(List<PdaRepairerDto> repairers) {
        return new RepairerPickerUiState(Mode.SEARCHING, repairers, null);
    }

    static RepairerPickerUiState content(List<PdaRepairerDto> repairers) {
        return new RepairerPickerUiState(Mode.CONTENT, repairers, null);
    }

    static RepairerPickerUiState error(List<PdaRepairerDto> repairers, String message) {
        return new RepairerPickerUiState(Mode.ERROR, repairers, message);
    }

    public Mode getMode() { return mode; }
    public List<PdaRepairerDto> getRepairers() { return repairers; }
    public String getMessage() { return message; }
}
