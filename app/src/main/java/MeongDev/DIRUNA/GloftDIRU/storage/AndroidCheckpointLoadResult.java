package MeongDev.DIRUNA.GloftDIRU.storage;

/** Explicit load outcome; missing/corrupt data is never treated as an empty save. */
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

    private AndroidCheckpointLoadResult(Status status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public static AndroidCheckpointLoadResult of(Status status, String detail) {
        if (status == null) throw new IllegalArgumentException("status is required");
        return new AndroidCheckpointLoadResult(status, detail);
    }

    public Status status() { return status; }
    public String detail() { return detail; }
    public boolean restored() { return status == Status.RESTORED; }
}
