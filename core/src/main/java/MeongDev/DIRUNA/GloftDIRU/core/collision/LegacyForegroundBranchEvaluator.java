package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Pure evaluator for the two foreground branches that directly control the
 * n3/player-layer gate in the pinned legacy helper.
 *
 * Other foreground types are deliberately NOT_HANDLED here.
 */
public final class LegacyForegroundBranchEvaluator {
    public enum BranchStatus { EVALUATED, NOT_HANDLED }
    public enum PlayerLayerFlow { EVALUATE_PLAYER_LAYER, SKIP_PLAYER_LAYER }
    public enum CollisionFlagEffect { UNCHANGED, SET_TRUE, SET_FALSE }

    public static final class Decision {
        private final BranchStatus status;
        private final PlayerLayerFlow playerLayerFlow;
        private final CollisionFlagEffect collisionFlagEffect;
        private Decision(BranchStatus status, PlayerLayerFlow flow,
                         CollisionFlagEffect effect) {
            this.status=status; this.playerLayerFlow=flow; this.collisionFlagEffect=effect;
        }
        public BranchStatus getStatus(){return status;}
        public boolean isHandled(){return status==BranchStatus.EVALUATED;}
        public PlayerLayerFlow getPlayerLayerFlow(){return playerLayerFlow;}
        public CollisionFlagEffect getCollisionFlagEffect(){return collisionFlagEffect;}
    }

    private LegacyForegroundBranchEvaluator(){}

    public static Decision evaluate(int foregroundRaw) {
        int type=foregroundRaw & 0xFF;
        if(type==7) {
            int packedNibble=(foregroundRaw >> 8 & 0xF0) >> 4;
            return new Decision(BranchStatus.EVALUATED,
                    packedNibble < 2 ? PlayerLayerFlow.SKIP_PLAYER_LAYER
                            : PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    CollisionFlagEffect.UNCHANGED);
        }
        if(type==3) {
            int signedHighByte=foregroundRaw >> 8;
            return new Decision(BranchStatus.EVALUATED,
                    PlayerLayerFlow.SKIP_PLAYER_LAYER,
                    signedHighByte < 3 ? CollisionFlagEffect.SET_TRUE
                            : CollisionFlagEffect.SET_FALSE);
        }
        return new Decision(BranchStatus.NOT_HANDLED,
                PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                CollisionFlagEffect.UNCHANGED);
    }
}
