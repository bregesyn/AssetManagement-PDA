package com.ruoyi.asset.pda.core.uhf;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UhfTagReadingTest {
    @Test
    public void normalizesEpcAndTracksLatestReading() {
        UhfTagReading first = new UhfTagReading(" e200aa ", -50, 1, 100L, 100L);
        UhfTagReading next = first.next(-45, 120L);

        assertEquals("E200AA", next.getEpc());
        assertEquals(-45, next.getRssi());
        assertEquals(2, next.getReadCount());
        assertEquals(100L, next.getFirstSeenAt());
        assertEquals(120L, next.getLastSeenAt());
    }

    @Test
    public void rejectsOddLengthOrNonHexEpc() {
        assertInvalid("ABC");
        assertInvalid("E200-ZZ");
        assertInvalid("  ");
    }

    private static void assertInvalid(String value) {
        try {
            UhfTagReading.normalizeEpc(value);
            fail("非法 EPC 应被拒绝: " + value);
        } catch (IllegalArgumentException expected) {
            // 预期行为
        }
    }
}
