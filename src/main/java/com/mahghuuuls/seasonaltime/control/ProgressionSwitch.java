package com.mahghuuuls.seasonaltime.control;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandGameRule;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.mahghuuuls.seasonaltime.config.Settings;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Owns the time progression switch and the meaning of the vanilla {@code doDaylightCycle} rule.
 *
 * <p>Vanilla must never advance world time itself, or it and Seasonal Time would both move the
 * clock in the same tick, so the real rule stays false while the game runs. Admins still use the
 * rule they know: {@code /gamerule doDaylightCycle false} pauses Seasonal Time, {@code true}
 * resumes it, and the query shows the switch. The switch is saved with the world.
 *
 * <p>On a clean stop the real rule is written as the switch's value before the world is saved, so
 * a world whose mod is removed afterwards behaves like vanilla. After a crash the saved rule stays
 * false; with the mod still installed nothing is lost, because the saved switch is used, but a
 * world whose mod is then removed needs the rule set by hand.
 *
 * <p>A world opened for the first time with this mod starts from its current rule: a world frozen
 * in vanilla stays frozen, and a new world runs. Later, a rule found true in the Overworld, at load
 * or during play, always means "running": this mod only ever saves it true when time was running,
 * so a true rule comes from that or from someone turning vanilla's cycle on.
 *
 * <p>A dedicated server stopped by a signal (Ctrl+C, kill) skips Forge's stopping event, so like a
 * crash it saves the rule as false.
 *
 * <p>When the Overworld is not in {@code controlledDimensions}, its rule is left to vanilla: it is
 * not held off, {@code /gamerule doDaylightCycle} is not intercepted, and it is not rewritten at a
 * stop. The saved switch then only pauses dimensions with their own clock; only a rule found true
 * at Overworld load changes it. When a reload takes a dimension out of control, its rule is handed
 * back at once with the switch's value.
 */
public final class ProgressionSwitch {

    static final String DAYLIGHT_RULE = "doDaylightCycle";

    private final Logger logger;
    private final Supplier<Settings> settings;
    /** Per dimension, because each world counts its own total ticks. */
    private final Map<Integer, LogLimiter> ruleLogs = new HashMap<Integer, LogLimiter>();
    /** Dimensions other than the Overworld whose own rule this switch holds off; restored at a clean stop. */
    private final Set<Integer> heldOwnRules = new HashSet<Integer>();
    /** Null while no Overworld is loaded. Read once per use: RCON commands run off the server thread. */
    private volatile ProgressionData data;

    public ProgressionSwitch(Logger logger, Supplier<Settings> settings) {
        this.logger = logger;
        this.settings = settings;
    }

    private boolean overworldControlled() {
        return ClockOwnership.isOverworldControlled(settings.get());
    }

    /**
     * Hands rules back to vanilla after the controlled list changed: the Overworld's when it left
     * the list, and each held dimension's own rule when that dimension left it. Each gets the
     * switch's value, so time runs there again unless Seasonal Time was paused. Server thread only.
     */
    void controlledListChanged(Settings previous, Settings now) {
        ProgressionData current = data;
        if (current == null) {
            return;
        }
        String value = String.valueOf(current.running());
        if (ClockOwnership.isOverworldControlled(previous) && !ClockOwnership.isOverworldControlled(now)) {
            World overworld = DimensionManager.getWorld(ClockOwnership.OVERWORLD);
            if (overworld != null) {
                overworld.getGameRules().setOrCreateGameRule(DAYLIGHT_RULE, value);
            }
        }
        for (Iterator<Integer> held = heldOwnRules.iterator(); held.hasNext(); ) {
            int dimension = held.next();
            if (!now.isControlledDimension(dimension)) {
                World world = DimensionManager.getWorld(dimension);
                if (world != null) {
                    world.getGameRules().setOrCreateGameRule(DAYLIGHT_RULE, value);
                }
                held.remove();
            }
        }
    }

    /** "running" or "paused" for a clock Seasonal Time advances; "vanilla" for one it leaves alone. */
    public String stateFor(ClockRole role) {
        if (role == ClockRole.NOT_CONTROLLED) {
            return "vanilla (not controlled here)";
        }
        return isRunning() ? "running" : "paused";
    }

    /**
     * Reads the saved switch when the Overworld loads, creating it from the current rule the first
     * time, and only then turns the real rule off. Both happen here, in this order, because the
     * first-time value comes from the rule.
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != ClockOwnership.OVERWORLD) {
            return;
        }
        GameRules rules = world.getGameRules();
        boolean ruleOn = rules.getBoolean(DAYLIGHT_RULE);
        MapStorage storage = world.getMapStorage();
        ProgressionData loaded = (ProgressionData) storage.getOrLoadData(ProgressionData.class, ProgressionData.NAME);
        if (loaded == null) {
            loaded = new ProgressionData(ProgressionData.NAME);
            loaded.setRunning(ruleOn);
            loaded.markDirty();
            storage.setData(ProgressionData.NAME, loaded);
        } else if (ruleOn) {
            loaded.setRunning(true);
        }
        data = loaded;
        if (ruleOn && overworldControlled()) {
            rules.setOrCreateGameRule(DAYLIGHT_RULE, "false");
            if (settings.get().debug()) {
                logger.info("[debug] doDaylightCycle was true at world load; set to false, time progression "
                        + (loaded.running() ? "running" : "paused"));
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (!world.isRemote && world.provider.getDimension() == ClockOwnership.OVERWORLD) {
            data = null;
            ruleLogs.clear();
            heldOwnRules.clear();
        }
    }

    /**
     * {@code /gamerule doDaylightCycle} and {@code /gamerule doDaylightCycle true|false} read or set
     * the switch instead of the real rule. Any other value is left to vanilla, which refuses it with
     * its own message before changing anything.
     */
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        ProgressionData current = data;
        String[] args = event.getParameters();
        if (current == null || !(event.getCommand() instanceof CommandGameRule) || args.length < 1
                || args.length > 2 || !DAYLIGHT_RULE.equals(args[0]) || !overworldControlled()) {
            return;
        }
        if (args.length == 1) {
            event.setCanceled(true);
            event.getSender().sendMessage(new TextComponentString(DAYLIGHT_RULE + " = " + current.running()));
            return;
        }
        if (!"true".equals(args[1]) && !"false".equals(args[1])) {
            return;
        }
        event.setCanceled(true);
        current.setRunning(Boolean.parseBoolean(args[1]));
        CommandBase.notifyCommandListener(event.getSender(), event.getCommand(), "commands.gamerule.success",
                DAYLIGHT_RULE, args[1]);
    }

    /**
     * Makes sure vanilla will not advance world time in this world's tick. If the real rule was
     * turned on by anything other than the command, that is read as "resume". The debug line is
     * limited to one a minute, in case something keeps turning the rule on.
     */
    void holdVanillaCycleOff(World world) {
        if (world.provider.getDimension() != ClockOwnership.OVERWORLD) {
            heldOwnRules.add(world.provider.getDimension());
        }
        GameRules rules = world.getGameRules();
        if (rules.getBoolean(DAYLIGHT_RULE)) {
            rules.setOrCreateGameRule(DAYLIGHT_RULE, "false");
            // Only the Overworld's rule is the admin's switch. A dimension with its own world info
            // has its own rules, whose default "true" must not resume time.
            ProgressionData current = data;
            boolean resumed = current != null && world.provider.getDimension() == ClockOwnership.OVERWORLD;
            if (resumed) {
                current.setRunning(true);
            }
            if (settings.get().debug() && ruleLog(world.provider.getDimension()).allow(world.getTotalWorldTime())) {
                logger.info("[debug] dim {}: doDaylightCycle was true; set back to false{}",
                        world.provider.getDimension(), resumed ? ", time progression running" : "");
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

    /** True when Seasonal Time should advance time. Running until the saved switch is loaded. */
    public boolean isRunning() {
        ProgressionData current = data;
        return current == null || current.running();
    }

    /**
     * Writes the switch into the real rule for the save that follows a clean stop: in the
     * Overworld, and in every still-loaded dimension whose own rule this switch held off. Nothing
     * ticks after this, so the rules are not turned back off. A dimension with its own rules that
     * was unloaded earlier kept the rule false in its save.
     */
    public void writeRuleForSave() {
        ProgressionData current = data;
        if (current == null) {
            return;
        }
        String value = String.valueOf(current.running());
        World overworld = DimensionManager.getWorld(ClockOwnership.OVERWORLD);
        if (overworld != null && overworldControlled()) {
            overworld.getGameRules().setOrCreateGameRule(DAYLIGHT_RULE, value);
        }
        for (int dimension : heldOwnRules) {
            World world = DimensionManager.getWorld(dimension);
            if (world != null) {
                world.getGameRules().setOrCreateGameRule(DAYLIGHT_RULE, value);
            }
        }
    }
}
