package com.mahghuuuls.seasonaltime.control;

import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.season.SeasonReading;
import com.mahghuuuls.seasonaltime.time.CycleDurations;
import com.mahghuuuls.seasonaltime.time.DaylightCurve;

/** Chooses a clock's day and night lengths from the settings and the season (REQ-021, REQ-024). */
final class SeasonalSplit {

    private SeasonalSplit() {
    }

    /**
     * Seasonal adjustment applies only when it is switched on and the season source has a season
     * for this world. "No season" covers a missing or failed season mod and a dimension without seasons.
     */
    static boolean active(Settings settings, SeasonReading season) {
        return settings.seasonalAdjustmentEnabled() && season.available();
    }

    /** The seasonal split of the cycle length when active, otherwise the base lengths. */
    static CycleDurations durations(Settings settings, SeasonReading season) {
        if (!active(settings, season)) {
            return CycleDurations.base(settings.baseDayDurationMinutes(), settings.baseNightDurationMinutes());
        }
        return CycleDurations.seasonal(settings.cycleLengthMinutes(), DaylightCurve.daylightPercent(
                season.yearPosition(), settings.summerSolsticeDaylightPercent(),
                settings.winterSolsticeDaylightPercent()));
    }
}
