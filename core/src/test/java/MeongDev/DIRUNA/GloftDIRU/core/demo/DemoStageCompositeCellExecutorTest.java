package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class DemoStageCompositeCellExecutorTest {
    private static final class FakeStage implements DemoStageMutationAdapter.StageGrid {
        final List<String> writes = new ArrayList<String>();
        public int width() { return 4; }
        public int height() { return 4; }
        public void setPlayerLayer(int x, int y, byte value) {
            writes.add("P:" + x + ":" + y + ":" + value);
        }
        public void setBackground(int x, int y, int value) {
            writes.add("B:" + x + ":" + y + ":" + value);
        }
        public void setForeground(int x, int y, short value) {
            writes.add("F:" + x + ":" + y + ":" + value);
        }
    }

    private static DemoLegacyBufferLayout.RecordSlot record(byte[] bytes) {
        if (bytes.length != 8) throw new IllegalArgumentException("normalized opcode record must be 8 bytes");
        // Legacy opcode-26 reads u32 at +6 from the shared packed group array.
        // The final two bytes therefore come from the following sibling record.
        byte[] group = new byte[]{
                0, 2,
                8, 0, 0, 0,
                2, 0, 0, 0,
                bytes[0], bytes[1], bytes[2], bytes[3],
                bytes[4], bytes[5], bytes[6], bytes[7],
                52, 0
        };
        return DemoLegacyBufferLayout.packedGroup(group).slot(0);
    }

    @Test public void opcode25PreservesForegroundBackgroundAndCallbackFallThroughOrder() {
        FakeStage stage = new FakeStage();
        byte[] bytes = new byte[]{25, 0, 2, 0, 1, 0, 5, 0x12};
        DemoStageCompositeCellExecutor executor =
                new DemoStageCompositeCellExecutor(new DemoStageMutationAdapter(stage));

        DemoStageCompositeCellExecutor.Result result = executor.execute(record(bytes));

        assertEquals(Arrays.asList(
                "F:2:1:4613",
                "B:2:1:3412485",
                "P:2:1:5", "B:2:1:0",
                "P:2:1:0", "B:2:1:4613"), stage.writes);
        assertEquals(Arrays.asList(25, 26, 5, 9, 27), result.completedLabels());
        assertFalse(result.dialogueReached());
        assertArrayEquals(new byte[]{25, 0, 2, 0, 1, 0, 5, 0x12}, bytes);
    }

    @Test public void opcode26WithClearGateReachesDialogueWithoutCompletingLabel27() {
        FakeStage stage = new FakeStage();
        byte[] bytes = new byte[]{26, 0, 1, 0, 2, 0, 7, 0};
        DemoStageCompositeCellExecutor executor =
                new DemoStageCompositeCellExecutor(new DemoStageMutationAdapter(stage));

        DemoStageCompositeCellExecutor.Result result = executor.execute(record(bytes));

        assertEquals(Arrays.asList(
                "B:1:2:3407879",
                "P:1:2:7", "B:1:2:0",
                "P:1:2:0", "B:1:2:7"), stage.writes);
        assertEquals(Arrays.asList(26, 5, 9), result.completedLabels());
        assertTrue(result.dialogueReached());
    }

    @Test public void opcode25PreservesSignedHighByteWhenBuildingForegroundValue() {
        FakeStage stage = new FakeStage();
        byte[] bytes = new byte[]{25, 0, 1, 0, 1, 0, 5, (byte) 0x80};
        DemoStageCompositeCellExecutor executor =
                new DemoStageCompositeCellExecutor(new DemoStageMutationAdapter(stage));

        executor.execute(record(bytes));

        assertEquals("F:1:1:-32763", stage.writes.get(0));
    }

    @Test public void invalidCoordinatesFailBeforeAnyCompositeStageWrite() {
        FakeStage stage = new FakeStage();
        byte[] bytes = new byte[]{25, 0, (byte) 0xFF, (byte) 0xFF, 1, 0, 5, 0x12};
        DemoStageCompositeCellExecutor executor =
                new DemoStageCompositeCellExecutor(new DemoStageMutationAdapter(stage));

        try {
            executor.execute(record(bytes));
            fail("negative signed-short coordinate must fail");
        } catch (DemoStageMutationAdapter.StageBoundsException expected) {
            assertEquals(-1, expected.x());
            assertTrue(stage.writes.isEmpty());
        }
    }

    @Test public void unsupportedEntryOpcodeFailsClosedWithoutWrites() {
        FakeStage stage = new FakeStage();
        DemoStageCompositeCellExecutor executor =
                new DemoStageCompositeCellExecutor(new DemoStageMutationAdapter(stage));

        try {
            executor.execute(record(new byte[]{9, 0, 1, 0, 1, 0, 7, 0, 0, 0}));
            fail("opcode 9 belongs to the single callback executor");
        } catch (IllegalArgumentException expected) {
            assertTrue(stage.writes.isEmpty());
        }
    }
}
