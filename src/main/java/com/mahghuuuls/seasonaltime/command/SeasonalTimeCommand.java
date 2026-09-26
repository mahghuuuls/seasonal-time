package com.mahghuuuls.seasonaltime.command;

import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.control.ClockRole;
import com.mahghuuuls.seasonaltime.control.ClockStatus;
import com.mahghuuuls.seasonaltime.control.ProgressionSwitch;
import com.mahghuuuls.seasonaltime.control.TimeController;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.server.command.CommandTreeBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@code /seasonaltime}: read the timing and the controller's state, change settings, and reload
 * the config file. Changes go only through {@link SeasonalTimeConfig}.
 *
 * <p>Every subcommand needs permission level 2 (operator). Forge's command tree checks the
 * subcommand's own level, and a plain {@link CommandBase} defaults to 4, so each subcommand states
 * 2 explicitly.
 */
public final class SeasonalTimeCommand extends CommandTreeBase {

    static final int PERMISSION_LEVEL = 2;

    public SeasonalTimeCommand(SeasonalTimeConfig config, TimeController controller, ProgressionSwitch progression) {
        Supplier<Settings> settings = config::settings;
        addSubcommand(new Get(settings, controller));
        addSubcommand(new Debug(settings, controller, progression));
        addSubcommand(new Set(config));
        addSubcommand(new Seasonal(config));
        addSubcommand(new Reload(config));
    }

    @Override
    public String getName() {
        return "seasonaltime";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/seasonaltime <get [day|night|seasonal|effective] | debug | set <option> <value> | seasonal <true|false> "
                + "| reload>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return PERMISSION_LEVEL;
    }

    private static void reply(ICommandSender sender, List<String> lines) {
        for (String line : lines) {
            sender.sendMessage(new TextComponentString(line));
        }
    }

    /** A subcommand that changes the configuration; it writes only through {@link SeasonalTimeConfig}. */
    private abstract static class Change extends CommandBase {

        final SeasonalTimeConfig config;

        Change(SeasonalTimeConfig config) {
            this.config = config;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return PERMISSION_LEVEL;
        }
    }

    private static final class Set extends Change {

        private static final String[] OPTIONS = {"day", "night", "summerDaylightPercent", "winterDaylightPercent"};

        Set(SeasonalTimeConfig config) {
            super(config);
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/seasonaltime set <day|night> <minutes> | set <summerDaylightPercent|winterDaylightPercent> <percent>";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }
            try {
                switch (args[0]) {
                    case "day":
                        config.setBaseDayDurationMinutes(args[1]);
                        break;
                    case "night":
                        config.setBaseNightDurationMinutes(args[1]);
                        break;
                    case "summerDaylightPercent":
                        config.setSummerSolsticeDaylightPercent(args[1]);
                        break;
                    case "winterDaylightPercent":
                        config.setWinterSolsticeDaylightPercent(args[1]);
                        break;
                    default:
                        throw new WrongUsageException(getUsage(sender));
                }
            } catch (IllegalArgumentException refused) {
                throw new CommandException(refused.getMessage());
            }
            Settings now = config.settings();
            reply(sender, args[0].equals("day") || args[0].equals("night")
                    ? TimeReport.base(now) : TimeReport.solstices(now));
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                              BlockPos targetPos) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, OPTIONS) : Collections.<String>emptyList();
        }
    }

    private static final class Seasonal extends Change {

        Seasonal(SeasonalTimeConfig config) {
            super(config);
        }

        @Override
        public String getName() {
            return "seasonal";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/seasonaltime seasonal <true|false>";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            config.setSeasonalAdjustmentEnabled(parseBoolean(args[0]));
            reply(sender, Collections.singletonList(
                    "Seasonal adjustment enabled: " + config.settings().seasonalAdjustmentEnabled()));
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                              BlockPos targetPos) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, "true", "false")
                    : Collections.<String>emptyList();
        }
    }

    private static final class Reload extends Change {

        Reload(SeasonalTimeConfig config) {
            super(config);
        }

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/seasonaltime reload";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length > 0) {
                throw new WrongUsageException(getUsage(sender));
            }
            // A changed dimension list is picked up by the controller on its next server tick.
            List<String> warnings = config.load();
            List<String> lines = new ArrayList<String>();
            lines.add("Reloaded the config file" + (warnings.isEmpty() ? "." : " with corrections:"));
            lines.addAll(warnings);
            lines.addAll(TimeReport.base(config.settings()));
            reply(sender, lines);
        }
    }

    private abstract static class Sub extends CommandBase {

        final Supplier<Settings> settings;
        final TimeController controller;

        Sub(Supplier<Settings> settings, TimeController controller) {
            this.settings = settings;
            this.controller = controller;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return PERMISSION_LEVEL;
        }

        ClockStatus statusFor(ICommandSender sender) {
            return controller.statusShownIn(sender.getEntityWorld());
        }
    }

    private static final class Get extends Sub {

        Get(Supplier<Settings> settings, TimeController controller) {
            super(settings, controller);
        }

        @Override
        public String getName() {
            return "get";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/seasonaltime get [day|night|seasonal|effective]";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            Settings current = settings.get();
            if (args.length == 0) {
                reply(sender, TimeReport.base(current));
                return;
            }
            if (args.length > 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            switch (args[0]) {
                case "day":
                    reply(sender, Collections.singletonList(TimeReport.day(current)));
                    break;
                case "night":
                    reply(sender, Collections.singletonList(TimeReport.night(current)));
                    break;
                case "seasonal":
                    reply(sender, TimeReport.seasonal(current, statusFor(sender)));
                    break;
                case "effective":
                    reply(sender, TimeReport.effective(statusFor(sender)));
                    break;
                default:
                    throw new WrongUsageException(getUsage(sender));
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                              BlockPos targetPos) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "day", "night", "seasonal", "effective");
            }
            return Collections.emptyList();
        }
    }

    private static final class Debug extends Sub {

        private final ProgressionSwitch progression;

        Debug(Supplier<Settings> settings, TimeController controller, ProgressionSwitch progression) {
            super(settings, controller);
            this.progression = progression;
        }

        @Override
        public String getName() {
            return "debug";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/seasonaltime debug";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length > 0) {
                throw new WrongUsageException(getUsage(sender));
            }
            World world = sender.getEntityWorld();
            ClockRole role = controller.roleOf(world);
            reply(sender, TimeReport.debug(settings.get(), controller.statusShownIn(world), world.getWorldTime(), role,
                    Loader.isModLoaded("sereneseasons"), progression.stateFor(role)));
        }
    }
}
