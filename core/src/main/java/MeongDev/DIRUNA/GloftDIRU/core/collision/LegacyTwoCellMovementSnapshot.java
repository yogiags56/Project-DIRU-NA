package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Immutable, named-coordinate samples for the legacy 0/8/9/47 movement branch.
 * Coordinates follow i.aBoolean(int n, int n2, boolean probe):
 * target = (playerX + 2*n, playerY + 2*n2);
 * adjacent blocker = (playerX + n, playerY + 1).
 *
 * The caller must sample these exact cells and supply eBoolean(targetX,targetY).
 * This value object does not access stage arrays.
 */
public final class LegacyTwoCellMovementSnapshot {
    private final int playerX, playerY, deltaX, deltaY;
    private final int targetX, targetY, adjacentPlayerX, adjacentPlayerY;
    private final int playerLayer, targetForeground, adjacentPlayerLayer, adjacentForeground;
    private final int movementCounter, movementStateBits, kByte, dInt, eInt;
    private final boolean probeMode, destinationPassable, incomingReturnFlag;

    public LegacyTwoCellMovementSnapshot(
            int playerX, int playerY, int deltaX, int deltaY,
            int playerLayer, int targetForeground,
            int adjacentPlayerLayer, int adjacentForeground,
            int movementCounter, int movementStateBits,
            int kByte, int dInt, int eInt,
            boolean probeMode, boolean destinationPassable,
            boolean incomingReturnFlag) {
        this.playerX=playerX; this.playerY=playerY;
        this.deltaX=deltaX; this.deltaY=deltaY;
        this.targetX=playerX+(deltaX<<1);
        this.targetY=playerY+(deltaY<<1);
        this.adjacentPlayerX=playerX+deltaX;
        this.adjacentPlayerY=playerY+1;
        this.playerLayer=playerLayer;
        this.targetForeground=targetForeground;
        this.adjacentPlayerLayer=adjacentPlayerLayer;
        this.adjacentForeground=adjacentForeground;
        this.movementCounter=movementCounter;
        this.movementStateBits=movementStateBits;
        this.kByte=kByte; this.dInt=dInt; this.eInt=eInt;
        this.probeMode=probeMode;
        this.destinationPassable=destinationPassable;
        this.incomingReturnFlag=incomingReturnFlag;
    }

    public int getPlayerX(){return playerX;}
    public int getPlayerY(){return playerY;}
    public int getDeltaX(){return deltaX;}
    public int getDeltaY(){return deltaY;}
    public int getTargetX(){return targetX;}
    public int getTargetY(){return targetY;}
    public int getAdjacentPlayerX(){return adjacentPlayerX;}
    public int getAdjacentPlayerY(){return adjacentPlayerY;}
    public int getPlayerLayer(){return playerLayer;}
    public int getTargetForeground(){return targetForeground;}
    public int getAdjacentPlayerLayer(){return adjacentPlayerLayer;}
    public int getAdjacentForeground(){return adjacentForeground;}
    public int getMovementCounter(){return movementCounter;}
    public int getMovementStateBits(){return movementStateBits;}
    public boolean isProbeMode(){return probeMode;}
    public boolean isDestinationPassable(){return destinationPassable;}
    public boolean getIncomingReturnFlag(){return incomingReturnFlag;}

    public boolean isEarlyReturnConditionMet() {
        return kByte==4 && (targetX==dInt || targetX==dInt+1) && targetY>=eInt;
    }
}
