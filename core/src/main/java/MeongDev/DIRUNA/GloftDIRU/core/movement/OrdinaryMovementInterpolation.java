package MeongDev.DIRUNA.GloftDIRU.core.movement;

import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyDirectionResolver;

/**
 * Source-derived clock for the ordinary movement interpolation path only.
 * It does not decide collision, mutate stage cells, or commit player coordinates.
 * Special movement and scripted displacement use separate state machines.
 */
public final class OrdinaryMovementInterpolation {
    public static final int INITIAL_PROGRESS = 18;
    public static final int PROGRESS_STEP = 6;
    private int progress;
    private int direction = LegacyDirectionResolver.NONE;

    public boolean isActive() { return progress > 0; }
    public int progress() { return progress; }
    public int direction() { return direction; }

    /** Starts the clock after a caller has independently accepted a move. */
    public void beginAcceptedMove(int legacyDirection) {
        if (legacyDirection < LegacyDirectionResolver.UP
                || legacyDirection > LegacyDirectionResolver.LEFT) {
            throw new IllegalArgumentException("ordinary direction must be 1..4");
        }
        if (isActive()) {
            throw new IllegalStateException("ordinary movement interpolation is already active");
        }
        direction = legacyDirection;
        progress = INITIAL_PROGRESS;
    }

    /** Advances one source simulation update; returns remaining progress. */
    public int advanceOneUpdate() {
        if (!isActive()) return 0;
        progress = Math.max(0, progress - PROGRESS_STEP);
        if (progress == 0) direction = LegacyDirectionResolver.NONE;
        return progress;
    }

    public void reset() {
        progress = 0;
        direction = LegacyDirectionResolver.NONE;
    }
}
