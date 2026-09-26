package com.mahghuuuls.seasonaltime.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to player: the Serene Seasons day length in ticks, or 0 for "no alignment" (ARC-005).
 * Serene Seasons 1.12.2 does not send its settings to players, so the aligned value travels here.
 */
public final class DayAlignmentMessage implements IMessage {

    private int dayTicks;

    /** For Forge, which creates the message before reading it. */
    public DayAlignmentMessage() {
    }

    DayAlignmentMessage(int dayTicks) {
        this.dayTicks = dayTicks;
    }

    int dayTicks() {
        return dayTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dayTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dayTicks);
    }

    /** Runs on the network thread, so it only hands the value to the game's main thread. */
    static final class Handler implements IMessageHandler<DayAlignmentMessage, IMessage> {

        private final DayAlignmentReceiver receiver;

        Handler(DayAlignmentReceiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public IMessage onMessage(DayAlignmentMessage message, MessageContext context) {
            int dayTicks = Math.max(0, message.dayTicks);
            FMLCommonHandler.instance().getWorldThread(context.netHandler)
                    .addScheduledTask(() -> receiver.receive(dayTicks));
            return null;
        }
    }
}
