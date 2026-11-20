package MeongDev.DIRUNA.GloftDIRU.storage;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CheckpointPersistenceService;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;

/**
 * App-private Android entry point for saving and restoring DIRU-NA checkpoints.
 * The core module remains Android-independent.
 */
public final class AndroidCheckpointStorage {
    public static final String DEFAULT_FILE_NAME = "diru-na.checkpoint";

    private final CheckpointPersistenceService persistence;

    public AndroidCheckpointStorage(Context context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        File privateFiles = appContext.getFilesDir();
        this.persistence = new CheckpointPersistenceService(
                new AndroidAtomicCheckpointStore(privateFiles, DEFAULT_FILE_NAME));
    }

    public void save(CrossTableCheckpoint checkpoint) throws IOException {
        persistence.save(checkpoint);
    }

    public CrossTableLifecycleCoordinator.Status loadInto(
            CrossTableLifecycleCoordinator destination) throws IOException {
        return persistence.loadInto(destination);
    }
}
