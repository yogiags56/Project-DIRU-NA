package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;

import static org.junit.Assert.*;

public final class LegacyCollisionTraceTest {
    @Test
    public void preservesOutOfBoundsReturnAndCommitAsSeparateFacts() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.STAGE_BOUNDS,
                false, true, false, true, true, true,
                false, false, false, true);

        assertTrue(trace.isReturnFlagB2());
        assertTrue(trace.isTilePositionCommitted());
        assertTrue(trace.isProgressInitialized());
    }

    @Test
    public void probeCanReportReturnFlagWithoutCommittingTile() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_5_FORCED_MOVEMENT,
                true, true, false, true, false, false,
                false, false, false, false);

        assertTrue(trace.isProbeMode());
        assertTrue(trace.isReturnFlagB2());
        assertFalse(trace.isTilePositionCommitted());
    }

    @Test
    public void keepsInteractionFlagIndependentFromReturnFlag() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_2_INTERACTION,
                false, true, true, true, true, true,
                true, true, true, true);

        assertTrue(trace.isReturnFlagB2());
        assertTrue(trace.isInteractionFlagB3());
    }

    @Test
    public void preservesAnimationWithoutTileCommit() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.EARLY_RETURN_KBYTE_4,
                false, false, false, true, false, false,
                true, true, false, true);

        assertTrue(trace.isAnimationRequested());
        assertFalse(trace.isTilePositionCommitted());
    }

    @Test
    public void preservesWorldMutationDuringProbe() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_19_SCRIPTED_PATH,
                true, false, false, false, false, false,
                false, false, true, true);

        assertTrue(trace.isProbeMode());
        assertTrue(trace.isWorldMutationObserved());
    }

    @Test
    public void doesNotCollapseForegroundBranches() {
        assertNotEquals(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_5_FORCED_MOVEMENT,
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_28_FORCED_MOVEMENT);
        assertNotEquals(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_2_INTERACTION,
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_4_CHECKPOINT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTileCommitInProbeMode() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                true, true, false, true, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTileCommitWithoutB2() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                false, false, false, true, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTileCommitWhenMovementCounterIsNonzero() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                false, true, false, false, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsProgressInitializationWithoutTileCommit() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                false, true, false, true, false, true,
                false, false, false, false);
    }

    private static LegacyCollisionTrace trace(
            LegacyCollisionTrace.Branch branch,
            boolean probe, boolean b2, boolean b3, boolean counterZero,
            boolean committed, boolean progress,
            boolean movingFlag, boolean animation,
            boolean worldMutation, boolean playerMutation) {
        return new LegacyCollisionTrace(branch, probe, b2, b3, counterZero,
                committed, progress, movingFlag, animation,
                worldMutation, playerMutation);
    }
}
