package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic 15-slot lifecycle fixtures; not a claim about live level reachability. */
public final class LegacyBLongArrLifecycleTest {
    @Test
    public void sequentialAllocationsCanOccupyAndResolveSlotEight() {
        long[] words = new long[LegacyPackedSlotAllocator.SLOT_COUNT];

        int lastSlot = -1;
        for (int i = 0; i < 9; i++) {
            LegacyPackedSlotAllocator.Result result =
                    LegacyPackedSlotAllocator.allocate(words,
                            (byte) (20 + i), (byte) (30 + i),
                            (byte) (40 + i), (byte) 0);
            lastSlot = result.getSlotIndex();
            assertEquals(i, lastSlot);
            words = result.getWords();
        }

        LegacyByteIndexedLongRecordCodec table =
                new LegacyByteIndexedLongRecordCodec(words);
        assertEquals(8, lastSlot);
        assertEquals(8, table.findFirstByCoordinates(38, 48));
    }

    @Test
    public void typeThreeWholeWordReleaseMakesLowestSlotReusable() {
        long[] words = new long[LegacyPackedSlotAllocator.SLOT_COUNT];
        for (int i = 0; i < 9; i++) {
            LegacyPackedSlotAllocator.Result result =
                    LegacyPackedSlotAllocator.allocate(words,
                            (byte) (10 + i), (byte) (20 + i),
                            (byte) (30 + i), (byte) 0);
            words = result.getWords();
        }

        LegacyByteIndexedLongRecordCodec table =
                new LegacyByteIndexedLongRecordCodec(words);
        table.write(3, (byte) 3, 0, 4);
        table.clear(3);

        LegacyPackedSlotAllocator.Result reused =
                LegacyPackedSlotAllocator.allocate(wordsAfterClear(words, 3),
                        (byte) 99, (byte) 98, (byte) 97, (byte) 0);
        assertEquals(3, reused.getSlotIndex());
    }

    @Test
    public void coordinateLookupPrefersEarlierDuplicateEvenWhenLaterSlotIsLive() {
        long[] words = new long[LegacyPackedSlotAllocator.SLOT_COUNT];
        words[1] = (1L << 0) | (7L << 13) | (9L << 20);
        words[8] = (1L << 0) | (7L << 13) | (9L << 20);

        LegacyByteIndexedLongRecordCodec table =
                new LegacyByteIndexedLongRecordCodec(words);
        assertEquals(1, table.findFirstByCoordinates(7, 9));
    }

    private static long[] wordsAfterClear(long[] source, int slot) {
        long[] copy = source.clone();
        copy[slot] = 0L;
        return copy;
    }
}
