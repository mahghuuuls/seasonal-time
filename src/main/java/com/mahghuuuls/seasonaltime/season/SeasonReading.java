package com.mahghuuuls.seasonaltime.season;

/**
 * Where in the seasonal year a world is, as far as the season provider knows.
 *
 * <p>Immutable. {@link #UNAVAILABLE} stands for "no season here": the provider is missing, does
 * not cover this dimension, failed, or is suspended.
 */
public final class SeasonReading {

    public static final SeasonReading UNAVAILABLE = new SeasonReading(false, 0.0, null, null);

    private final boolean available;
    private final double yearPosition;
    private final String season;
    private final String subSeason;

    private SeasonReading(boolean available, double yearPosition, String season, String subSeason) {
        this.available = available;
        this.yearPosition = yearPosition;
        this.season = season;
        this.subSeason = subSeason;
    }

    /**
     * @param yearPosition 0 inclusive to 1 exclusive; 0 is the start of Early Spring
     * @param season       display name, e.g. "SUMMER"
     * @param subSeason    display name, e.g. "MID_SUMMER"
     */
    public static SeasonReading of(double yearPosition, String season, String subSeason) {
        return new SeasonReading(true, yearPosition, season, subSeason);
    }

    /**
     * The year position of a season counter in a year of {@code yearTicks} ticks. The counter may
     * be negative or run past the end of the year; it wraps, so the result is always 0 inclusive to
     * 1 exclusive.
     */
    public static double yearPosition(long counter, long yearTicks) {
        if (yearTicks <= 0) {
            throw new IllegalArgumentException("yearTicks must be positive: " + yearTicks);
        }
        return Math.floorMod(counter, yearTicks) / (double) yearTicks;
    }

    public boolean available() {
        return available;
    }

    public double yearPosition() {
        return yearPosition;
    }

    /** Null when unavailable. */
    public String season() {
        return season;
    }

    /** Null when unavailable. */
    public String subSeason() {
        return subSeason;
    }
}
