package com.mahghuuuls.seasonaltime;

import com.mahghuuuls.seasonaltime.command.SeasonalTimeCommand;
import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.control.ProgressionSwitch;
import com.mahghuuuls.seasonaltime.control.TimeController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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

    private SeasonalTimeConfig config;
    private ProgressionSwitch progression;
    private TimeController timeController;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new SeasonalTimeConfig(event.getSuggestedConfigurationFile(), LOGGER::warn);
        config.load();
        LOGGER.info("{} {} loaded: {}", Tags.MOD_NAME, Tags.VERSION, config.settings());

        progression = new ProgressionSwitch(LOGGER, config::settings);
        timeController = new TimeController(config::settings, progression, LOGGER);
        MinecraftForge.EVENT_BUS.register(progression);
        MinecraftForge.EVENT_BUS.register(timeController);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new SeasonalTimeCommand(config, timeController, progression));
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
    }

}
