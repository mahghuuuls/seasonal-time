package com.mahghuuuls.seasonaltime.command;

import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.control.ClockRole;
import com.mahghuuuls.seasonaltime.control.ClockStatus;
import com.mahghuuuls.seasonaltime.season.DayAlignment;
import com.mahghuuuls.seasonaltime.season.SeasonReading;
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
        ClockStatus status = new ClockStatus(0, 6000, 42.0, 18.0, 0.24, 0.5, SeasonReading.of(0.375, "SUMMER", "MID_SUMMER"), true);
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
        List<String> lines = TimeReport.seasonal(settings, new ClockStatus(0, 0, 30, 30, 1, 0, SeasonReading.UNAVAILABLE, false));
        assertEquals("Solstice timing: MIDDLE", lines.get(2));
        assertEquals("Summer solstice daylight: 80.0%", lines.get(3));
        assertEquals("Winter solstice daylight: 40.0%", lines.get(4));
        assertEquals("Current daylight percentage: 50.0%", lines.get(5));
    }

    @Test
    void seasonalShowsARealisticTiming() throws IOException {
        Settings settings = settings("seasonal {", "    S:solsticeTiming=REALISTIC", "}");
        ClockStatus status = new ClockStatus(0, 0, 30, 30, 1, 0, SeasonReading.UNAVAILABLE, false);
        assertEquals("Solstice timing: REALISTIC", TimeReport.seasonal(settings, status).get(2));
        List<String> debug = TimeReport.debug(settings, status, 0, ClockRole.CONTROLLED, false,
                TimeReport.alignment(settings, DayAlignment.NONE), "running");
        assertEquals("Solstice timing: REALISTIC", debug.get(16));
    }

    @Test
    void debugPrintsEveryRequiredField() throws IOException {
        ClockStatus status = new ClockStatus(0, 30500, 30.0, 30.0, 1.0 / 3.0, 0.25, SeasonReading.UNAVAILABLE, false);
        List<String> lines = TimeReport.debug(settings(), status, 30500, ClockRole.CONTROLLED, false,
                TimeReport.alignment(settings(), DayAlignment.NONE), "running");
        String[] labels = {"World time: 30500", "Time of day: 6500", "Segment: DAY", "Base day duration",
                "Base night duration", "Total cycle duration", "Effective day duration", "Effective night duration",
                "Current daylight percentage", "Season:", "Sub-season:", "Year position:",
                "Fractional remainder: 0.2500", "World time units per tick: 0.3333", "Serene Seasons installed: no",
                "Seasonal adjustment active:", "Solstice timing: MIDDLE", "Serene Seasons day alignment:", "Time progression: running",
                "Controlled dimension: yes"};
        assertEquals(labels.length, lines.size());
        for (int i = 0; i < labels.length; i++) {
            assertTrue(lines.get(i).startsWith(labels[i]), lines.get(i) + " does not start with " + labels[i]);
        }
    }

    @Test
    void debugForAnUncontrolledClockStillShowsItsWorldTime() throws IOException {
        List<String> lines = TimeReport.debug(settings(), null, 18000, ClockRole.NOT_CONTROLLED, true,
                TimeReport.alignment(settings(), aligned(true, 72000)), "paused");
        assertEquals("World time: 18000", lines.get(0));
        assertEquals("Segment: NIGHT", lines.get(2));
        assertEquals("World time units per tick: n/a", lines.get(13));
        assertEquals("Serene Seasons installed: yes", lines.get(14));
        assertEquals("Solstice timing: MIDDLE", lines.get(16));
        assertEquals("Serene Seasons day alignment: on, Serene Seasons day length 72000 ticks", lines.get(17));
        assertEquals("Time progression: paused", lines.get(18));
        assertEquals("Controlled dimension: no", lines.get(19));
    }

    /** A Serene Seasons day alignment in a given state. */
    private static DayAlignment aligned(boolean inPlace, int dayTicks) {
        return new DayAlignment() {
            @Override
            public boolean installed() {
                return true;
            }

            @Override
            public boolean aligned() {
                return inPlace;
            }

            @Override
            public boolean update(int ticks) {
                return false;
            }

            @Override
            public int dayTicks() {
                return dayTicks;
            }

            @Override
            public int ownDayTicks() {
                return 24000;
            }
        };
    }

    @Test
    void alignmentShowsTheRealStateAndTheValueInUse() throws IOException {
        Settings on = settings();
        assertEquals("Serene Seasons day alignment: n/a", TimeReport.alignment(on, DayAlignment.NONE));
        assertEquals("Serene Seasons day alignment: on, Serene Seasons day length 72000 ticks",
                TimeReport.alignment(on, aligned(true, 72000)));
        assertEquals("Serene Seasons day alignment: not applied, Serene Seasons day length 24000 ticks",
                TimeReport.alignment(on, aligned(false, 24000)));
        Settings off = settings("seasonal {", "    B:alignSereneSeasonsDay=false", "}");
        assertEquals("Serene Seasons day alignment: off, Serene Seasons day length 24000 ticks",
                TimeReport.alignment(off, aligned(false, 24000)));
    }

    @Test
    void everySubcommandNeedsOperatorLevel() {
        SeasonalTimeCommand command = new SeasonalTimeCommand(new SeasonalTimeConfig(new File("unused"), message -> { }), null, null, null);
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
