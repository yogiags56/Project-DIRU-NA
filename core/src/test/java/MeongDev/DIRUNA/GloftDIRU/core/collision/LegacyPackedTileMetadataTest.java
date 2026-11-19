package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;

import static org.junit.Assert.*;

public final class LegacyPackedTileMetadataTest {
    @Test
    public void writesSelectedNineBitGroupAndMarksTwoAdjacentByteCells() {
        int[][] packed = {{0x40000000, 0}};
        byte[][] states = {{5, 6, 7}};
        LegacyPackedTileMetadata metadata = new LegacyPackedTileMetadata(packed, states);

        metadata.write(0, 0, 2, 7, 3, 4);

        assertEquals(7, metadata.read(0, 0, 2, 3, 4));
        assertEquals(0x40000000 | (7 << 21), packed[0][0]);
        assertEquals(24, states[0][0]);
        assertEquals(24, states[0][1]);
        assertEquals(7, states[0][2]);
    }

    @Test
    public void replacesOnlyTheSelectedFieldAndPreservesOtherBits() {
        int initial = 0x40000000 | (3 << 12) | 0x35;
        int[][] packed = {{initial}};
        byte[][] states = {{0, 0}};
        LegacyPackedTileMetadata metadata = new LegacyPackedTileMetadata(packed, states);

        metadata.write(0, 0, 1, 6, 3, 4);

        assertEquals(6, metadata.read(0, 0, 1, 3, 4));
        assertEquals(0x40000000 | (6 << 12) | 0x35, packed[0][0]);
    }

    @Test
    public void groupStrideIsNineBits() {
        int[][] packed = {{0}};
        byte[][] states = {{0, 0}};
        LegacyPackedTileMetadata metadata = new LegacyPackedTileMetadata(packed, states);

        metadata.write(0, 0, 0, 5, 3, 4);
        metadata.write(0, 0, 1, 6, 3, 4);
        metadata.write(0, 0, 2, 7, 3, 4);

        assertEquals(5, metadata.read(0, 0, 0, 3, 4));
        assertEquals(6, metadata.read(0, 0, 1, 3, 4));
        assertEquals(7, metadata.read(0, 0, 2, 3, 4));
    }

    @Test
    public void setterRetainsUnmaskedIntValueSemantics() {
        int[][] packed = {{0}};
        byte[][] states = {{0, 0}};
        LegacyPackedTileMetadata metadata = new LegacyPackedTileMetadata(packed, states);

        metadata.write(0, 0, 0, 0x1F, 3, 4);

        // The original setter shifts the raw int value; it does not mask it to width.
        assertEquals(0x1F << 3, packed[0][0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingAdjacentByteCellBeforeMutation() {
        int[][] packed = {{0}};
        byte[][] states = {{0}};
        LegacyPackedTileMetadata metadata = new LegacyPackedTileMetadata(packed, states);

        metadata.write(0, 0, 0, 1, 3, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsFieldOutsideThirtyTwoBitWord() {
        int[][] packed = {{0}};
        byte[][] states = {{0, 0}};
        LegacyPackedTileMetadata metadata = new LegacyPackedTileMetadata(packed, states);

        metadata.write(0, 0, 2, 1, 8, 9);
    }
}
