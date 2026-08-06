package com.ruoyi.asset.pda.feature.borrow;

import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 借用人搜索状态；现场需要显式输入关键词，避免小屏加载整家公司名单。 */
public final class BorrowerPickerUiState {
    public enum Mode {
        IDLE,
        SEARCHING,
        CONTENT,
        ERROR
    }

    private final Mode mode;
    private final List<PdaMasterDataDto> borrowers;
    private final String message;

    public BorrowerPickerUiState(Mode mode, List<PdaMasterDataDto> borrowers,
            String message) {
        this.mode = mode == null ? Mode.IDLE : mode;
        this.borrowers = borrowers == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(borrowers));
        this.message = message;
    }

    public Mode getMode() {
        return mode;
    }

    public List<PdaMasterDataDto> getBorrowers() {
        return borrowers;
    }

    public String getMessage() {
        return message;
    }
}
