package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoStageMutationAdapterTest {
    private static final class FakeStage implements DemoStageMutationAdapter.StageGrid {
        final byte[][] player;
        final int[][] background;
        final short[][] foreground;
        final StringBuilder writes = new StringBuilder();

        FakeStage(int width, int height) {
            player = new byte[width][height];
            background = new int[width][height];
            foreground = new short[width][height];
        }
        public int width() { return player.length; }
        public int height() { return player.length == 0 ? 0 : player[0].length; }
        public void setPlayerLayer(int x, int y, byte value) {
            writes.append("P");
            player[x][y] = value;
        }
        public void setBackground(int x, int y, int value) {
            writes.append("B");
            background[x][y] = value;
        }
        public void setForeground(int x, int y, short value) {
            writes.append("F");
            foreground[x][y] = value;
        }
    }

    @Test public void callbackWritesPlayerLayerBeforeBackground() {
        FakeStage stage = new FakeStage(4, 3);
        DemoStageMutationAdapter adapter = new DemoStageMutationAdapter(stage);
        adapter.applyCallback(2, 1, (byte) 7, 0x12345678);
        assertEquals("PB", stage.writes.toString());
        assertEquals(7, stage.player[2][1]);
        assertEquals(0x12345678, stage.background[2][1]);
    }

    @Test public void callbackCanRepresentOpcode8And9ZeroPlayerLayer() {
        FakeStage stage = new FakeStage(2, 2);
        DemoStageMutationAdapter adapter = new DemoStageMutationAdapter(stage);
        adapter.applyCallback(1, 1, (byte) 0, 0x1234);
        assertEquals(0, stage.player[1][1]);
        assertEquals(0x1234, stage.background[1][1]);
    }

    @Test public void foregroundAndBackgroundWritesRemainDistinct() {
        FakeStage stage = new FakeStage(3, 3);
        DemoStageMutationAdapter adapter = new DemoStageMutationAdapter(stage);
        adapter.setForegroundCell(1, 2, (short) 0x4567);
        adapter.setBackgroundCell(1, 2, 0x12345678);
        assertEquals("FB", stage.writes.toString());
        assertEquals((short) 0x4567, stage.foreground[1][2]);
        assertEquals(0x12345678, stage.background[1][2]);
    }

    @Test public void rejectsInvalidCoordinatesBeforeAnyCallbackWrite() {
        FakeStage stage = new FakeStage(2, 2);
        DemoStageMutationAdapter adapter = new DemoStageMutationAdapter(stage);
        try {
            adapter.applyCallback(2, 0, (byte) 9, 99);
            fail("expected out-of-bounds coordinate");
        } catch (DemoStageMutationAdapter.StageBoundsException e) {
            assertEquals(2, e.x());
            assertEquals(0, e.y());
        }
        assertEquals("", stage.writes.toString());
        assertEquals(0, stage.player[1][0]);
        assertEquals(0, stage.background[1][0]);
    }

    @Test public void rejectsNegativeCoordinatesAndAllowsLastValidCell() {
        FakeStage stage = new FakeStage(2, 2);
        DemoStageMutationAdapter adapter = new DemoStageMutationAdapter(stage);
        try {
            adapter.setForegroundCell(-1, 0, (short) 1);
            fail("expected negative coordinate");
        } catch (DemoStageMutationAdapter.StageBoundsException expected) {
            assertEquals(-1, expected.x());
        }
        adapter.setForegroundCell(1, 1, (short) 42);
        assertEquals(42, stage.foreground[1][1]);
    }

    @Test public void rejectsNullStage() {
        try {
            new DemoStageMutationAdapter(null);
            fail("expected missing stage");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("required"));
        }
    }
}
