package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Immutable source-oriented input snapshot for one collision-helper request.
 * Raw packed values are retained; this type does not interpret tile semantics.
 */
public final class LegacyCollisionInputSnapshot {
    private final int playerX;
    private final int playerY;
    private final int stageWidth;
    private final int stageHeight;
    private final int deltaX;
    private final int deltaY;
    private final boolean probeMode;
    private final int packedPlayerState;
    private final int activeDirection;
    private final int scriptedCounter;
    private final int interpolationProgress;
    private final int movementCounter;
    private final int auxiliaryMovementCounter;
    private final boolean scriptedMovement;
    private final boolean auxiliaryGridPresent;
    private final int auxiliaryTargetValue;
    private final int globalGateValue;
    private final int playerType10Mode;
    private final int boundaryMode;
    private final int foregroundCellValue;
    private final int playerLayerCellValue;
    private final int playerCellMetadata;
    private final int foregroundAboveValue;
    private final int playerLayerAboveValue;

    public LegacyCollisionInputSnapshot(
            int playerX, int playerY, int stageWidth, int stageHeight,
            int deltaX, int deltaY, boolean probeMode,
            int packedPlayerState, int activeDirection,
            int scriptedCounter, int interpolationProgress,
            int movementCounter, int auxiliaryMovementCounter,
            boolean scriptedMovement, boolean auxiliaryGridPresent,
            int auxiliaryTargetValue, int globalGateValue,
            int playerType10Mode, int boundaryMode,
            int foregroundCellValue, int playerLayerCellValue,
            int playerCellMetadata, int foregroundAboveValue,
            int playerLayerAboveValue) {
        if (stageWidth <= 0 || stageHeight <= 0) {
            throw new IllegalArgumentException("stage dimensions must be positive");
        }
        this.playerX = playerX;
        this.playerY = playerY;
        this.stageWidth = stageWidth;
        this.stageHeight = stageHeight;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.probeMode = probeMode;
        this.packedPlayerState = packedPlayerState;
        this.activeDirection = activeDirection;
        this.scriptedCounter = scriptedCounter;
        this.interpolationProgress = interpolationProgress;
        this.movementCounter = movementCounter;
        this.auxiliaryMovementCounter = auxiliaryMovementCounter;
        this.scriptedMovement = scriptedMovement;
        this.auxiliaryGridPresent = auxiliaryGridPresent;
        this.auxiliaryTargetValue = auxiliaryTargetValue;
        this.globalGateValue = globalGateValue;
        this.playerType10Mode = playerType10Mode;
        this.boundaryMode = boundaryMode;
        this.foregroundCellValue = foregroundCellValue;
        this.playerLayerCellValue = playerLayerCellValue;
        this.playerCellMetadata = playerCellMetadata;
        this.foregroundAboveValue = foregroundAboveValue;
        this.playerLayerAboveValue = playerLayerAboveValue;
    }

    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }
    public int getStageWidth() { return stageWidth; }
    public int getStageHeight() { return stageHeight; }
    public int getDeltaX() { return deltaX; }
    public int getDeltaY() { return deltaY; }
    public boolean isProbeMode() { return probeMode; }
    public int getPackedPlayerState() { return packedPlayerState; }
    public int getActiveDirection() { return activeDirection; }
    public int getScriptedCounter() { return scriptedCounter; }
    public int getInterpolationProgress() { return interpolationProgress; }
    public int getMovementCounter() { return movementCounter; }
    public int getAuxiliaryMovementCounter() { return auxiliaryMovementCounter; }
    public boolean isScriptedMovement() { return scriptedMovement; }
    public boolean isAuxiliaryGridPresent() { return auxiliaryGridPresent; }
    public int getAuxiliaryTargetValue() { return auxiliaryTargetValue; }
    public int getGlobalGateValue() { return globalGateValue; }
    public int getPlayerType10Mode() { return playerType10Mode; }
    public int getBoundaryMode() { return boundaryMode; }
    public int getForegroundCellValue() { return foregroundCellValue; }
    public int getPlayerLayerCellValue() { return playerLayerCellValue; }
    public int getPlayerCellMetadata() { return playerCellMetadata; }
    public int getForegroundAboveValue() { return foregroundAboveValue; }
    public int getPlayerLayerAboveValue() { return playerLayerAboveValue; }

    public int getTargetX() { return playerX + deltaX; }
    public int getTargetY() { return playerY + deltaY; }
}
