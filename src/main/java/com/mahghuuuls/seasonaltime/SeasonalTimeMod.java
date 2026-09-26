package com.mahghuuuls.seasonaltime;

import com.mahghuuuls.seasonaltime.command.SeasonalTimeCommand;
import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.control.ProgressionSwitch;
import com.mahghuuuls.seasonaltime.control.TimeController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
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
    private TimeController timeController;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new SeasonalTimeConfig(event.getSuggestedConfigurationFile(), LOGGER::warn);
        config.load();
        LOGGER.info("{} {} loaded: {}", Tags.MOD_NAME, Tags.VERSION, config.settings());

        timeController = new TimeController(config::settings, new ProgressionSwitch(LOGGER), LOGGER);
        MinecraftForge.EVENT_BUS.register(timeController);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new SeasonalTimeCommand(config::settings, timeController));
    }

}
