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
    public void failedAtomicReplacementLeavesOldFileAndCleansTemporaryFile() throws Exception {
        Path target = temporaryFolder.getRoot().toPath().resolve("checkpoint.bin");
        byte[] knownGood = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        AtomicFileCheckpointStore initial = new AtomicFileCheckpointStore(target);
        initial.write(knownGood);

        AtomicFileCheckpointStore failing = new AtomicFileCheckpointStore(target,
                (source, destination) -> {
                    throw new IOException("simulated failure before atomic replacement");
                });
        try {
            failing.write(new byte[] { 1, 2, 3, 4 });
            fail("simulated replacement failure must be reported");
        } catch (IOException expected) {
            assertArrayEquals(knownGood, initial.read());
            try (java.util.stream.Stream<Path> children = Files.list(target.getParent())) {
                assertEquals(1L, children.count());
            }
        }
    }

    @Test
    public void unsupportedAtomicMoveFailsClosedAndPreservesOldFile() throws Exception {
        Path target = temporaryFolder.getRoot().toPath().resolve("checkpoint.bin");
        byte[] knownGood = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        AtomicFileCheckpointStore initial = new AtomicFileCheckpointStore(target);
        initial.write(knownGood);

        AtomicFileCheckpointStore unsupported = new AtomicFileCheckpointStore(target,
                (source, destination) -> {
                    throw new java.nio.file.AtomicMoveNotSupportedException(
                            source.toString(), destination.toString(), "simulated filesystem");
                });
        try {
            unsupported.write(new byte[] { 9, 8, 7 });
            fail("unsupported atomic move must fail");
        } catch (IOException expected) {
            assertArrayEquals(knownGood, initial.read());
            try (java.util.stream.Stream<Path> children = Files.list(target.getParent())) {
                assertEquals(1L, children.count());
            }
        }
    }

    @Test
    public void successfulReplacementChangesFileToNewCompleteCheckpoint() throws Exception {
        Path target = temporaryFolder.getRoot().toPath().resolve("checkpoint.bin");
        AtomicFileCheckpointStore store = new AtomicFileCheckpointStore(target);
        byte[] first = CrossTableCheckpointBinaryCodec.encode(emptyCheckpoint());
        store.write(first);
        byte[] second = first.clone();
        second[second.length - 1] ^= 0x01;
        store.write(second);
        assertArrayEquals(second, store.read());
    }
}
