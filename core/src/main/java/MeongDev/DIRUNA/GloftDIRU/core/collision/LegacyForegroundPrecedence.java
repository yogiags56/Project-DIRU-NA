package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Source-grounded foreground-to-player-layer control-flow policy for
 * aBoolean(dx,dy,probe) at the pinned legacy source revision.
 *
 * This models only whether execution reaches the player-layer switch and
 * whether the foreground branch marks the local collision flag. It does not
 * model branch side effects or the final method return.
 */
public final class LegacyForegroundPrecedence {
    public enum PlayerLayerFlow {
        EVALUATE_PLAYER_LAYER,
        SKIP_PLAYER_LAYER
    }

    public static final class Decision {
        private final PlayerLayerFlow playerLayerFlow;
        private final boolean foregroundSetsCollisionTrue;
        private final boolean foregroundSetsCollisionFalse;
        private Decision(PlayerLayerFlow flow, boolean trueFlag, boolean falseFlag) {
            this.playerLayerFlow=flow;
            this.foregroundSetsCollisionTrue=trueFlag;
            this.foregroundSetsCollisionFalse=falseFlag;
        }
        public PlayerLayerFlow getPlayerLayerFlow(){return playerLayerFlow;}
        public boolean foregroundSetsCollisionTrue(){return foregroundSetsCollisionTrue;}
        public boolean foregroundSetsCollisionFalse(){return foregroundSetsCollisionFalse;}
    }

    private LegacyForegroundPrecedence(){}

    public static Decision decide(int foregroundRaw, boolean probeMode) {
        int type=foregroundRaw & 0xFF;
        int metadata=foregroundRaw >> 8;
        switch(type) {
            case 7:
                if (((foregroundRaw >> 8 & 0xF0) >> 4) < 2) {
                    return new Decision(PlayerLayerFlow.SKIP_PLAYER_LAYER,false,false);
                }
                return evaluate(false,false);
            case 3:
                if (metadata < 3) {
                    return new Decision(PlayerLayerFlow.SKIP_PLAYER_LAYER,true,false);
                }
                return new Decision(PlayerLayerFlow.SKIP_PLAYER_LAYER,false,true);
            default:
                // All other explicit foreground branches in this switch
                // leave n3==0 and flow into the player-layer switch.
                return evaluate(false,false);
        }
    }

    private static Decision evaluate(boolean trueFlag, boolean falseFlag) {
        return new Decision(PlayerLayerFlow.EVALUATE_PLAYER_LAYER,trueFlag,falseFlag);
    }
}
