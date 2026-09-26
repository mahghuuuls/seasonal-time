package com.mahghuuuls.seasonaltime.integration.sereneseasons;

import com.mahghuuuls.seasonaltime.season.DayAlignment;

import java.util.function.BiConsumer;

/**
 * Makes one Serene Seasons day a given number of ticks long on the side it runs on, and gives
 * Serene Seasons back its own setting (REQ-025, ARC-005).
 *
 * <p>Serene Seasons keeps each synced setting as an entry that holds the value in use and the value
 * read from its config file. Only the value in use is written, so the config file value is never
 * lost and never needs to be remembered here. When Serene Seasons' config is edited in game, it
 * replaces the entry with the file value; the next {@link #update} writes the aligned value again.
 *
 * <p>No call throws: the first failure to read or write the entry is reported with its cause and
 * turns alignment off until restart. This class names no Serene Seasons type: {@link Slot} reads and
 * writes the entry, so the logic is tested without Serene Seasons. Each side calls its own instance
 * from one thread.
 */
final class SereneSeasonsDayAlignment implements DayAlignment {

    /** Serene Seasons' year is 12 sub-seasons; it multiplies day ticks, sub-season days, and 12 in an int. */
    static final int SUB_SEASONS = 12;

    /** Serene Seasons' Day Duration entry. */
    interface Slot {

        /** The value in use, or null when Serene Seasons has no such entry. */
        String value();

        /** The value from Serene Seasons' config file, or null when Serene Seasons has no such entry. */
        String fileValue();

        /** Sets the value in use; does nothing when Serene Seasons has no such entry. */
        void write(String value);

        /** Serene Seasons' sub-season length in days. */
        int subSeasonDays();
    }

    private final Slot slot;
    /** Receives a message and, for a failure, its cause; null for a warning without one. */
    private final BiConsumer<String, Throwable> problems;
    /** True while the value in use is this side's aligned value rather than Serene Seasons' own. */
    private boolean overriding;
    /** The day length last refused as too long, so the warning is written once per value. */
    private int refusedDayTicks;
    private boolean broken;

    SereneSeasonsDayAlignment(Slot slot, BiConsumer<String, Throwable> problems) {
        this.slot = slot;
        this.problems = problems;
    }

    @Override
    public boolean installed() {
        return true;
    }

    @Override
    public boolean aligned() {
        return overriding;
    }

    @Override
    public boolean update(int dayTicks) {
        if (broken) {
            return false;
        }
        try {
            return dayTicks > 0 ? align(dayTicks) : restore();
        } catch (RuntimeException | LinkageError failure) {
            fail(failure);
            return false;
        }
    }

    private boolean align(int dayTicks) {
        int subSeasonDays = slot.subSeasonDays();
        if ((long) dayTicks * subSeasonDays * SUB_SEASONS > Integer.MAX_VALUE) {
            if (refusedDayTicks != dayTicks) {
                refusedDayTicks = dayTicks;
                problems.accept("Serene Seasons day alignment skipped: a " + dayTicks + "-tick day with "
                        + subSeasonDays + "-day sub-seasons makes a year longer than Serene Seasons can count; "
                        + "Serene Seasons uses its own Day Duration setting", null);
            }
            restore();
            return false;
        }
        refusedDayTicks = 0;
        String wanted = Integer.toString(dayTicks);
        if (!wanted.equals(slot.value())) {
            slot.write(wanted);
        }
        overriding = true;
        return true;
    }

    private boolean restore() {
        if (overriding) {
            String own = slot.fileValue();
            if (own != null) {
                slot.write(own);
            }
            overriding = false;
        }
        return true;
    }

    @Override
    public int dayTicks() {
        return read(true);
    }

    @Override
    public int ownDayTicks() {
        return read(false);
    }

    private int read(boolean inUse) {
        if (broken) {
            return 0;
        }
        try {
            return ticks(inUse ? slot.value() : slot.fileValue());
        } catch (RuntimeException | LinkageError failure) {
            fail(failure);
            return 0;
        }
    }

    private void fail(Throwable failure) {
        broken = true;
        overriding = false;
        problems.accept("Reading or changing the Serene Seasons day length failed; Serene Seasons day alignment "
                + "is off until restart", failure);
    }

    private static int ticks(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }
}
