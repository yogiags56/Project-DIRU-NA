package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class DemoOpcodeDispatchPlanTest {
    @Test public void preservesOverlayFallThrough() {
        assertEquals(Arrays.asList(16,14,12), DemoOpcodeDispatchPlan.forOpcode(16, false).reachedLabels());
        assertEquals(Arrays.asList(17,15,12), DemoOpcodeDispatchPlan.forOpcode(17, false).reachedLabels());
        assertEquals(Arrays.asList(18,15,12), DemoOpcodeDispatchPlan.forOpcode(18, false).reachedLabels());
        assertEquals(Arrays.asList(14,12), DemoOpcodeDispatchPlan.forOpcode(14, false).reachedLabels());
        assertEquals(Arrays.asList(15,12), DemoOpcodeDispatchPlan.forOpcode(15, false).reachedLabels());
    }

    @Test public void preservesControlAndDurationFallThrough() {
        assertEquals(Arrays.asList(11,10,13), DemoOpcodeDispatchPlan.forOpcode(11, false).reachedLabels());
        assertEquals(Arrays.asList(10,13), DemoOpcodeDispatchPlan.forOpcode(10, false).reachedLabels());
    }

    @Test public void preservesStageMutationFallThroughSequences() {
        assertEquals(Arrays.asList(25,26,5,8,9,27,2),
                DemoOpcodeDispatchPlan.forOpcode(25, false).reachedLabels());
        assertEquals(Arrays.asList(26,5,8,9,27,2),
                DemoOpcodeDispatchPlan.forOpcode(26, false).reachedLabels());
        assertEquals(Arrays.asList(5,8,9,27,2),
                DemoOpcodeDispatchPlan.forOpcode(5, false).reachedLabels());
        assertEquals(Arrays.asList(8,9,27,2),
                DemoOpcodeDispatchPlan.forOpcode(8, false).reachedLabels());
        assertEquals(Arrays.asList(9,27,2),
                DemoOpcodeDispatchPlan.forOpcode(9, false).reachedLabels());
    }

    @Test public void preservesPromptDialogueAndImmediateCompletionFallThrough() {
        assertEquals(Arrays.asList(7,27,2), DemoOpcodeDispatchPlan.forOpcode(7, false).reachedLabels());
        assertEquals(Arrays.asList(27,2), DemoOpcodeDispatchPlan.forOpcode(27, false).reachedLabels());
        assertEquals(Arrays.asList(2), DemoOpcodeDispatchPlan.forOpcode(2, false).reachedLabels());
    }

    @Test public void skipModeAllowsOnlyVerifiedLegacyAllowList() {
        int[] allowed = {5,9,10,25,26};
        for (int opcode : allowed) {
            DemoOpcodeDispatchPlan plan = DemoOpcodeDispatchPlan.forOpcode(opcode, true);
            assertFalse(plan.isSkipped());
            assertFalse(plan.reachedLabels().isEmpty());
        }
        int[] blocked = {1,2,4,6,7,8,11,12,13,14,15,16,17,18,27};
        for (int opcode : blocked) {
            DemoOpcodeDispatchPlan plan = DemoOpcodeDispatchPlan.forOpcode(opcode, true);
            assertTrue("opcode " + opcode, plan.isSkipped());
            assertTrue(plan.reachedLabels().isEmpty());
        }
    }

    @Test public void rejectsUnknownAndNotYetSupportedOpcodes() {
        for (int opcode : new int[]{0,3,19,20,21,22,23,24,28}) {
            try {
                DemoOpcodeDispatchPlan.forOpcode(opcode, false);
                fail("expected rejection for opcode " + opcode);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test public void rejectsGroupRecordPlanning() {
        DemoInterpreterRuntimeState state = new DemoInterpreterRuntimeState(
                DemoScriptNormalizer.normalize(DemoScriptRecordDecoder.decode(
                        new byte[]{1,0, 0,0})));
        try {
            DemoOpcodeDispatchPlan.forRecord(state.buffers().get(0), false);
            fail("group records are containers, not executable leaves");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("leaf"));
        }
    }
}
