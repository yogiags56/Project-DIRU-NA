package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Pure decision model for legacy player-layer case 48; never mutates stage arrays. */
public final class LegacyPlayerLayer48Evaluator {
    public static final class Decision {
        private final boolean accepted, worldMutationRequested, setMovementBlock;
        private final boolean clearLocalDirection, clearSourceA, clearSourceB;
        private final boolean writeDestinationA, writeDestinationB, writeMetadataA, writeMetadataB;
        private final boolean writeAnimationByte, writeForegroundCache;
        private final int counterAfter, directionBits, metadataBValue;
        private Decision(boolean accepted, boolean worldMutationRequested,
                boolean setMovementBlock, boolean clearLocalDirection,
                boolean clearSourceA, boolean clearSourceB,
                boolean writeDestinationA, boolean writeDestinationB,
                boolean writeMetadataA, boolean writeMetadataB,
                boolean writeAnimationByte, boolean writeForegroundCache,
                int counterAfter, int directionBits, int metadataBValue) {
            this.accepted=accepted; this.worldMutationRequested=worldMutationRequested;
            this.setMovementBlock=setMovementBlock; this.clearLocalDirection=clearLocalDirection;
            this.clearSourceA=clearSourceA; this.clearSourceB=clearSourceB;
            this.writeDestinationA=writeDestinationA; this.writeDestinationB=writeDestinationB;
            this.writeMetadataA=writeMetadataA; this.writeMetadataB=writeMetadataB;
            this.writeAnimationByte=writeAnimationByte; this.writeForegroundCache=writeForegroundCache;
            this.counterAfter=counterAfter; this.directionBits=directionBits;
            this.metadataBValue=metadataBValue;
        }
        public boolean isAccepted(){return accepted;}
        public boolean isWorldMutationRequested(){return worldMutationRequested;}
        public boolean shouldSetMovementBlock(){return setMovementBlock;}
        public boolean shouldClearLocalDirection(){return clearLocalDirection;}
        public boolean shouldClearSourceA(){return clearSourceA;}
        public boolean shouldClearSourceB(){return clearSourceB;}
        public boolean shouldWriteDestinationA(){return writeDestinationA;}
        public boolean shouldWriteDestinationB(){return writeDestinationB;}
        public boolean shouldWriteMetadataA(){return writeMetadataA;}
        public boolean shouldWriteMetadataB(){return writeMetadataB;}
        public boolean shouldWriteAnimationByte(){return writeAnimationByte;}
        public boolean shouldWriteForegroundCache(){return writeForegroundCache;}
        public int getCounterAfter(){return counterAfter;}
        public int getDirectionBits(){return directionBits;}
        public int getMetadataBValue(){return metadataBValue;}
    }
    private LegacyPlayerLayer48Evaluator(){}
    public static Decision evaluate(LegacyPlayerLayer48Snapshot s) {
        if(s==null) throw new IllegalArgumentException("snapshot");
        int after=s.getMovementCounter()-1;
        boolean accepted=(after<0 || s.isProbeMode())
                && s.getDeltaX()!=0
                && s.getDestinationPlayerA()<0
                && s.getDestinationPlayerB()<0
                && s.getGuardPlayerLayer()>=0;
        if(accepted) {
            boolean mutate=!s.isProbeMode();
            return new Decision(true,mutate,true,false,
                    mutate,mutate,mutate,mutate,mutate,mutate,mutate,mutate,
                    after,s.getDirectionBits(),
                    (s.getSourceMetadataB() & 0xFFFFFFF0) | s.getDirectionBits());
        }
        return new Decision(false,false,s.getDeltaX()!=0,true,
                false,false,false,false,false,false,false,false,
                after,s.getDirectionBits(),0);
    }
}
