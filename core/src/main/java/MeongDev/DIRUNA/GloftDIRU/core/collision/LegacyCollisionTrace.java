package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Lossless, source-oriented record of one legacy collision-helper observation.
 *
 * This is deliberately a trace model, not a collision engine. The helper's
 * boolean return and its side effects are kept as separate facts.
 */
public final class LegacyCollisionTrace {
    public enum Branch {
        STAGE_BOUNDS,
        FOREGROUND_TYPE_1,
        FOREGROUND_TYPE_2_INTERACTION,
        FOREGROUND_TYPE_3_METADATA,
        FOREGROUND_TYPE_4_CHECKPOINT,
        FOREGROUND_TYPE_5_FORCED_MOVEMENT,
        FOREGROUND_TYPE_7_METADATA,
        FOREGROUND_TYPE_19_SCRIPTED_PATH,
        FOREGROUND_TYPE_28_FORCED_MOVEMENT,
        PLAYER_LAYER_34_35,
        PLAYER_LAYER_31_49,
        PLAYER_LAYER_BLOCKING_SET,
        PLAYER_LAYER_TYPE_10,
        PLAYER_LAYER_TYPE_48,
        PLAYER_LAYER_EMPTY,
        PLAYER_LAYER_OTHER,
        EARLY_RETURN_KBYTE_4
    }

    private final Branch branch;
    private final boolean probeMode;
    private final boolean returnFlagB2;
    private final boolean interactionFlagB3;
    private final boolean movementCounterZero;
    private final boolean tilePositionCommitted;
    private final boolean progressInitialized;
    private final boolean packedMovingFlagSet;
    private final boolean animationRequested;
    private final boolean worldMutationObserved;
    private final boolean playerStateMutationObserved;

    public LegacyCollisionTrace(
            Branch branch,
            boolean probeMode,
            boolean returnFlagB2,
            boolean interactionFlagB3,
            boolean movementCounterZero,
            boolean tilePositionCommitted,
            boolean progressInitialized,
            boolean packedMovingFlagSet,
            boolean animationRequested,
            boolean worldMutationObserved,
            boolean playerStateMutationObserved) {
        if (branch == null) {
            throw new IllegalArgumentException("branch must not be null");
        }
        if (tilePositionCommitted && probeMode) {
            throw new IllegalArgumentException("probe mode cannot commit tile position");
        }
        if (tilePositionCommitted && (!returnFlagB2 || !movementCounterZero)) {
            throw new IllegalArgumentException(
                    "tile commit requires b2=true and aWInt=0");
        }
        if (progressInitialized != tilePositionCommitted) {
            throw new IllegalArgumentException(
                    "source initializes jInt=18 in the same branch that commits tile position");
        }
        this.branch = branch;
        this.probeMode = probeMode;
        this.returnFlagB2 = returnFlagB2;
        this.interactionFlagB3 = interactionFlagB3;
        this.movementCounterZero = movementCounterZero;
        this.tilePositionCommitted = tilePositionCommitted;
        this.progressInitialized = progressInitialized;
        this.packedMovingFlagSet = packedMovingFlagSet;
        this.animationRequested = animationRequested;
        this.worldMutationObserved = worldMutationObserved;
        this.playerStateMutationObserved = playerStateMutationObserved;
    }

    public Branch getBranch() { return branch; }
    public boolean isProbeMode() { return probeMode; }
    public boolean isReturnFlagB2() { return returnFlagB2; }
    public boolean isInteractionFlagB3() { return interactionFlagB3; }
    public boolean isMovementCounterZero() { return movementCounterZero; }
    public boolean isTilePositionCommitted() { return tilePositionCommitted; }
    public boolean isProgressInitialized() { return progressInitialized; }
    public boolean isPackedMovingFlagSet() { return packedMovingFlagSet; }
    public boolean isAnimationRequested() { return animationRequested; }
    public boolean isWorldMutationObserved() { return worldMutationObserved; }
    public boolean isPlayerStateMutationObserved() { return playerStateMutationObserved; }
}
