package com.mahghuuuls.seasonaltime.command;

import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.control.ClockRole;
import com.mahghuuuls.seasonaltime.control.ClockStatus;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraftforge.fml.relauncher.TestMinecraftHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeReportTest {

    @TempDir
    File directory;

    @BeforeEach
    void giveForgeAGameDirectory() {
        TestMinecraftHome.setIfAbsent(directory);
    }

    private Settings settings(String... lines) throws IOException {
        File file = new File(directory, "seasonaltime.cfg");
        if (lines.length > 0) {
            Files.write(file.toPath(), String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        }
        SeasonalTimeConfig config = new SeasonalTimeConfig(file, message -> { });
        config.load();
        return config.settings();
    }

    @Test
    void getShowsBaseDurationsAndCycle() throws IOException {
        Settings settings = settings("time {", "    I:baseDayDurationMinutes=40", "    I:baseNightDurationMinutes=20", "}");
        assertEquals(java.util.Arrays.asList("Base day duration: 40.0 minutes", "Base night duration: 20.0 minutes",
                "Total cycle duration: 60.0 minutes"), TimeReport.base(settings));
    }

    @Test
    void effectiveUsesTheClockStatusWhenPresent() throws IOException {
        ClockStatus status = new ClockStatus(0, 6000, 42.0, 18.0, 0.24, 0.5);
        List<String> lines = TimeReport.effective(status);
        assertEquals("Effective day duration: 42.0 minutes", lines.get(0));
        assertEquals("Effective night duration: 18.0 minutes", lines.get(1));
        assertEquals("Current daylight percentage: 70.0%", lines.get(2));
    }

    @Test
    void effectiveIsNotAvailableForAClockSeasonalTimeDoesNotAdvance() {
        List<String> lines = TimeReport.effective(null);
        assertEquals("Effective day duration: n/a", lines.get(0));
        assertEquals("Current daylight percentage: n/a", lines.get(2));
    }

    @Test
    void seasonalShowsSummerAndWinterInTheirOwnLines() throws IOException {
        Settings settings = settings("seasonal {", "    D:summerSolsticeDaylightPercent=80",
                "    D:winterSolsticeDaylightPercent=40", "}");
        List<String> lines = TimeReport.seasonal(settings, new ClockStatus(0, 0, 30, 30, 1, 0));
        assertEquals("Summer solstice daylight: 80.0%", lines.get(2));
        assertEquals("Winter solstice daylight: 40.0%", lines.get(3));
        assertEquals("Current daylight percentage: 50.0%", lines.get(4));
    }

    @Test
    void debugPrintsEveryRequiredField() throws IOException {
        ClockStatus status = new ClockStatus(0, 30500, 30.0, 30.0, 1.0 / 3.0, 0.25);
        List<String> lines = TimeReport.debug(settings(), status, 30500, ClockRole.CONTROLLED, false, "running");
        String[] labels = {"World time: 30500", "Time of day: 6500", "Segment: DAY", "Base day duration",
                "Base night duration", "Total cycle duration", "Effective day duration", "Effective night duration",
                "Current daylight percentage", "Season:", "Sub-season:", "Year position:",
                "Fractional remainder: 0.2500", "World time units per tick: 0.3333", "Serene Seasons installed: no",
                "Seasonal adjustment active:", "Serene Seasons day alignment:", "Time progression: running",
                "Controlled dimension: yes"};
        assertEquals(labels.length, lines.size());
        for (int i = 0; i < labels.length; i++) {
            assertTrue(lines.get(i).startsWith(labels[i]), lines.get(i) + " does not start with " + labels[i]);
        }
    }

    @Test
    void debugForAnUncontrolledClockStillShowsItsWorldTime() throws IOException {
        List<String> lines = TimeReport.debug(settings(), null, 18000, ClockRole.NOT_CONTROLLED, true, "paused");
        assertEquals("World time: 18000", lines.get(0));
        assertEquals("Segment: NIGHT", lines.get(2));
        assertEquals("World time units per tick: n/a", lines.get(13));
        assertEquals("Serene Seasons installed: yes", lines.get(14));
        assertEquals("Time progression: paused", lines.get(17));
        assertEquals("Controlled dimension: no", lines.get(18));
    }

    @Test
    void everySubcommandNeedsOperatorLevel() {
        SeasonalTimeCommand command = new SeasonalTimeCommand(new SeasonalTimeConfig(new File("unused"), message -> { }), null, null);
        assertEquals(2, command.getRequiredPermissionLevel());
        java.util.Set<String> names = new java.util.TreeSet<String>();
        for (ICommand sub : command.getSubCommands()) {
            names.add(sub.getName());
        }
        assertEquals(new java.util.TreeSet<String>(java.util.Arrays.asList("debug", "get", "reload", "seasonal", "set")),
                names);
        for (ICommand sub : command.getSubCommands()) {
            assertEquals(2, ((CommandBase) sub).getRequiredPermissionLevel(), sub.getName());
        }
    }
}
