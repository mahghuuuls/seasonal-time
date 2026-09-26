package com.mahghuuuls.seasonaltime.network;

import org.apache.logging.log4j.Logger;

/**
 * A player's game side of Serene Seasons day alignment. FML picks the implementation with
 * {@code @SidedProxy}: the client package's on a game with a screen, {@link Ignore} on a dedicated
 * server, so common code never names a client class.
 */
public interface DayAlignmentReceiver {

    /** Called once, in pre-initialization. */
    void start(Logger logger);

    /** Called on the game's main thread with the server's day length in ticks; 0 means no alignment. */
    void receive(int dayTicks);

    /** A dedicated server never receives the message. */
    final class Ignore implements DayAlignmentReceiver {

        @Override
        public void start(Logger logger) {
        }

        @Override
        public void receive(int dayTicks) {
        }
    }
}
