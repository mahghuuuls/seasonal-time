package com.mahghuuuls.seasonaltime.client;

import com.mahghuuuls.seasonaltime.integration.sereneseasons.SereneSeasonsIntegration;
import com.mahghuuuls.seasonaltime.network.DayAlignmentReceiver;
import com.mahghuuuls.seasonaltime.season.DayAlignment;
import com.mahghuuuls.seasonaltime.season.NoSeasonSource;
import com.mahghuuuls.seasonaltime.season.SeasonReading;
import com.mahghuuuls.seasonaltime.season.SeasonSource;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.Logger;

/**
 * The player's game side of Serene Seasons day alignment (REQ-025, ARC-005). Serene Seasons 1.12.2
 * does not send its settings to players, so the server's value arrives in a message and is applied
 * here; the game's own Serene Seasons setting comes back when the player leaves the server.
 *
 * <p>A game that hosts the server (singleplayer, or LAN host) shares the server's Serene Seasons
 * settings in memory, so it never writes them: the server side does. Only a game connected to
 * another machine's server writes.
 *
 * <p>FML creates this class through {@code @SidedProxy} on a game with a screen only. Everything
 * here runs on the game's main thread.
 */
public final class ClientDayAlignment implements DayAlignmentReceiver {

    private static final int CHECK_INTERVAL_TICKS = 20;
    /**
     * Serene Seasons drops its own season message while the joining player does not exist yet and
     * then counts from Early Spring until its next message, so the season is read 5 seconds in.
     */
    private static final int REPORT_DELAY_TICKS = 100;

    private Logger logger;
    private DayAlignment alignment = DayAlignment.NONE;
    private SeasonSource seasons = new NoSeasonSource();
    /** The server's value; 0 when not connected or when the server does not align. */
    private int dayTicks;
    private int ticks;
    /** The tick at which to log the value and the resulting sub-season; -1 for none. */
    private int reportAt = -1;

    @Override
    public void start(Logger logger) {
        this.logger = logger;
        if (Loader.isModLoaded(SereneSeasonsIntegration.MOD_ID)) {
            alignment = SereneSeasonsIntegration.dayAlignment((message, cause) -> {
                if (cause == null) {
                    logger.warn(message);
                } else {
                    logger.error(message, cause);
                }
            });
            seasons = SereneSeasonsIntegration.seasonSource();
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void receive(int dayTicks) {
        this.dayTicks = dayTicks;
        apply();
        if (alignment.installed()) {
            if (dayTicks > 0) {
                logger.info("Serene Seasons day length from the server: {} ticks{}", dayTicks, hostsTheServer()
                        ? " (shared with the server this game hosts)" : "");
            } else {
                logger.info("The server does not align the Serene Seasons day; this game uses its own Day Duration "
                        + "setting ({} ticks)", alignment.ownDayTicks());
            }
        }
        reportAt = ticks + REPORT_DELAY_TICKS;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().player == null) {
            return;
        }
        ticks++;
        if (ticks % CHECK_INTERVAL_TICKS == 0) {
            // Serene Seasons resets its setting when its config is edited in game.
            apply();
        }
        if (ticks == reportAt) {
            reportAt = -1;
            report();
        }
    }

    /** Posted on the network thread, so the work goes to the main thread. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            dayTicks = 0;
            reportAt = -1;
            if (alignment.aligned()) {
                alignment.update(0);
                logger.info("Left the server; Serene Seasons uses this game's own Day Duration setting again ({} "
                        + "ticks, in use: {} ticks)", alignment.ownDayTicks(), alignment.dayTicks());
            }
        });
    }

    /** Writes the server's value, unless this game hosts the server and so already shares it. */
    private void apply() {
        if (!hostsTheServer()) {
            alignment.update(dayTicks);
        }
    }

    private static boolean hostsTheServer() {
        return Minecraft.getMinecraft().isIntegratedServerRunning();
    }

    /** For comparison with the server's debug output: what Serene Seasons computes in this game. */
    private void report() {
        if (alignment.installed()) {
            logger.info("Serene Seasons in this game: day length in use {} ticks, sub-season {}", alignment.dayTicks(),
                    subSeason());
        }
    }

    private String subSeason() {
        World world = Minecraft.getMinecraft().world;
        if (world == null) {
            return "n/a";
        }
        try {
            SeasonReading reading = seasons.read(world);
            return reading.available() ? reading.subSeason() : "n/a";
        } catch (RuntimeException | LinkageError failure) {
            return "n/a (" + failure + ")";
        }
    }
}
