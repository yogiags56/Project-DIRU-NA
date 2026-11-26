package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Pure foreground branch outcomes for legacy foreground types 1, 5 and 6. */
public final class LegacyForegroundType156Evaluator {
    public enum Status { EVALUATED, NOT_HANDLED }
    public enum PlayerLayerFlow { EVALUATE_PLAYER_LAYER, SKIP_PLAYER_LAYER }
    public enum CollisionEffect { UNCHANGED, SET_TRUE }
    public static final class Decision {
        private final Status status;
        private final PlayerLayerFlow flow;
        private final CollisionEffect collision;
        private final boolean resetTimer120;
        private final boolean incrementCounter;
        private final boolean invokeType1Clear;
        private final boolean setLocalXBoolean;
        private final boolean setAtBoolean;
        private Decision(Status s,PlayerLayerFlow f,CollisionEffect c,boolean timer,
                         boolean counter,boolean clear,boolean x,boolean at) {
            status=s;flow=f;collision=c;resetTimer120=timer;incrementCounter=counter;
            invokeType1Clear=clear;setLocalXBoolean=x;setAtBoolean=at;
        }
        public Status getStatus(){return status;}
        public boolean isHandled(){return status==Status.EVALUATED;}
        public PlayerLayerFlow getPlayerLayerFlow(){return flow;}
        public CollisionEffect getCollisionEffect(){return collision;}
        public boolean shouldResetTimer120(){return resetTimer120;}
        public boolean shouldIncrementCounter(){return incrementCounter;}
        public boolean shouldInvokeType1Clear(){return invokeType1Clear;}
        public boolean shouldSetLocalXBoolean(){return setLocalXBoolean;}
        public boolean shouldSetAtBoolean(){return setAtBoolean;}
    }
    private LegacyForegroundType156Evaluator(){}
    public static Decision evaluate(int foregroundRaw,boolean probeMode) {
        switch(foregroundRaw & 0xFF) {
            case 1:
                return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        CollisionEffect.UNCHANGED,!probeMode,!probeMode,!probeMode,false,false);
            case 5:
                return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        CollisionEffect.SET_TRUE,false,false,false,!probeMode,!probeMode);
            case 6:
                return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        CollisionEffect.UNCHANGED,false,false,false,false,false);
            default:
                return new Decision(Status.NOT_HANDLED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        CollisionEffect.UNCHANGED,false,false,false,false,false);
        }
    }
}
