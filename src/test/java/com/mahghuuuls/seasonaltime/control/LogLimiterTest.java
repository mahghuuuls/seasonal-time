package com.mahghuuuls.seasonaltime.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLimiterTest {

    @Test
    void firstLinePassesAtAnyTimeIncludingZero() {
        assertTrue(new LogLimiter(1200).allow(0));
        assertTrue(new LogLimiter(1200).allow(5_000_000L));
    }

    @Test
    void repeatsWithinTheIntervalAreHeldBack() {
        LogLimiter limiter = new LogLimiter(1200);
        assertTrue(limiter.allow(100));
        assertFalse(limiter.allow(101));
        assertFalse(limiter.allow(1299));
        assertTrue(limiter.allow(1300));
        assertFalse(limiter.allow(1301));
    }
}
