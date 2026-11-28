package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoInterpreterRuntimeStateTest {
    private static DemoInterpreterRuntimeState state(int... wire) {
        byte[] bytes = new byte[wire.length];
        for (int i = 0; i < wire.length; i++) bytes[i] = (byte) wire[i];
        return new DemoInterpreterRuntimeState(
                DemoScriptNormalizer.normalize(DemoScriptRecordDecoder.decode(bytes)));
    }

    @Test public void keepsRuntimeCompletionOutsideNormalizedBytes() {
        DemoInterpreterRuntimeState state = state(2,0, 7, 7);
        DemoInterpreterRuntimeState.RecordState record = state.buffers().get(0);
        byte[] before = record.copyNormalizedBytes();
        record.setCompleted(true);
        assertTrue(record.isCompleted());
        assertArrayEquals(before, record.copyNormalizedBytes());
    }

    @Test public void groupCompletionRequiresEveryChildAndResetClearsChildren() {
        DemoInterpreterRuntimeState state = state(1,0, 0,2, 7, 7);
        DemoInterpreterRuntimeState.RecordState group = state.buffers().get(0);
        assertTrue(group.isGroup());
        assertFalse(group.isCompleted());
        group.children().get(0).setCompleted(true);
        assertFalse(group.isCompleted());
        group.children().get(1).setCompleted(true);
        assertTrue(group.isCompleted());
        state.reset();
        assertFalse(group.isCompleted());
        assertFalse(group.children().get(0).isCompleted());
        assertFalse(group.children().get(1).isCompleted());
    }

    @Test public void emptyGroupIsComplete() {
        DemoInterpreterRuntimeState state = state(1,0, 0,0);
        assertTrue(state.buffers().get(0).isCompleted());
    }

    @Test public void tickAndSelectionAreIndependent() {
        DemoInterpreterRuntimeState state = state(2,0, 7, 7);
        state.advanceTick();
        state.advanceTick();
        assertEquals(2, state.tick());
        assertEquals(0, state.selectedBufferIndex());
        assertTrue(state.selectNextBuffer());
        assertEquals(1, state.selectedBufferIndex());
        assertEquals(0, state.tick());
        assertFalse(state.selectNextBuffer());
        assertEquals(-1, state.selectedBufferIndex());
    }

    @Test public void resetClearsCountersSkipAndAllBuffers() {
        DemoInterpreterRuntimeState state = state(2,0, 27,1,0, 7, 7);
        DemoInterpreterRuntimeState.RecordState first = state.buffers().get(0);
        first.setRevealCounter(4);
        first.setScrollPosition(9);
        first.setCompleted(true);
        state.setSkipRequested(true);
        state.advanceTick();
        assertTrue(state.selectNextBuffer());
        state.reset();
        assertEquals(0, state.tick());
        assertEquals(0, state.selectedBufferIndex());
        assertFalse(state.skipRequested());
        assertFalse(first.isCompleted());
        assertEquals(0, first.revealCounter());
        assertEquals(0, first.scrollPosition());
    }

    @Test public void emptyScriptHasNoSelectionAndCanReset() {
        DemoInterpreterRuntimeState state = state(0,0);
        assertEquals(0, state.bufferCount());
        assertEquals(-1, state.selectedBufferIndex());
        assertFalse(state.selectNextBuffer());
        state.reset();
        assertEquals(-1, state.selectedBufferIndex());
    }

    @Test public void rejectsNestedGroupBeforeRuntimeConstruction() {
        try {
            state(1,0, 0,1, 0,0);
            fail("expected nested group rejection");
        } catch (DemoParseException expected) {
            assertEquals(DemoParseException.Code.NESTED_GROUP, expected.code());
        }
    }
}
