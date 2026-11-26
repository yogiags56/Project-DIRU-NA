package MeongDev.DIRUNA.GloftDIRU.core.movement;

import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;

/** Immutable render-facing projection; reading it never advances simulation. */
public final class GameplayRenderSnapshot {
    private final StageGrid stage;
    private final PlayerMotionState player;
    private final long simulationTick;
    public GameplayRenderSnapshot(StageGrid stage, PlayerMotionState player, long simulationTick) {
        if (stage == null || player == null) throw new IllegalArgumentException("stage and player are required");
        if (simulationTick < 0) throw new IllegalArgumentException("simulation tick cannot be negative");
        if (!stage.contains(player.tileX(), player.tileY())) throw new IllegalArgumentException("player must be inside stage");
        this.stage=stage; this.player=player; this.simulationTick=simulationTick;
    }
    public StageGrid stage(){return stage;}
    public PlayerMotionState player(){return player;}
    public long simulationTick(){return simulationTick;}
}
