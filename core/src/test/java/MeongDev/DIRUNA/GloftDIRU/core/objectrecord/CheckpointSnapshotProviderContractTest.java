package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.ObjectRecordTable.*;

/**
 * Executable examples for the snapshot-provider boundary.
 *
 * These tests use a deliberately small test provider. They do not claim that
 * the application has a production gameplay provider; that requires the
 * concrete game engine and its authoritative state thread.
 */
public final class CheckpointSnapshotProviderContractTest {
    private static final class TestProvider implements CheckpointSnapshotProvider {
        private final CrossTableLifecycleCoordinator live;
        private boolean ready = true;

        TestProvider(CrossTableLifecycleCoordinator live) {
            this.live = live;
        }

        @Override
        public CrossTableCheckpoint captureCheckpoint() {
            if (!ready) throw new IllegalStateException("game state is not ready");
            return new CrossTableCheckpoint(live.snapshot(), Map.of());
        }

        @Override
        public CrossTableLifecycleCoordinator.Status restoreCheckpoint(
                CrossTableCheckpoint checkpoint) {
            if (checkpoint == null)
                return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;
            return checkpoint.restoreInto(live);
        }
    }

    private static final class CountingStore implements CheckpointStore {
        private final AtomicInteger writes = new AtomicInteger();
        private byte[] value;

        @Override
        public void write(byte[] encodedCheckpoint) {
            writes.incrementAndGet();
            value = encodedCheckpoint.clone();
        }

        @Override
        public byte[] read() throws IOException {
            if (value == null) throw new IOException("missing");
            return value.clone();
        }
    }

    @Test
    public void capturedCheckpointIsDetachedFromLaterLiveMutations() {
        CrossTableLifecycleCoordinator live = new CrossTableLifecycleCoordinator();
        TestProvider provider = new TestProvider(live);

        CrossTableCheckpoint captured = provider.captureCheckpoint();
        assertNotNull(captured);

        assertEquals(Status.OK, live.objects().allocate(RecordType.TYPE_1, 8, 9, 1).status());

        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, captured.restoreInto(restored));
        assertEquals(0, restored.snapshot().objectSnapshot().records().size());
        assertEquals(1, live.snapshot().objectSnapshot().records().size());
    }

    @Test
    public void failedCaptureDoesNotReachPersistenceStore() {
        CrossTableLifecycleCoordinator live = new CrossTableLifecycleCoordinator();
        TestProvider provider = new TestProvider(live);
        provider.ready = false;
        CountingStore store = new CountingStore();
        CheckpointPersistenceService persistence = new CheckpointPersistenceService(store);

        try {
            CrossTableCheckpoint checkpoint = provider.captureCheckpoint();
            persistence.save(checkpoint);
            fail("capture must fail while live game state is unavailable");
        } catch (IllegalStateException expected) {
            assertEquals(0, store.writes.get());
        } catch (IOException unexpected) {
            fail("no persistence I/O should be attempted: " + unexpected.getMessage());
        }
    }

    @Test
    public void providerRestoreRejectsInvalidCandidateWithoutChangingLiveState() {
        CrossTableLifecycleCoordinator live = new CrossTableLifecycleCoordinator();
        assertEquals(Status.OK, live.objects().allocate(RecordType.TYPE_1, 1, 2, 0).status());
        TestProvider provider = new TestProvider(live);
        int before = live.snapshot().objectSnapshot().records().size();

        CrossTableCheckpoint invalid = new CrossTableCheckpoint(
                CrossTableCheckpoint.CURRENT_SCHEMA_VERSION + 1,
                live.snapshot(), Map.of());

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                provider.restoreCheckpoint(invalid));
        assertEquals(before, live.snapshot().objectSnapshot().records().size());
    }
}
