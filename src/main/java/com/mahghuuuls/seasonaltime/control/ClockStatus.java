package com.mahghuuuls.seasonaltime.control;

import com.mahghuuuls.seasonaltime.time.CycleDurations;

import java.util.Locale;

/**
 * What the time controller did for one dimension in its latest tick.
 *
 * <p>Immutable. The controller replaces it every tick it runs; commands and the debug log read it,
 * so they never need to know how the controller works.
 */
public final class ClockStatus {

    private final int dimension;
    private final long worldTime;
    private final double dayMinutes;
    private final double nightMinutes;
    private final double unitsPerTick;
    private final double remainder;

    public ClockStatus(int dimension, long worldTime, double dayMinutes, double nightMinutes, double unitsPerTick,
                double remainder) {
        this.dimension = dimension;
        this.worldTime = worldTime;
        this.dayMinutes = dayMinutes;
        this.nightMinutes = nightMinutes;
        this.unitsPerTick = unitsPerTick;
        this.remainder = remainder;
    }

    public int dimension() {
        return dimension;
    }

    public long worldTime() {
        return worldTime;
    }

    public long timeOfDay() {
        return CycleDurations.timeOfDay(worldTime);
    }

    public boolean isDay() {
        return CycleDurations.isDay(worldTime);
    }

    /** Effective real minutes of daytime in use. */
    public double dayMinutes() {
        return dayMinutes;
    }

    /** Effective real minutes of nighttime in use. */
    public double nightMinutes() {
        return nightMinutes;
    }

    /** The share of the cycle that is daytime, 0 to 100. */
    public double daylightPercent() {
        return 100.0 * dayMinutes / (dayMinutes + nightMinutes);
    }

    public double unitsPerTick() {
        return unitsPerTick;
    }

    public double remainder() {
        return remainder;
    }

    /** One line for the debug log. */
    public String summary() {
        return String.format(Locale.ROOT,
                "dim %d: worldTime=%d timeOfDay=%d segment=%s day=%.1fmin night=%.1fmin unitsPerTick=%.4f "
                        + "remainder=%.4f",
                dimension, worldTime, timeOfDay(), isDay() ? "DAY" : "NIGHT", dayMinutes, nightMinutes,
                unitsPerTick, remainder);
    }
}
