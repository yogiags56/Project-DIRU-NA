package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

/** Source-derived field transition fixtures; no claim of full engine execution. */
public final class LegacyTypeTwoThreeFiveTransitionTest {
    @Test
    public void typeTwoCanBeReactivatedAsTypeThreeByMovementConsumer() {
        long[] words = new long[15];
        LegacyPackedLongRecordCodec objects = new LegacyPackedLongRecordCodec(words);
        objects.write(0, 2, 28, 3);
        objects.write(8, 3, 54, 3);
        objects.write(0, 12, 31, 6);
        objects.write(0, 17, 37, 6);

        // Source case-1 movement completion promotes the referenced B slot to type 3.
        long[] bWords = new long[15];
        LegacyByteIndexedLongRecordCodec b = new LegacyByteIndexedLongRecordCodec(bWords);
        b.write(8, (byte) 1, 0, 4);
        objects.write(0, 1, 57, 1);
        objects.write(0, 3, 28, 3);
        b.write(8, (byte) 3, 0, 4);
        objects.write(0, 5, 28, 3);

        assertEquals(5, objects.read(0, 28, 3));
        assertEquals(3, b.readType(8));
    }

    @Test
    public void typeTwoCanRemainTypeTwoWhenMovementIsNotComplete() {
        long[] words = new long[15];
        LegacyPackedLongRecordCodec objects = new LegacyPackedLongRecordCodec(words);
        objects.write(0, 2, 28, 3);
        objects.write(0, 0, 57, 1);
        objects.write(0, 4, 47, 2);
        assertEquals(2, objects.read(0, 28, 3));
    }

    @Test
    public void typeThreeIsTheOnlyObservedWholeWordReleaseMarker() {
        long[] bWords = new long[15];
        LegacyByteIndexedLongRecordCodec b = new LegacyByteIndexedLongRecordCodec(bWords);
        b.write(4, (byte) 2, 0, 4);
        assertEquals(2, b.readType(4));
        assertNotEquals(0L, bWords[4]);
        b.write(4, (byte) 3, 0, 4);
        assertEquals(3, b.readType(4));
        b.clear(4);
        assertEquals(0L, bWords[4]);
    }

    @Test
    public void typeFiveRemainsOccupiedInTheObjectAllocatorScan() {
        long[] words = new long[15];
        LegacyPackedLongRecordCodec objects = new LegacyPackedLongRecordCodec(words);
        objects.write(0, 5, 28, 3);
        objects.write(1, 1, 28, 3);
        assertEquals(5, objects.read(0, 28, 3));
        assertEquals(1, objects.read(1, 28, 3));
        assertEquals(3, LegacyPackedLongRecordCodec.ObjectLayout.on(objects)
                .findFirstFreeRecordId());
    }
}