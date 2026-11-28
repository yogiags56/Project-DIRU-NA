package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Pure decision model for legacy player-layer -1. */
public final class LegacyPlayerLayerMinusOneEvaluator {
    public static final class Decision {
        private final boolean returnFlag, clearLocalDirection, clearMovementBlock;
        private final boolean edgeBlocked, doorException;
        private Decision(boolean result,boolean clearDirection,boolean clearBlock,
                         boolean edge,boolean exception){
            returnFlag=result;clearLocalDirection=clearDirection;
            clearMovementBlock=clearBlock;edgeBlocked=edge;doorException=exception;
        }
        public boolean getReturnFlag(){return returnFlag;}
        public boolean shouldClearLocalDirection(){return clearLocalDirection;}
        public boolean shouldClearMovementBlock(){return clearMovementBlock;}
        public boolean isEdgeBlocked(){return edgeBlocked;}
        public boolean isDoorException(){return doorException;}
    }
    private LegacyPlayerLayerMinusOneEvaluator(){}
    public static Decision evaluate(LegacyPlayerLayerMinusOneSnapshot s,boolean incomingReturnFlag){
        if(s==null) throw new IllegalArgumentException("snapshot");
        if(s.isStageEdge()) return new Decision(true,false,true,true,false);
        boolean nearDoor=s.getDeltaX()!=0
                && s.getNearBelowPlayerLayer()==0
                && (s.getNearBelowMetadata()&0x7)==0x3;
        boolean doorA=s.getDoorCounterA()>=24
                && ((s.getAbovePlayerLayer()==28 && (s.getAboveMetadata()&0x8)==0)
                || (s.getBelowPlayerLayer()==28 && (s.getBelowMetadata()&0x8)==0));
        boolean doorB=s.getDoorCounterB()>=24
                && (s.getAbovePlayerLayer()==28 || s.getBelowPlayerLayer()==28);
        boolean exception=(nearDoor || doorA || doorB) && !s.isProbeMode();
        if(exception) return new Decision(incomingReturnFlag,true,true,false,true);
        return new Decision(true,false,true,false,false);
    }
}
