package com.mahghuuuls.seasonaltime.network;

import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.season.DayAlignment;
import com.mahghuuuls.seasonaltime.time.CycleDurations;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * The server's side of Serene Seasons day alignment (REQ-025): one Serene Seasons day lasts one
 * cycle on the server, and every player's game is told the same value.
 *
 * <p>The value is applied before the worlds load, checked once a second and at once after a
 * settings change (Serene Seasons resets it when its config is edited in game), sent to every
 * player when it changes and to each player at login. Serene Seasons gets its own setting back when
 * alignment is turned off or the server stops. Runs on the server thread.
 */
public final class DayAlignmentSync {

    /** Who hears about the value: every player, or one player who just joined. */
    public interface Players {

        void sendToAll(int dayTicks);

        void sendTo(EntityPlayerMP player, int dayTicks);
    }

    private static final int CHECK_INTERVAL_TICKS = 20;

    private final Supplier<Settings> settings;
    private final DayAlignment alignment;
    private final Players players;
    private final Logger logger;
    /** The value in place and told to players; 0 for no alignment. */
    private int told;
    private Settings seen;
    private int ticks;

    public DayAlignmentSync(Supplier<Settings> settings, DayAlignment alignment, Players players, Logger logger) {
        this.settings = settings;
        this.alignment = alignment;
        this.players = players;
        this.logger = logger;
    }

    /** The Serene Seasons day length for these settings: one cycle in ticks, or 0 for no alignment. */
    static int dayTicksFor(Settings settings, boolean installed) {
        return installed && settings.alignSereneSeasonsDay() ? settings.cycleLengthMinutes() * CycleDurations.TICKS_PER_MINUTE : 0;
    }

    /**
     * Applies the value before the worlds load, so Serene Seasons creates a new world's season data
     * with the aligned day. No player is connected yet.
     */
    public void serverAboutToStart() {
        told = 0;
        seen = null;
        ticks = 0;
        refresh(false);
        if (alignment.installed() && told == 0) {
            logger.info("Serene Seasons day alignment is off; Serene Seasons uses its own Day Duration setting "
                    + "({} ticks)", alignment.ownDayTicks());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tick();
        }
    }

    void tick() {
        ticks++;
        if (settings.get() != seen || ticks % CHECK_INTERVAL_TICKS == 0) {
            refresh(true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            playerJoined((EntityPlayerMP) event.player);
        }
    }

    void playerJoined(EntityPlayerMP player) {
        players.sendTo(player, told);
    }

    /** Gives Serene Seasons back its own day length; on a game a player hosts, that is the player's too. */
    public void serverStopping() {
        alignment.update(0);
        told = 0;
    }

    private void refresh(boolean send) {
        Settings current = settings.get();
        seen = current;
        int wanted = dayTicksFor(current, alignment.installed());
        // Also writes the value again when Serene Seasons has reset it. A value that cannot be
        // applied is not told to players, so they keep the same day length as the server.
        int inPlace = alignment.update(wanted) ? wanted : 0;
        if (inPlace == told) {
            return;
        }
        report(inPlace, current);
        told = inPlace;
        if (send) {
            players.sendToAll(inPlace);
        }
    }

    private void report(int inPlace, Settings current) {
        if (inPlace == 0) {
            logger.info("Serene Seasons day alignment is off; Serene Seasons uses its own Day Duration setting "
                    + "({} ticks)", alignment.ownDayTicks());
        } else if (told == 0) {
            logger.info("Serene Seasons day alignment is on: its Day Duration setting ({} ticks in its config) is "
                    + "replaced by {} ticks, one cycle of {} minutes", alignment.ownDayTicks(), inPlace,
                    current.cycleLengthMinutes());
        } else {
            logger.info("Serene Seasons day alignment: one Serene Seasons day is now {} ticks, one cycle of {} minutes",
                    inPlace, current.cycleLengthMinutes());
        }
    }
}
