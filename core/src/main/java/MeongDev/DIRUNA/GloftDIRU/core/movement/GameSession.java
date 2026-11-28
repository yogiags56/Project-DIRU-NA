package MeongDev.DIRUNA.GloftDIRU.core.movement;

import MeongDev.DIRUNA.GloftDIRU.core.input.HeldKeyState;
import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyDirectionResolver;
import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyDirectionVector;
import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyInputDispatcher;
import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;

/**
 * Single-writer, platform-independent owner for the first ordinary-movement slice.
 * Collision semantics are injected and fail closed when no policy is supplied.
 * Special movement, scripted displacement, and interaction branches are not implemented.
 */
public final class GameSession {
    public enum MoveDecision { ACCEPTED, BLOCKED, UNSUPPORTED }
    public interface MoveAcceptancePolicy {
        /** Must be a pure observation. The session commits coordinates only for ACCEPTED. */
        MoveDecision evaluate(StageGrid stage, int fromX, int fromY, int toX, int toY);
    }
    public enum TickKind { MOVEMENT_STARTED, MOVEMENT_ADVANCED, BLOCKED, UNSUPPORTED, NO_ACTION, INPUT_ACTION_UNSUPPORTED }
    public static final class TickResult {
        private final TickKind kind; private final LegacyInputDispatcher.Kind inputKind;
        private TickResult(TickKind kind, LegacyInputDispatcher.Kind inputKind) { this.kind=kind; this.inputKind=inputKind; }
        public TickKind kind(){return kind;}
        public LegacyInputDispatcher.Kind inputKind(){return inputKind;}
    }

    private final StageGrid stage;
    private final MoveAcceptancePolicy movePolicy;
    private final OrdinaryMovementInterpolation interpolation = new OrdinaryMovementInterpolation();
    private final Thread ownerThread;
    private int tileX, tileY, pendingDirection;
    private long simulationTick;
    private volatile GameplayRenderSnapshot renderSnapshot;

    public GameSession(StageGrid stage, int spawnX, int spawnY, MoveAcceptancePolicy movePolicy) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        if (!stage.contains(spawnX, spawnY)) throw new IllegalArgumentException("spawn must be inside stage");
        this.stage=stage; this.tileX=spawnX; this.tileY=spawnY; this.movePolicy=movePolicy;
        this.ownerThread=Thread.currentThread();
        publishSnapshot();
    }

    /** One deterministic simulation update. Input is a bit-mask snapshot, not a mutable callback object. */
    public TickResult tick(int heldKeyMask, LegacyInputDispatcher.Context context) {
        assertOwnerThread();
        if (context == null) throw new IllegalArgumentException("input context is required");
        simulationTick++;
        if (interpolation.isActive()) {
            interpolation.advanceOneUpdate();
            publishSnapshot();
            return new TickResult(TickKind.MOVEMENT_ADVANCED, null);
        }
        HeldKeyState keys = new HeldKeyState();
        keys.press(heldKeyMask);
        LegacyInputDispatcher.Result input = LegacyInputDispatcher.dispatch(keys, context);
        pendingDirection = input.direction();
        if (input.kind() != LegacyInputDispatcher.Kind.NORMAL_DIRECTION) {
            publishSnapshot();
            TickKind kind = input.kind() == LegacyInputDispatcher.Kind.NO_INPUT
                    ? TickKind.NO_ACTION : TickKind.INPUT_ACTION_UNSUPPORTED;
            return new TickResult(kind, input.kind());
        }
        int dx=LegacyDirectionVector.tileDeltaX(pendingDirection);
        int dy=LegacyDirectionVector.tileDeltaY(pendingDirection);
        int targetX=tileX+dx, targetY=tileY+dy;
        if (!stage.contains(targetX, targetY)) {
            pendingDirection=LegacyDirectionResolver.NONE;
            publishSnapshot();
            return new TickResult(TickKind.BLOCKED, input.kind());
        }
        MoveDecision decision = movePolicy == null ? MoveDecision.UNSUPPORTED
                : movePolicy.evaluate(stage, tileX, tileY, targetX, targetY);
        if (decision == null || decision == MoveDecision.UNSUPPORTED) {
            pendingDirection=LegacyDirectionResolver.NONE;
            publishSnapshot();
            return new TickResult(TickKind.UNSUPPORTED, input.kind());
        }
        if (decision == MoveDecision.BLOCKED) {
            pendingDirection=LegacyDirectionResolver.NONE;
            publishSnapshot();
            return new TickResult(TickKind.BLOCKED, input.kind());
        }
        // The audited helper commits logical tile coordinates when its accepted branch is written back;
        // interpolation starts in that same accepted transition.
        tileX=targetX; tileY=targetY;
        interpolation.beginAcceptedMove(pendingDirection);
        publishSnapshot();
        return new TickResult(TickKind.MOVEMENT_STARTED, input.kind());
    }

    /**
     * Captures the immutable core projection only at a settled ordinary-movement
     * boundary. This is a narrow state-owner seam, not a complete gameplay
     * checkpoint and not a persistence API: stage preprocessing, collision
     * runtime arrays, progression, optional modes, and object tables are not
     * represented by this projection.
     *
     * Must be called by the GameSession owner thread.
     */
    public GameplayRenderSnapshot captureStableView() {
        assertOwnerThread();
        if (interpolation.isActive()) {
            throw new IllegalStateException(
                    "gameplay view is not stable while movement interpolation is active");
        }
        return renderSnapshot;
    }

    public GameplayRenderSnapshot renderSnapshot() { return renderSnapshot; }
    private void publishSnapshot() {
        int active=interpolation.isActive()?interpolation.direction():LegacyDirectionResolver.NONE;
        renderSnapshot=new GameplayRenderSnapshot(stage,
                new PlayerMotionState(tileX,tileY,pendingDirection,active,interpolation.progress()),simulationTick);
    }
    private void assertOwnerThread() {
        if (Thread.currentThread()!=ownerThread)
            throw new IllegalStateException("GameSession may only be mutated by its owner thread");
    }
}
