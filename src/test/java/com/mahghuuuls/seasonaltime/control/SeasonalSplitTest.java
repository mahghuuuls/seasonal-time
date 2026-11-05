package com.mahghuuuls.seasonaltime.control;

import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.season.GuardedSeasonSource;
import com.mahghuuuls.seasonaltime.season.SeasonReading;
import com.mahghuuuls.seasonaltime.season.SeasonSource;
import com.mahghuuuls.seasonaltime.time.CycleDurations;
import com.mahghuuuls.seasonaltime.time.DaylightCurve;
import com.mahghuuuls.seasonaltime.time.SolsticeTiming;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.TestMinecraftHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which day and night lengths a clock uses for each combination of settings and season. */
class SeasonalSplitTest {

    private static final double EPSILON = 1e-6;
    private static final SeasonReading MID_SPRING = SeasonReading.of(SolsticeTiming.MIDDLE.springEquinox(), "SPRING", "MID_SPRING");
    private static final SeasonReading MID_SUMMER = SeasonReading.of(SolsticeTiming.MIDDLE.summerSolstice(), "SUMMER", "MID_SUMMER");
    private static final SeasonReading MID_WINTER = SeasonReading.of(SolsticeTiming.MIDDLE.winterSolstice(), "WINTER", "MID_WINTER");

    @TempDir
    File directory;

    private SeasonalTimeConfig config;

    @BeforeEach
    void defaultSettings() {
        TestMinecraftHome.setIfAbsent(directory);
        config = new SeasonalTimeConfig(new File(directory, "seasonaltime.cfg"), message -> { });
        config.load();
    }

    private static void assertLengths(double day, double night, CycleDurations durations) {
        assertEquals(day, durations.dayMinutes(), EPSILON, "day");
        assertEquals(night, durations.nightMinutes(), EPSILON, "night");
    }

    @Test
    void activeOnlyWhenSwitchedOnAndASeasonIsKnown() {
        assertTrue(SeasonalSplit.active(config.settings(), MID_SUMMER));
        assertFalse(SeasonalSplit.active(config.settings(), SeasonReading.UNAVAILABLE));
        config.setSeasonalAdjustmentEnabled(false);
        assertFalse(SeasonalSplit.active(config.settings(), MID_SUMMER));
    }

    @Test
    void theSolsticesSplitTheDefaultCycle() {
        // Defaults: day 30, night 30, summer 70, winter 30.
        assertLengths(42.0, 18.0, SeasonalSplit.durations(config.settings(), MID_SUMMER));
        assertLengths(18.0, 42.0, SeasonalSplit.durations(config.settings(), MID_WINTER));
    }

    @Test
    void theRealisticTimingFromTheSettingsMovesTheSummerPeak() {
        config.setSolsticeTiming("realistic");
        SeasonReading realisticPeak = SeasonReading.of(SolsticeTiming.REALISTIC.summerSolstice(), "SUMMER", "EARLY_SUMMER");
        assertLengths(42.0, 18.0, SeasonalSplit.durations(config.settings(), realisticPeak));
        assertTrue(SeasonalSplit.durations(config.settings(), MID_SUMMER).dayMinutes() < 42.0 - EPSILON,
                "the middle of Mid Summer is past the realistic peak");
    }

    @Test
    void anEquinoxSplitsAnUnevenBaseEvenly() {
        config.setBaseDayDurationMinutes("40");
        config.setBaseNightDurationMinutes("20");
        assertLengths(30.0, 30.0, SeasonalSplit.durations(config.settings(), MID_SPRING));
    }

    @Test
    void withoutASeasonOrWithTheSwitchOffTheBaseLengthsApply() {
        config.setBaseDayDurationMinutes("40");
        config.setBaseNightDurationMinutes("20");
        assertLengths(40.0, 20.0, SeasonalSplit.durations(config.settings(), SeasonReading.UNAVAILABLE));
        config.setSeasonalAdjustmentEnabled(false);
        assertLengths(40.0, 20.0, SeasonalSplit.durations(config.settings(), MID_SUMMER));
    }

    @Test
    void aFailingSeasonSourceGivesTheBaseLengths() {
        config.setBaseDayDurationMinutes("40");
        config.setBaseNightDurationMinutes("20");
        GuardedSeasonSource failing = new GuardedSeasonSource(new SeasonSource() {
            @Override
            public boolean installed() {
                return true;
            }

            @Override
            public SeasonReading read(World world) {
                throw new IllegalStateException("season mod broke");
            }
        }, (message, cause) -> { });
        SeasonReading reading = failing.read(null);
        assertFalse(SeasonalSplit.active(config.settings(), reading));
        assertLengths(40.0, 20.0, SeasonalSplit.durations(config.settings(), reading));
    }
}
