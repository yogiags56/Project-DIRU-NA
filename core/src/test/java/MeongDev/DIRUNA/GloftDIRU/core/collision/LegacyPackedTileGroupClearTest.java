package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPackedTileGroupClearTest {
    @Test
    public void clearsOnlySelectedNineBitGroup() {
        int word = 0x60000000 | (0x155 << 18) | (0x0A5 << 9) | 0x12F;
        int[][] packed = {{word}};
        byte[][] states = {{3}};
        new LegacyPackedTileGroupClear(packed, states).clear(0, 0, 1);

        assertEquals(0x60000000 | (0x155 << 18) | 0x12F, packed[0][0]);
        assertEquals(24, states[0][0]);
    }

    @Test
    public void doesNotRefreshAdjacentCell() {
        int[][] packed = {{0}};
        byte[][] states = {{7, 9}};
        new LegacyPackedTileGroupClear(packed, states).clear(0, 0, 2);

        assertEquals(24, states[0][0]);
        assertEquals(9, states[0][1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGroupOutsideThreeBands() {
        new LegacyPackedTileGroupClear(new int[][]{{0}}, new byte[][]{{0}}).clear(0, 0, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCellBeforeMutation() {
        int[][] packed = {{0}};
        byte[][] states = {{0}};
        new LegacyPackedTileGroupClear(packed, states).clear(0, 1, 0);
    }
}
