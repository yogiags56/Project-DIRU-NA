package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyRelocationReferenceModelTest {
    @Test
    public void relocationFromSlotEightAliasesReferenceAndSetsBit57() {
        long[] bWords = new long[15];
        bWords[8] = 1L | (23L << 13) | (41L << 20);
        long[] aWords = new long[15];

        LegacyRelocationReferenceModel.Result result =
                LegacyRelocationReferenceModel.relocate(bWords, aWords, 23, 41);

        assertTrue(result.didMatch());
        assertEquals(8, result.getMatchedBSlot());
        assertEquals(2, new LegacyByteIndexedLongRecordCodec(result.getBWords()).readType(8));

        LegacyPackedLongRecordCodec aCodec =
                new LegacyPackedLongRecordCodec(result.getAWords());
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(aCodec);
        assertEquals(1, result.getAllocatedARecordId());
        assertEquals(0, objects.readMode(1));
        assertEquals(1, aCodec.read(0, 57, 1));
    }

    @Test
    public void relocationFromSlotSevenKeepsBit57Clear() {
        long[] bWords = new long[15];
        bWords[7] = 1L | (12L << 13) | (19L << 20);

        LegacyRelocationReferenceModel.Result result =
                LegacyRelocationReferenceModel.relocate(bWords, new long[15], 12, 19);

        assertEquals(7, result.getMatchedBSlot());
        LegacyPackedLongRecordCodec aCodec =
                new LegacyPackedLongRecordCodec(result.getAWords());
        assertEquals(7, aCodec.read(0, 54, 3));
        assertEquals(0, aCodec.read(0, 57, 1));
    }

    @Test
    public void missingCoordinateDoesNotMarkOrAllocate() {
        long[] bWords = new long[15];
        long[] aWords = new long[15];

        LegacyRelocationReferenceModel.Result result =
                LegacyRelocationReferenceModel.relocate(bWords, aWords, 5, 6);

        assertFalse(result.didMatch());
        assertEquals(-1, result.getMatchedBSlot());
        assertEquals(-1, result.getAllocatedARecordId());
        assertArrayEquals(bWords, result.getBWords());
        assertArrayEquals(aWords, result.getAWords());
    }

    @Test
    public void zeroCoordinateCanMatchFirstEmptySlotAndTriggerRelocation() {
        LegacyRelocationReferenceModel.Result result =
                LegacyRelocationReferenceModel.relocate(new long[15], new long[15], 0, 0);

        assertTrue(result.didMatch());
        assertEquals(0, result.getMatchedBSlot());
        assertEquals(2, new LegacyByteIndexedLongRecordCodec(result.getBWords()).readType(0));
        LegacyPackedLongRecordCodec aCodec =
                new LegacyPackedLongRecordCodec(result.getAWords());
        assertEquals(0, aCodec.read(0, 54, 3));
        assertEquals(0, aCodec.read(0, 57, 1));
    }
}
