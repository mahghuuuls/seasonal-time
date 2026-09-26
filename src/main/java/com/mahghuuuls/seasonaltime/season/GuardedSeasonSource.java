package com.mahghuuuls.seasonaltime.season;

import net.minecraft.world.World;

/**
 * Keeps a failing season mod from breaking the time controller.
 *
 * <p>A failed read counts as "no season" for that refresh, so the base day and night lengths
 * apply. The first failure is logged with its cause. After {@link #SUSPEND_AFTER} failures in a
 * row the source is suspended, which is logged once, until {@link #resume()} (a config reload or a
 * restart). A successful read resets the count. Called once per second per controlled world, never
 * every tick.
 */
public final class GuardedSeasonSource implements SeasonSource {

    public static final int SUSPEND_AFTER = 5;

    /** Receives a message and, for the first failure, its cause. */
    public interface Problems {
        void report(String message, Throwable cause);
    }

    private final SeasonSource delegate;
    private final Problems problems;
    private int failuresInARow;
    private boolean loggedFirst;
    private boolean suspended;

    public GuardedSeasonSource(SeasonSource delegate, Problems problems) {
        this.delegate = delegate;
        this.problems = problems;
    }

    @Override
    public boolean installed() {
        return delegate.installed();
    }

    public boolean suspended() {
        return suspended;
    }

    @Override
    public SeasonReading read(World world) {
        if (suspended) {
            return SeasonReading.UNAVAILABLE;
        }
        try {
            SeasonReading reading = delegate.read(world);
            failuresInARow = 0;
            return reading;
        } catch (RuntimeException | LinkageError failure) {
            failuresInARow++;
            if (!loggedFirst) {
                loggedFirst = true;
                problems.report("Reading the season failed; using the base day and night lengths", failure);
            }
            if (failuresInARow >= SUSPEND_AFTER) {
                suspended = true;
                problems.report("Reading the season failed " + SUSPEND_AFTER + " times in a row; seasonal adjustment "
                        + "is suspended until /seasonaltime reload or a restart", null);
            }
            return SeasonReading.UNAVAILABLE;
        }
    }

    /** Lifts a suspension and starts counting again; the next failure is logged again. */
    public void resume() {
        suspended = false;
        failuresInARow = 0;
        loggedFirst = false;
    }
}
