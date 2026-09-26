package com.mahghuuuls.seasonaltime.integration.sereneseasons;

import com.mahghuuuls.seasonaltime.season.DayAlignment;
import com.mahghuuuls.seasonaltime.season.SeasonSource;

import java.util.function.BiConsumer;

/**
 * The one entry point from common code into the Serene Seasons integration.
 *
 * <p>Call only after {@code Loader.isModLoaded("sereneseasons")} is true. This class names no
 * Serene Seasons type in its signatures, so loading it is safe; the Serene Seasons classes are
 * loaded only when a season is read or the day length is set.
 */
public final class SereneSeasonsIntegration {

    public static final String MOD_ID = "sereneseasons";

    private SereneSeasonsIntegration() {
    }

    public static SeasonSource seasonSource() {
        return new SereneSeasonsSeasonSource();
    }

    /**
     * One per side: the server and a player's game each set their own Serene Seasons day length.
     *
     * @param problems receives a message and, for a failure, its cause (null for a plain warning)
     */
    public static DayAlignment dayAlignment(BiConsumer<String, Throwable> problems) {
        return new SereneSeasonsDayAlignment(new SereneSeasonsDaySlot(), problems);
    }
}
