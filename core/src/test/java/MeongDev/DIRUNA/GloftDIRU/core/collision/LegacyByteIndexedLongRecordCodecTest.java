package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyByteIndexedLongRecordCodecTest {
    @Test
    public void readsTypeSubtypeAndCoordinatesFromObservedOffsets() {
        long[] words = new long[15];
        words[4] = 3L | (2L << 4) | (45L << 13) | (91L << 20);
        LegacyByteIndexedLongRecordCodec codec =
                new LegacyByteIndexedLongRecordCodec(words);

        assertEquals(3, codec.readType(4));
        assertEquals(2, codec.readSubtype(4));
        assertEquals(45, codec.readX(4));
        assertEquals(91, codec.readY(4));
    }

    @Test
    public void coordinateLookupReturnsFirstMatchWithoutCheckingType() {
        long[] words = new long[15];
        words[2] = (7L << 13) | (8L << 20);
        words[9] = 3L | (7L << 13) | (8L << 20);
        LegacyByteIndexedLongRecordCodec codec =
                new LegacyByteIndexedLongRecordCodec(words);

        assertEquals(2, codec.findFirstByCoordinates(7, 8));
        assertEquals(-1, codec.findFirstByCoordinates(8, 7));
    }

    @Test
    public void clearZerosOnlySelectedRecord() {
        long[] words = {11L, 22L, 33L, 44L, 55L, 66L, 77L, 88L,
                99L, 111L, 122L, 133L, 144L, 155L, 166L};
        LegacyByteIndexedLongRecordCodec codec =
                new LegacyByteIndexedLongRecordCodec(words);

        codec.clear(5);

        assertEquals(0L, words[5]);
        assertEquals(55L, words[4]);
        assertEquals(77L, words[6]);
    }

    @Test
    public void sourceStyleWritePreservesOtherBitsForInRangeValue() {
        long[] words = new long[15];
        words[0] = (5L << 13) | (12L << 20) | 3L;
        LegacyByteIndexedLongRecordCodec codec =
                new LegacyByteIndexedLongRecordCodec(words);

        codec.write(0, (byte) 6, 13, 7);

        assertEquals(6, codec.readX(0));
        assertEquals(12, codec.readY(0));
        assertEquals(3, codec.readType(0));
    }

    @Test
    public void lookupReturnsMinusOneWhenNoCoordinatesMatch() {
        LegacyByteIndexedLongRecordCodec codec =
                new LegacyByteIndexedLongRecordCodec(new long[15]);
        assertEquals(-1, codec.findFirstByCoordinates(1, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSlotOutsideZeroBasedRange() {
        LegacyByteIndexedLongRecordCodec codec =
                new LegacyByteIndexedLongRecordCodec(new long[15]);
        codec.readType(15);
    }
}
