package com.mahghuuuls.seasonaltime.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seasonal daylight curve and the day and night split it produces. */
class DaylightCurveTest {

    private static final double EPSILON = 1e-9;

    @Test
    void solsticesAndEquinoxesHitTheirValues() {
        assertEquals(70.0, DaylightCurve.daylightPercent(DaylightCurve.SUMMER_SOLSTICE, 70, 30), EPSILON);
        assertEquals(30.0, DaylightCurve.daylightPercent(DaylightCurve.WINTER_SOLSTICE, 70, 30), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(DaylightCurve.SPRING_EQUINOX, 70, 30), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(DaylightCurve.AUTUMN_EQUINOX, 70, 30), EPSILON);
    }

    @Test
    void solsticePositionsAreTheMiddlesOfMidSummerAndMidWinter() {
        // Twelve equal sub-seasons from Early Spring: Mid Summer is the 5th, Mid Winter the 11th.
        assertEquals(4.5 / 12, DaylightCurve.SUMMER_SOLSTICE, EPSILON);
        assertEquals(10.5 / 12, DaylightCurve.WINTER_SOLSTICE, EPSILON);
        assertEquals(1.5 / 12, DaylightCurve.SPRING_EQUINOX, EPSILON);
        assertEquals(7.5 / 12, DaylightCurve.AUTUMN_EQUINOX, EPSILON);
    }

    @Test
    void asymmetricExtremesAreEachHonoured() {
        assertEquals(80.0, DaylightCurve.daylightPercent(DaylightCurve.SUMMER_SOLSTICE, 80, 40), EPSILON);
        assertEquals(40.0, DaylightCurve.daylightPercent(DaylightCurve.WINTER_SOLSTICE, 80, 40), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(DaylightCurve.SPRING_EQUINOX, 80, 40), EPSILON);
        assertEquals(50.0, DaylightCurve.daylightPercent(DaylightCurve.AUTUMN_EQUINOX, 80, 40), EPSILON);
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
            assertEquals(at70and30[i], DaylightCurve.daylightPercent(positions[i], 70, 30), EPSILON,
                    "70 / 30 at " + positions[i]);
            assertEquals(at80and40[i], DaylightCurve.daylightPercent(positions[i], 80, 40), EPSILON,
                    "80 / 40 at " + positions[i]);
        }
    }

    @Test
    void theYearWrapsWithoutAJump() {
        assertEquals(DaylightCurve.daylightPercent(0.0, 70, 30), DaylightCurve.daylightPercent(1.0, 70, 30), EPSILON);
        double lateWinter = DaylightCurve.daylightPercent(0.999999, 70, 30);
        double earlySpring = DaylightCurve.daylightPercent(0.000001, 70, 30);
        assertTrue(Math.abs(lateWinter - earlySpring) < 0.001);
    }

    @Test
    void risesFromWinterToSummerAndFallsFromSummerToWinter() {
        double previous = DaylightCurve.daylightPercent(DaylightCurve.WINTER_SOLSTICE - 1.0, 80, 40);
        for (int i = 1; i <= 500; i++) {
            double position = DaylightCurve.WINTER_SOLSTICE - 1.0 + i * 0.5 / 500;
            double value = DaylightCurve.daylightPercent(position, 80, 40);
            assertTrue(value >= previous - EPSILON, "not rising at " + position);
            previous = value;
        }
        for (int i = 1; i <= 500; i++) {
            double position = DaylightCurve.SUMMER_SOLSTICE + i * 0.5 / 500;
            double value = DaylightCurve.daylightPercent(position, 80, 40);
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
            double change = Math.abs(DaylightCurve.daylightPercent(position + step, 70, 30)
                    - DaylightCurve.daylightPercent(position, 70, 30));
            largest = Math.max(largest, change);
        }
        assertTrue(largest < 0.1, "largest step " + largest);
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
        CycleDurations equinox = CycleDurations.seasonal(40 + 20, DaylightCurve.daylightPercent(DaylightCurve.SPRING_EQUINOX, 70, 30));
        assertEquals(30.0, equinox.dayMinutes(), 1e-6);
        assertEquals(30.0, equinox.nightMinutes(), 1e-6);
    }
}
