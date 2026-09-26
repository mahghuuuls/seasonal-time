package com.mahghuuuls.seasonaltime.season;

import net.minecraft.world.World;

/** Used when no season mod is installed: never a season. */
public final class NoSeasonSource implements SeasonSource {

    @Override
    public boolean installed() {
        return false;
    }

    @Override
    public SeasonReading read(World world) {
        return SeasonReading.UNAVAILABLE;
    }
}
