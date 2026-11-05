package com.mahghuuuls.seasonaltime.config;

import net.minecraftforge.fml.relauncher.TestMinecraftHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.minecraftforge.common.config.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the shipped configuration against {@code config-defaults.csv}.
 *
 * <p>The checks run through the real file. The (category, option) pairs Forge writes must equal the
 * CSV's; a fresh file must give the CSV defaults; and each numeric bound is tested by writing a value
 * just inside and just outside it.
 *
 * <p>The CSV read here is a copy of the table kept with the project's design documents; the
 * {@code checkConfigDefaultsCopy} Gradle task fails the build when the two differ.
 */
class ConfigDefaultsParityTest {

    @TempDir
    File directory;

    @BeforeEach
    void giveForgeAGameDirectory() {
        TestMinecraftHome.setIfAbsent(directory);
    }

    private static final class Row {
        String category;
        String type;
        String defaultValue;
        String min;
        String max;
    }

    private static Map<String, Row> readCsv() throws IOException {
        InputStream in = ConfigDefaultsParityTest.class.getResourceAsStream("/config-defaults.csv");
        assertNotNull(in, "config-defaults.csv is missing from test resources");
        Map<String, Row> rows = new LinkedHashMap<String, Row>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            assertEquals("category,option,type,default,min,max,comment_summary", reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                // The last column is free text and may hold commas; only the first six are read.
                String[] cells = line.split(",", 7);
                Row row = new Row();
                row.category = cells[0];
                row.type = cells[2];
                row.defaultValue = cells[3];
                row.min = cells[4];
                row.max = cells[5];
                rows.put(cells[1], row);
            }
        } finally {
            reader.close();
        }
        return rows;
    }

    private Map<String, String> loadedValues(String... lines) throws IOException {
        File file = new File(directory, "seasonaltime.cfg");
        file.delete();
        if (lines.length > 0) {
            Files.write(file.toPath(), String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        }
        SeasonalTimeConfig config = new SeasonalTimeConfig(file, message -> { });
        config.load();
        Settings s = config.settings();
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put(SeasonalTimeConfig.BASE_DAY, String.valueOf(s.baseDayDurationMinutes()));
        values.put(SeasonalTimeConfig.BASE_NIGHT, String.valueOf(s.baseNightDurationMinutes()));
        values.put(SeasonalTimeConfig.SEASONAL_ADJUSTMENT, String.valueOf(s.seasonalAdjustmentEnabled()));
        values.put(SeasonalTimeConfig.SUMMER_PERCENT, String.valueOf(s.summerSolsticeDaylightPercent()));
        values.put(SeasonalTimeConfig.WINTER_PERCENT, String.valueOf(s.winterSolsticeDaylightPercent()));
        values.put(SeasonalTimeConfig.SOLSTICE_TIMING, s.solsticeTiming().name());
        values.put(SeasonalTimeConfig.ALIGN_SERENE_SEASONS_DAY, String.valueOf(s.alignSereneSeasonsDay()));
        values.put(SeasonalTimeConfig.CONTROLLED_DIMENSIONS, joinInts(s.controlledDimensions()));
        values.put(SeasonalTimeConfig.DEBUG, String.valueOf(s.debug()));
        return values;
    }

    private static String joinInts(int[] values) {
        List<String> parts = new ArrayList<String>();
        for (int value : values) {
            parts.add(String.valueOf(value));
        }
        return String.join(" ", parts);
    }

    @Test
    void theWrittenOptionsAndCategoriesMatchTheCsv() throws IOException {
        loadedValues();
        Configuration written = new Configuration(new File(directory, "seasonaltime.cfg"));
        Set<String> inFile = new TreeSet<String>();
        for (String category : written.getCategoryNames()) {
            for (String option : written.getCategory(category).keySet()) {
                inFile.add(category + "." + option);
            }
        }
        Set<String> inCsv = new TreeSet<String>();
        for (Map.Entry<String, Row> entry : readCsv().entrySet()) {
            inCsv.add(entry.getValue().category + "." + entry.getKey());
        }
        assertEquals(inCsv, inFile, "options or categories differ from config-defaults.csv");
    }

    @Test
    void theDefaultsMatchTheCsv() throws IOException {
        Map<String, Row> csv = readCsv();
        Map<String, String> shipped = loadedValues();
        assertEquals(csv.keySet(), shipped.keySet(), "option set differs from config-defaults.csv");
        for (Map.Entry<String, Row> entry : csv.entrySet()) {
            assertEquals(entry.getValue().defaultValue, shipped.get(entry.getKey()),
                    "default of " + entry.getKey());
        }
    }

    @Test
    void theNumericBoundsMatchTheCsv() throws IOException {
        for (Map.Entry<String, Row> entry : readCsv().entrySet()) {
            Row row = entry.getValue();
            String key = entry.getKey();
            if (row.type.equals("int")) {
                int min = Integer.parseInt(row.min);
                int max = Integer.parseInt(row.max);
                assertEquals(String.valueOf(min), valueAfterWriting(row, "I", key, String.valueOf(min)), key);
                assertEquals(String.valueOf(max), valueAfterWriting(row, "I", key, String.valueOf(max)), key);
                assertEquals(String.valueOf(min), valueAfterWriting(row, "I", key, String.valueOf(min - 1)), key);
                assertEquals(String.valueOf(max), valueAfterWriting(row, "I", key, String.valueOf(max + 1)), key);
            } else if (row.type.equals("double")) {
                double min = Double.parseDouble(row.min);
                double max = Double.parseDouble(row.max);
                assertEquals(String.valueOf(min), valueAfterWriting(row, "D", key, String.valueOf(min)), key);
                assertEquals(String.valueOf(max), valueAfterWriting(row, "D", key, String.valueOf(max)), key);
                assertEquals(String.valueOf(min), valueAfterWriting(row, "D", key, String.valueOf(min - 0.05)), key);
                assertEquals(String.valueOf(max), valueAfterWriting(row, "D", key, String.valueOf(max + 0.05)), key);
            }
        }
    }

    private String valueAfterWriting(Row row, String typePrefix, String key, String value) throws IOException {
        return loadedValues(row.category + " {", "    " + typePrefix + ":" + key + "=" + value, "}").get(key);
    }

    @Test
    void anUnknownWordForAnEnumGivesTheCsvDefault() throws IOException {
        for (Map.Entry<String, Row> entry : readCsv().entrySet()) {
            Row row = entry.getValue();
            if (row.type.equals("enum")) {
                assertEquals(row.defaultValue, valueAfterWriting(row, "S", entry.getKey(), "NO_SUCH_WORD"), entry.getKey());
            }
        }
    }

    @Test
    void everyCsvTypeIsHandled() throws IOException {
        for (Row row : readCsv().values()) {
            assertTrue(Arrays.asList("int", "double", "boolean", "intlist", "enum").contains(row.type), row.type);
        }
    }
}
