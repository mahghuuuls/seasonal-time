package com.mahghuuuls.seasonaltime.control;

import com.mahghuuuls.seasonaltime.config.SeasonalTimeConfig;
import com.mahghuuuls.seasonaltime.config.Settings;
import net.minecraftforge.fml.relauncher.TestMinecraftHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The controlled-dimension rule for every combination of listing and clock ownership. */
class ClockOwnershipTest {

    @TempDir
    File directory;

    @BeforeEach
    void giveForgeAGameDirectory() {
        TestMinecraftHome.setIfAbsent(directory);
    }

    private Settings listing(String... ids) throws IOException {
        StringBuilder lines = new StringBuilder("dimensions {\n    I:controlledDimensions <\n");
        for (String id : ids) {
            lines.append("        ").append(id).append('\n');
        }
        lines.append("     >\n}\n");
        File file = new File(directory, "seasonaltime.cfg");
        Files.write(file.toPath(), lines.toString().getBytes(StandardCharsets.UTF_8));
        SeasonalTimeConfig config = new SeasonalTimeConfig(file, message -> { });
        config.load();
        return config.settings();
    }

    @Test
    void defaultListControlsTheOverworldAndItsFollowers() throws IOException {
        Settings settings = listing("0");
        assertEquals(ClockRole.CONTROLLED, ClockOwnership.role(0, true, settings));
        assertEquals(ClockRole.FOLLOWS_OVERWORLD, ClockOwnership.role(-1, false, settings), "Nether");
        assertEquals(ClockRole.FOLLOWS_OVERWORLD, ClockOwnership.role(1, false, settings), "End");
        assertEquals(ClockRole.NOT_CONTROLLED, ClockOwnership.role(7, true, settings), "unlisted own clock");
    }

    @Test
    void aListedFollowerStillOnlyFollows() throws IOException {
        Settings settings = listing("0", "-1");
        assertEquals(ClockRole.FOLLOWS_OVERWORLD, ClockOwnership.role(-1, false, settings));
    }

    @Test
    void aListedOwnClockIsControlled() throws IOException {
        Settings settings = listing("0", "7");
        assertEquals(ClockRole.CONTROLLED, ClockOwnership.role(7, true, settings));
    }

    @Test
    void withoutTheOverworldListedFollowersAreNotControlled() throws IOException {
        Settings settings = listing("7");
        assertEquals(ClockRole.NOT_CONTROLLED, ClockOwnership.role(0, true, settings));
        assertEquals(ClockRole.NOT_CONTROLLED, ClockOwnership.role(-1, false, settings));
        assertEquals(ClockRole.CONTROLLED, ClockOwnership.role(7, true, settings));
    }
}
