package com.mahghuuuls.seasonaltime.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seasonal daylight curve and the day and night split it produces. */
class DaylightCurveTest {

    private static final double EPSILON = 1e-9;

    @Test
    void solsticesAndEquinoxesHitTheirValues() {
        assertEquals(70.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.summerSolstice(), 70, 30, SolsticeTiming.MIDDLE), EPSILON);
        assertEquals(30.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.winterSolstice(), 70, 30, SolsticeTiming.MIDDLE), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.springEquinox(), 70, 30, SolsticeTiming.MIDDLE), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.autumnEquinox(), 70, 30, SolsticeTiming.MIDDLE), EPSILON);
    }

    @Test
    void solsticePositionsAreTheMiddlesOfMidSummerAndMidWinter() {
        // Twelve equal sub-seasons from Early Spring: Mid Summer is the 5th, Mid Winter the 11th.
        assertEquals(4.5 / 12, SolsticeTiming.MIDDLE.summerSolstice(), EPSILON);
        assertEquals(10.5 / 12, SolsticeTiming.MIDDLE.winterSolstice(), EPSILON);
        assertEquals(1.5 / 12, SolsticeTiming.MIDDLE.springEquinox(), EPSILON);
        assertEquals(7.5 / 12, SolsticeTiming.MIDDLE.autumnEquinox(), EPSILON);
    }

    @Test
    void asymmetricExtremesAreEachHonoured() {
        assertEquals(80.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.summerSolstice(), 80, 40, SolsticeTiming.MIDDLE), EPSILON);
        assertEquals(40.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.winterSolstice(), 80, 40, SolsticeTiming.MIDDLE), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.springEquinox(), 80, 40, SolsticeTiming.MIDDLE), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.autumnEquinox(), 80, 40, SolsticeTiming.MIDDLE), EPSILON);
    }

    @Test
    void everyEighthOfTheYearHasItsExpectedValue() {
        // Between the named points: 50 plus or minus the half-range times sin(45 degrees).
        double[] positions = {0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
        double[] at70and30 = {35.857864376269, 50, 64.142135623731, 70, 64.142135623731, 50, 35.857864376269, 30,
                35.857864376269};
        double[] at80and40 = {42.928932188135, 50, 71.213203435596, 80, 71.213203435596, 50, 42.928932188135, 40,
                42.928932188135};
        for (int i = 0; i < positions.length; i++) {
            assertEquals(at70and30[i], DaylightCurve.daylightPercent(positions[i], 70, 30, SolsticeTiming.MIDDLE), EPSILON,
                    "70 / 30 at " + positions[i]);
            assertEquals(at80and40[i], DaylightCurve.daylightPercent(positions[i], 80, 40, SolsticeTiming.MIDDLE), EPSILON,
                    "80 / 40 at " + positions[i]);
        }
    }

    @Test
    void theYearWrapsWithoutAJump() {
        assertEquals(DaylightCurve.daylightPercent(0.0, 70, 30, SolsticeTiming.MIDDLE), DaylightCurve.daylightPercent(1.0, 70, 30, SolsticeTiming.MIDDLE), EPSILON);
        double lateWinter = DaylightCurve.daylightPercent(0.999999, 70, 30, SolsticeTiming.MIDDLE);
        double earlySpring = DaylightCurve.daylightPercent(0.000001, 70, 30, SolsticeTiming.MIDDLE);
        assertTrue(Math.abs(lateWinter - earlySpring) < 0.001);
    }

    @Test
    void risesFromWinterToSummerAndFallsFromSummerToWinter() {
        double previous = DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.winterSolstice() - 1.0, 80, 40, SolsticeTiming.MIDDLE);
        for (int i = 1; i <= 500; i++) {
            double position = SolsticeTiming.MIDDLE.winterSolstice() - 1.0 + i * 0.5 / 500;
            double value = DaylightCurve.daylightPercent(position, 80, 40, SolsticeTiming.MIDDLE);
            assertTrue(value >= previous - EPSILON, "not rising at " + position);
            previous = value;
        }
        for (int i = 1; i <= 500; i++) {
            double position = SolsticeTiming.MIDDLE.summerSolstice() + i * 0.5 / 500;
            double value = DaylightCurve.daylightPercent(position, 80, 40, SolsticeTiming.MIDDLE);
            assertTrue(value <= previous + EPSILON, "not falling at " + position);
            previous = value;
        }
    }

    @Test
    void oneRefreshChangesLessThanATenthOfAPercentAtTheDefaultYear() {
        // Default Serene Seasons year with alignment: 12 sub-seasons x 7 days x 72000 ticks.
        double yearTicks = 12.0 * 7 * 72000;
        double step = 20 / yearTicks;
        double largest = 0;
        for (double position = 0; position < 1; position += step) {
            double change = Math.abs(DaylightCurve.daylightPercent(position + step, 70, 30, SolsticeTiming.MIDDLE)
                    - DaylightCurve.daylightPercent(position, 70, 30, SolsticeTiming.MIDDLE));
            largest = Math.max(largest, change);
        }
        assertTrue(largest < 0.1, "largest step " + largest);
    }

    @Test
    void realisticTimingPutsThePointsTwoThirdsThroughTheEarlySubSeasons() {
        SolsticeTiming realistic = SolsticeTiming.REALISTIC;
        // Sub-season n covers n/12 to (n+1)/12, Early Spring first: Early Summer is 3, Early Autumn 6, Early Winter 9.
        assertEquals((0 + 2.0 / 3) / 12, realistic.springEquinox(), EPSILON);
        assertEquals((3 + 2.0 / 3) / 12, realistic.summerSolstice(), EPSILON);
        assertEquals((6 + 2.0 / 3) / 12, realistic.autumnEquinox(), EPSILON);
        assertEquals((9 + 2.0 / 3) / 12, realistic.winterSolstice(), EPSILON);
        assertEquals(70.0, DaylightCurve.daylightPercent(11.0 / 36, 70, 30, realistic), EPSILON);
        assertEquals(30.0, DaylightCurve.daylightPercent(29.0 / 36, 70, 30, realistic), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(1.0 / 18, 70, 30, realistic), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(5.0 / 9, 70, 30, realistic), EPSILON);
        assertEquals(80.0, DaylightCurve.daylightPercent(11.0 / 36, 80, 40, realistic), EPSILON);
        assertEquals(40.0, DaylightCurve.daylightPercent(29.0 / 36, 80, 40, realistic), EPSILON);
    }

    @Test
    void realisticTimingRisesAndFalls() {
        SolsticeTiming realistic = SolsticeTiming.REALISTIC;
        double previous = DaylightCurve.daylightPercent(realistic.winterSolstice() - 1.0, 80, 40, realistic);
        for (int i = 1; i <= 500; i++) {
            double value = DaylightCurve.daylightPercent(realistic.winterSolstice() - 1.0 + i * 0.5 / 500, 80, 40, realistic);
            assertTrue(value >= previous - EPSILON, "not rising at step " + i);
            previous = value;
        }
        for (int i = 1; i <= 500; i++) {
            double value = DaylightCurve.daylightPercent(realistic.summerSolstice() + i * 0.5 / 500, 80, 40, realistic);
            assertTrue(value <= previous + EPSILON, "not falling at step " + i);
            previous = value;
        }
    }

    private static double averageOverSubSeason(int subSeason, SolsticeTiming timing) {
        double sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += DaylightCurve.daylightPercent((subSeason + (i + 0.5) / 1000) / 12, 70, 30, timing);
        }
        return sum / 1000;
    }

    @Test
    void realisticEarlySpringIsBrighterThanLateAutumn() {
        // In the real calendar late autumn is close to the winter solstice and early spring is further from it.
        double earlySpring = averageOverSubSeason(0, SolsticeTiming.REALISTIC);
        double lateAutumn = averageOverSubSeason(8, SolsticeTiming.REALISTIC);
        assertTrue(earlySpring > lateAutumn + 10, earlySpring + " vs " + lateAutumn);
    }

    @Test
    void middleKeepsThePositionsOfVersionOne() {
        assertEquals(0.125, SolsticeTiming.MIDDLE.springEquinox(), 0.0);
        assertEquals(0.375, SolsticeTiming.MIDDLE.summerSolstice(), 0.0);
        assertEquals(0.625, SolsticeTiming.MIDDLE.autumnEquinox(), 0.0);
        assertEquals(0.875, SolsticeTiming.MIDDLE.winterSolstice(), 0.0);
    }

    @Test
    void wordsAreReadWithoutRegardToCase() {
        assertEquals(SolsticeTiming.REALISTIC, SolsticeTiming.fromWord(" realistic "));
        assertEquals(SolsticeTiming.MIDDLE, SolsticeTiming.fromWord("Middle"));
        assertEquals(null, SolsticeTiming.fromWord("south"));
        assertEquals("MIDDLE or REALISTIC", SolsticeTiming.fileWordsText());
        assertEquals(java.util.Arrays.asList("middle", "realistic"), SolsticeTiming.commandWords());
    }

    @Test
    void seasonalSplitKeepsTheCycleLength() {
        CycleDurations summer = CycleDurations.seasonal(60, 70);
        assertEquals(42.0, summer.dayMinutes(), EPSILON);
        assertEquals(18.0, summer.nightMinutes(), EPSILON);
        CycleDurations equinox = CycleDurations.seasonal(60, 50);
        assertEquals(30.0, equinox.dayMinutes(), EPSILON);
        assertEquals(30.0, equinox.nightMinutes(), EPSILON);
        CycleDurations winter = CycleDurations.seasonal(60, 30);
        assertEquals(18.0, winter.dayMinutes(), EPSILON);
        assertEquals(42.0, winter.nightMinutes(), EPSILON);
    }

    @Test
    void seasonalSplitIgnoresTheBaseRatio() {
        // Base 40 / 20 has a cycle of 60; at an equinox the split is 30 / 30, not 40 / 20.
        CycleDurations equinox = CycleDurations.seasonal(40 + 20, DaylightCurve.daylightPercent(SolsticeTiming.MIDDLE.springEquinox(), 70, 30, SolsticeTiming.MIDDLE));
        assertEquals(30.0, equinox.dayMinutes(), 1e-6);
        assertEquals(30.0, equinox.nightMinutes(), 1e-6);
    }
}
