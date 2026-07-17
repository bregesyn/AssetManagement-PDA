package com.ruoyi.asset.pda.core.uhf;

import java.util.Locale;

/**
 * 不暴露厂商 TagModel 的项目读数模型。
 */
public final class UhfTagReading {
    private final String epc;
    private final int rssi;
    private final int readCount;
    private final long firstSeenAt;
    private final long lastSeenAt;

    public UhfTagReading(String epc, int rssi, int readCount, long firstSeenAt, long lastSeenAt) {
        this.epc = normalizeEpc(epc);
        if (readCount < 1) {
            throw new IllegalArgumentException("读取次数必须大于 0");
        }
        if (firstSeenAt < 0 || lastSeenAt < firstSeenAt) {
            throw new IllegalArgumentException("标签读取时间无效");
        }
        this.rssi = rssi;
        this.readCount = readCount;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public String getEpc() {
        return epc;
    }

    public int getRssi() {
        return rssi;
    }

    public int getReadCount() {
        return readCount;
    }

    public long getFirstSeenAt() {
        return firstSeenAt;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    public UhfTagReading next(int latestRssi, long seenAt) {
        return new UhfTagReading(epc, latestRssi, readCount + 1, firstSeenAt, seenAt);
    }

    public static String normalizeEpc(String value) {
        if (value == null) {
            throw new IllegalArgumentException("EPC 不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || (normalized.length() & 1) != 0) {
            throw new IllegalArgumentException("EPC 必须是非空的完整字节十六进制串");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            boolean hexadecimal = character >= '0' && character <= '9'
                    || character >= 'A' && character <= 'F';
            if (!hexadecimal) {
                throw new IllegalArgumentException("EPC 只能包含十六进制字符");
            }
        }
        return normalized;
    }
}
