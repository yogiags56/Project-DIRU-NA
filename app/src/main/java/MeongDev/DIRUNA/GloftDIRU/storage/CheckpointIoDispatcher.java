package MeongDev.DIRUNA.GloftDIRU.storage;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;

/**
 * Serializes checkpoint disk I/O away from the UI thread.
 * Callers capture immutable checkpoint snapshots before submitting saves.
 */
public final class CheckpointIoDispatcher implements AutoCloseable {
    public interface LoadCallback {
        void onComplete(AndroidCheckpointLoadResult result);
    }

    public interface SaveCallback {
        void onComplete(IOException failure);
    }

    private final AndroidCheckpointStorage storage;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "diru-na-checkpoint-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());

    public CheckpointIoDispatcher(AndroidCheckpointStorage storage) {
        if (storage == null) throw new IllegalArgumentException("storage is required");
        this.storage = storage;
    }

    public void loadInto(CrossTableLifecycleCoordinator destination, LoadCallback callback) {
        if (destination == null || callback == null)
            throw new IllegalArgumentException("destination and callback are required");
        io.execute(() -> {
            AndroidCheckpointLoadResult result = storage.loadSafelyInto(destination);
            main.post(() -> callback.onComplete(result));
        });
    }

    public void save(CrossTableCheckpoint immutableSnapshot, SaveCallback callback) {
        if (immutableSnapshot == null || callback == null)
            throw new IllegalArgumentException("snapshot and callback are required");
        io.execute(() -> {
            IOException failure = null;
            try {
                storage.save(immutableSnapshot);
            } catch (IOException ex) {
                failure = ex;
            } catch (RuntimeException ex) {
                failure = new IOException("checkpoint save failed", ex);
            }
            IOException result = failure;
            main.post(() -> callback.onComplete(result));
        });
    }

    @Override
    public void close() {
        io.shutdown();
    }
}
