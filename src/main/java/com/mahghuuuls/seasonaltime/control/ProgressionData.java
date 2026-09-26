package com.mahghuuuls.seasonaltime.control;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldSavedData;

/**
 * The one piece of state Seasonal Time saves: whether time is running or paused.
 *
 * <p>Stored in the Overworld's shared storage as {@code data/seasonaltime.dat}. World time and the
 * season are never copied here; Minecraft and Serene Seasons save those themselves.
 */
public final class ProgressionData extends WorldSavedData {

    static final String NAME = "seasonaltime";
    private static final String RUNNING = "Running";

    /** Written on the server thread, read by RCON commands. */
    private volatile boolean running = true;

    /** Used by Minecraft when it loads the file; the name must be the storage key. */
    public ProgressionData(String name) {
        super(name);
    }

    boolean running() {
        return running;
    }

    void setRunning(boolean running) {
        if (this.running != running) {
            this.running = running;
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        running = !nbt.hasKey(RUNNING) || nbt.getBoolean(RUNNING);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean(RUNNING, running);
        return nbt;
    }
}
