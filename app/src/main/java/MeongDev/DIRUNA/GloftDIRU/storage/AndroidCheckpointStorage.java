package MeongDev.DIRUNA.GloftDIRU.storage;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CheckpointPersistenceService;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpointBinaryCodec;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;

/**
 * App-private Android entry point for saving and restoring DIRU-NA checkpoints.
 * The core module remains Android-independent.
 */
public final class AndroidCheckpointStorage {
    public static final String DEFAULT_FILE_NAME = "diru-na.checkpoint";

    private final CheckpointPersistenceService persistence;
    private final AndroidAtomicCheckpointStore store;

    public AndroidCheckpointStorage(Context context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        File privateFiles = appContext.getFilesDir();
        this.store = new AndroidAtomicCheckpointStore(privateFiles, DEFAULT_FILE_NAME);
        this.persistence = new CheckpointPersistenceService(store);
    }

    public void save(CrossTableCheckpoint checkpoint) throws IOException {
        persistence.save(checkpoint);
    }

    /**
     * Loads without silently resetting state. A missing file is distinct from
     * corrupt bytes, invalid model data, and an underlying storage read error.
     */
    public AndroidCheckpointLoadResult loadSafelyInto(
            CrossTableLifecycleCoordinator destination) {
        if (destination == null) {
            return AndroidCheckpointLoadResult.of(
                    AndroidCheckpointLoadResult.Status.INVALID_SNAPSHOT,
                    "destination coordinator is required");
        }
        if (!store.hasRecoverableFile()) {
            return AndroidCheckpointLoadResult.of(
                    AndroidCheckpointLoadResult.Status.MISSING, "no checkpoint file exists");
        }

        final byte[] encoded;
        try {
            encoded = store.read();
        } catch (IOException storageFailure) {
            return AndroidCheckpointLoadResult.of(
                    AndroidCheckpointLoadResult.Status.STORAGE_ERROR,
                    storageFailure.getMessage());
        }

        try {
            CrossTableLifecycleCoordinator.Status restored =
                    CrossTableCheckpointBinaryCodec.decodeInto(encoded, destination);
            if (restored == CrossTableLifecycleCoordinator.Status.OK) {
                return AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.RESTORED, "checkpoint restored");
            }
            return AndroidCheckpointLoadResult.of(
                    AndroidCheckpointLoadResult.Status.INVALID_SNAPSHOT,
                    "checkpoint failed semantic validation: " + restored);
        } catch (IOException malformed) {
            return AndroidCheckpointLoadResult.of(
                    AndroidCheckpointLoadResult.Status.CORRUPT, malformed.getMessage());
        }
    }

    /** Legacy throwing API retained for callers that need strict propagation. */
    public CrossTableLifecycleCoordinator.Status loadInto(
            CrossTableLifecycleCoordinator destination) throws IOException {
        return persistence.loadInto(destination);
    }
}
