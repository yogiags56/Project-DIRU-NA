package MeongDev.DIRUNA.GloftDIRU.storage;

import static org.junit.Assert.*;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Map;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.ObjectRecordTable;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpointBinaryCodec;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AndroidCheckpointStorageTest {
    private Context context;
    private File checkpointFile;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        checkpointFile = new File(context.getFilesDir(),
                AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        new AndroidAtomicCheckpointStore(context.getFilesDir(),
                AndroidCheckpointStorage.DEFAULT_FILE_NAME).delete();
    }

    @After public void tearDown() {
        new AndroidAtomicCheckpointStore(context.getFilesDir(),
                AndroidCheckpointStorage.DEFAULT_FILE_NAME).delete();
    }

    private byte[] validEmptyCheckpoint() throws Exception {
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(
                new CrossTableLifecycleCoordinator().snapshot(), Map.of());
        return CrossTableCheckpointBinaryCodec.encode(checkpoint);
    }

    @Test public void atomicStoreWritesAndReadsCheckpointBytes() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        byte[] expected = validEmptyCheckpoint();
        store.write(expected);
        assertArrayEquals(expected, store.read());
        assertTrue(store.hasRecoverableFile());
    }

    @Test public void loadCandidateDoesNotMutateCoordinatorUntilExplicitApply() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        store.write(validEmptyCheckpoint());

        AndroidCheckpointLoadResult candidate =
                new AndroidCheckpointStorage(context).loadCandidate();
        assertTrue(candidate.restored());
        assertNotNull(candidate.checkpoint());

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, candidate.applyTo(destination));
    }

    @Test public void loadSafelyDistinguishesMissingCheckpoint() {
        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadSafelyInto(
                        new CrossTableLifecycleCoordinator());
        assertEquals(AndroidCheckpointLoadResult.Status.MISSING, result.status());
    }

    @Test public void loadSafelyRestoresValidCheckpoint() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        store.write(validEmptyCheckpoint());

        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadSafelyInto(
                        new CrossTableLifecycleCoordinator());
        assertEquals(AndroidCheckpointLoadResult.Status.RESTORED, result.status());
    }

    private static long stateOneRaw(int direction, int movement, int x, int y) {
        return ((long) y << 20) | ((long) x << 13) | ((long) movement << 6)
                | ((long) direction << 4) | 1L;
    }

    @Test public void corruptCheckpointIsReportedAndNotSilentlyReset() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        store.write(new byte[] { 0x44, 0x49, 0x52, 0x55, 0, 0, 0, 99 });

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        ObjectRecordTable.TileGroup liveTile = new ObjectRecordTable.TileGroup(3, 4, 0);
        CrossTableLifecycleCoordinator.AttachResult live = destination.attach(
                ObjectRecordTable.RecordType.TYPE_2, 3, 4, 0, liveTile, 2,
                stateOneRaw(1, 0, 3, 4));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, live.status());

        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadSafelyInto(destination);
        assertEquals(AndroidCheckpointLoadResult.Status.CORRUPT, result.status());
        assertEquals(2, destination.associatedSlot(live.objectHandle()));
        assertEquals(ObjectRecordTable.Status.OK,
                destination.objects().lookup(liveTile).status());
        assertTrue(checkpointFile.exists());
    }

    @Test public void interruptedWriteBackupIsRecoveredOnRead() throws Exception {
        byte[] knownGood = validEmptyCheckpoint();
        Files.write(new File(checkpointFile.getPath() + ".bak").toPath(), knownGood);
        Files.write(checkpointFile.toPath(), new byte[] { 1, 2, 3 });

        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        assertArrayEquals(knownGood, store.read());
    }

    @Test public void newStorageInstanceRestoresCheckpointAfterProcessRecreation() throws Exception {
        AndroidCheckpointStorage writer = new AndroidCheckpointStorage(context);
        CrossTableCheckpoint expected = new CrossTableCheckpoint(
                new CrossTableLifecycleCoordinator().snapshot(), Map.of());
        writer.save(expected);

        // A new adapter instance models application process recreation: no
        // in-memory storage object is reused for the subsequent load.
        AndroidCheckpointStorage reopened = new AndroidCheckpointStorage(context);
        AndroidCheckpointLoadResult candidate = reopened.loadCandidate();
        assertTrue(candidate.restored());

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, candidate.applyTo(destination));
    }

    @Test public void backupOnlyCheckpointIsRecognizedAndRecovered() throws Exception {
        byte[] knownGood = validEmptyCheckpoint();
        File backup = new File(checkpointFile.getPath() + ".bak");
        Files.write(backup.toPath(), knownGood);
        assertFalse(checkpointFile.exists());

        AndroidCheckpointStorage reopened = new AndroidCheckpointStorage(context);
        AndroidCheckpointLoadResult candidate = reopened.loadCandidate();

        assertTrue(candidate.restored());
        assertArrayEquals(knownGood, new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME).read());
    }


    @Test public void truncatedCheckpointIsReportedAsCorruptAndPreserved() throws Exception {
        byte[] valid = validEmptyCheckpoint();
        byte[] truncated = java.util.Arrays.copyOf(valid, valid.length / 2);
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        store.write(truncated);

        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadCandidate();

        assertEquals(AndroidCheckpointLoadResult.Status.CORRUPT, result.status());
        assertTrue(store.hasRecoverableFile());
        assertArrayEquals(truncated, store.read());
    }


    @Test public void emptyCheckpointFileIsStorageErrorAndPreserved() throws Exception {
        Files.write(checkpointFile.toPath(), new byte[0]);
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);

        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadCandidate();

        assertEquals(AndroidCheckpointLoadResult.Status.STORAGE_ERROR, result.status());
        assertTrue(store.hasRecoverableFile());
        assertEquals(0, checkpointFile.length());
    }

    @Test public void unsupportedBinaryFormatVersionIsCorruptAndPreserved() throws Exception {
        byte[] encoded = validEmptyCheckpoint();
        encoded[4] = 0;
        encoded[5] = 0;
        encoded[6] = 0;
        encoded[7] = 99;
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        store.write(encoded);

        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadCandidate();

        assertEquals(AndroidCheckpointLoadResult.Status.CORRUPT, result.status());
        assertArrayEquals(encoded, store.read());
    }

    @Test public void invalidDestinationIsReportedWithoutFileMutation() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        byte[] valid = validEmptyCheckpoint();
        store.write(valid);

        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadSafelyInto(null);
        assertEquals(AndroidCheckpointLoadResult.Status.INVALID_SNAPSHOT, result.status());
        assertArrayEquals(valid, store.read());
    }

    @Test public void oversizedWriteIsRejectedWithoutReplacingPreviousCheckpoint() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        byte[] previous = validEmptyCheckpoint();
        store.write(previous);

        try {
            store.write(new byte[CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES + 1]);
            fail("oversized payload must be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(store.hasRecoverableFile());
            assertArrayEquals(previous, store.read());
        }
    }

    @Test public void oversizedStoredFileIsRejectedAndPreserved() throws Exception {
        byte[] oversized = new byte[CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES + 1];
        try (FileOutputStream output = new FileOutputStream(checkpointFile)) {
            output.write(oversized);
        }

        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadCandidate();

        assertEquals(AndroidCheckpointLoadResult.Status.STORAGE_ERROR, result.status());
        assertTrue(store.hasRecoverableFile());
        assertEquals(oversized.length, checkpointFile.length());
    }
}
