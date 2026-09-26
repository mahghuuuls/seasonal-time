package com.mahghuuuls.seasonaltime.integration.sereneseasons;

import com.mahghuuuls.seasonaltime.season.SeasonReading;
import com.mahghuuuls.seasonaltime.season.SeasonSource;
import net.minecraft.world.World;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig;

/**
 * Reads the season from Serene Seasons 1.2.18 for Minecraft 1.12.2.
 *
 * <p>The year position is the season counter divided by the cycle length, both from Serene
 * Seasons' own state, so it follows Serene Seasons' own settings (sub-season length, starting
 * sub-season) and moves in one-tick steps with no jumps between sub-seasons.
 *
 * <p>Serene Seasons' public API has no dimension whitelist check; its internal
 * {@code SeasonsConfig.isDimensionWhitelisted} is used for that, here and nowhere else.
 */
final class SereneSeasonsSeasonSource implements SeasonSource {

    @Override
    public boolean installed() {
        return true;
    }

    @Override
    public SeasonReading read(World world) {
        if (!SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension())) {
            return SeasonReading.UNAVAILABLE;
        }
        ISeasonState state = SeasonHelper.getSeasonState(world);
        if (state == null || state.getCycleDuration() <= 0) {
            return SeasonReading.UNAVAILABLE;
        }
        // The counter can reach the cycle length and one more before Serene Seasons wraps it; the
        // position wraps those to the start of Early Spring, which is where Serene Seasons puts them.
        double position = SeasonReading.yearPosition(state.getSeasonCycleTicks(), state.getCycleDuration());
        return SeasonReading.of(position, state.getSeason().name(), state.getSubSeason().name());
    }
}
