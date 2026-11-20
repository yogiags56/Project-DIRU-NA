package MeongDev.DIRUNA.GloftDIRU.storage;

import static org.junit.Assert.*;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Map;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
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

    @Test public void corruptCheckpointIsReportedAndNotSilentlyReset() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        store.write(new byte[] { 0x44, 0x49, 0x52, 0x55, 0, 0, 0, 99 });

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        AndroidCheckpointLoadResult result =
                new AndroidCheckpointStorage(context).loadSafelyInto(destination);
        assertEquals(AndroidCheckpointLoadResult.Status.CORRUPT, result.status());
        assertTrue(destination.snapshot().objectSnapshot().records().isEmpty());
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

    @Test public void oversizedWriteIsRejected() throws Exception {
        AndroidAtomicCheckpointStore store = new AndroidAtomicCheckpointStore(
                context.getFilesDir(), AndroidCheckpointStorage.DEFAULT_FILE_NAME);
        try {
            store.write(new byte[CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES + 1]);
            fail("oversized payload must be rejected");
        } catch (java.io.IOException expected) {
            assertFalse(store.hasRecoverableFile());
        }
    }
}
