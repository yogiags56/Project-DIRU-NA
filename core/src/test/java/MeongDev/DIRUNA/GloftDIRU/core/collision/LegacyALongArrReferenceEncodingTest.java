package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression coverage for the aLongArr offset-54 long-shift semantics. */
public final class LegacyALongArrReferenceEncodingTest {
    @Test
    public void offset54ValuesZeroThroughFourteenExposeThreeBitAliasAndBit57Spill() {
        for (int value = 0; value <= 14; value++) {
            long[] words = new long[LegacyPackedLongRecordCodec.SLOT_COUNT];
            LegacyPackedLongRecordCodec codec = new LegacyPackedLongRecordCodec(words);

            codec.write(0, value, 54, 3);

            assertEquals("three-bit getter value for input " + value,
                    value & 7, codec.read(0, 54, 3));
            assertEquals("bit 57 for input " + value,
                    value >= 8 ? 1 : 0, codec.read(0, 57, 1));
            assertEquals("bits 58..63 remain clear for input " + value,
                    0, codec.read(0, 58, 6));
        }
    }

    @Test
    public void negativeSentinelSetsHighBitsWhenWrittenWithoutGuard() {
        long[] words = new long[LegacyPackedLongRecordCodec.SLOT_COUNT];
        LegacyPackedLongRecordCodec codec = new LegacyPackedLongRecordCodec(words);

        codec.write(0, -1, 54, 3);

        assertEquals(7, codec.read(0, 54, 3));
        assertEquals(1, codec.read(0, 57, 1));
        assertEquals(63, codec.read(0, 58, 6));
    }

    @Test
    public void typeSevenAllocatorPathDoesNotWriteOffset54() {
        long[] words = new long[LegacyPackedLongRecordCodec.SLOT_COUNT];
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(
                        new LegacyPackedLongRecordCodec(words));

        int id = objects.allocateTypeOne(10, 11, 0, 2, 0);

        assertEquals(1, id);
        assertEquals(0, objects.readMode(id));
    }
}
