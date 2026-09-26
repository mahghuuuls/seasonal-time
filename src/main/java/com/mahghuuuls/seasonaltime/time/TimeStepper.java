package com.mahghuuuls.seasonaltime.time;

/**
 * Turns a fractional rate into whole world-time steps without losing the fractions.
 *
 * <p>World time only moves in whole units, but a long day needs less than one unit per tick. The
 * part that does not make a whole unit yet is carried to the next tick, so over a full cycle the
 * total matches the rate exactly and there are no periodic jumps from lost fractions.
 */
public final class TimeStepper {

    private double remainder;

    /**
     * Adds one tick at the given rate and returns how many whole units world time should move.
     *
     * @param unitsPerTick a finite rate of zero or more
     */
    public long step(double unitsPerTick) {
        if (!(unitsPerTick >= 0) || Double.isInfinite(unitsPerTick)) {
            throw new IllegalArgumentException("rate must be finite and not negative: " + unitsPerTick);
        }
        double total = remainder + unitsPerTick;
        long whole = (long) Math.floor(total);
        remainder = total - whole;
        return whole;
    }

    /**
     * Advances one tick of a clock at {@code worldTime}: the rate is that of the segment the clock
     * is in before this step.
     *
     * @return the whole units to add to world time this tick
     */
    public long stepAt(CycleDurations durations, long worldTime) {
        return step(durations.unitsPerTick(worldTime));
    }

    /** Drops the carried fraction, for when world time was set from outside. */
    public void reset() {
        remainder = 0.0;
    }

    /** The carried fraction, 0 inclusive to 1 exclusive. */
    public double remainder() {
        return remainder;
    }
}
