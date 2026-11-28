package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Immutable, explicitly sampled cells for source player-layer case 48.
 * Coordinates reproduce the legacy expressions; this class does not access arrays.
 */
public final class LegacyPlayerLayer48Snapshot {
    private final int playerX, playerY, deltaX, deltaY;
    private final int targetX, targetY, sourceX, sourceY;
    private final int offsetA, offsetB;
    private final int movementCounter, currentMetadata, currentPlayerLayer;
    private final int destinationPlayerA, destinationPlayerB, guardPlayerLayer;
    private final int sourceMetadataA, sourceMetadataB;
    private final boolean probeMode;

    public LegacyPlayerLayer48Snapshot(
            int playerX, int playerY, int deltaX, int deltaY,
            int movementCounter, int currentMetadata, int currentPlayerLayer,
            int destinationPlayerA, int destinationPlayerB, int guardPlayerLayer,
            int sourceMetadataA, int sourceMetadataB, boolean probeMode) {
        this.playerX=playerX; this.playerY=playerY;
        this.deltaX=deltaX; this.deltaY=deltaY;
        this.targetX=playerX+(deltaX<<1);
        this.targetY=playerY+(deltaY<<1);
        this.sourceX=playerX+deltaX;
        this.sourceY=playerY+deltaY;
        if((currentMetadata & 0x8)!=0) { this.offsetA=0; this.offsetB=1; }
        else { this.offsetA=-1; this.offsetB=0; }
        this.movementCounter=movementCounter;
        this.currentMetadata=currentMetadata;
        this.currentPlayerLayer=currentPlayerLayer;
        this.destinationPlayerA=destinationPlayerA;
        this.destinationPlayerB=destinationPlayerB;
        this.guardPlayerLayer=guardPlayerLayer;
        this.sourceMetadataA=sourceMetadataA;
        this.sourceMetadataB=sourceMetadataB;
        this.probeMode=probeMode;
    }

    public int getPlayerX(){return playerX;}
    public int getPlayerY(){return playerY;}
    public int getDeltaX(){return deltaX;}
    public int getDeltaY(){return deltaY;}
    public int getTargetX(){return targetX;}
    public int getTargetY(){return targetY;}
    public int getSourceX(){return sourceX;}
    public int getSourceY(){return sourceY;}
    public int getDestinationAX(){return targetX;}
    public int getDestinationAY(){return targetY+offsetA;}
    public int getDestinationBX(){return targetX;}
    public int getDestinationBY(){return targetY+offsetB;}
    public int getSourceAX(){return sourceX;}
    public int getSourceAY(){return sourceY+offsetA;}
    public int getSourceBX(){return sourceX;}
    public int getSourceBY(){return sourceY+offsetB;}
    public int getGuardX(){return sourceX;}
    public int getGuardY(){return sourceY+offsetB+1;}
    public int getMovementCounter(){return movementCounter;}
    public int getCurrentMetadata(){return currentMetadata;}
    public int getCurrentPlayerLayer(){return currentPlayerLayer;}
    public int getDestinationPlayerA(){return destinationPlayerA;}
    public int getDestinationPlayerB(){return destinationPlayerB;}
    public int getGuardPlayerLayer(){return guardPlayerLayer;}
    public int getSourceMetadataA(){return sourceMetadataA;}
    public int getSourceMetadataB(){return sourceMetadataB;}
    public boolean isProbeMode(){return probeMode;}
    public int getDirectionBits(){return deltaX>0?2:4;}
}
