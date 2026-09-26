package com.mahghuuuls.seasonaltime.integration.sereneseasons;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Writing and restoring the Serene Seasons day length, with a fake Day Duration entry. */
class SereneSeasonsDayAlignmentTest {

    /** Serene Seasons' Day Duration entry: the value in use and the value from its config file. */
    private static final class FakeSlot implements SereneSeasonsDayAlignment.Slot {
        String value = "24000";
        String fileValue = "24000";
        int subSeasonDays = 7;
        int writes;
        boolean failing;

        private void maybeFail() {
            if (failing) {
                throw new NoSuchFieldError("value");
            }
        }

        @Override
        public String value() {
            maybeFail();
            return value;
        }

        @Override
        public String fileValue() {
            maybeFail();
            return fileValue;
        }

        @Override
        public void write(String value) {
            maybeFail();
            writes++;
            this.value = value;
        }

        @Override
        public int subSeasonDays() {
            maybeFail();
            return subSeasonDays;
        }

        /** What Serene Seasons does when its config is edited in game: a new entry from the file. */
        void reloadedFromFile(String file) {
            value = file;
            fileValue = file;
        }
    }

    private final FakeSlot slot = new FakeSlot();
    private final List<String> warnings = new ArrayList<String>();
    private final List<Throwable> causes = new ArrayList<Throwable>();
    private final SereneSeasonsDayAlignment alignment = new SereneSeasonsDayAlignment(slot, (message, cause) -> {
        warnings.add(message);
        causes.add(cause);
    });

    @Test
    void alignsAndKeepsTheFileValue() {
        assertFalse(alignment.aligned());
        assertTrue(alignment.update(72000));
        assertTrue(alignment.aligned());
        assertEquals("72000", slot.value);
        assertEquals("24000", slot.fileValue);
        assertEquals(72000, alignment.dayTicks());
        assertEquals(24000, alignment.ownDayTicks());
    }

    @Test
    void theSameValueAgainWritesNothing() {
        alignment.update(72000);
        alignment.update(72000);
        assertEquals(1, slot.writes);
    }

    @Test
    void aNewCycleLengthIsWritten() {
        alignment.update(72000);
        alignment.update(84000);
        assertEquals("84000", slot.value);
    }

    @Test
    void aResetBySereneSeasonsIsWrittenAgainAndItsNewFileValueIsKept() {
        alignment.update(72000);
        slot.reloadedFromFile("30000");
        alignment.update(72000);
        assertEquals("72000", slot.value);
        alignment.update(0);
        assertEquals("30000", slot.value);
    }

    @Test
    void zeroRestoresTheFileValueOnceAndThenLeavesSereneSeasonsAlone() {
        alignment.update(72000);
        assertTrue(alignment.update(0));
        assertEquals("24000", slot.value);
        assertFalse(alignment.aligned());
        slot.value = "48000";
        alignment.update(0);
        assertEquals("48000", slot.value);
    }

    @Test
    void zeroWithoutAlignmentWritesNothing() {
        alignment.update(0);
        assertEquals(0, slot.writes);
    }

    @Test
    void aYearLongerThanSereneSeasonsCanCountIsRefusedOnce() {
        // Serene Seasons counts a year in an int: day ticks x sub-season days x 12.
        slot.subSeasonDays = 51;
        assertTrue(alignment.update(3456000));
        slot.subSeasonDays = 52;
        assertFalse(alignment.update(3456000));
        assertEquals("24000", slot.value, "the aligned value is taken back");
        assertFalse(alignment.aligned());
        assertFalse(alignment.update(3456000));
        assertEquals(1, warnings.size(), warnings.toString());
        assertEquals(null, causes.get(0));
    }

    @Test
    void aFailingEntryTurnsAlignmentOffWithOneErrorAndItsCause() {
        slot.failing = true;
        assertFalse(alignment.update(72000));
        assertFalse(alignment.update(72000));
        assertEquals(0, alignment.dayTicks());
        assertEquals(0, alignment.ownDayTicks());
        assertEquals(1, warnings.size());
        assertTrue(causes.get(0) instanceof NoSuchFieldError);
        slot.failing = false;
        assertFalse(alignment.update(72000), "off until restart");
        assertEquals("24000", slot.value);
    }

    @Test
    void aFailingReadNeverThrowsAndTurnsAlignmentOff() {
        alignment.update(72000);
        slot.failing = true;
        assertEquals(0, alignment.dayTicks());
        assertFalse(alignment.aligned());
        assertEquals(0, alignment.ownDayTicks());
        assertFalse(alignment.update(0));
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void aMissingOrUnreadableEntryReadsAsZero() {
        slot.value = null;
        slot.fileValue = "a lot";
        assertEquals(0, alignment.dayTicks());
        assertEquals(0, alignment.ownDayTicks());
    }
}
