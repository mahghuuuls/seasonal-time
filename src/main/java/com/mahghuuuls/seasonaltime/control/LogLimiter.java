package com.mahghuuuls.seasonaltime.control;

/**
 * Lets a repeated debug line through at most once per interval of world ticks.
 *
 * <p>A debug line that reacts to something another mod or a command block may do every tick must
 * not print every tick. The first call always passes.
 */
final class LogLimiter {

    private final long intervalTicks;
    private boolean logged;
    private long lastLoggedAt;

    LogLimiter(long intervalTicks) {
        this.intervalTicks = intervalTicks;
    }

    /** True when a line may be logged at this total world time; records it as logged. */
    boolean allow(long now) {
        if (logged && now - lastLoggedAt < intervalTicks) {
            return false;
        }
        logged = true;
        lastLoggedAt = now;
        return true;
    }
}
