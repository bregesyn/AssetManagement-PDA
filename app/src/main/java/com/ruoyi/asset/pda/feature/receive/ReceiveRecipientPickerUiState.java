package com.ruoyi.asset.pda.feature.receive;

import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 人员搜索页状态：必须先输入关键词，避免在小屏 PDA 上加载整家公司长名单。 */
public final class ReceiveRecipientPickerUiState {
    public enum Mode {
        IDLE,
        SEARCHING,
        CONTENT,
        ERROR
    }

    private final Mode mode;
    private final List<PdaMasterDataDto> recipients;
    private final String message;

    public ReceiveRecipientPickerUiState(Mode mode,
            List<PdaMasterDataDto> recipients, String message) {
        this.mode = mode == null ? Mode.IDLE : mode;
        this.recipients = recipients == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(recipients));
        this.message = message;
    }

    public Mode getMode() {
        return mode;
    }

    public List<PdaMasterDataDto> getRecipients() {
        return recipients;
    }

    public String getMessage() {
        return message;
    }
}
