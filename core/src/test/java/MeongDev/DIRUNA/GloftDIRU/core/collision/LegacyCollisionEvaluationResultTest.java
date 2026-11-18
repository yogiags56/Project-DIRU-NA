package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionEvaluationResultTest {
    @Test public void keepsReturnAndAuxiliaryBranchAsSeparateChannels() {
        LegacyCollisionEvaluationResult r = new LegacyCollisionEvaluationResult(
                LegacyCollisionEvaluationResult.Completion.NORMAL_RETURN,
                true,true,false,true,false);
        assertTrue(r.getReturnValue());
        assertTrue(r.getLocalCollisionFlag());
        assertFalse(r.getAuxiliaryBranchFlag());
        assertTrue(r.wasPlayerLayerEvaluated());
    }

    @Test public void recordsAuxiliaryBranchWithoutAssumingReturnValue() {
        LegacyCollisionEvaluationResult r = new LegacyCollisionEvaluationResult(
                LegacyCollisionEvaluationResult.Completion.NORMAL_RETURN,
                false,false,true,false,false);
        assertFalse(r.getReturnValue());
        assertTrue(r.getAuxiliaryBranchFlag());
        assertFalse(r.wasPlayerLayerEvaluated());
    }

    @Test public void recordsEarlyFalseReturnAndSuppressedStateCommit() {
        LegacyCollisionEvaluationResult r = new LegacyCollisionEvaluationResult(
                LegacyCollisionEvaluationResult.Completion.EARLY_RETURN,
                false,false,false,true,true);
        assertTrue(r.isEarlyReturn());
        assertFalse(r.getReturnValue());
        assertTrue(r.isStateCommitSuppressed());
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsEarlyReturnMarkedTrue() {
        new LegacyCollisionEvaluationResult(
                LegacyCollisionEvaluationResult.Completion.EARLY_RETURN,
                true,false,false,false,false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsNullCompletion() {
        new LegacyCollisionEvaluationResult(null,false,false,false,false,false);
    }
}
