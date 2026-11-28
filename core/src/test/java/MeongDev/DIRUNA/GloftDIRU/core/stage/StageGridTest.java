package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class StageGridTest {
    @Test public void storesIndependentRowMajorLayersAndDefensivelyCopies() {
        int[] player = {-1, 2, 3, 4};
        int[] background = {-128, 0, 1, 127};
        int[] foreground = {0, 17, 28, 255};
        StageGrid grid = new StageGrid(2, 2, player, background, foreground);
        player[0] = 99;
        assertEquals(-1, grid.playerAt(0, 0));
        assertEquals(127, grid.backgroundAt(1, 1));
        assertEquals(255, grid.foregroundAt(1, 1));
        int[] copy = grid.copyBackgroundLayer();
        copy[0] = 5;
        assertEquals(-128, grid.backgroundAt(0, 0));
    }

    @Test public void validatesCoordinatesWithoutIndexingOutsideGrid() {
        StageGrid grid = new StageGrid(2, 2, new int[4], new int[4], new int[4]);
        assertTrue(grid.contains(1, 1));
        assertFalse(grid.contains(-1, 0));
        assertFalse(grid.contains(2, 0));
        try { grid.playerAt(2, 0); fail("expected bounds failure"); }
        catch (IndexOutOfBoundsException expected) { }
    }

    @Test public void rejectsInvalidDimensionsAndLayerLengths() {
        assertInvalid(0, 1, new int[0], new int[0], new int[0]);
        assertInvalid(Integer.MAX_VALUE, 2, new int[0], new int[0], new int[0]);
        assertInvalid(2, 2, new int[3], new int[4], new int[4]);
    }

    private static void assertInvalid(int w, int h, int[] p, int[] b, int[] f) {
        try { new StageGrid(w, h, p, b, f); fail("expected invalid stage"); }
        catch (IllegalArgumentException expected) { }
    }
}
