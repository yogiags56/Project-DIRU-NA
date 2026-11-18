package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Source-oriented record of one legacy collision-helper observation.
 * This records facts; it is not a collision engine.
 */
public final class LegacyCollisionTrace {
    public enum Branch {
        STAGE_BOUNDS, FOREGROUND_TYPE_1, FOREGROUND_TYPE_2_INTERACTION,
        FOREGROUND_TYPE_3_METADATA, FOREGROUND_TYPE_4_CHECKPOINT,
        FOREGROUND_TYPE_5_FORCED_MOVEMENT, FOREGROUND_TYPE_7_METADATA,
        FOREGROUND_TYPE_19_SCRIPTED_PATH, FOREGROUND_TYPE_28_FORCED_MOVEMENT,
        PLAYER_LAYER_34_35, PLAYER_LAYER_31_49, PLAYER_LAYER_BLOCKING_SET,
        PLAYER_LAYER_TYPE_10, PLAYER_LAYER_TYPE_48, PLAYER_LAYER_EMPTY,
        PLAYER_LAYER_OTHER, EARLY_RETURN_KBYTE_4
    }

    private final Branch branch;
    private final boolean probeMode;
    private final boolean helperReturnedNormally;
    private final boolean returnFlagB2;
    private final boolean interactionFlagB3;
    private final boolean movementCounterZero;
    private final boolean localTileDeltaApplied;
    private final boolean localProgressInitialized;
    private final boolean tilePositionCommitted;
    private final boolean playerSnapshotWrittenBack;
    private final boolean packedMovingFlagSet;
    private final boolean animationRequested;
    private final boolean worldMutationObserved;
    private final boolean playerStateMutationObserved;

    public LegacyCollisionTrace(
            Branch branch, boolean probeMode, boolean helperReturnedNormally,
            boolean returnFlagB2, boolean interactionFlagB3,
            boolean movementCounterZero, boolean localTileDeltaApplied,
            boolean localProgressInitialized, boolean tilePositionCommitted,
            boolean playerSnapshotWrittenBack, boolean packedMovingFlagSet,
            boolean animationRequested, boolean worldMutationObserved,
            boolean playerStateMutationObserved) {
        if (branch == null) {
            throw new IllegalArgumentException("branch must not be null");
        }
        if (localTileDeltaApplied != localProgressInitialized) {
            throw new IllegalArgumentException(
                    "source coordinate and progress locals are updated together");
        }
        if (localTileDeltaApplied && (!returnFlagB2 || !movementCounterZero)) {
            throw new IllegalArgumentException(
                    "local coordinate/progress update requires b2=true and aWInt=0");
        }
        boolean expectedWriteback = !probeMode && helperReturnedNormally;
        if (playerSnapshotWrittenBack != expectedWriteback) {
            throw new IllegalArgumentException(
                    "snapshot writeback requires non-probe normal helper exit");
        }
        if (tilePositionCommitted != (localTileDeltaApplied && expectedWriteback)) {
            throw new IllegalArgumentException(
                    "persisted tile commit must match local update plus snapshot writeback");
        }
        this.branch = branch;
        this.probeMode = probeMode;
        this.helperReturnedNormally = helperReturnedNormally;
        this.returnFlagB2 = returnFlagB2;
        this.interactionFlagB3 = interactionFlagB3;
        this.movementCounterZero = movementCounterZero;
        this.localTileDeltaApplied = localTileDeltaApplied;
        this.localProgressInitialized = localProgressInitialized;
        this.tilePositionCommitted = tilePositionCommitted;
        this.playerSnapshotWrittenBack = playerSnapshotWrittenBack;
        this.packedMovingFlagSet = packedMovingFlagSet;
        this.animationRequested = animationRequested;
        this.worldMutationObserved = worldMutationObserved;
        this.playerStateMutationObserved = playerStateMutationObserved;
    }

    public Branch getBranch() { return branch; }
    public boolean isProbeMode() { return probeMode; }
    public boolean isHelperReturnedNormally() { return helperReturnedNormally; }
    public boolean isReturnFlagB2() { return returnFlagB2; }
    public boolean isInteractionFlagB3() { return interactionFlagB3; }
    public boolean isMovementCounterZero() { return movementCounterZero; }
    public boolean isLocalTileDeltaApplied() { return localTileDeltaApplied; }
    public boolean isLocalProgressInitialized() { return localProgressInitialized; }
    public boolean isTilePositionCommitted() { return tilePositionCommitted; }
    public boolean isPlayerSnapshotWrittenBack() { return playerSnapshotWrittenBack; }
    public boolean isPackedMovingFlagSet() { return packedMovingFlagSet; }
    public boolean isAnimationRequested() { return animationRequested; }
    public boolean isWorldMutationObserved() { return worldMutationObserved; }
    public boolean isPlayerStateMutationObserved() { return playerStateMutationObserved; }
}
