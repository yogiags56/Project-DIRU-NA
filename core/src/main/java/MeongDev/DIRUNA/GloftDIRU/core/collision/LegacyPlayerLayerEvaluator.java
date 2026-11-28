package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Pure evaluator for the source branches whose player-layer result is
 * independent of multi-cell geometry. Complex branches remain NOT_HANDLED.
 * Metadata writes are returned as intents and are never applied here.
 */
public final class LegacyPlayerLayerEvaluator {
    public enum Status { EVALUATED, NOT_HANDLED }
    public enum Effect { NONE, CLEAR_DIRECTION_AND_MOVEMENT_BLOCK, REQUEST_PLAYER_METADATA_ONE }
    public static final class Decision {
        private final Status status;
        private final LegacyPlayerLayerClassifier.Kind kind;
        private final boolean returnFlag;
        private final Effect effect;
        private Decision(Status status, LegacyPlayerLayerClassifier.Kind kind,
                         boolean returnFlag, Effect effect) {
            this.status=status; this.kind=kind; this.returnFlag=returnFlag; this.effect=effect;
        }
        public Status getStatus(){return status;}
        public boolean isHandled(){return status==Status.EVALUATED;}
        public LegacyPlayerLayerClassifier.Kind getKind(){return kind;}
        public boolean getReturnFlag(){return returnFlag;}
        public Effect getEffect(){return effect;}
    }
    private LegacyPlayerLayerEvaluator(){}

    public static Decision evaluate(int playerLayerRaw, boolean probeMode,
                                    int type10Mode, int currentPlayerMetadata,
                                    boolean incomingReturnFlag) {
        LegacyPlayerLayerClassifier.Kind kind =
                LegacyPlayerLayerClassifier.classify(playerLayerRaw);
        switch(kind) {
            case PROBE_DEPENDENT_34_35:
                return result(kind, probeMode, Effect.NONE);
            case NON_BLOCKING_31_49:
                return result(kind, false, Effect.NONE);
            case BLOCKING_EXPLICIT_SET:
                return result(kind, true, Effect.NONE);
            case TYPE_10_MODE_DEPENDENT:
                if(type10Mode == 3) {
                    Effect effect = !probeMode && currentPlayerMetadata <= 0
                            ? Effect.REQUEST_PLAYER_METADATA_ONE : Effect.NONE;
                    return result(kind, true, effect);
                }
                return result(kind, incomingReturnFlag, Effect.NONE);
            case TYPE_28_SPECIAL:
                return result(kind, incomingReturnFlag, Effect.CLEAR_DIRECTION_AND_MOVEMENT_BLOCK);
            default:
                return new Decision(Status.NOT_HANDLED, kind, incomingReturnFlag, Effect.NONE);
        }
    }
    private static Decision result(LegacyPlayerLayerClassifier.Kind kind,
                                   boolean flag, Effect effect) {
        return new Decision(Status.EVALUATED, kind, flag, effect);
    }
}
