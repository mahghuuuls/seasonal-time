package com.mahghuuuls.seasonaltime.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where the solstices and equinoxes fall in the seasonal year.
 *
 * <p>The year position runs from 0 at the start of Early Spring to 1 at the end of Late Winter, in
 * twelve equal sub-seasons. Each timing names the spring equinox; the summer solstice, the autumn
 * equinox, and the winter solstice follow a quarter year apart.
 */
public enum SolsticeTiming {

    /** The middle of Mid Spring, Mid Summer, Mid Autumn, and Mid Winter. */
    MIDDLE(1.0 / 8),

    /**
     * Two thirds of the way through Early Spring, Early Summer, Early Autumn, and Early Winter.
     * With spring starting on March 1, as in the meteorological seasons, this puts the points within
     * a few days of the real equinoxes and solstices, about three weeks into each season.
     */
    REALISTIC(1.0 / 18);

    private final double springEquinox;

    SolsticeTiming(double springEquinox) {
        this.springEquinox = springEquinox;
    }

    public double springEquinox() {
        return springEquinox;
    }

    public double summerSolstice() {
        return springEquinox + 0.25;
    }

    public double autumnEquinox() {
        return springEquinox + 0.5;
    }

    public double winterSolstice() {
        return springEquinox + 0.75;
    }

    /** The timing a word names, ignoring case and surrounding spaces; null when it names none. */
    public static SolsticeTiming fromWord(String word) {
        if (word == null) {
            return null;
        }
        String wanted = word.trim().toUpperCase(Locale.ROOT);
        for (SolsticeTiming timing : values()) {
            if (timing.name().equals(wanted)) {
                return timing;
            }
        }
        return null;
    }

    /** The accepted words as players type them: lower case, in declared order. */
    public static List<String> commandWords() {
        List<String> words = new ArrayList<String>();
        for (SolsticeTiming timing : values()) {
            words.add(timing.name().toLowerCase(Locale.ROOT));
        }
        return words;
    }

    /** The accepted words as the config file writes them, for comments and messages: "MIDDLE or REALISTIC". */
    public static String fileWordsText() {
        StringBuilder text = new StringBuilder();
        SolsticeTiming[] all = values();
        for (int i = 0; i < all.length; i++) {
            if (i > 0) {
                text.append(i == all.length - 1 ? " or " : ", ");
            }
            text.append(all[i].name());
        }
        return text.toString();
    }
}
