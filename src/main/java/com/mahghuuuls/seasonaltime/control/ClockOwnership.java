package com.mahghuuuls.seasonaltime.control;

import com.mahghuuuls.seasonaltime.config.Settings;
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
     * True when Seasonal Time advances this world's clock: the dimension is listed in
     * {@code controlledDimensions} and keeps its own clock. A listed dimension that follows the
     * Overworld is never advanced separately; it moves with the Overworld.
     */
    static boolean isControlled(World world, Settings settings) {
        return role(world.provider.getDimension(), ownsClock(world), settings) == ClockRole.CONTROLLED;
    }

    /**
     * The whole role rule, without Minecraft types: a listed dimension with its own clock is
     * controlled; a dimension on the Overworld's clock follows it while the Overworld is controlled;
     * anything else is left alone.
     */
    static ClockRole role(int dimension, boolean ownsClock, Settings settings) {
        if (ownsClock && settings.isControlledDimension(dimension)) {
            return ClockRole.CONTROLLED;
        }
        if (!ownsClock && isOverworldControlled(settings)) {
            return ClockRole.FOLLOWS_OVERWORLD;
        }
        return ClockRole.NOT_CONTROLLED;
    }

    /** True when the Overworld's clock, and with it every dimension on that clock, is controlled. */
    static boolean isOverworldControlled(Settings settings) {
        return settings.isControlledDimension(OVERWORLD);
    }

    /** True when a listed dimension shares the Overworld's clock, so listing it has no effect of its own. */
    static boolean isListedButFollowsOverworld(World world, Settings settings) {
        return settings.isControlledDimension(world.provider.getDimension()) && !ownsClock(world);
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
