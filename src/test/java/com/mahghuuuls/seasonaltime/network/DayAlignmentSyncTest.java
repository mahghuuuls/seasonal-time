package com.mahghuuuls.seasonaltime.network;

import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.season.DayAlignment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.relauncher.TestMinecraftHome;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** When the server applies, re-applies, restores, and tells players the Serene Seasons day length. */
class DayAlignmentSyncTest {

    private static final class FakeAlignment implements DayAlignment {
        final List<Integer> updates = new ArrayList<Integer>();
        boolean installed = true;
        boolean refusing;

        @Override
        public boolean installed() {
            return installed;
        }

        @Override
        public boolean aligned() {
            return false;
        }

        @Override
        public boolean update(int dayTicks) {
            updates.add(dayTicks);
            return !(refusing && dayTicks > 0);
        }

        @Override
        public int dayTicks() {
            return 0;
        }

        @Override
        public int ownDayTicks() {
            return 24000;
        }

        int last() {
            return updates.get(updates.size() - 1);
        }
    }

    private static final class FakePlayers implements DayAlignmentSync.Players {
        final List<Integer> toAll = new ArrayList<Integer>();
        final List<Integer> toOne = new ArrayList<Integer>();

        @Override
        public void sendToAll(int dayTicks) {
            toAll.add(dayTicks);
        }

        @Override
        public void sendTo(EntityPlayerMP player, int dayTicks) {
            toOne.add(dayTicks);
        }
    }

    @TempDir
    File directory;

    private File file;
    private SeasonalTimeConfig config;
    private final FakeAlignment alignment = new FakeAlignment();
    private final FakePlayers players = new FakePlayers();
    private DayAlignmentSync sync;

    @BeforeEach
    void defaultSettings() {
        TestMinecraftHome.setIfAbsent(directory);
        file = new File(directory, "seasonaltime.cfg");
        config = new SeasonalTimeConfig(file, message -> { });
        config.load();
        sync = new DayAlignmentSync(config::settings, alignment, players, LogManager.getLogger("DayAlignmentSyncTest"));
    }

    private void ticks(int count) {
        for (int i = 0; i < count; i++) {
            sync.tick();
        }
    }

    private void reloadWithAlignment(boolean on) throws IOException {
        Files.write(file.toPath(), ("seasonal {\n    B:alignSereneSeasonsDay=" + on + "\n}\n")
                .getBytes(StandardCharsets.UTF_8));
        config.load();
    }

    @Test
    void oneSereneSeasonsDayIsOneCycle() throws IOException {
        assertEquals(72000, DayAlignmentSync.dayTicksFor(config.settings(), true));
        config.setBaseDayDurationMinutes("40");
        assertEquals(84000, DayAlignmentSync.dayTicksFor(config.settings(), true));
        config.setBaseDayDurationMinutes("1440");
        config.setBaseNightDurationMinutes("1440");
        assertEquals(3456000, DayAlignmentSync.dayTicksFor(config.settings(), true));
        assertEquals(0, DayAlignmentSync.dayTicksFor(config.settings(), false));
        reloadWithAlignment(false);
        assertEquals(0, DayAlignmentSync.dayTicksFor(config.settings(), true));
    }

    @Test
    void theValueIsAppliedAtAboutToStartWithoutSending() {
        sync.serverAboutToStart();
        assertEquals(Collections.singletonList(72000), alignment.updates);
        assertEquals(Collections.<Integer>emptyList(), players.toAll);
    }

    @Test
    void aJoiningPlayerIsToldTheValue() {
        sync.serverAboutToStart();
        sync.playerJoined(null);
        assertEquals(Collections.singletonList(72000), players.toOne);
    }

    @Test
    void theValueIsWrittenAgainOnceASecondButSentOnlyWhenItChanges() {
        sync.serverAboutToStart();
        ticks(19);
        assertEquals(1, alignment.updates.size());
        ticks(1);
        assertEquals(Arrays.asList(72000, 72000), alignment.updates);
        assertEquals(Collections.<Integer>emptyList(), players.toAll);
    }

    @Test
    void aNewCycleLengthIsAppliedAndSentOnTheNextTick() {
        sync.serverAboutToStart();
        config.setBaseDayDurationMinutes("40");
        ticks(1);
        assertEquals(84000, alignment.last());
        assertEquals(Collections.singletonList(84000), players.toAll);
    }

    @Test
    void turningAlignmentOffRestoresAndTellsPlayers() throws IOException {
        sync.serverAboutToStart();
        reloadWithAlignment(false);
        ticks(1);
        assertEquals(0, alignment.last());
        assertEquals(Collections.singletonList(0), players.toAll);
    }

    @Test
    void aValueThatCannotBeAppliedIsNotTold() {
        alignment.refusing = true;
        sync.serverAboutToStart();
        sync.playerJoined(null);
        config.setBaseDayDurationMinutes("40");
        ticks(1);
        assertEquals(Collections.singletonList(0), players.toOne);
        assertEquals(Collections.<Integer>emptyList(), players.toAll);
    }

    @Test
    void aValueThatStopsWorkingIsWithdrawnFromPlayers() {
        sync.serverAboutToStart();
        alignment.refusing = true;
        ticks(20);
        assertEquals(Collections.singletonList(0), players.toAll);
    }

    @Test
    void stoppingGivesSereneSeasonsItsOwnSetting() {
        sync.serverAboutToStart();
        sync.serverStopping();
        assertEquals(0, alignment.last());
        sync.playerJoined(null);
        assertEquals(Collections.singletonList(0), players.toOne);
    }

    @Test
    void withoutSereneSeasonsNothingIsAlignedOrSent() {
        alignment.installed = false;
        sync.serverAboutToStart();
        ticks(40);
        sync.playerJoined(null);
        assertEquals(Collections.<Integer>emptyList(), players.toAll);
        assertEquals(Collections.singletonList(0), players.toOne);
        for (int update : alignment.updates) {
            assertEquals(0, update, "no day length is set without Serene Seasons");
        }
    }
}
