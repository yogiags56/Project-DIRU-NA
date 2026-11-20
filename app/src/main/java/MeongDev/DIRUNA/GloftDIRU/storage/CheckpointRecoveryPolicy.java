package MeongDev.DIRUNA.GloftDIRU.storage;

/**
 * UI-neutral recovery policy. It describes what the application may offer;
 * it never deletes, overwrites, or silently resets a checkpoint.
 */
public final class CheckpointRecoveryPolicy {
    public enum Action {
        CONTINUE_FROM_CHECKPOINT,
        OFFER_NEW_GAME_OR_IMPORT,
        OFFER_RETRY_OR_PRESERVE_FOR_DIAGNOSIS,
        OFFER_STORAGE_RETRY
    }

    private CheckpointRecoveryPolicy() { }

    public static Action actionFor(AndroidCheckpointLoadResult result) {
        if (result == null) throw new IllegalArgumentException("load result is required");
        switch (result.status()) {
            case RESTORED:
                return result.restored()
                        ? Action.CONTINUE_FROM_CHECKPOINT
                        : Action.OFFER_RETRY_OR_PRESERVE_FOR_DIAGNOSIS;
            case MISSING:
                return Action.OFFER_NEW_GAME_OR_IMPORT;
            case CORRUPT:
            case INVALID_SNAPSHOT:
                return Action.OFFER_RETRY_OR_PRESERVE_FOR_DIAGNOSIS;
            case STORAGE_ERROR:
                return Action.OFFER_STORAGE_RETRY;
            default:
                throw new IllegalStateException("unhandled checkpoint status");
        }
    }
}
