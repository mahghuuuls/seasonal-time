package com.mahghuuuls.seasonaltime.config;

import net.minecraftforge.fml.relauncher.TestMinecraftHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link SeasonalTimeConfig} against real files through Forge's own reader and writer,
 * which is the production path: nothing here stubs the file format.
 */
class SeasonalTimeConfigTest {

    @TempDir
    File directory;

    @BeforeEach
    void giveForgeAGameDirectory() {
        TestMinecraftHome.setIfAbsent(directory);
    }

    private final List<String> warnings = new ArrayList<String>();

    private SeasonalTimeConfig configWith(String... lines) throws IOException {
        File file = new File(directory, "seasonaltime.cfg");
        if (lines.length > 0) {
            Files.write(file.toPath(), String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        }
        return new SeasonalTimeConfig(file, warnings::add);
    }

    /**
     * The warnings other than "is missing". Most tests write a file holding only the option under
     * test, so every other option is reported as missing from that existing file; those reports are
     * covered by their own test.
     */
    private List<String> problems() {
        List<String> result = new ArrayList<String>();
        for (String warning : warnings) {
            if (!warning.contains(" is missing;")) {
                result.add(warning);
            }
        }
        return result;
    }

    private String fileText() throws IOException {
        return new String(Files.readAllBytes(new File(directory, "seasonaltime.cfg").toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void freshFileIsWrittenWithEveryOptionAndItsComments() throws IOException {
        SeasonalTimeConfig config = configWith();
        assertTrue(config.load().isEmpty());
        String text = fileText();
        for (String key : new String[]{"baseDayDurationMinutes", "baseNightDurationMinutes",
                "seasonalAdjustmentEnabled", "summerSolsticeDaylightPercent", "winterSolsticeDaylightPercent",
                "alignSereneSeasonsDay", "controlledDimensions", "debug"}) {
            assertTrue(text.contains(key), key + " missing from the generated file");
        }
        assertTrue(text.contains("only day + night (the cycle length) is used"), "seasonal ratio comment missing");
        assertTrue(text.contains("\"Day Duration\" setting is ignored"), "alignment comment missing");
        assertTrue(text.contains("Whole minutes, 1-1440. Default: 30."), "duration range missing");
        assertTrue(text.contains("Percent, 50.1-99.0. Default: 70.0."), "summer range missing");
        assertTrue(text.contains("Percent, 1.0-49.9. Default: 30.0."), "winter range missing");
        assertTrue(text.contains("true or false. Default: true."), "boolean default missing");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void durationBelowMinimumUsesMinimumAndWarnsOnce() throws IOException {
        SeasonalTimeConfig config = configWith("time {", "    I:baseDayDurationMinutes=0", "}");
        config.load();
        assertEquals(1, config.settings().baseDayDurationMinutes());
        assertEquals(1, problems().size());
        assertTrue(problems().get(0).contains("time.baseDayDurationMinutes"));
        assertTrue(fileText().contains("I:baseDayDurationMinutes=1"), "correction not written back");
    }

    @Test
    void durationAboveMaximumUsesMaximum() throws IOException {
        SeasonalTimeConfig config = configWith("time {", "    I:baseNightDurationMinutes=5000", "}");
        config.load();
        assertEquals(1440, config.settings().baseNightDurationMinutes());
        assertEquals(1, problems().size());
    }

    @Test
    void unreadableDurationUsesDefault() throws IOException {
        SeasonalTimeConfig config = configWith("time {", "    I:baseDayDurationMinutes=abc", "}");
        config.load();
        assertEquals(30, config.settings().baseDayDurationMinutes());
        assertEquals(1, problems().size());
        assertTrue(problems().get(0).contains("'abc'"));
    }

    @Test
    void unreadableBooleanUsesDefaultAndWarns() throws IOException {
        SeasonalTimeConfig config = configWith("debug {", "    B:debug=yes", "}");
        config.load();
        assertFalse(config.settings().debug());
        assertEquals(1, problems().size());
        assertTrue(problems().get(0).contains("debug.debug = 'yes'"), problems().get(0));
        assertTrue(problems().get(0).contains("using false"), problems().get(0));
    }

    @Test
    void unreadablePercentUsesDefaultAndWarns() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:summerSolsticeDaylightPercent=abc", "}");
        config.load();
        assertEquals(70.0, config.settings().summerSolsticeDaylightPercent());
        assertEquals(1, problems().size());
        assertTrue(problems().get(0).contains("'abc'"), problems().get(0));
    }

    @Test
    void unreadableWinterPercentUsesDefault() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:winterSolsticeDaylightPercent=abc", "}");
        config.load();
        assertEquals(30.0, config.settings().winterSolsticeDaylightPercent());
        assertEquals(1, problems().size());
    }

    @Test
    void wrongTypePrefixIsReplacedSoTheWarningDoesNotRepeat() throws IOException {
        SeasonalTimeConfig config = configWith(
                "dimensions {", "    S:controlledDimensions <", "        0", "     >", "}");
        config.load();
        assertEquals(1, problems().size());
        assertTrue(fileText().contains("I:controlledDimensions <"), "type prefix not corrected");
        warnings.clear();
        config.load();
        assertTrue(warnings.isEmpty(), "warning repeated on the next load: " + warnings);
    }

    @Test
    void correctedPercentIsWrittenBackSoTheNextLoadIsQuiet() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:summerSolsticeDaylightPercent=40", "}");
        config.load();
        assertTrue(problems().get(0).contains("using 50.1"), problems().get(0));
        assertTrue(fileText().contains("D:summerSolsticeDaylightPercent=50.1"), "correction not written back");
        warnings.clear();
        config.load();
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void summerBelowRangeMovesToItsLowestValue() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:summerSolsticeDaylightPercent=40", "}");
        config.load();
        assertEquals(50.1, config.settings().summerSolsticeDaylightPercent());
        assertEquals(1, problems().size());
    }

    @Test
    void summerAboveRangeMovesToItsHighestValue() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:summerSolsticeDaylightPercent=99.5", "}");
        config.load();
        assertEquals(99.0, config.settings().summerSolsticeDaylightPercent());
        assertEquals(1, problems().size());
    }

    @Test
    void winterAboveRangeMovesToItsHighestValue() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:winterSolsticeDaylightPercent=60", "}");
        config.load();
        assertEquals(49.9, config.settings().winterSolsticeDaylightPercent());
        assertEquals(1, problems().size());
    }

    @Test
    void optionMissingFromAnExistingFileIsAddedWithAWarning() throws IOException {
        SeasonalTimeConfig config = configWith();
        config.load();
        String without = fileText().replaceAll("(?m)^\\s*B:debug=false\\s*$", "");
        Files.write(new File(directory, "seasonaltime.cfg").toPath(), without.getBytes(StandardCharsets.UTF_8));
        warnings.clear();
        config.load();
        assertFalse(config.settings().debug());
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("debug.debug is missing; using false"), warnings.get(0));
        assertTrue(fileText().contains("B:debug=false"), "missing option not written back");
    }

    @Test
    void notANumberPercentUsesDefault() throws IOException {
        SeasonalTimeConfig config = configWith("seasonal {", "    D:summerSolsticeDaylightPercent=NaN", "}");
        config.load();
        assertEquals(70.0, config.settings().summerSolsticeDaylightPercent());
        assertEquals(1, problems().size());
    }

    @Test
    void validNonDefaultValuesAreKeptWithoutWarnings() throws IOException {
        SeasonalTimeConfig config = configWith(
                "time {", "    I:baseDayDurationMinutes=40", "    I:baseNightDurationMinutes=20", "}",
                "seasonal {", "    D:summerSolsticeDaylightPercent=80", "    D:winterSolsticeDaylightPercent=40",
                "    B:alignSereneSeasonsDay=false", "}",
                "dimensions {", "    I:controlledDimensions <", "        0", "        7", "     >", "}",
                "debug {", "    B:debug=true", "}");
        config.load();
        Settings settings = config.settings();
        assertEquals(40, settings.baseDayDurationMinutes());
        assertEquals(20, settings.baseNightDurationMinutes());
        assertEquals(60, settings.cycleLengthMinutes());
        assertEquals(80.0, settings.summerSolsticeDaylightPercent());
        assertEquals(40.0, settings.winterSolsticeDaylightPercent());
        assertFalse(settings.alignSereneSeasonsDay());
        assertArrayEquals(new int[]{0, 7}, settings.controlledDimensions());
        assertTrue(settings.isControlledDimension(7));
        assertTrue(settings.debug());
        assertTrue(problems().isEmpty(), warnings.toString());
    }

    @Test
    void unreadableDimensionListUsesDefault() throws IOException {
        SeasonalTimeConfig config = configWith(
                "dimensions {", "    I:controlledDimensions <", "        zero", "     >", "}");
        config.load();
        assertArrayEquals(new int[]{0}, config.settings().controlledDimensions());
        assertEquals(1, problems().size());
    }

    @Test
    void settersSaveAndPublishWithoutTouchingOtherValues() throws IOException {
        SeasonalTimeConfig config = configWith();
        config.load();
        config.setBaseDayDurationMinutes(40);
        config.setBaseNightDurationMinutes(25);
        config.setSummerSolsticeDaylightPercent(75.5);
        config.setWinterSolsticeDaylightPercent(22.5);
        config.setSeasonalAdjustmentEnabled(false);
        assertEquals(40, config.settings().baseDayDurationMinutes());
        assertEquals(25, config.settings().baseNightDurationMinutes());
        assertEquals(75.5, config.settings().summerSolsticeDaylightPercent());
        assertEquals(22.5, config.settings().winterSolsticeDaylightPercent());
        assertFalse(config.settings().seasonalAdjustmentEnabled());
        assertTrue(config.settings().alignSereneSeasonsDay(), "an unrelated value changed");

        SeasonalTimeConfig reread = new SeasonalTimeConfig(new File(directory, "seasonaltime.cfg"), warnings::add);
        reread.load();
        assertEquals(40, reread.settings().baseDayDurationMinutes());
        assertEquals(25, reread.settings().baseNightDurationMinutes());
        assertEquals(75.5, reread.settings().summerSolsticeDaylightPercent());
        assertEquals(22.5, reread.settings().winterSolsticeDaylightPercent());
        assertFalse(reread.settings().seasonalAdjustmentEnabled());
    }

    @Test
    void settersRefuseInvalidValuesAndChangeNothing() throws IOException {
        SeasonalTimeConfig config = configWith();
        config.load();
        IllegalArgumentException day = assertThrows(IllegalArgumentException.class,
                () -> config.setBaseDayDurationMinutes(0));
        assertTrue(day.getMessage().contains("1 to 1440"));
        assertThrows(IllegalArgumentException.class, () -> config.setSummerSolsticeDaylightPercent(40));
        assertThrows(IllegalArgumentException.class, () -> config.setWinterSolsticeDaylightPercent(Double.NaN));
        assertEquals(30, config.settings().baseDayDurationMinutes());
        assertEquals(70.0, config.settings().summerSolsticeDaylightPercent());
        assertEquals(30.0, config.settings().winterSolsticeDaylightPercent());
    }

    @Test
    void commandTextSettersRefuseWithTheRangeAndChangeNothing() throws IOException {
        SeasonalTimeConfig config = configWith();
        config.load();
        IllegalArgumentException text = assertThrows(IllegalArgumentException.class,
                () -> config.setBaseDayDurationMinutes("abc"));
        assertTrue(text.getMessage().contains("1 to 1440"), text.getMessage());
        IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                () -> config.setBaseNightDurationMinutes("0"));
        assertTrue(zero.getMessage().startsWith("Night") && zero.getMessage().contains("1 to 1440"), zero.getMessage());
        assertTrue(text.getMessage().startsWith("Day"), text.getMessage());
        IllegalArgumentException summer = assertThrows(IllegalArgumentException.class,
                () -> config.setSummerSolsticeDaylightPercent("40"));
        assertTrue(summer.getMessage().contains("50.1 to 99.0"), summer.getMessage());
        IllegalArgumentException winter = assertThrows(IllegalArgumentException.class,
                () -> config.setWinterSolsticeDaylightPercent("warm"));
        assertTrue(winter.getMessage().contains("1.0 to 49.9"), winter.getMessage());
        assertEquals(30, config.settings().baseDayDurationMinutes());
        assertEquals(30, config.settings().baseNightDurationMinutes());
        assertTrue(fileText().contains("I:baseDayDurationMinutes=30"));
    }

    @Test
    void commandTextSettersAcceptValidValues() throws IOException {
        SeasonalTimeConfig config = configWith();
        config.load();
        config.setBaseDayDurationMinutes(" 40 ");
        config.setBaseNightDurationMinutes("25");
        config.setSummerSolsticeDaylightPercent(" 75.5 ");
        config.setWinterSolsticeDaylightPercent("22.5");
        assertEquals(40, config.settings().baseDayDurationMinutes());
        assertEquals(25, config.settings().baseNightDurationMinutes());
        assertEquals(75.5, config.settings().summerSolsticeDaylightPercent());
        assertEquals(22.5, config.settings().winterSolsticeDaylightPercent());
        String saved = fileText();
        assertTrue(saved.contains("I:baseDayDurationMinutes=40") && saved.contains("I:baseNightDurationMinutes=25"),
                "not saved to the file");
    }

    @Test
    void reloadReadsEditsMadeOnDisk() throws IOException {
        SeasonalTimeConfig config = configWith();
        config.load();
        String edited = fileText().replace("I:baseDayDurationMinutes=30", "I:baseDayDurationMinutes=20");
        Files.write(new File(directory, "seasonaltime.cfg").toPath(), edited.getBytes(StandardCharsets.UTF_8));
        config.load();
        assertEquals(20, config.settings().baseDayDurationMinutes());
    }
}
