package MeongDev.DIRUNA.GloftDIRU.storage;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CheckpointSaveSequence;

/**
 * Serializes checkpoint disk I/O away from the UI thread.
 *
 * The caller must capture an immutable checkpoint snapshot on the game-state
 * owner thread before calling save(). Newer queued save requests supersede
 * older requests that have not started writing yet.
 */
public final class CheckpointIoDispatcher implements AutoCloseable {
    public interface LoadCallback {
        /** Candidate is decoded on the I/O thread and must be applied on the game thread. */
        void onComplete(AndroidCheckpointLoadResult result);
    }

    public interface SaveCallback {
        void onComplete(CheckpointSaveResult result);
    }

    private final CheckpointStorage storage;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "diru-na-checkpoint-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CheckpointSaveSequence saveSequence = new CheckpointSaveSequence();
    private boolean closed;

    public CheckpointIoDispatcher(CheckpointStorage storage) {
        if (storage == null) throw new IllegalArgumentException("storage is required");
        this.storage = storage;
    }

    /**
     * Decodes in the background but never mutates game state there.
     * The callback receives a candidate to apply on the game-state owner thread.
     */
    public synchronized void load(LoadCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback is required");
        if (closed) throw new IllegalStateException("dispatcher is closed");
        io.execute(() -> {
            AndroidCheckpointLoadResult result;
            try {
                result = storage.loadCandidate();
                if (result == null) {
                    result = AndroidCheckpointLoadResult.of(
                            AndroidCheckpointLoadResult.Status.STORAGE_ERROR,
                            "checkpoint storage returned no load result");
                }
            } catch (RuntimeException ex) {
                result = AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.STORAGE_ERROR,
                        "checkpoint load failed: " + ex.getClass().getSimpleName());
            }
            AndroidCheckpointLoadResult completed = result;
            main.post(() -> callback.onComplete(completed));
        });
    }

    /**
     * Enqueues an immutable snapshot. A queued save is skipped if a newer
     * request arrived before it began writing, preventing stale queued state
     * from replacing a newer checkpoint.
     *
     * @return monotonically increasing request sequence
     */
    public synchronized long save(CrossTableCheckpoint immutableSnapshot,
                                  SaveCallback callback) {
        if (immutableSnapshot == null || callback == null)
            throw new IllegalArgumentException("snapshot and callback are required");
        if (closed) throw new IllegalStateException("dispatcher is closed");
        long sequence = saveSequence.request();
        try {
            io.execute(() -> {
                if (isSuperseded(sequence)) {
                    postSave(callback, new CheckpointSaveResult(
                            sequence, CheckpointSaveResult.Status.SUPERSEDED, null));
                    return;
                }

                IOException failure = null;
                try {
                    storage.save(immutableSnapshot);
                } catch (IOException ex) {
                    failure = ex;
                } catch (RuntimeException ex) {
                    failure = new IOException("checkpoint save failed", ex);
                }

                CheckpointSaveResult result = failure == null
                        ? new CheckpointSaveResult(sequence,
                                CheckpointSaveResult.Status.SAVED, null)
                        : new CheckpointSaveResult(sequence,
                                CheckpointSaveResult.Status.FAILED, failure);
                postSave(callback, result);
            });
        } catch (RejectedExecutionException rejected) {
            IOException failure = new IOException("checkpoint dispatcher rejected save", rejected);
            postSave(callback, new CheckpointSaveResult(
                    sequence, CheckpointSaveResult.Status.FAILED, failure));
        }
        return sequence;
    }

    private boolean isSuperseded(long sequence) {
        return saveSequence.isSuperseded(sequence);
    }

    private void postSave(SaveCallback callback, CheckpointSaveResult result) {
        main.post(() -> callback.onComplete(result));
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        io.shutdown();
    }
}
