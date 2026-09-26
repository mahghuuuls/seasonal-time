package com.mahghuuuls.seasonaltime.season;

import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The failure policy around a season mod, with a fake source that fails on demand. */
class GuardedSeasonSourceTest {

    private static final class FakeSource implements SeasonSource {
        boolean failing;
        boolean missingClass;
        int reads;

        @Override
        public boolean installed() {
            return true;
        }

        @Override
        public SeasonReading read(World world) {
            reads++;
            if (failing) {
                throw new IllegalStateException("season mod broke");
            }
            if (missingClass) {
                // What a season mod version without an expected class gives.
                throw new NoClassDefFoundError("sereneseasons/api/season/SeasonHelper");
            }
            return SeasonReading.of(0.375, "SUMMER", "MID_SUMMER");
        }
    }

    private final List<String> messages = new ArrayList<String>();
    private final List<Throwable> causes = new ArrayList<Throwable>();
    private final FakeSource fake = new FakeSource();
    private final GuardedSeasonSource guarded = new GuardedSeasonSource(fake, (message, cause) -> {
        messages.add(message);
        causes.add(cause);
    });

    @Test
    void aWorkingSourcePassesReadingsThrough() {
        SeasonReading reading = guarded.read(null);
        assertTrue(reading.available());
        assertEquals(0.375, reading.yearPosition());
        assertTrue(messages.isEmpty());
    }

    @Test
    void aFailureGivesNoSeasonAndIsLoggedOnceWithItsCause() {
        fake.failing = true;
        assertSame(SeasonReading.UNAVAILABLE, guarded.read(null));
        assertSame(SeasonReading.UNAVAILABLE, guarded.read(null));
        assertEquals(1, messages.size());
        assertTrue(causes.get(0) instanceof IllegalStateException);
        assertFalse(guarded.suspended());
    }

    @Test
    void fiveFailuresInARowSuspendAndStopCalling() {
        fake.failing = true;
        for (int i = 0; i < GuardedSeasonSource.SUSPEND_AFTER; i++) {
            guarded.read(null);
        }
        assertTrue(guarded.suspended());
        assertEquals(2, messages.size(), messages.toString());
        assertTrue(messages.get(1).contains("suspended"));
        int readsBefore = fake.reads;
        fake.failing = false;
        assertSame(SeasonReading.UNAVAILABLE, guarded.read(null));
        assertEquals(readsBefore, fake.reads, "a suspended source must not be called");
    }

    @Test
    void aSuccessResetsTheCount() {
        fake.failing = true;
        for (int i = 0; i < GuardedSeasonSource.SUSPEND_AFTER - 1; i++) {
            guarded.read(null);
        }
        fake.failing = false;
        guarded.read(null);
        fake.failing = true;
        for (int i = 0; i < GuardedSeasonSource.SUSPEND_AFTER - 1; i++) {
            guarded.read(null);
        }
        assertFalse(guarded.suspended());
    }

    @Test
    void aMissingClassIsCaughtToo() {
        fake.missingClass = true;
        assertSame(SeasonReading.UNAVAILABLE, guarded.read(null));
        assertEquals(1, messages.size());
        assertTrue(causes.get(0) instanceof NoClassDefFoundError);
    }

    @Test
    void resumeLogsTheNextFailureAgain() {
        fake.failing = true;
        guarded.read(null);
        guarded.resume();
        guarded.read(null);
        assertEquals(2, messages.size(), messages.toString());
        assertFalse(guarded.suspended());
    }

    @Test
    void resumeLiftsTheSuspension() {
        fake.failing = true;
        for (int i = 0; i < GuardedSeasonSource.SUSPEND_AFTER; i++) {
            guarded.read(null);
        }
        fake.failing = false;
        guarded.resume();
        assertTrue(guarded.read(null).available());
    }
}
