package MeongDev.DIRUNA.GloftDIRU.storage;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;

/**
 * Explicit load outcome. A decoded checkpoint is carried as an immutable
 * candidate and is applied only by the caller on the game-state owner thread.
 */
public final class AndroidCheckpointLoadResult {
    public enum Status {
        RESTORED,
        MISSING,
        INVALID_SNAPSHOT,
        CORRUPT,
        STORAGE_ERROR
    }

    private final Status status;
    private final String detail;
    private final CrossTableCheckpoint checkpoint;

    private AndroidCheckpointLoadResult(Status status, String detail,
                                        CrossTableCheckpoint checkpoint) {
        this.status = status;
        this.detail = detail;
        this.checkpoint = checkpoint;
    }

    public static AndroidCheckpointLoadResult of(Status status, String detail) {
        if (status == null) throw new IllegalArgumentException("status is required");
        return new AndroidCheckpointLoadResult(status, detail, null);
    }

    public static AndroidCheckpointLoadResult restored(CrossTableCheckpoint checkpoint) {
        if (checkpoint == null) throw new IllegalArgumentException("checkpoint is required");
        return new AndroidCheckpointLoadResult(Status.RESTORED, "checkpoint decoded", checkpoint);
    }

    public Status status() { return status; }
    public String detail() { return detail; }
    public boolean restored() { return status == Status.RESTORED && checkpoint != null; }
    public CrossTableCheckpoint checkpoint() { return checkpoint; }

    /** Apply the validated candidate on the caller's game-state owner thread. */
    public CrossTableLifecycleCoordinator.Status applyTo(
            CrossTableLifecycleCoordinator destination) {
        if (!restored() || destination == null)
            return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;
        return checkpoint.restoreInto(destination);
    }
}
