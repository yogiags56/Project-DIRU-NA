package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CheckpointPersistenceServiceTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static CrossTableCheckpoint emptyCheckpoint() {
        return new CrossTableCheckpoint(
                new CrossTableLifecycleCoordinator().snapshot(), Map.of());
    }

    @Test
    public void atomicFileStoreWritesAndReadsCompleteCheckpoint() throws Exception {
        Path file = temporaryFolder.getRoot().toPath().resolve("save/diru-na.chk");
        AtomicFileCheckpointStore store = new AtomicFileCheckpointStore(file);
        byte[] expected = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        store.write(expected);
        assertArrayEquals(expected, store.read());
        assertTrue(Files.exists(file));
    }

    @Test
    public void serviceRoundTripRestoresCheckpointIntoCoordinator() throws Exception {
        Path file = temporaryFolder.getRoot().toPath().resolve("checkpoint.bin");
        CheckpointPersistenceService service = new CheckpointPersistenceService(
                new AtomicFileCheckpointStore(file));
        CrossTableCheckpoint checkpoint = emptyCheckpoint();
        service.save(checkpoint);
        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, service.loadInto(destination));
        assertTrue(destination.snapshot().objectSnapshot().records().isEmpty());
        assertTrue(destination.snapshot().objectSnapshot().tileLinks().isEmpty());
    }

    @Test
    public void rejectedWritePreservesLastKnownGoodFile() throws Exception {
        Path file = temporaryFolder.getRoot().toPath().resolve("checkpoint.bin");
        AtomicFileCheckpointStore store = new AtomicFileCheckpointStore(file);
        byte[] knownGood = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        store.write(knownGood);
        try {
            store.write(new byte[CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES + 1]);
            fail("oversized checkpoint must be rejected");
        } catch (IOException expected) {
            assertArrayEquals(knownGood, store.read());
        }
    }

    @Test
    public void truncatedFileDoesNotMutateLiveCoordinator() throws Exception {
        Path file = temporaryFolder.getRoot().toPath().resolve("checkpoint.bin");
        AtomicFileCheckpointStore store = new AtomicFileCheckpointStore(file);
        byte[] valid = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        store.write(valid);
        Files.write(file, java.util.Arrays.copyOf(valid, valid.length - 1));

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        ObjectRecordTable.TileGroup tile = new ObjectRecordTable.TileGroup(9, 8, 1);
        CrossTableLifecycleCoordinator.AttachResult live = destination.attach(
                ObjectRecordTable.RecordType.TYPE_1, 9, 8, 1, tile, 2,
                1L | ((long) 9 << 13) | ((long) 8 << 20));
        assertTrue(live.succeeded());
        try {
            new CheckpointPersistenceService(store).loadInto(destination);
            fail("truncated checkpoint must be rejected");
        } catch (IOException expected) {
            assertEquals(ObjectRecordTable.Status.OK, destination.objects().lookup(tile).status());
            assertEquals(ObjectRecordTable.RecordType.TYPE_1,
                    destination.objects().typeOf(live.objectHandle()).orElseThrow());
            assertEquals(2, destination.associatedSlot(live.objectHandle()));
        }
    }

    @Test
    public void missingFileIsReportedWithoutMutatingDestination() throws Exception {
        Path file = temporaryFolder.getRoot().toPath().resolve("missing.chk");
        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        try {
            new CheckpointPersistenceService(new AtomicFileCheckpointStore(file))
                    .loadInto(destination);
            fail("missing file must be reported");
        } catch (IOException expected) {
            assertTrue(destination.snapshot().objectSnapshot().records().isEmpty());
        }
    }

    @Test
    public void failedAtomicReplacementLeavesNoTemporaryFiles() throws Exception {
        Path directory = temporaryFolder.getRoot().toPath().resolve("folder-target");
        Files.createDirectories(directory);
        Path target = directory.resolve("checkpoint.bin");
        AtomicFileCheckpointStore store = new AtomicFileCheckpointStore(target);
        byte[] valid = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        try {
            store.write(valid);
            fail("replacing a non-empty directory target must fail");
        } catch (IOException expected) {
            try (java.util.stream.Stream<Path> children = Files.list(directory)) {
                assertEquals(0L, children.count());
            }
        }
    }
}
