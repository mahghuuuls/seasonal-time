package com.mahghuuuls.seasonaltime.control;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The saved switch through the same NBT calls Minecraft makes when it saves and loads the file. */
class ProgressionDataTest {

    @Test
    void pausedSurvivesASaveAndLoad() {
        ProgressionData saved = new ProgressionData(ProgressionData.NAME);
        saved.setRunning(false);
        NBTTagCompound nbt = saved.writeToNBT(new NBTTagCompound());

        ProgressionData loaded = new ProgressionData(ProgressionData.NAME);
        loaded.readFromNBT(nbt);
        assertFalse(loaded.running());
    }

    @Test
    void theSavedFormatIsFixed() throws NoSuchMethodException {
        assertEquals("seasonaltime", ProgressionData.NAME, "file name data/seasonaltime.dat");
        ProgressionData data = new ProgressionData(ProgressionData.NAME);
        data.setRunning(false);
        NBTTagCompound nbt = data.writeToNBT(new NBTTagCompound());
        assertTrue(nbt.hasKey("Running"), "saved key");
        assertTrue(java.lang.reflect.Modifier.isPublic(ProgressionData.class.getModifiers()),
                "Minecraft's saved-data loader needs a public class");
        assertTrue(java.lang.reflect.Modifier.isPublic(ProgressionData.class.getConstructor(String.class).getModifiers()),
                "Minecraft creates saved data through a public (String) constructor");
        assertEquals("doDaylightCycle", ProgressionSwitch.DAYLIGHT_RULE);
    }

    @Test
    void aFileWithoutTheKeyReadsAsRunning() {
        ProgressionData loaded = new ProgressionData(ProgressionData.NAME);
        loaded.setRunning(false);
        loaded.readFromNBT(new NBTTagCompound());
        assertTrue(loaded.running());
    }

    @Test
    void changingTheSwitchMarksItForSaving() {
        ProgressionData data = new ProgressionData(ProgressionData.NAME);
        assertFalse(data.isDirty());
        data.setRunning(true);
        assertFalse(data.isDirty(), "no change, nothing to save");
        data.setRunning(false);
        assertTrue(data.isDirty());
    }
}
