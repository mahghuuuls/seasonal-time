package com.mahghuuuls.seasonaltime.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** The mod's one network channel and its one message, from server to player. */
public final class SeasonalTimeNetwork implements DayAlignmentSync.Players {

    public static final String CHANNEL = "seasonaltime";
    private static final int DAY_ALIGNMENT_MESSAGE = 0;

    private final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);

    /**
     * Registers the message on both sides, so the server can send it; only a player's game
     * receives it, and hands it to {@code receiver}.
     */
    public SeasonalTimeNetwork(DayAlignmentReceiver receiver) {
        channel.registerMessage(new DayAlignmentMessage.Handler(receiver), DayAlignmentMessage.class,
                DAY_ALIGNMENT_MESSAGE, Side.CLIENT);
    }

    @Override
    public void sendToAll(int dayTicks) {
        channel.sendToAll(new DayAlignmentMessage(dayTicks));
    }

    @Override
    public void sendTo(EntityPlayerMP player, int dayTicks) {
        channel.sendTo(new DayAlignmentMessage(dayTicks), player);
    }
}
