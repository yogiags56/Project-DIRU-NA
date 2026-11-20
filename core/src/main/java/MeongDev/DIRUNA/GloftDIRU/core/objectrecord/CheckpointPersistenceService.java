package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.io.IOException;

/** Coordinates codec validation with a pluggable whole-blob persistence adapter. */
public final class CheckpointPersistenceService {
    private final CheckpointStore store;

    public CheckpointPersistenceService(CheckpointStore store) {
        if (store == null) throw new IllegalArgumentException("checkpoint store is required");
        this.store = store;
    }

    public void save(CrossTableCheckpoint checkpoint) throws IOException {
        store.write(CrossTableCheckpointBinaryCodec.encode(checkpoint));
    }

    public CrossTableLifecycleCoordinator.Status loadInto(
            CrossTableLifecycleCoordinator destination) throws IOException {
        if (destination == null)
            return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;
        byte[] encoded = store.read();
        return CrossTableCheckpointBinaryCodec.decodeInto(encoded, destination);
    }
}
