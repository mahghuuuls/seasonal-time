package com.mahghuuuls.seasonaltime.config;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The only owner of {@code config/seasonaltime.cfg}.
 *
 * <p>Everything that reads a setting reads the published {@link Settings}; everything that changes
 * one goes through a setter here. The defaults and bounds below must match the project's
 * {@code config-defaults.csv}, which a test enforces. Keep them private: a caller that needs a
 * range gets it from the setter's exception message, so no second copy of a bound can appear.
 *
 * <p>Invalid values never stop the game. A number outside its range moves to the nearest end of
 * the range. A value that cannot be read as its type uses the default. An option missing from a
 * file that already existed is added with its default; a first install, where every option is
 * missing, is not reported. Each correction is reported once through the warning sink and written
 * back to the file, so the file always shows what is actually in use.
 */
public final class SeasonalTimeConfig {

    static final String CATEGORY_TIME = "time";
    static final String CATEGORY_SEASONAL = "seasonal";
    static final String CATEGORY_DIMENSIONS = "dimensions";
    static final String CATEGORY_DEBUG = "debug";

    static final String BASE_DAY = "baseDayDurationMinutes";
    static final String BASE_NIGHT = "baseNightDurationMinutes";
    static final String SEASONAL_ADJUSTMENT = "seasonalAdjustmentEnabled";
    static final String SUMMER_PERCENT = "summerSolsticeDaylightPercent";
    static final String WINTER_PERCENT = "winterSolsticeDaylightPercent";
    static final String ALIGN_SERENE_SEASONS_DAY = "alignSereneSeasonsDay";
    static final String CONTROLLED_DIMENSIONS = "controlledDimensions";
    static final String DEBUG = "debug";

    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final int MIN_DURATION_MINUTES = 1;
    private static final int MAX_DURATION_MINUTES = 1440;

    private static final double DEFAULT_SUMMER_PERCENT = 70.0;
    private static final double MIN_SUMMER_PERCENT = 50.1;
    private static final double MAX_SUMMER_PERCENT = 99.0;

    private static final double DEFAULT_WINTER_PERCENT = 30.0;
    private static final double MIN_WINTER_PERCENT = 1.0;
    private static final double MAX_WINTER_PERCENT = 49.9;

    private static final boolean DEFAULT_SEASONAL_ADJUSTMENT = true;
    private static final boolean DEFAULT_ALIGN_SERENE_SEASONS_DAY = true;
    private static final int[] DEFAULT_CONTROLLED_DIMENSIONS = {0};
    private static final boolean DEFAULT_DEBUG = false;

    private static final String SPLIT_NOTE = " While seasonal adjustment is active, only day + night (the cycle "
            + "length) is used and the season decides the split.";

    private final File file;
    private final Consumer<String> warnings;
    private Configuration configuration;
    private volatile Settings settings;

    public SeasonalTimeConfig(File file, Consumer<String> warnings) {
        this.file = file;
        this.warnings = warnings;
    }

    /**
     * Reads the file from disk, corrects what is invalid, writes the file back with every option
     * and its current comment, and publishes the result. Used at start-up and by reload.
     *
     * @return the warnings issued, in order; empty when the file was valid
     */
    public synchronized List<String> load() {
        List<String> issued = new ArrayList<String>();
        // Decides whether a missing option is reported: on a first install every option is missing.
        boolean fileExisted = file.isFile();
        Configuration loaded = new Configuration(file);
        loaded.setCategoryComment(CATEGORY_TIME, "How many real minutes day and night last.");
        loaded.setCategoryComment(CATEGORY_SEASONAL, "Seasonal day length. Needs Serene Seasons.");
        loaded.setCategoryComment(CATEGORY_DIMENSIONS, "Which dimensions are controlled.");
        loaded.setCategoryComment(CATEGORY_DEBUG, "Diagnostics.");

        readDuration(loaded, fileExisted, BASE_DAY, "Real minutes for daytime (world time 0-11999)." + SPLIT_NOTE, issued);
        readDuration(loaded, fileExisted, BASE_NIGHT, "Real minutes for nighttime (world time 12000-23999)." + SPLIT_NOTE,
                issued);
        readBoolean(loaded, fileExisted, CATEGORY_SEASONAL, SEASONAL_ADJUSTMENT, DEFAULT_SEASONAL_ADJUSTMENT,
                "With Serene Seasons installed, split the cycle length between day and night by the time of year. "
                        + "Has no effect without Serene Seasons.", issued);
        readPercent(loaded, fileExisted, SUMMER_PERCENT, DEFAULT_SUMMER_PERCENT, MIN_SUMMER_PERCENT, MAX_SUMMER_PERCENT,
                "Share of the cycle that is daytime at the summer solstice (the middle of Mid Summer).", issued);
        readPercent(loaded, fileExisted, WINTER_PERCENT, DEFAULT_WINTER_PERCENT, MIN_WINTER_PERCENT, MAX_WINTER_PERCENT,
                "Share of the cycle that is daytime at the winter solstice (the middle of Mid Winter).", issued);
        readBoolean(loaded, fileExisted, CATEGORY_SEASONAL, ALIGN_SERENE_SEASONS_DAY, DEFAULT_ALIGN_SERENE_SEASONS_DAY,
                "With Serene Seasons installed, set its day length to this mod's cycle length so its days match "
                        + "Minecraft days. While on, the Serene Seasons \"Day Duration\" setting is ignored. Its "
                        + "config file is not changed.", issued);
        readDimensions(loaded, fileExisted, issued);
        readBoolean(loaded, fileExisted, CATEGORY_DEBUG, DEBUG, DEFAULT_DEBUG,
                "Write a Seasonal Time status line to the log every 60 seconds.", issued);

        // Saved every time, not only on a change, so that an existing file also receives the
        // current comments and ranges.
        loaded.save();
        configuration = loaded;
        settings = snapshot();
        return issued;
    }

    /** The values in use now. Never null after {@link #load()}. */
    public Settings settings() {
        return settings;
    }

    public synchronized void setBaseDayDurationMinutes(int minutes) {
        requireDuration(minutes, "Day");
        change(CATEGORY_TIME, BASE_DAY).set(minutes);
        publish();
    }

    public synchronized void setBaseNightDurationMinutes(int minutes) {
        requireDuration(minutes, "Night");
        change(CATEGORY_TIME, BASE_NIGHT).set(minutes);
        publish();
    }

    public synchronized void setSummerSolsticeDaylightPercent(double percent) {
        requirePercent(percent, "Summer solstice daylight percent", MIN_SUMMER_PERCENT, MAX_SUMMER_PERCENT);
        change(CATEGORY_SEASONAL, SUMMER_PERCENT).set(percent);
        publish();
    }

    public synchronized void setWinterSolsticeDaylightPercent(double percent) {
        requirePercent(percent, "Winter solstice daylight percent", MIN_WINTER_PERCENT, MAX_WINTER_PERCENT);
        change(CATEGORY_SEASONAL, WINTER_PERCENT).set(percent);
        publish();
    }

    public synchronized void setSeasonalAdjustmentEnabled(boolean enabled) {
        change(CATEGORY_SEASONAL, SEASONAL_ADJUSTMENT).set(enabled);
        publish();
    }

    private void publish() {
        configuration.save();
        settings = snapshot();
    }

    /**
     * Builds the published values from the file's properties, which every read and setter has
     * already left valid. This is the only place a {@link Settings} is constructed.
     */
    private Settings snapshot() {
        return new Settings(
                value(CATEGORY_TIME, BASE_DAY).getInt(),
                value(CATEGORY_TIME, BASE_NIGHT).getInt(),
                value(CATEGORY_SEASONAL, SEASONAL_ADJUSTMENT).getBoolean(),
                value(CATEGORY_SEASONAL, SUMMER_PERCENT).getDouble(),
                value(CATEGORY_SEASONAL, WINTER_PERCENT).getDouble(),
                value(CATEGORY_SEASONAL, ALIGN_SERENE_SEASONS_DAY).getBoolean(),
                value(CATEGORY_DIMENSIONS, CONTROLLED_DIMENSIONS).getIntList(),
                value(CATEGORY_DEBUG, DEBUG).getBoolean());
    }

    private Property change(String category, String key) {
        if (configuration == null) {
            throw new IllegalStateException("load() must run before a setting is changed");
        }
        return value(category, key);
    }

    private Property value(String category, String key) {
        return configuration.getCategory(category).get(key);
    }

    private static void requireDuration(int minutes, String name) {
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(name + " duration must be a whole number of minutes from "
                    + MIN_DURATION_MINUTES + " to " + MAX_DURATION_MINUTES + ".");
        }
    }

    private static void requirePercent(double percent, String name, double min, double max) {
        if (!isFinite(percent) || percent < min || percent > max) {
            throw new IllegalArgumentException(name + " must be from " + format(min) + " to " + format(max) + ".");
        }
    }

    private void readDuration(Configuration loaded, boolean fileExisted, String key, String comment, List<String> issued) {
        boolean present = readablePresent(loaded, fileExisted, CATEGORY_TIME, key, Property.Type.INTEGER, false,
                "is not a whole number", String.valueOf(DEFAULT_DURATION_MINUTES), issued);
        Property property = loaded.get(CATEGORY_TIME, key, DEFAULT_DURATION_MINUTES,
                comment + " Whole minutes, " + MIN_DURATION_MINUTES + "-" + MAX_DURATION_MINUTES + ". Default: "
                        + DEFAULT_DURATION_MINUTES + ".", MIN_DURATION_MINUTES, MAX_DURATION_MINUTES);
        if (!present) {
            return;
        }
        int value = property.getInt();
        int corrected = Math.max(MIN_DURATION_MINUTES, Math.min(MAX_DURATION_MINUTES, value));
        if (corrected != value) {
            warn(issued, CATEGORY_TIME, key, String.valueOf(value), "is outside " + MIN_DURATION_MINUTES + "-"
                    + MAX_DURATION_MINUTES, String.valueOf(corrected));
            property.set(corrected);
        }
    }

    private void readPercent(Configuration loaded, boolean fileExisted, String key, double defaultValue, double min, double max,
                             String comment, List<String> issued) {
        boolean present = readablePresent(loaded, fileExisted, CATEGORY_SEASONAL, key, Property.Type.DOUBLE, false,
                "is not a number", format(defaultValue), issued);
        Property property = loaded.get(CATEGORY_SEASONAL, key, defaultValue,
                comment + " Percent, " + format(min) + "-" + format(max) + ". Default: " + format(defaultValue) + ".",
                min, max);
        if (!present) {
            return;
        }
        double value = property.getDouble();
        if (!isFinite(value)) {
            warn(issued, CATEGORY_SEASONAL, key, property.getString(), "is not a number", format(defaultValue));
            property.set(defaultValue);
            return;
        }
        double corrected = Math.max(min, Math.min(max, value));
        if (corrected != value) {
            warn(issued, CATEGORY_SEASONAL, key, property.getString(),
                    "is outside " + format(min) + "-" + format(max), format(corrected));
            property.set(corrected);
        }
    }

    private void readBoolean(Configuration loaded, boolean fileExisted, String category, String key, boolean defaultValue,
                             String comment, List<String> issued) {
        readablePresent(loaded, fileExisted, category, key, Property.Type.BOOLEAN, false, "is not true or false",
                String.valueOf(defaultValue), issued);
        loaded.get(category, key, defaultValue, comment + " true or false. Default: " + defaultValue + ".");
    }

    private void readDimensions(Configuration loaded, boolean fileExisted, List<String> issued) {
        readablePresent(loaded, fileExisted, CATEGORY_DIMENSIONS, CONTROLLED_DIMENSIONS, Property.Type.INTEGER, true,
                "is not a list of whole numbers", Arrays.toString(DEFAULT_CONTROLLED_DIMENSIONS), issued);
        loaded.get(CATEGORY_DIMENSIONS, CONTROLLED_DIMENSIONS, DEFAULT_CONTROLLED_DIMENSIONS,
                "Dimension IDs whose clock this mod advances, one per line. The Nether, the End, and most modded "
                        + "dimensions share the Overworld clock and follow it without being listed. Default: "
                        + Arrays.toString(DEFAULT_CONTROLLED_DIMENSIONS) + ".");
    }

    /**
     * Returns true when the option is in the file and readable as its type. Otherwise reports it,
     * so the typed getter that follows fills in the default: a missing option is reported only when
     * the file already existed, and an unreadable one is removed first.
     *
     * <p>This must run before the typed getter. Forge's typed getters quietly replace an unreadable
     * value with the default, so this is the only point at which the mistake can still be seen.
     * Removing the entry also drops a wrong type prefix, which Forge would otherwise keep, making the
     * same warning return on every load.
     */
    private boolean readablePresent(Configuration loaded, boolean fileExisted, String category, String key, Property.Type type,
                                    boolean list, String problem, String defaultText, List<String> issued) {
        ConfigCategory section = loaded.hasCategory(category) ? loaded.getCategory(category) : null;
        if (section == null || !section.containsKey(key)) {
            if (fileExisted) {
                warn(issued, category, key, null, "is missing", defaultText);
            }
            return false;
        }
        Property raw = section.get(key);
        boolean readable;
        if (raw.getType() != type || raw.isList() != list) {
            readable = false;
        } else if (list) {
            readable = raw.isIntList();
        } else if (type == Property.Type.INTEGER) {
            readable = raw.isIntValue();
        } else if (type == Property.Type.DOUBLE) {
            readable = raw.isDoubleValue();
        } else {
            readable = raw.isBooleanValue();
        }
        if (readable) {
            return true;
        }
        String written = raw.isList() ? Arrays.toString(raw.getStringList()) : raw.getString();
        section.remove(key);
        warn(issued, category, key, written, problem, defaultText);
        return false;
    }

    private void warn(List<String> issued, String category, String key, String found, String problem, String used) {
        String value = found == null ? "" : " = '" + found + "'";
        String message = "Config value " + category + "." + key + value + " " + problem + "; using " + used + ".";
        issued.add(message);
        warnings.accept(message);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
