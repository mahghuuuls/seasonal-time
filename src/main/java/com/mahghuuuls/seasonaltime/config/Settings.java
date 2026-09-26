package com.mahghuuuls.seasonaltime.config;

import java.util.Arrays;

/**
 * One consistent set of configuration values.
 *
 * <p>Immutable. A reload or a command publishes a whole new instance, so a reader never sees half
 * of an update. Every value here has already been validated by {@link SeasonalTimeConfig}.
 */
public final class Settings {

    private final int baseDayDurationMinutes;
    private final int baseNightDurationMinutes;
    private final boolean seasonalAdjustmentEnabled;
    private final double summerSolsticeDaylightPercent;
    private final double winterSolsticeDaylightPercent;
    private final boolean alignSereneSeasonsDay;
    private final int[] controlledDimensions;
    private final boolean debug;

    Settings(int baseDayDurationMinutes, int baseNightDurationMinutes, boolean seasonalAdjustmentEnabled,
             double summerSolsticeDaylightPercent, double winterSolsticeDaylightPercent,
             boolean alignSereneSeasonsDay, int[] controlledDimensions, boolean debug) {
        this.baseDayDurationMinutes = baseDayDurationMinutes;
        this.baseNightDurationMinutes = baseNightDurationMinutes;
        this.seasonalAdjustmentEnabled = seasonalAdjustmentEnabled;
        this.summerSolsticeDaylightPercent = summerSolsticeDaylightPercent;
        this.winterSolsticeDaylightPercent = winterSolsticeDaylightPercent;
        this.alignSereneSeasonsDay = alignSereneSeasonsDay;
        this.controlledDimensions = controlledDimensions.clone();
        this.debug = debug;
    }

    /** Real minutes for world time 0 to 11999 when seasonal adjustment is not active. */
    public int baseDayDurationMinutes() {
        return baseDayDurationMinutes;
    }

    /** Real minutes for world time 12000 to 23999 when seasonal adjustment is not active. */
    public int baseNightDurationMinutes() {
        return baseNightDurationMinutes;
    }

    /** Base day plus base night: the real length of one full cycle, in minutes. */
    public int cycleLengthMinutes() {
        return baseDayDurationMinutes + baseNightDurationMinutes;
    }

    public boolean seasonalAdjustmentEnabled() {
        return seasonalAdjustmentEnabled;
    }

    public double summerSolsticeDaylightPercent() {
        return summerSolsticeDaylightPercent;
    }

    public double winterSolsticeDaylightPercent() {
        return winterSolsticeDaylightPercent;
    }

    public boolean alignSereneSeasonsDay() {
        return alignSereneSeasonsDay;
    }

    /** A copy, so callers cannot change the published settings. */
    public int[] controlledDimensions() {
        return controlledDimensions.clone();
    }

    public boolean isControlledDimension(int dimension) {
        for (int controlled : controlledDimensions) {
            if (controlled == dimension) {
                return true;
            }
        }
        return false;
    }

    public boolean debug() {
        return debug;
    }

    @Override
    public String toString() {
        return "Settings{day=" + baseDayDurationMinutes + ", night=" + baseNightDurationMinutes
                + ", seasonal=" + seasonalAdjustmentEnabled + ", summer=" + summerSolsticeDaylightPercent
                + ", winter=" + winterSolsticeDaylightPercent + ", align=" + alignSereneSeasonsDay
                + ", dimensions=" + Arrays.toString(controlledDimensions) + ", debug=" + debug + "}";
    }
}
