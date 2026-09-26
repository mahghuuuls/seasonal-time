package com.mahghuuuls.seasonaltime.season;

/**
 * Sets a season mod's day length on the side it runs on (Serene Seasons day alignment, REQ-025).
 *
 * <p>The only seam between the alignment sync and a season mod's settings: common code never sees
 * the season mod's types. Each side (server thread, client main thread) uses its own instance.
 */
public interface DayAlignment {

    /** For a game without a season mod: there is nothing to align. */
    DayAlignment NONE = new DayAlignment() {
        @Override
        public boolean installed() {
            return false;
        }

        @Override
        public boolean aligned() {
            return false;
        }

        @Override
        public boolean update(int dayTicks) {
            return false;
        }

        @Override
        public int dayTicks() {
            return 0;
        }

        @Override
        public int ownDayTicks() {
            return 0;
        }
    };

    boolean installed();

    /** Whether this side's aligned value is in place now. */
    boolean aligned();

    /**
     * Makes one season-mod day {@code dayTicks} ticks long. With 0, gives the season mod back its
     * own setting if this side changed it, and otherwise leaves it alone.
     *
     * @return whether the requested state is in place; false when it cannot be applied, which is
     *         logged once
     */
    boolean update(int dayTicks);

    /** The season mod's day length in use, in ticks; 0 when unknown. Never throws. */
    int dayTicks();

    /** The day length in the season mod's own config, in ticks; 0 when unknown. Never throws. */
    int ownDayTicks();
}
