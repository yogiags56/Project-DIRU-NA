package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

/**
 * Thread-safe sequence gate for coalescing queued checkpoint saves.
 *
 * <p>Call {@link #request()} when a save is accepted for queueing. A worker
 * should call {@link #isSuperseded(long)} immediately before beginning its
 * write. This gate does not cancel an already-started write.</p>
 */
public final class CheckpointSaveSequence {
    private long latestRequested;

    /** Returns the monotonically increasing sequence assigned to a request. */
    public synchronized long request() {
        if (latestRequested == Long.MAX_VALUE)
            throw new IllegalStateException("checkpoint save sequence exhausted");
        return ++latestRequested;
    }

    /**
     * Returns true when a newer request has been accepted since this request.
     * The caller must perform this check immediately before starting I/O.
     */
    public synchronized boolean isSuperseded(long sequence) {
        if (sequence <= 0 || sequence > latestRequested)
            throw new IllegalArgumentException("unknown checkpoint save sequence");
        return sequence < latestRequested;
    }

    public synchronized long latestRequested() {
        return latestRequested;
    }
}
