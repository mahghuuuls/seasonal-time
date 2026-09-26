package com.mahghuuuls.seasonaltime;

import com.mahghuuuls.seasonaltime.command.SeasonalTimeCommand;
import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.control.ProgressionSwitch;
import com.mahghuuuls.seasonaltime.control.TimeController;
import com.mahghuuuls.seasonaltime.integration.sereneseasons.SereneSeasonsIntegration;
import com.mahghuuuls.seasonaltime.network.DayAlignmentReceiver;
import com.mahghuuuls.seasonaltime.network.DayAlignmentSync;
import com.mahghuuuls.seasonaltime.network.SeasonalTimeNetwork;
import com.mahghuuuls.seasonaltime.season.DayAlignment;
import com.mahghuuuls.seasonaltime.season.GuardedSeasonSource;
import com.mahghuuuls.seasonaltime.season.NoSeasonSource;
import com.mahghuuuls.seasonaltime.season.SeasonSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:forge@[14.23.5.2847,);after:sereneseasons@[1.2.18,)"
)
public class SeasonalTimeMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    /** The player's game side of day alignment; FML sets it by side, so no client class is named here. */
    @SidedProxy(modId = Tags.MOD_ID, clientSide = "com.mahghuuuls.seasonaltime.client.ClientDayAlignment",
            serverSide = "com.mahghuuuls.seasonaltime.network.DayAlignmentReceiver$Ignore")
    private static DayAlignmentReceiver playerSide;

    private SeasonalTimeConfig config;
    private ProgressionSwitch progression;
    private TimeController timeController;
    private DayAlignment dayAlignment;
    private DayAlignmentSync dayAlignmentSync;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new SeasonalTimeConfig(event.getSuggestedConfigurationFile(), LOGGER::warn);
        config.load();
        LOGGER.info("{} {} loaded: {}", Tags.MOD_NAME, Tags.VERSION, config.settings());

        boolean sereneSeasons = Loader.isModLoaded(SereneSeasonsIntegration.MOD_ID);
        progression = new ProgressionSwitch(LOGGER, config::settings);
        timeController = new TimeController(config::settings, progression, seasons(sereneSeasons), LOGGER);
        MinecraftForge.EVENT_BUS.register(progression);
        MinecraftForge.EVENT_BUS.register(timeController);

        dayAlignment = sereneSeasons ? SereneSeasonsIntegration.dayAlignment(SeasonalTimeMod::logProblem)
                : DayAlignment.NONE;
        playerSide.start(LOGGER);
        dayAlignmentSync = new DayAlignmentSync(config::settings, dayAlignment, new SeasonalTimeNetwork(playerSide),
                LOGGER);
        MinecraftForge.EVENT_BUS.register(dayAlignmentSync);
    }

    /**
     * Serene Seasons classes are touched only when Serene Seasons is installed, through the
     * integration's single entry point.
     */
    private static GuardedSeasonSource seasons(boolean sereneSeasons) {
        SeasonSource source;
        if (sereneSeasons) {
            source = SereneSeasonsIntegration.seasonSource();
            LOGGER.info("Serene Seasons found; seasonal adjustment is available");
        } else {
            source = new NoSeasonSource();
            LOGGER.info("Serene Seasons is not installed; seasonal adjustment is unavailable, the base day and "
                    + "night lengths apply");
        }
        return new GuardedSeasonSource(source, SeasonalTimeMod::logProblem);
    }

    /** A warning, or an error with its stack trace when there is a cause. */
    private static void logProblem(String message, Throwable cause) {
        if (cause == null) {
            LOGGER.warn(message);
        } else {
            LOGGER.error(message, cause);
        }
    }

    /** Before the worlds load, so a new world's Serene Seasons data starts with the aligned day. */
    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        dayAlignmentSync.serverAboutToStart();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new SeasonalTimeCommand(config, timeController, progression, dayAlignment));
    }

    /** Every dimension is registered by now, including those other mods add while the server starts. */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        timeController.reportDimensions();
    }

    /** Runs before the final world save, so the saved rule shows whether time was running. */
    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        progression.writeRuleForSave();
        dayAlignmentSync.serverStopping();
    }

}
