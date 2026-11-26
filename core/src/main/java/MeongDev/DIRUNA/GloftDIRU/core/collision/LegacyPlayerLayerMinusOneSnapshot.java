package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Immutable caller-sampled facts for legacy player-layer -1. */
public final class LegacyPlayerLayerMinusOneSnapshot {
    private final int playerX, playerY, deltaX, deltaY, stageWidth, stageHeight;
    private final int nearBelowPlayerLayer, nearBelowMetadata;
    private final int abovePlayerLayer, aboveMetadata, belowPlayerLayer, belowMetadata;
    private final int doorCounterA, doorCounterB;
    private final boolean probeMode;

    public LegacyPlayerLayerMinusOneSnapshot(
            int playerX,int playerY,int deltaX,int deltaY,int stageWidth,int stageHeight,
            int nearBelowPlayerLayer,int nearBelowMetadata,
            int abovePlayerLayer,int aboveMetadata,int belowPlayerLayer,int belowMetadata,
            int doorCounterA,int doorCounterB,boolean probeMode) {
        this.playerX=playerX;this.playerY=playerY;this.deltaX=deltaX;this.deltaY=deltaY;
        this.stageWidth=stageWidth;this.stageHeight=stageHeight;
        this.nearBelowPlayerLayer=nearBelowPlayerLayer;this.nearBelowMetadata=nearBelowMetadata;
        this.abovePlayerLayer=abovePlayerLayer;this.aboveMetadata=aboveMetadata;
        this.belowPlayerLayer=belowPlayerLayer;this.belowMetadata=belowMetadata;
        this.doorCounterA=doorCounterA;this.doorCounterB=doorCounterB;this.probeMode=probeMode;
    }
    public int getPlayerX(){return playerX;}
    public int getPlayerY(){return playerY;}
    public int getDeltaX(){return deltaX;}
    public int getDeltaY(){return deltaY;}
    public int getTargetX(){return playerX+deltaX;}
    public int getTargetY(){return playerY+deltaY;}
    public int getStageWidth(){return stageWidth;}
    public int getStageHeight(){return stageHeight;}
    public int getNearBelowX(){return getTargetX();}
    public int getNearBelowY(){return playerY+1;}
    public int getAboveX(){return getTargetX();}
    public int getAboveY(){return getTargetY()-1;}
    public int getBelowX(){return getTargetX();}
    public int getBelowY(){return getTargetY()+1;}
    public int getNearBelowPlayerLayer(){return nearBelowPlayerLayer;}
    public int getNearBelowMetadata(){return nearBelowMetadata;}
    public int getAbovePlayerLayer(){return abovePlayerLayer;}
    public int getAboveMetadata(){return aboveMetadata;}
    public int getBelowPlayerLayer(){return belowPlayerLayer;}
    public int getBelowMetadata(){return belowMetadata;}
    public int getDoorCounterA(){return doorCounterA;}
    public int getDoorCounterB(){return doorCounterB;}
    public boolean isProbeMode(){return probeMode;}
    public boolean isStageEdge(){
        int x=getTargetX(),y=getTargetY();
        return x==0 || y==0 || x==stageWidth-1 || y==stageHeight-1;
    }
}
