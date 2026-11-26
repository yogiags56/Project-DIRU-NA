package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionOutcomeComposerTest {
    private static LegacyCollisionTrace trace(
            boolean probe, boolean normalExit, boolean b2, boolean b3,
            boolean counterZero, boolean localDelta, boolean committed,
            boolean writeback) {
        return new LegacyCollisionTrace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_2_INTERACTION,
                probe, normalExit, b2, b3, counterZero, localDelta, localDelta,
                committed, writeback, false, false, false, false);
    }

    @Test public void preservesB2AndB3AsIndependentResultChannels() {
        LegacyCollisionEvaluationResult result =
                LegacyCollisionOutcomeComposer.compose(
                        trace(false, true, true, true, true, true, true, true),
                        true, false);

        assertEquals(LegacyCollisionEvaluationResult.Completion.NORMAL_RETURN,
                result.getCompletion());
        assertTrue(result.getReturnValue());
        assertTrue(result.getLocalCollisionFlag());
        assertTrue(result.getAuxiliaryBranchFlag());
        assertFalse(result.wasPlayerLayerEvaluated());
        assertFalse(result.isStateCommitSuppressed());
    }

    @Test public void probeTraceReportsSuppressedWriteback() {
        LegacyCollisionEvaluationResult result =
                LegacyCollisionOutcomeComposer.compose(
                        trace(true, true, true, false, true, true, false, false),
                        false, true);

        assertTrue(result.getReturnValue());
        assertFalse(result.getAuxiliaryBranchFlag());
        assertTrue(result.wasPlayerLayerEvaluated());
        assertTrue(result.isStateCommitSuppressed());
    }

    @Test public void earlyReturnRemainsEarlyAndSuppressesCommit() {
        LegacyCollisionTrace early = new LegacyCollisionTrace(
                LegacyCollisionTrace.Branch.EARLY_RETURN_KBYTE_4,
                true, false, false, false, false, false, false, false, false,
                true, true, false, true);
        LegacyCollisionEvaluationResult result =
                LegacyCollisionOutcomeComposer.compose(early, false, true);

        assertTrue(result.isEarlyReturn());
        assertFalse(result.getReturnValue());
        assertTrue(result.isStateCommitSuppressed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingTrace() {
        LegacyCollisionOutcomeComposer.compose(null, false, false);
    }
}
