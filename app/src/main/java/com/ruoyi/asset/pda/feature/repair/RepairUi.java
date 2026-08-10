package com.ruoyi.asset.pda.feature.repair;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;

import java.util.Collections;
import java.util.List;

/**
 * 报修页面共享展示规则。
 *
 * <p>状态值以服务端字典为准；客户端只为状态轨提供固定的工业现场语义，未知状态始终保留原始编码，
 * 避免在新状态上线而客户端尚未发版时误导维修人员。</p>
 */
final class RepairUi {
    static final String STATUS_DRAFT = "DRAFT";
    static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    static final String STATUS_WAIT_REPAIR = "WAIT_REPAIR";
    static final String STATUS_REJECTED = "REJECTED";
    static final String STATUS_REPAIRING = "REPAIRING";
    static final String STATUS_REPAIR_DONE = "REPAIR_DONE";

    static final String REPAIRER_INTERNAL = "INTERNAL";
    static final String REPAIRER_EXTERNAL = "EXTERNAL";

    private RepairUi() {
    }

    static String statusLabel(String status, List<PdaDictItemDto> options) {
        if (status == null || status.trim().isEmpty()) {
            return "-";
        }
        for (PdaDictItemDto item : safe(options)) {
            if (status.equals(item.getValue())) {
                return item.getLabel();
            }
        }
        return status;
    }

    static int statusRailColor(String status) {
        if (STATUS_REJECTED.equals(status)) {
            return R.color.pda_error;
        }
        if (STATUS_REPAIR_DONE.equals(status)) {
            return R.color.pda_success;
        }
        if (STATUS_PENDING_CONFIRM.equals(status) || STATUS_WAIT_REPAIR.equals(status)
                || STATUS_REPAIRING.equals(status)) {
            return R.color.pda_pending;
        }
        return R.color.pda_primary;
    }

    static String displayText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static String datePart(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    static List<PdaDictItemDto> safe(List<PdaDictItemDto> options) {
        return options == null ? Collections.emptyList() : options;
    }
}
