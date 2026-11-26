package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Pure outcome model for legacy foreground type 2 and inventory slot 9. */
public final class LegacyForegroundType2Evaluator {
    public enum Status { EVALUATED, NOT_HANDLED }
    public enum PlayerLayerFlow { EVALUATE_PLAYER_LAYER, SKIP_PLAYER_LAYER }
    public static final class Decision {
        private final Status status;
        private final PlayerLayerFlow flow;
        private final boolean auxiliaryFlagSet;
        private final boolean updateADInt;
        private final int nextADInt;
        private final boolean updateAEInt;
        private final int nextAEInt;
        private Decision(Status s, PlayerLayerFlow f, boolean b3,
                         boolean updateD, int d, boolean updateE, int e) {
            status=s; flow=f; auxiliaryFlagSet=b3;
            updateADInt=updateD; nextADInt=d; updateAEInt=updateE; nextAEInt=e;
        }
        public Status getStatus(){return status;}
        public boolean isHandled(){return status==Status.EVALUATED;}
        public PlayerLayerFlow getPlayerLayerFlow(){return flow;}
        public boolean shouldSetAuxiliaryBranchFlag(){return auxiliaryFlagSet;}
        public boolean shouldUpdateADInt(){return updateADInt;}
        public int getNextADInt(){return nextADInt;}
        public boolean shouldUpdateAEInt(){return updateAEInt;}
        public int getNextAEInt(){return nextAEInt;}
    }
    private LegacyForegroundType2Evaluator(){}
    public static Decision evaluate(int foregroundRaw, boolean probeMode,
                                     int inventorySlot9, int currentADInt, int currentAEInt) {
        if ((foregroundRaw & 0xFF) != 2)
            return new Decision(Status.NOT_HANDLED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    false,false,currentADInt,false,currentAEInt);
        if (probeMode)
            return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    false,false,currentADInt,false,currentAEInt);
        int high = foregroundRaw >> 8;
        if (high == 0) {
            if (inventorySlot9 >= 1)
                return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        true,true,7,true,0);
            return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    true,true,13,true,1);
        }
        if (high == 1) {
            if (inventorySlot9 >= 2)
                return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        true,true,7,true,1);
            return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    true,true,13,true,1);
        }
        return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                true,false,currentADInt,false,currentAEInt);
    }
}
