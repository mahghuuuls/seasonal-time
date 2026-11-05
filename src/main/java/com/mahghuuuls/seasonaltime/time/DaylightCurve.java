package com.mahghuuuls.seasonaltime.time;

/**
 * The share of the cycle that is daytime at a point in the seasonal year.
 *
 * <p>The year position runs from 0 at the start of Early Spring to 1 at the end of Late Winter,
 * in twelve equal sub-seasons. {@link SolsticeTiming} says where the solstices and equinoxes fall.
 * Between them the value follows a sine wave, so it changes smoothly and only in one direction
 * between a solstice and the next equinox. Both timings use the same wave; only its start moves.
 *
 * <p>Summer and winter need not be symmetric: the half of the wave above 50 is scaled to the
 * summer value and the half below to the winter value. Both halves are zero at the equinoxes, so
 * the curve stays continuous there. Its slope changes at an equinox when the two sides differ,
 * which is not visible because the value itself does not jump.
 */
public final class DaylightCurve {

    private DaylightCurve() {
    }

    /**
     * @param yearPosition  0 to 1; values outside wrap around the year
     * @param summerPercent daylight percentage at the summer solstice, above 50
     * @param winterPercent daylight percentage at the winter solstice, below 50
     * @param timing        where the solstices and equinoxes fall
     * @return daylight percentage, between winter and summer
     */
    public static double daylightPercent(double yearPosition, double summerPercent, double winterPercent,
                                         SolsticeTiming timing) {
        double wave = Math.sin(2.0 * Math.PI * (yearPosition - timing.springEquinox()));
        return wave >= 0 ? 50.0 + wave * (summerPercent - 50.0) : 50.0 + wave * (50.0 - winterPercent);
    }
}
