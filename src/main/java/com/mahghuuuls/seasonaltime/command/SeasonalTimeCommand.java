package com.mahghuuuls.seasonaltime.command;

import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.control.ClockStatus;
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

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@code /seasonaltime}: read the timing in use and the controller's state.
 *
 * <p>Every subcommand needs permission level 2 (operator). Forge's command tree checks the
 * subcommand's own level, and a plain {@link CommandBase} defaults to 4, so each subcommand states
 * 2 explicitly.
 */
public final class SeasonalTimeCommand extends CommandTreeBase {

    static final int PERMISSION_LEVEL = 2;

    public SeasonalTimeCommand(Supplier<Settings> settings, TimeController controller) {
        addSubcommand(new Get(settings, controller));
        addSubcommand(new Debug(settings, controller));
    }

    @Override
    public String getName() {
        return "seasonaltime";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/seasonaltime <get [day|night|seasonal|effective] | debug>";
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

        Debug(Supplier<Settings> settings, TimeController controller) {
            super(settings, controller);
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
            reply(sender, TimeReport.debug(settings.get(), controller.statusShownIn(world), world.getWorldTime(),
                    controller.roleOf(world), Loader.isModLoaded("sereneseasons")));
        }
    }
}
