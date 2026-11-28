package MeongDev.DIRUNA.GloftDIRU.core.movement;

import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyDirectionResolver;

/** Immutable projection of the player fields owned by the gameplay session. */
public final class PlayerMotionState {
    private final int tileX, tileY, pendingDirection, activeDirection, progress;
    public PlayerMotionState(int tileX, int tileY, int pendingDirection,
                             int activeDirection, int progress) {
        if (pendingDirection < LegacyDirectionResolver.NONE || pendingDirection > LegacyDirectionResolver.LEFT)
            throw new IllegalArgumentException("invalid pending direction");
        if (activeDirection < LegacyDirectionResolver.NONE || activeDirection > LegacyDirectionResolver.LEFT)
            throw new IllegalArgumentException("invalid active direction");
        if (progress < 0 || progress > OrdinaryMovementInterpolation.INITIAL_PROGRESS)
            throw new IllegalArgumentException("invalid ordinary movement progress");
        if ((progress == 0) != (activeDirection == LegacyDirectionResolver.NONE))
            throw new IllegalArgumentException("active direction and progress must agree");
        this.tileX=tileX; this.tileY=tileY; this.pendingDirection=pendingDirection;
        this.activeDirection=activeDirection; this.progress=progress;
    }
    public int tileX(){return tileX;}
    public int tileY(){return tileY;}
    public int pendingDirection(){return pendingDirection;}
    public int activeDirection(){return activeDirection;}
    public int interpolationProgress(){return progress;}
}
