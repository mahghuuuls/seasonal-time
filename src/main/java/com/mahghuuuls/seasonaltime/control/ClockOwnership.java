package com.mahghuuuls.seasonaltime.control;

import net.minecraft.world.World;
import net.minecraft.world.storage.DerivedWorldInfo;

/**
 * Decides which worlds share a clock.
 *
 * <p>In 1.12.2 every dimension other than the Overworld normally uses {@link DerivedWorldInfo},
 * which reads the Overworld's world time and ignores its own {@code setWorldTime}. Such a world
 * follows the Overworld clock: it must never be advanced separately, and its players must receive
 * the Overworld's time.
 */
final class ClockOwnership {

    static final int OVERWORLD = 0;

    private ClockOwnership() {
    }

    /**
     * True when Seasonal Time advances this world's clock. For now only the Overworld; configured
     * dimensions with their own clock are added later.
     */
    static boolean isControlled(World world) {
        return world.provider.getDimension() == OVERWORLD;
    }

    /** True when this world keeps its own clock rather than following the Overworld's. */
    static boolean ownsClock(World world) {
        return world.provider.getDimension() == OVERWORLD || !(world.getWorldInfo() instanceof DerivedWorldInfo);
    }

    /** True when {@code world} shows the time of {@code owner}'s clock. */
    static boolean sharesClockOf(World world, World owner) {
        if (world == owner) {
            return true;
        }
        return owner.provider.getDimension() == OVERWORLD && !ownsClock(world);
    }
}
