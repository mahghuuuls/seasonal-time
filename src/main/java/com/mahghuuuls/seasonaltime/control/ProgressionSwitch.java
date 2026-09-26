package com.mahghuuuls.seasonaltime.control;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Owns the meaning of the vanilla {@code doDaylightCycle} rule while Seasonal Time runs.
 *
 * <p>Vanilla must never advance world time itself, or it and Seasonal Time would both move the
 * clock in the same tick. So the rule stays false; if anything turns it on, it is turned off again
 * before vanilla's world tick reads it.
 */
public final class ProgressionSwitch {

    static final String DAYLIGHT_RULE = "doDaylightCycle";

    private final Logger logger;
    /** Per dimension, because each world counts its own total ticks. */
    private final Map<Integer, LogLimiter> ruleLogs = new HashMap<Integer, LogLimiter>();

    public ProgressionSwitch(Logger logger) {
        this.logger = logger;
    }

    /**
     * Makes sure vanilla will not advance world time in this world's tick. The debug line is
     * limited to one a minute, in case something keeps turning the rule on.
     */
    void holdVanillaCycleOff(World world, boolean debug) {
        GameRules rules = world.getGameRules();
        if (rules.getBoolean(DAYLIGHT_RULE)) {
            rules.setOrCreateGameRule(DAYLIGHT_RULE, "false");
            if (debug && ruleLog(world.provider.getDimension()).allow(world.getTotalWorldTime())) {
                logger.info("[debug] doDaylightCycle was true; set back to false (Seasonal Time advances time)");
            }
        }
    }

    private LogLimiter ruleLog(int dimension) {
        LogLimiter limiter = ruleLogs.get(dimension);
        if (limiter == null) {
            limiter = new LogLimiter(20 * 60);
            ruleLogs.put(dimension, limiter);
        }
        return limiter;
    }

    /** True when Seasonal Time should advance time. Always true until pausing exists. */
    boolean isRunning(World world) {
        return true;
    }
}
