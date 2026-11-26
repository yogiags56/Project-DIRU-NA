package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Pure outcome model for legacy foreground types 4 (checkpoint) and 28.
 * Both branches exit the foreground switch block, then fall through to the
 * player-layer switch (n3 remains zero). It describes requested effects;
 * it never commits them to game state.
 */
public final class LegacyForegroundType428Evaluator {
    public enum Status { EVALUATED, NOT_HANDLED }
    public enum PlayerLayerFlow { EVALUATE_PLAYER_LAYER, SKIP_PLAYER_LAYER }
    public enum LocalXBooleanEffect { UNCHANGED, SET_TRUE }

    public static final class Decision {
        private final Status status;
        private final PlayerLayerFlow playerLayerFlow;
        private final LocalXBooleanEffect localXBooleanEffect;
        private final boolean requestSaveCheckpoint;
        private final boolean updateCheckpointProgress;
        private final int nextCheckpointProgress;
        private final boolean updateCheckpointTimer;
        private final int nextCheckpointTimer;

        private Decision(Status status, PlayerLayerFlow flow, LocalXBooleanEffect localEffect,
                         boolean save, boolean updateProgress, int nextProgress,
                         boolean updateTimer, int nextTimer) {
            this.status=status;
            this.playerLayerFlow=flow;
            this.localXBooleanEffect=localEffect;
            this.requestSaveCheckpoint=save;
            this.updateCheckpointProgress=updateProgress;
            this.nextCheckpointProgress=nextProgress;
            this.updateCheckpointTimer=updateTimer;
            this.nextCheckpointTimer=nextTimer;
        }
        public Status getStatus(){return status;}
        public boolean isHandled(){return status==Status.EVALUATED;}
        public PlayerLayerFlow getPlayerLayerFlow(){return playerLayerFlow;}
        public LocalXBooleanEffect getLocalXBooleanEffect(){return localXBooleanEffect;}
        public boolean shouldRequestSaveCheckpoint(){return requestSaveCheckpoint;}
        public boolean shouldUpdateCheckpointProgress(){return updateCheckpointProgress;}
        public int getNextCheckpointProgress(){return nextCheckpointProgress;}
        public boolean shouldUpdateCheckpointTimer(){return updateCheckpointTimer;}
        public int getNextCheckpointTimer(){return nextCheckpointTimer;}
    }

    private LegacyForegroundType428Evaluator(){}

    /**
     * @param foregroundRaw packed legacy foreground cell
     * @param probeMode legacy probe flag
     * @param checkpointProgress current aCInt value
     * @param globalStageClock current i.aSInt value
     */
    public static Decision evaluate(int foregroundRaw, boolean probeMode,
                                    int checkpointProgress, int globalStageClock) {
        int type=foregroundRaw & 0xFF;
        if(type==4) {
            if(!probeMode) {
                int checkpointValue=(foregroundRaw & 0xFFFFFF00) >> 8;
                if(checkpointValue >= checkpointProgress) {
                    return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                            LocalXBooleanEffect.UNCHANGED,true,true,checkpointValue+1,
                            true,globalStageClock+13);
                }
            }
            return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    LocalXBooleanEffect.UNCHANGED,false,false,checkpointProgress,
                    false,0);
        }
        if(type==28) {
            return new Decision(Status.EVALUATED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                    probeMode ? LocalXBooleanEffect.UNCHANGED
                            : LocalXBooleanEffect.SET_TRUE,
                    false,false,checkpointProgress,false,0);
        }
        return new Decision(Status.NOT_HANDLED,PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                LocalXBooleanEffect.UNCHANGED,false,false,checkpointProgress,false,0);
    }
}
