package com.mahghuuuls.seasonaltime.time;

/**
 * The share of the cycle that is daytime at a point in the seasonal year.
 *
 * <p>The year position runs from 0 at the start of Early Spring to 1 at the end of Late Winter,
 * in twelve equal sub-seasons. The summer solstice is the middle of Mid Summer (0.375), the winter
 * solstice the middle of Mid Winter (0.875), and the equinoxes the middles of Mid Spring (0.125)
 * and Mid Autumn (0.625). Between them the value follows a sine wave, so it changes smoothly and
 * only in one direction between a solstice and the next equinox.
 *
 * <p>Summer and winter need not be symmetric: the half of the wave above 50 is scaled to the
 * summer value and the half below to the winter value. Both halves are zero at the equinoxes, so
 * the curve stays continuous there. Its slope changes at an equinox when the two sides differ,
 * which is not visible because the value itself does not jump.
 */
public final class DaylightCurve {

    public static final double SPRING_EQUINOX = 0.125;
    public static final double SUMMER_SOLSTICE = 0.375;
    public static final double AUTUMN_EQUINOX = 0.625;
    public static final double WINTER_SOLSTICE = 0.875;

    private DaylightCurve() {
    }

    /**
     * @param yearPosition  0 to 1; values outside wrap around the year
     * @param summerPercent daylight percentage at the summer solstice, above 50
     * @param winterPercent daylight percentage at the winter solstice, below 50
     * @return daylight percentage, between winter and summer
     */
    public static double daylightPercent(double yearPosition, double summerPercent, double winterPercent) {
        double wave = Math.sin(2.0 * Math.PI * (yearPosition - SPRING_EQUINOX));
        return wave >= 0 ? 50.0 + wave * (summerPercent - 50.0) : 50.0 + wave * (50.0 - winterPercent);
    }
}
