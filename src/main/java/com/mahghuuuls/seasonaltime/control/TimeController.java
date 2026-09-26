package com.mahghuuuls.seasonaltime.control;

import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.time.CycleDurations;
import com.mahghuuuls.seasonaltime.time.TimeStepper;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The only code in this mod that writes world time.
 *
 * <p>It runs in the server's world tick at {@code START}, which Forge fires before vanilla's own
 * {@code WorldServer.tick()}. Each tick it keeps vanilla's daylight cycle off, advances world time
 * by the configured rate, and, whenever the whole-unit time changed, sends the vanilla time packet
 * to every player who sees that clock. With the daylight rule off, a client does not advance time
 * by itself, so these packets are what move the sky smoothly.
 *
 * <p>World time itself stays the only clock. The controller remembers only the value it last
 * wrote. When the world shows anything else, something outside changed it ({@code /time}, another
 * mod, a sleep mod), and the controller carries on from the new value instead of restoring its own.
 *
 * <p>Vanilla skips the night after everyone sleeps only while its daylight rule is on, but it
 * always wakes the sleepers. So when everyone is asleep this controller makes the jump to morning
 * itself, just before vanilla's tick, and leaves the waking and the weather reset to vanilla.
 */
public final class TimeController {

    private static final int DEBUG_INTERVAL_TICKS = 20 * 60;

    private final Supplier<Settings> settings;
    private final ProgressionSwitch progression;
    private final Logger logger;
    private final Map<Integer, Clock> clocks = new HashMap<Integer, Clock>();
    /** Listed dimensions already reported as following the Overworld, so each is reported once. */
    private final Set<Integer> reportedFollowers = new HashSet<Integer>();
    /** The settings whose controlled list was last reported; null until the server has started. */
    private Settings reportedList;
    // Read by commands, which RCON runs off the server thread.
    private final Map<Integer, ClockStatus> statuses = new ConcurrentHashMap<Integer, ClockStatus>();
    private Settings durationsFor;
    private CycleDurations durations;

    public TimeController(Supplier<Settings> settings, ProgressionSwitch progression, Logger logger) {
        this.settings = settings;
        this.progression = progression;
        this.logger = logger;
    }

    /** The latest status for a dimension's own clock, or null when this controller has not run it. */
    public ClockStatus status(int dimension) {
        return statuses.get(dimension);
    }

    /** How this controller treats the given world's clock. */
    public ClockRole roleOf(World world) {
        return ClockOwnership.role(world.provider.getDimension(), ClockOwnership.ownsClock(world), settings.get());
    }

    /**
     * Reports the listed dimensions that do not exist, and the loaded listed dimensions that follow
     * the Overworld. Runs once the server has started, when every dimension is registered.
     */
    public void reportDimensions() {
        reportDimensions(settings.get());
    }

    /** Reports for exactly these settings, and records them as the last reported list. */
    private void reportDimensions(Settings reported) {
        reportedList = reported;
        for (int dimension : new TreeSet<Integer>(asList(reported.controlledDimensions()))) {
            if (!DimensionManager.isDimensionRegistered(dimension)) {
                logger.warn("controlledDimensions lists dimension {}, which does not exist; it is ignored", dimension);
            }
        }
        for (WorldServer world : DimensionManager.getWorlds()) {
            reportIfFollower(world, reported);
        }
    }

    /**
     * Notices a changed controlled list on the server thread (a reload may run on the RCON thread),
     * hands rules back for dimensions that left control, and reports the new list.
     */
    private void checkListChange(Settings current) {
        Settings previous = reportedList;
        if (previous == null || current == previous) {
            reportedList = current;
            return;
        }
        if (Arrays.equals(current.controlledDimensions(), previous.controlledDimensions())) {
            reportedList = current;
            return;
        }
        progression.controlledListChanged(previous, current);
        reportedFollowers.clear();
        reportDimensions(current);
    }

    private static List<Integer> asList(int[] values) {
        List<Integer> list = new ArrayList<Integer>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote) {
            reportIfFollower(world, settings.get());
        }
    }

    private void reportIfFollower(World world, Settings current) {
        if (ClockOwnership.isListedButFollowsOverworld(world, current)
                && reportedFollowers.add(world.provider.getDimension())) {
            logger.info("controlledDimensions lists dimension {}, which shares the Overworld clock; it follows the "
                    + "Overworld and is not advanced separately", world.provider.getDimension());
        }
    }

    /** The status of the clock the given world shows, or null when that clock is not advanced here. */
    public ClockStatus statusShownIn(World world) {
        switch (roleOf(world)) {
            case CONTROLLED:
                return status(world.provider.getDimension());
            case FOLLOWS_OVERWORLD:
                return status(ClockOwnership.OVERWORLD);
            default:
                return null;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (!world.isRemote) {
            int dimension = world.provider.getDimension();
            clocks.remove(dimension);
            statuses.remove(dimension);
            if (dimension == ClockOwnership.OVERWORLD) {
                // The server is stopping (the Overworld is only unloaded then); the next run reports again.
                reportedFollowers.clear();
                reportedList = null;
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !event.side.isServer() || event.world.isRemote) {
            return;
        }
        World world = event.world;
        Settings current = settings.get();
        if (world.provider.getDimension() == ClockOwnership.OVERWORLD) {
            checkListChange(current);
        }
        if (!ClockOwnership.isControlled(world, current)) {
            return;
        }
        progression.holdVanillaCycleOff(world);

        int dimension = world.provider.getDimension();
        long before = world.getWorldTime();
        Clock clock = clocks.get(dimension);
        if (clock == null) {
            clock = new Clock(before);
            clocks.put(dimension, clock);
        }
        TimeStepper stepper = clock.stepper;
        CycleDurations durations = durationsFor(current);
        if (!progression.isRunning()) {
            // Paused: follow whatever sets the time, so a /time set made now is not reported as an
            // outside change when time resumes. Nothing advances; the status still updates.
            clock.lastWritten = before;
            publish(world, current, new ClockStatus(dimension, before, durations.dayMinutes(),
                    durations.nightMinutes(), 0.0, stepper.remainder()), true);
            return;
        }
        if (before != clock.lastWritten) {
            stepper.reset();
            if (current.debug() && clock.outsideChangeLog.allow(world.getTotalWorldTime())) {
                logger.info("[debug] dim {}: world time changed from outside, {} -> {}; continuing from it",
                        dimension, clock.lastWritten, before);
            }
        }

        long after;
        if (world instanceof WorldServer && ((WorldServer) world).areAllPlayersAsleep()) {
            after = CycleDurations.nextMorning(before);
            stepper.reset();
            if (current.debug()) {
                logger.info("[debug] dim {}: everyone is asleep, {} -> {}", dimension, before, after);
            }
        } else {
            after = before + stepper.stepAt(durations, before);
        }
        if (after != before) {
            world.setWorldTime(after);
            sendTime(world);
        }
        clock.lastWritten = after;

        publish(world, current, new ClockStatus(dimension, after, durations.dayMinutes(), durations.nightMinutes(),
                durations.unitsPerTick(after), stepper.remainder()), false);
    }

    /** Stores the latest status and writes the once-a-minute debug summary. */
    private void publish(World world, Settings current, ClockStatus status, boolean paused) {
        statuses.put(status.dimension(), status);
        if (current.debug() && world.getTotalWorldTime() % DEBUG_INTERVAL_TICKS == 0) {
            logger.info("[debug] {}{}", status.summary(), paused ? " (paused)" : "");
        }
    }

    /**
     * Per-clock state: the carried fraction and the world time this controller last wrote. A new
     * clock starts from the world's current time, so its first tick is never an outside change.
     */
    private static final class Clock {
        final TimeStepper stepper = new TimeStepper();
        final LogLimiter outsideChangeLog = new LogLimiter(DEBUG_INTERVAL_TICKS);
        long lastWritten;

        Clock(long worldTime) {
            this.lastWritten = worldTime;
        }
    }

    /** Rebuilt only when the settings change, which keeps the tick free of that allocation. */
    private CycleDurations durationsFor(Settings current) {
        if (current != durationsFor) {
            durations = CycleDurations.base(current.baseDayDurationMinutes(), current.baseNightDurationMinutes());
            durationsFor = current;
        }
        return durations;
    }

    /** Sends the owner's time to the players of every world that shows this clock. */
    private static void sendTime(World owner) {
        MinecraftServer server = owner.getMinecraftServer();
        if (server == null) {
            return;
        }
        for (WorldServer world : server.worlds) {
            if (ClockOwnership.sharesClockOf(world, owner)) {
                server.getPlayerList().sendPacketToAllPlayersInDimension(
                        new SPacketTimeUpdate(world.getTotalWorldTime(), world.getWorldTime(), false),
                        world.provider.getDimension());
            }
        }
    }
}
