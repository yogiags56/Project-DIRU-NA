package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionTraceTest {
    @Test public void nonProbeBoundsPathCanCommitLocalAndPersistedTileDelta() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.STAGE_BOUNDS,
                false, true, true, false, true, true, true, true, true,
                false, false, false, true);
        assertTrue(trace.isReturnFlagB2());
        assertTrue(trace.isLocalTileDeltaApplied());
        assertTrue(trace.isLocalProgressInitialized());
        assertTrue(trace.isTilePositionCommitted());
        assertTrue(trace.isPlayerSnapshotWrittenBack());
    }

    @Test public void probeCanCalculateLocalDeltaWithoutPersistingSnapshot() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_5_FORCED_MOVEMENT,
                true, true, true, false, true, true, true, false, false,
                false, false, false, false);
        assertTrue(trace.isProbeMode());
        assertTrue(trace.isReturnFlagB2());
        assertTrue(trace.isLocalTileDeltaApplied());
        assertTrue(trace.isLocalProgressInitialized());
        assertFalse(trace.isTilePositionCommitted());
        assertFalse(trace.isPlayerSnapshotWrittenBack());
    }

    @Test public void probeEarlyReturnCanRequestAnimationWithoutWriteback() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.EARLY_RETURN_KBYTE_4,
                true, false, false, false, false, false, false, false, false,
                true, true, false, true);
        assertTrue(trace.isAnimationRequested());
        assertFalse(trace.isHelperReturnedNormally());
        assertFalse(trace.isPlayerSnapshotWrittenBack());
        assertFalse(trace.isTilePositionCommitted());
    }

    @Test public void interactionFlagIsIndependentFromReturnFlag() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_2_INTERACTION,
                false, true, true, true, true, true, true, true, true,
                true, true, true, true);
        assertTrue(trace.isReturnFlagB2());
        assertTrue(trace.isInteractionFlagB3());
    }

    @Test public void foregroundType19ProbeExitsWithoutScriptedWorldMutation() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.FOREGROUND_TYPE_19_SCRIPTED_PATH,
                true, true, false, false, true, false, false, false, false,
                false, false, false, false);
        assertFalse(trace.isWorldMutationObserved());
        assertFalse(trace.isTilePositionCommitted());
    }

    @Test public void nonProbeEarlyReturnDoesNotWriteBackSnapshot() {
        LegacyCollisionTrace trace = trace(
                LegacyCollisionTrace.Branch.EARLY_RETURN_KBYTE_4,
                false, false, false, false, false, false, false, false, false,
                true, true, false, true);
        assertFalse(trace.isPlayerSnapshotWrittenBack());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLocalUpdateWithoutB2() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                false, true, false, false, true, true, true, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLocalUpdateWhenMovementCounterNonzero() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                false, true, true, false, false, true, true, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsProbeWriteback() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                true, true, true, false, true, true, true, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCommitWithoutLocalCoordinateUpdate() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                false, true, true, false, true, false, false, true, true,
                false, false, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMismatchedLocalCoordinateAndProgressUpdates() {
        trace(LegacyCollisionTrace.Branch.PLAYER_LAYER_EMPTY,
                true, true, true, false, true, true, false, false, false,
                false, false, false, false);
    }

    private static LegacyCollisionTrace trace(
            LegacyCollisionTrace.Branch branch,
            boolean probe, boolean normalExit, boolean b2, boolean b3,
            boolean counterZero, boolean localDelta, boolean localProgress,
            boolean committed, boolean writeback, boolean movingFlag,
            boolean animation, boolean worldMutation, boolean playerMutation) {
        return new LegacyCollisionTrace(branch, probe, normalExit, b2, b3,
                counterZero, localDelta, localProgress, committed, writeback,
                movingFlag, animation, worldMutation, playerMutation);
    }
}
