package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class DemoStageCellCallbackExecutorTest {
    private static final class FakeStage implements DemoStageMutationAdapter.StageGrid {
        final byte[][] player = new byte[4][4];
        final int[][] background = new int[4][4];
        final short[][] foreground = new short[4][4];
        final List<String> writes = new ArrayList<String>();

        public int width() { return player.length; }
        public int height() { return player[0].length; }
        public void setPlayerLayer(int x, int y, byte value) {
            writes.add("P:" + x + ":" + y + ":" + value);
            player[x][y] = value;
        }
        public void setBackground(int x, int y, int value) {
            writes.add("B:" + x + ":" + y + ":" + value);
            background[x][y] = value;
        }
        public void setForeground(int x, int y, short value) {
            writes.add("F:" + x + ":" + y + ":" + value);
            foreground[x][y] = value;
        }
    }

    private static DemoLegacyBufferLayout.RecordSlot record(int opcode, int x, int y, int value) {
        return DemoLegacyBufferLayout.topLevelLeaf(new byte[]{
                (byte) opcode, 0,
                (byte) x, (byte) (x >>> 8),
                (byte) y, (byte) (y >>> 8),
                (byte) value, (byte) (value >>> 8)
        }).slot(0);
    }

    @Test public void opcode5PreservesBothImmediateCallbackInvocationsInOrder() {
        FakeStage stage = new FakeStage();
        DemoStageCellCallbackExecutor executor =
                new DemoStageCellCallbackExecutor(new DemoStageMutationAdapter(stage));

        DemoStageCellCallbackExecutor.Result result = executor.execute(record(5, 2, 1, 0x1234));

        assertEquals(Arrays.asList(5, 9), result.callbackLabels());
        assertEquals(Arrays.asList(
                "P:2:1:52", "B:2:1:0",
                "P:2:1:0", "B:2:1:4660"), stage.writes);
        assertEquals(0, stage.player[2][1]);
        assertEquals(0x1234, stage.background[2][1]);
    }

    @Test public void opcode8FallsThroughToOpcode9Callback() {
        FakeStage stage = new FakeStage();
        DemoStageCellCallbackExecutor executor =
                new DemoStageCellCallbackExecutor(new DemoStageMutationAdapter(stage));

        DemoStageCellCallbackExecutor.Result result = executor.execute(record(8, 1, 3, 0x5678));

        assertEquals(Arrays.asList(9), result.callbackLabels());
        assertEquals(Arrays.asList(
                "P:1:3:0", "B:1:3:22136"), stage.writes);
    }

    @Test public void opcode9ExecutesItsOwnCallback() {
        FakeStage stage = new FakeStage();
        DemoStageCellCallbackExecutor executor =
                new DemoStageCellCallbackExecutor(new DemoStageMutationAdapter(stage));

        DemoStageCellCallbackExecutor.Result result = executor.execute(record(9, 3, 2, 0x2345));

        assertEquals(Arrays.asList(9), result.callbackLabels());
        assertEquals(Arrays.asList("P:3:2:0", "B:3:2:9029"), stage.writes);
    }

    @Test public void signedShortCoordinatesArePreservedAndRejectedBeforeWrites() {
        FakeStage stage = new FakeStage();
        DemoStageCellCallbackExecutor executor =
                new DemoStageCellCallbackExecutor(new DemoStageMutationAdapter(stage));

        try {
            executor.execute(record(9, 0xFFFF, 1, 7));
            fail("negative signed-short x must be rejected");
        } catch (DemoStageMutationAdapter.StageBoundsException expected) {
            assertEquals(-1, expected.x());
            assertTrue(stage.writes.isEmpty());
        }
    }

    @Test public void unsupportedOpcodeFailsClosedWithoutMutation() {
        FakeStage stage = new FakeStage();
        DemoStageCellCallbackExecutor executor =
                new DemoStageCellCallbackExecutor(new DemoStageMutationAdapter(stage));

        try {
            executor.execute(record(25, 1, 1, 7));
            fail("opcode 25 requires the composite ordered-effect executor");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("does not support"));
            assertTrue(stage.writes.isEmpty());
        }
    }
}
