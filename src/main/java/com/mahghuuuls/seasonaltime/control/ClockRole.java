package com.mahghuuuls.seasonaltime.control;

/** How Seasonal Time treats a dimension's clock. */
public enum ClockRole {

    /** Seasonal Time advances this dimension's own clock. */
    CONTROLLED("yes"),
    /** The dimension shows the Overworld's clock, which Seasonal Time advances. */
    FOLLOWS_OVERWORLD("follows Overworld"),
    /** Seasonal Time does not touch this dimension's clock. */
    NOT_CONTROLLED("no");

    private final String label;

    ClockRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
