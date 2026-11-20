package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

/**
 * Boundary between the gameplay engine and checkpoint persistence.
 *
 * <p>Every method must be invoked on the thread that owns and mutates live
 * gameplay state. Disk I/O must never be performed by this provider.</p>
 *
 * <p>Implementations must capture a detached, immutable checkpoint. They must
 * not return a placeholder or empty checkpoint when gameplay state is not
 * ready. If capture cannot produce a complete snapshot, it must fail rather
 * than overwrite a previously saved checkpoint.</p>
 *
 * <p>Restore implementations must validate and apply the supplied checkpoint
 * atomically: on failure, the live game state must remain unchanged.</p>
 *
 * <p>This interface deliberately does not prescribe an Android Activity
 * lifecycle, executor, rendering loop, or concrete gameplay data model.</p>
 */
public interface CheckpointSnapshotProvider {
    /**
     * Captures a complete immutable snapshot from live gameplay state.
     *
     * @return a non-null detached checkpoint
     * @throws IllegalStateException if gameplay state is not ready or a
     *         complete snapshot cannot be captured
     */
    CrossTableCheckpoint captureCheckpoint();

    /**
     * Applies a previously decoded candidate to live gameplay state.
     *
     * @param checkpoint validated checkpoint candidate
     * @return OK only after the complete state has been applied
     */
    CrossTableLifecycleCoordinator.Status restoreCheckpoint(
            CrossTableCheckpoint checkpoint);
}
