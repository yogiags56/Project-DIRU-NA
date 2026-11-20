package MeongDev.DIRUNA.GloftDIRU.storage;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;

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

    private final AndroidCheckpointStorage storage;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "diru-na-checkpoint-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());
    private long nextSequence;
    private long latestRequestedSequence;
    private boolean closed;

    public CheckpointIoDispatcher(AndroidCheckpointStorage storage) {
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
            AndroidCheckpointLoadResult result = storage.loadCandidate();
            main.post(() -> callback.onComplete(result));
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
        long sequence = ++nextSequence;
        latestRequestedSequence = sequence;
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

    private synchronized boolean isSuperseded(long sequence) {
        return sequence < latestRequestedSequence;
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
