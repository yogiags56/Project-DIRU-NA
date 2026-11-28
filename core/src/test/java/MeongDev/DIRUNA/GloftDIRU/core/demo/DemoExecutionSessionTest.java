package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class DemoExecutionSessionTest {
    private static DemoInterpreterRuntimeState runtime(int... wire) {
        byte[] bytes = new byte[wire.length];
        for (int i = 0; i < wire.length; i++) bytes[i] = (byte) wire[i];
        return new DemoInterpreterRuntimeState(
                DemoScriptNormalizer.normalize(DemoScriptRecordDecoder.decode(bytes)));
    }

    private static DemoExecutionSession session(DemoInterpreterRuntimeState runtime) {
        return new DemoExecutionSession(runtime,
                new DemoOpcodeDryRunEngine(DemoOpcodeHandlerRegistry.dryRunRegistry()));
    }

    @Test public void traversesTopLevelRecordsInOrder() {
        DemoExecutionSession session = session(runtime(2,0, 1, 0,0,0,0,0,0,0,0,0,0, 6, 1,0,0,0));
        assertTrue(session.hasCurrentRecord());
        assertEquals(1, session.currentOpcode());
        assertEquals(Arrays.asList(1), session.step().visitedLabels());
        assertEquals(6, session.currentOpcode());
        assertEquals(Arrays.asList(6), session.step().visitedLabels());
        assertFalse(session.hasCurrentRecord());
    }

    @Test public void traversesChildrenOfPackedGroupThenNextTopLevelRecord() {
        DemoExecutionSession session = session(runtime(2,0,
                0,2, 1,0,0,0,0,0,0, 6,1,0,0,0,
                6,2,0,0,0));
        assertEquals(1, session.currentOpcode());
        session.step();
        assertEquals(6, session.currentOpcode());
        session.step();
        assertEquals(6, session.currentOpcode());
        assertEquals(1, session.currentBufferIndex());
        session.step();
        assertFalse(session.hasCurrentRecord());
    }

    @Test public void emptyGroupsAreSkippedWithoutProducingFakeRecords() {
        DemoExecutionSession session = session(runtime(2,0, 0,0, 6,1,0,0,0));
        assertEquals(6, session.currentOpcode());
        assertEquals(1, session.currentBufferIndex());
    }

    @Test public void unsupportedRecordDoesNotAdvanceCursorOrTick() {
        DemoExecutionSession session = session(runtime(1,0, 7));
        assertEquals(7, session.currentOpcode());
        try {
            session.step();
            fail("opcode 7 has no audited operand descriptor");
        } catch (IllegalArgumentException expected) {
            assertTrue(session.hasCurrentRecord());
            assertEquals(7, session.currentOpcode());
            assertEquals(0, session.tick());
        }
    }

    @Test public void resetRestartsCursorAndClearsRuntimeTick() {
        DemoExecutionSession session = session(runtime(2,0,
                6,1,0,0,0, 6,2,0,0,0));
        session.step();
        assertEquals(1, session.tick());
        assertEquals(6, session.currentOpcode());
        session.reset();
        assertEquals(1, session.currentOpcode());
        assertEquals(0, session.currentBufferIndex());
        assertEquals(0, session.tick());
    }

    @Test public void skipFlagIsPassedThroughWithoutChangingRecordBytes() {
        DemoInterpreterRuntimeState runtime = runtime(1,0, 1,0,0,0,0,0,0);
        byte[] before = runtime.buffers().get(0).copyNormalizedBytes();
        DemoExecutionSession session = session(runtime);
        session.setSkipRequested(true);
        DemoOpcodeDryRunEngine.Result result = session.step();
        assertTrue(result.isSkipped());
        assertTrue(result.visitedLabels().isEmpty());
        assertArrayEquals(before, runtime.buffers().get(0).copyNormalizedBytes());
    }

    @Test public void emptyScriptStartsExhaustedAndResetRemainsExhausted() {
        DemoExecutionSession session = session(runtime(0,0));
        assertFalse(session.hasCurrentRecord());
        session.reset();
        assertFalse(session.hasCurrentRecord());
    }
}
