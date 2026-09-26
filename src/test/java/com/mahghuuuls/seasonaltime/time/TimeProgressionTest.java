package com.mahghuuuls.seasonaltime.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link CycleDurations} and {@link TimeStepper} together tick by tick, the way the server
 * tick handler does, and checks the real time each half of the day takes.
 */
class TimeProgressionTest {

    /**
     * Simulates ticks through {@link TimeStepper#stepAt}, the call the server tick handler makes,
     * until world time reaches the target; returns the tick count. Fails instead of hanging when
     * time stops advancing.
     */
    private static long ticksToReach(CycleDurations durations, long start, long target, long[] maxStep) {
        TimeStepper stepper = new TimeStepper();
        long time = start;
        long ticks = 0;
        long limit = 10L * 1440 * 1200;
        while (time < target) {
            if (ticks > limit) {
                throw new AssertionError("world time stopped advancing at " + time);
            }
            long step = stepper.stepAt(durations, time);
            maxStep[0] = Math.max(maxStep[0], step);
            time += step;
            ticks++;
        }
        return ticks;
    }

    @Test
    void thirtyMinuteDayIsOneThirdUnitPerTick() {
        CycleDurations durations = CycleDurations.base(30, 30);
        assertEquals(1.0 / 3.0, durations.unitsPerTick(0), 1e-12);
        assertEquals(1.0 / 3.0, durations.unitsPerTick(18000), 1e-12);
    }

    @Test
    void thirtyThirtyCycleTakesSixtyMinutes() {
        long[] max = {0};
        long day = ticksToReach(CycleDurations.base(30, 30), 0, 12000, max);
        long night = ticksToReach(CycleDurations.base(30, 30), 12000, 24000, max);
        assertEquals(30 * 1200, day, 1);
        assertEquals(30 * 1200, night, 1);
        assertEquals(1, max[0], "a 30-minute day never needs more than one unit per tick");
    }

    @Test
    void unequalDayAndNightTakeTheirOwnLengths() {
        long[] max = {0};
        assertEquals(40 * 1200, ticksToReach(CycleDurations.base(40, 20), 0, 12000, max), 1);
        assertEquals(20 * 1200, ticksToReach(CycleDurations.base(40, 20), 12000, 24000, max), 1);
    }

    @Test
    void aFullCycleAdvancesExactlyOneDayWithoutDrift() {
        CycleDurations durations = CycleDurations.base(30, 30);
        TimeStepper stepper = new TimeStepper();
        long time = 0;
        for (int tick = 0; tick < 60 * 1200; tick++) {
            time += stepper.stepAt(durations, time);
        }
        assertEquals(24000, time, 1);
    }

    @Test
    void manyCyclesDoNotBuildUpRoundingError() {
        CycleDurations durations = CycleDurations.base(37, 23);
        TimeStepper stepper = new TimeStepper();
        long time = 0;
        for (int tick = 0; tick < 100 * 60 * 1200; tick++) {
            time += stepper.stepAt(durations, time);
        }
        assertEquals(100 * 24000L, time, 2);
    }

    @Test
    void slowestDayNeverAddsMoreThanOneUnit() {
        long[] max = {0};
        long ticks = ticksToReach(CycleDurations.base(1440, 1440), 0, 12000, max);
        assertEquals(1440 * 1200, ticks, 1);
        assertEquals(1, max[0]);
    }

    @Test
    void fastestDayAveragesTenUnitsAndNeverExceedsEleven() {
        long[] max = {0};
        long ticks = ticksToReach(CycleDurations.base(1, 1), 0, 12000, max);
        assertEquals(1200, ticks, 1);
        assertTrue(max[0] <= 11, "largest step " + max[0]);
    }

    @Test
    void segmentsFollowVanillaBoundariesForAnyWorldTime() {
        assertTrue(CycleDurations.isDay(0));
        assertTrue(CycleDurations.isDay(11999));
        assertFalse(CycleDurations.isDay(12000));
        assertFalse(CycleDurations.isDay(23999));
        assertTrue(CycleDurations.isDay(24000));
        assertFalse(CycleDurations.isDay(-1));
        assertTrue(CycleDurations.isDay(5_000_000_000L * 24000L + 100));
        assertEquals(23999, CycleDurations.timeOfDay(-1));
    }

    @Test
    void nextMorningIsTheStartOfTheFollowingDay() {
        assertEquals(24000, CycleDurations.nextMorning(13000));
        assertEquals(24000, CycleDurations.nextMorning(0));
        assertEquals(48000, CycleDurations.nextMorning(24000));
        assertEquals(48000, CycleDurations.nextMorning(47999));
        assertEquals(0, CycleDurations.nextMorning(-1));
        // Vanilla gives 0 here (its formula rounds toward zero); the true next day is -24000.
        assertEquals(-24000, CycleDurations.nextMorning(-30000));
        assertEquals(24000L * 1000001L, CycleDurations.nextMorning(24000L * 1000000L + 18000));
    }

    @Test
    void resetDropsTheCarriedFraction() {
        TimeStepper stepper = new TimeStepper();
        assertEquals(0, stepper.step(0.6));
        assertEquals(0.6, stepper.remainder(), 1e-12);
        stepper.reset();
        assertEquals(0, stepper.step(0.6));
    }

    @Test
    void invalidRatesAndDurationsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TimeStepper().step(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TimeStepper().step(-1));
        assertThrows(IllegalArgumentException.class, () -> CycleDurations.base(0, 30));
    }
}
