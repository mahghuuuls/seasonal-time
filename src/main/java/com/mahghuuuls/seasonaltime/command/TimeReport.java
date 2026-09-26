package com.mahghuuuls.seasonaltime.command;

import com.mahghuuuls.seasonaltime.config.Settings;
import com.mahghuuuls.seasonaltime.control.ClockRole;
import com.mahghuuuls.seasonaltime.control.ClockStatus;
import com.mahghuuuls.seasonaltime.time.CycleDurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The text of every read-only {@code /seasonaltime} reply.
 *
 * <p>Built from snapshots only ({@link Settings}, {@link ClockStatus}, and the world time the
 * sender sees), so it needs no running game. It only formats: effective values come from the
 * controller's status, and when there is none (a clock Seasonal Time does not advance) they print
 * {@code n/a}, as does every value that is not available yet.
 */
final class TimeReport {

    static final String NOT_AVAILABLE = "n/a";

    private TimeReport() {
    }

    static List<String> base(Settings settings) {
        return Arrays.asList(day(settings), night(settings), cycle(settings));
    }

    static String day(Settings settings) {
        return "Base day duration: " + minutes(settings.baseDayDurationMinutes());
    }

    static String night(Settings settings) {
        return "Base night duration: " + minutes(settings.baseNightDurationMinutes());
    }

    private static String cycle(Settings settings) {
        return "Total cycle duration: " + minutes(settings.cycleLengthMinutes());
    }

    static List<String> seasonal(Settings settings, ClockStatus status) {
        return Arrays.asList(
                "Seasonal adjustment enabled: " + settings.seasonalAdjustmentEnabled(),
                "Seasonal adjustment active: " + NOT_AVAILABLE,
                "Summer solstice daylight: " + percent(settings.summerSolsticeDaylightPercent()),
                "Winter solstice daylight: " + percent(settings.winterSolsticeDaylightPercent()),
                daylight(status));
    }

    static List<String> effective(ClockStatus status) {
        return Arrays.asList(
                "Effective day duration: " + (status == null ? NOT_AVAILABLE : minutes(status.dayMinutes())),
                "Effective night duration: " + (status == null ? NOT_AVAILABLE : minutes(status.nightMinutes())),
                daylight(status));
    }

    static List<String> debug(Settings settings, ClockStatus status, long worldTime, ClockRole role,
                              boolean sereneSeasonsInstalled) {
        List<String> lines = new ArrayList<String>();
        lines.add("World time: " + worldTime);
        lines.add("Time of day: " + CycleDurations.timeOfDay(worldTime));
        lines.add("Segment: " + (CycleDurations.isDay(worldTime) ? "DAY" : "NIGHT"));
        lines.add(day(settings));
        lines.add(night(settings));
        lines.add(cycle(settings));
        lines.addAll(effective(status));
        lines.add("Season: " + NOT_AVAILABLE);
        lines.add("Sub-season: " + NOT_AVAILABLE);
        lines.add("Year position: " + NOT_AVAILABLE);
        lines.add("Fractional remainder: " + (status == null ? NOT_AVAILABLE : decimal(status.remainder(), 4)));
        lines.add("World time units per tick: " + (status == null ? NOT_AVAILABLE : decimal(status.unitsPerTick(), 4)));
        lines.add("Serene Seasons installed: " + (sereneSeasonsInstalled ? "yes" : "no"));
        lines.add("Seasonal adjustment active: " + NOT_AVAILABLE);
        lines.add("Serene Seasons day alignment: " + NOT_AVAILABLE);
        lines.add("Time progression: " + NOT_AVAILABLE);
        lines.add("Controlled dimension: " + role.label());
        return lines;
    }

    private static String daylight(ClockStatus status) {
        return "Current daylight percentage: " + (status == null ? NOT_AVAILABLE : percent(status.daylightPercent()));
    }

    private static String minutes(double value) {
        return decimal(value, 1) + " minutes";
    }

    private static String percent(double value) {
        return decimal(value, 1) + "%";
    }

    private static String decimal(double value, int places) {
        return String.format(Locale.ROOT, "%." + places + "f", value);
    }
}
