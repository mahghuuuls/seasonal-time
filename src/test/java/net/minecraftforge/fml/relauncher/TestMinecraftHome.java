package net.minecraftforge.fml.relauncher;

import java.io.File;

/**
 * Gives Forge's {@code Configuration} the game directory it expects outside a running game.
 *
 * <p>{@code Configuration}'s file constructor reads the game directory that FML records during
 * launch, only to shorten the file's path for its own bookkeeping. A unit test has no launch, so
 * that directory is null and the constructor fails before it reads anything. This helper sits in
 * FML's package because the field is package-private; it exists only in the test source set.
 */
public final class TestMinecraftHome {

    private TestMinecraftHome() {
    }

    public static void setIfAbsent(File directory) {
        if (FMLInjectionData.minecraftHome == null) {
            FMLInjectionData.minecraftHome = directory;
        }
    }
}
