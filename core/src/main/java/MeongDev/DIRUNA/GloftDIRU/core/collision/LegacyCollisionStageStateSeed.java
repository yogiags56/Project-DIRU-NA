package MeongDev.DIRUNA.GloftDIRU.core.collision;

import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;

/** Detached initial collision-state import; constructor defensively copies all arrays. */
public final class LegacyCollisionStageStateSeed {
    private final StageGrid stage;
    private final int[] metadata,auxiliary,foregroundCache;
    private final byte[] bByteArrArr,cByteArrArr;
    private final int tileX,tileY,pendingDirection,activeDirection,interpolationProgress;
    private final int animationId,animationFrame,blockedFlag;

    /** Compatibility/test constructor; byte-state grids are explicitly zero seeded. */
    public LegacyCollisionStageStateSeed(StageGrid stage,int[] metadata,int[] auxiliary,
            int[] foregroundCache,int tileX,int tileY,int pendingDirection,int activeDirection,
            int interpolationProgress,int animationId,int animationFrame,int blockedFlag) {
        this(stage,metadata,auxiliary,foregroundCache,
                emptyBytes(stage),emptyBytes(stage),tileX,tileY,pendingDirection,activeDirection,
                interpolationProgress,animationId,animationFrame,blockedFlag);
    }

    public LegacyCollisionStageStateSeed(StageGrid stage,int[] metadata,int[] auxiliary,
            int[] foregroundCache,byte[] bByteArrArr,byte[] cByteArrArr,
            int tileX,int tileY,int pendingDirection,int activeDirection,
            int interpolationProgress,int animationId,int animationFrame,int blockedFlag) {
        if(stage==null) throw new IllegalArgumentException("stage is required");
        int count=Math.multiplyExact(stage.width(),stage.height());
        require("metadata",metadata,count);require("auxiliary",auxiliary,count);
        require("foregroundCache",foregroundCache,count);
        require("bByteArrArr",bByteArrArr,count);require("cByteArrArr",cByteArrArr,count);
        this.stage=new StageGrid(stage.width(),stage.height(),stage.copyPlayerLayer(),
                stage.copyBackgroundLayer(),stage.copyForegroundLayer());
        this.metadata=metadata.clone();this.auxiliary=auxiliary.clone();
        this.foregroundCache=foregroundCache.clone();
        this.bByteArrArr=bByteArrArr.clone();this.cByteArrArr=cByteArrArr.clone();
        this.tileX=tileX;this.tileY=tileY;this.pendingDirection=pendingDirection;
        this.activeDirection=activeDirection;this.interpolationProgress=interpolationProgress;
        this.animationId=animationId;this.animationFrame=animationFrame;this.blockedFlag=blockedFlag;
    }
    private static byte[] emptyBytes(StageGrid stage) {
        if(stage==null) throw new IllegalArgumentException("stage is required");
        return new byte[Math.multiplyExact(stage.width(),stage.height())];
    }
    private static void require(String name,int[] values,int count) {
        if(values==null || values.length!=count) throw new IllegalArgumentException(name+" length must equal stage cell count");
    }
    private static void require(String name,byte[] values,int count) {
        if(values==null || values.length!=count) throw new IllegalArgumentException(name+" length must equal stage cell count");
    }
    public StageGrid stage(){return new StageGrid(stage.width(),stage.height(),stage.copyPlayerLayer(),stage.copyBackgroundLayer(),stage.copyForegroundLayer());}
    public int[] copyMetadata(){return metadata.clone();}
    public int[] copyAuxiliary(){return auxiliary.clone();}
    public int[] copyForegroundCache(){return foregroundCache.clone();}
    public byte[] copyBByteArrArr(){return bByteArrArr.clone();}
    public byte[] copyCByteArrArr(){return cByteArrArr.clone();}
    public int tileX(){return tileX;} public int tileY(){return tileY;}
    public int pendingDirection(){return pendingDirection;} public int activeDirection(){return activeDirection;}
    public int interpolationProgress(){return interpolationProgress;}
    public int animationId(){return animationId;} public int animationFrame(){return animationFrame;}
    public int blockedFlag(){return blockedFlag;}
}
