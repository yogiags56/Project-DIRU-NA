package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyTypeSevenObjectAllocationModelTest {
    @Test
    public void typeSevenClearsOnlySelectedNineBitTileGroupAndMarksRenderState() {
        long[] words = new long[15];
        int[][] metadata = new int[3][3];
        byte[][] render = new byte[3][3];
        metadata[1][2] = (0x155 << 18) | (0x0A5 << 9) | 0x12F;
        int untouched = metadata[1][2] & 0x1FF;
        int upper = metadata[1][2] & (0x1FF << 18);

        LegacyTypeSevenObjectAllocationModel.Result result =
                LegacyTypeSevenObjectAllocationModel.allocate(words, metadata, render, 1, 2, 1, 0);

        assertEquals(1, result.getRecordId());
        long record = result.getObjectWords()[0];
        assertEquals(7, (int) ((record >>> 28) & 7L));
        assertEquals(0, (int) ((record >>> 54) & 7L));
        assertEquals(0, (int) ((record >>> 57) & 1L));
        assertEquals(0, result.getTileMetadata()[1][2] & (0x1FF << 9));
        assertEquals(untouched, result.getTileMetadata()[1][2] & 0x1FF);
        assertEquals(upper, result.getTileMetadata()[1][2] & (0x1FF << 18));
        assertEquals(24, result.getRenderState()[1][2]);
        assertEquals(metadata[1][2], (0x155 << 18) | (0x0A5 << 9) | 0x12F);
        assertEquals(0, render[1][2]);
    }

    @Test
    public void typeSevenUsesFirstFreeOneBasedRecordId() {
        long[] words = new long[15];
        words[0] = 1L << 28;
        words[1] = 2L << 28;
        LegacyTypeSevenObjectAllocationModel.Result result =
                LegacyTypeSevenObjectAllocationModel.allocate(words, new int[2][2], new byte[2][2], 0, 0, 0, -1);
        assertEquals(3, result.getRecordId());
        assertEquals(7, (int) ((result.getObjectWords()[2] >>> 28) & 7L));
        assertEquals(2, (int) ((result.getObjectWords()[2] >>> 45) & 3L));
    }
}