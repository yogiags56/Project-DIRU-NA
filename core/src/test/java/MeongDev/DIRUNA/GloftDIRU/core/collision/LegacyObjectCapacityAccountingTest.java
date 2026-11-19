package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

/** Capacity accounting across the nonzero object types observed in the pinned source. */
public final class LegacyObjectCapacityAccountingTest {
    @Test
    public void everyNonzeroObjectTypeConsumesOneAllocatorSlot() {
        long[] words = new long[LegacyPackedLongRecordCodec.SLOT_COUNT];
        LegacyPackedLongRecordCodec codec = new LegacyPackedLongRecordCodec(words);
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(codec);
        int[] types = {1, 2, 3, 5, 6, 7};
        for (int i = 0; i < types.length; i++) codec.write(i, types[i], 28, 3);
        assertEquals(7, objects.findFirstFreeRecordId());
        for (int id = 7; id <= 15; id++) codec.write(id - 1, 5, 28, 3);
        assertEquals(-1, objects.findFirstFreeRecordId());
    }

    @Test
    public void typeThreeMarkerDoesNotFreeCapacityUntilWholeWordClear() {
        long[] words = new long[LegacyPackedLongRecordCodec.SLOT_COUNT];
        LegacyPackedLongRecordCodec codec = new LegacyPackedLongRecordCodec(words);
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(codec);
        for (int id = 1; id <= 15; id++) codec.write(id - 1, 1, 28, 3);
        codec.write(4, 3, 28, 3);
        assertEquals(-1, objects.findFirstFreeRecordId());
        words[4] = 0L; // source fVoid(id): whole-word clear, not just type=0
        assertEquals(5, objects.findFirstFreeRecordId());
    }

    @Test
    public void typeFiveRemainsOccupiedUntilAnExplicitClearOrTableReset() {
        long[] words = new long[LegacyPackedLongRecordCodec.SLOT_COUNT];
        LegacyPackedLongRecordCodec codec = new LegacyPackedLongRecordCodec(words);
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(codec);
        codec.write(0, 5, 28, 3);
        assertEquals(2, objects.findFirstFreeRecordId());
        words[0] = 0L;
        assertEquals(1, objects.findFirstFreeRecordId());
    }
}