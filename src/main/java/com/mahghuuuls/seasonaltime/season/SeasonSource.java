package com.mahghuuuls.seasonaltime.season;

import net.minecraft.world.World;

/**
 * Where the time controller learns the season. The only seam between the controller and any
 * season mod: the controller never sees a season mod's own types.
 */
public interface SeasonSource {

    /** Whether a season mod is installed at all, for diagnostics. */
    boolean installed();

    /**
     * The season of this world now. Returns {@link SeasonReading#UNAVAILABLE} when there is none.
     * May throw when the season mod fails; callers go through {@link GuardedSeasonSource}.
     */
    SeasonReading read(World world);
}
