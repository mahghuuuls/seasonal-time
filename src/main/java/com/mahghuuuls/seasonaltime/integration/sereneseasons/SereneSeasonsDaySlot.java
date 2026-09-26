package com.mahghuuuls.seasonaltime.integration.sereneseasons;

import sereneseasons.api.config.SeasonsOption;
import sereneseasons.api.config.SyncedConfig;

/**
 * Serene Seasons 1.2.18's Day Duration entry in {@code SyncedConfig.optionsToSync}.
 *
 * <p>The entry is looked up on every call, because Serene Seasons replaces it when its config is
 * edited in game.
 */
final class SereneSeasonsDaySlot implements SereneSeasonsDayAlignment.Slot {

    private static SyncedConfig.SyncedConfigEntry entry() {
        return SyncedConfig.optionsToSync.get(SeasonsOption.DAY_DURATION.getOptionName());
    }

    @Override
    public String value() {
        SyncedConfig.SyncedConfigEntry entry = entry();
        return entry == null ? null : entry.value;
    }

    @Override
    public String fileValue() {
        SyncedConfig.SyncedConfigEntry entry = entry();
        return entry == null ? null : entry.defaultValue;
    }

    @Override
    public void write(String value) {
        SyncedConfig.SyncedConfigEntry entry = entry();
        if (entry != null) {
            entry.value = value;
        }
    }

    @Override
    public int subSeasonDays() {
        return SyncedConfig.getIntValue(SeasonsOption.SUB_SEASON_DURATION);
    }
}
