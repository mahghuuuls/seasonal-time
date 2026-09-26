package com.mahghuuuls.seasonaltime.time;

/**
 * How long day and night take in real time, and the world-time rate that follows from that.
 *
 * <p>This class owns the conversion from real minutes to world-time units per server tick. It
 * assumes the normal 20 ticks per second: a server running slower advances world time more slowly,
 * the same way vanilla does.
 *
 * <p>World time keeps its vanilla meaning: 0 to 11999 of each 24000 is day, 12000 to 23999 is
 * night. Only the real time spent in each half changes.
 */
public final class CycleDurations {

    public static final int TICKS_PER_MINUTE = 20 * 60;
    public static final long UNITS_PER_DAY = 24000L;
    public static final long UNITS_PER_SEGMENT = 12000L;

    private final double dayMinutes;
    private final double nightMinutes;

    private CycleDurations(double dayMinutes, double nightMinutes) {
        if (!(dayMinutes > 0) || !(nightMinutes > 0)) {
            throw new IllegalArgumentException("durations must be positive: day=" + dayMinutes
                    + ", night=" + nightMinutes);
        }
        this.dayMinutes = dayMinutes;
        this.nightMinutes = nightMinutes;
    }

    /** Day and night exactly as configured, used when seasonal adjustment is not active. */
    public static CycleDurations base(int dayMinutes, int nightMinutes) {
        return new CycleDurations(dayMinutes, nightMinutes);
    }

    /** Effective real minutes of daytime. */
    public double dayMinutes() {
        return dayMinutes;
    }

    /** Effective real minutes of nighttime. */
    public double nightMinutes() {
        return nightMinutes;
    }

    /** True for world time 0-11999 of each day, including negative world times. */
    public static boolean isDay(long worldTime) {
        return timeOfDay(worldTime) < UNITS_PER_SEGMENT;
    }

    /** World time within its day, 0 to 23999, also for negative or very large values. */
    public static long timeOfDay(long worldTime) {
        return Math.floorMod(worldTime, UNITS_PER_DAY);
    }

    /**
     * The first world time of the next day, the next multiple of 24000 after {@code worldTime}.
     *
     * <p>This is what vanilla sets after everyone has slept, for every world time from -24000 up.
     * Below that, vanilla's formula rounds toward zero and skips further ahead; such times cannot
     * be reached with vanilla commands, and the true next day is used here.
     */
    public static long nextMorning(long worldTime) {
        return worldTime - timeOfDay(worldTime) + UNITS_PER_DAY;
    }

    /** World-time units to add per server tick at this world time. */
    public double unitsPerTick(long worldTime) {
        double minutes = isDay(worldTime) ? dayMinutes : nightMinutes;
        return UNITS_PER_SEGMENT / (minutes * TICKS_PER_MINUTE);
    }
}
