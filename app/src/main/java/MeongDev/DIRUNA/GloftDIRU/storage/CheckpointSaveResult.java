package MeongDev.DIRUNA.GloftDIRU.storage;

import java.io.IOException;

/** Outcome for an ordered asynchronous checkpoint save request. */
public final class CheckpointSaveResult {
    public enum Status { SAVED, SUPERSEDED, FAILED }

    private final long sequence;
    private final Status status;
    private final IOException failure;

    public CheckpointSaveResult(long sequence, Status status, IOException failure) {
        if (sequence <= 0 || status == null)
            throw new IllegalArgumentException("valid sequence and status are required");
        if (status == Status.FAILED && failure == null)
            throw new IllegalArgumentException("failed save requires an exception");
        if (status != Status.FAILED && failure != null)
            throw new IllegalArgumentException("only failed saves carry an exception");
        this.sequence = sequence;
        this.status = status;
        this.failure = failure;
    }

    public long sequence() { return sequence; }
    public Status status() { return status; }
    public IOException failure() { return failure; }
}
