package com.mahghuuuls.seasonaltime.season;

import com.mahghuuuls.seasonaltime.time.DaylightCurve;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Turning a season counter into a year position. */
class SeasonReadingTest {

    private static final long SERENE_SEASONS_DEFAULT_DAY_TICKS = 24000;

    private static double percentAt(long counter, long yearTicks) {
        return DaylightCurve.daylightPercent(SeasonReading.yearPosition(counter, yearTicks), 70, 30);
    }

    @Test
    void theMiddlesOfTheMidSubSeasonsDoNotDependOnTheSubSeasonLength() {
        // Serene Seasons: sub-season n covers the counter from n sub-season lengths to the next one,
        // Early Spring first, and the year is 12 sub-seasons. Mid Spring is 1, Mid Summer 4,
        // Mid Autumn 7, Mid Winter 10.
        for (int days : new int[]{1, 3, 14}) {
            long subSeason = days * SERENE_SEASONS_DEFAULT_DAY_TICKS;
            long year = 12 * subSeason;
            long half = subSeason / 2;
            assertEquals(50.0, percentAt(subSeason + half, year), 1e-6, days + "-day sub-seasons, Mid Spring");
            assertEquals(70.0, percentAt(4 * subSeason + half, year), 1e-6, days + "-day sub-seasons, Mid Summer");
            assertEquals(50.0, percentAt(7 * subSeason + half, year), 1e-6, days + "-day sub-seasons, Mid Autumn");
            assertEquals(30.0, percentAt(10 * subSeason + half, year), 1e-6, days + "-day sub-seasons, Mid Winter");
        }
    }

    @Test
    void theCounterWrapsAtTheEndOfTheYear() {
        // Serene Seasons lets its counter reach the year length and one more before it wraps.
        long year = 12 * 3 * SERENE_SEASONS_DEFAULT_DAY_TICKS;
        assertEquals(0.0, SeasonReading.yearPosition(0, year));
        assertEquals(0.0, SeasonReading.yearPosition(year, year));
        assertEquals(1.0 / year, SeasonReading.yearPosition(year + 1, year), 1e-15);
        assertEquals((year - 1) / (double) year, SeasonReading.yearPosition(-1, year), 1e-15);
        assertTrue(SeasonReading.yearPosition(year - 1, year) < 1.0);
    }

    @Test
    void aYearWithoutTicksIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SeasonReading.yearPosition(5, 0));
    }
}
