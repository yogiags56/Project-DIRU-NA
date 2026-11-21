package MeongDev.DIRUNA.GloftDIRU.storage;

import android.os.Looper;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.Shadows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public final class CheckpointIoDispatcherTest {
    private static final long TIMEOUT_SECONDS = 3;

    private static CrossTableCheckpoint checkpoint() {
        return new CrossTableCheckpoint(
                new CrossTableLifecycleCoordinator().snapshot(),
                Collections.emptyMap());
    }

    private static final class FakeStorage implements CheckpointStorage {
        final AtomicInteger writes = new AtomicInteger();
        final List<CrossTableCheckpoint> saved =
                Collections.synchronizedList(new ArrayList<>());
        volatile CountDownLatch loadEntered;
        volatile CountDownLatch releaseLoad;
        volatile CountDownLatch saveEntered;
        volatile CountDownLatch releaseSave;
        volatile boolean failSave;

        @Override public void save(CrossTableCheckpoint value) throws IOException {
            int number = writes.incrementAndGet();
            if (saveEntered != null) saveEntered.countDown();
            awaitIfPresent(releaseSave);
            if (failSave) throw new IOException("synthetic write failure");
            saved.add(value);
        }

        @Override public AndroidCheckpointLoadResult loadCandidate() {
            if (loadEntered != null) loadEntered.countDown();
            awaitIfPresent(releaseLoad);
            return AndroidCheckpointLoadResult.of(
                    AndroidCheckpointLoadResult.Status.MISSING, "test missing");
        }

        private static void awaitIfPresent(CountDownLatch latch) {
            if (latch == null) return;
            try {
                if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    throw new AssertionError("timed out waiting for test latch");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("test worker interrupted", ex);
            }
        }
    }

    @After public void drainMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue("timed out waiting for worker", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test public void queuedOlderSaveIsSupersededAndNewestSaveIsWritten() throws Exception {
        FakeStorage storage = new FakeStorage();
        storage.loadEntered = new CountDownLatch(1);
        storage.releaseLoad = new CountDownLatch(1);
        CheckpointIoDispatcher dispatcher = new CheckpointIoDispatcher(storage);
        CountDownLatch loadDone = new CountDownLatch(1);
        dispatcher.load(result -> loadDone.countDown());
        await(storage.loadEntered);

        List<CheckpointSaveResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch savesDone = new CountDownLatch(2);
        dispatcher.save(checkpoint(), result -> { results.add(result); savesDone.countDown(); });
        dispatcher.save(checkpoint(), result -> { results.add(result); savesDone.countDown(); });

        storage.releaseLoad.countDown();
        await(loadDone);
        await(savesDone);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, storage.writes.get());
        assertEquals(1, storage.saved.size());
        assertEquals(2, results.size());
        assertEquals(CheckpointSaveResult.Status.SUPERSEDED, results.get(0).status());
        assertEquals(CheckpointSaveResult.Status.SAVED, results.get(1).status());
        dispatcher.close();
    }

    @Test public void alreadyStartedWriteIsAllowedToFinishBeforeNewerSave() throws Exception {
        FakeStorage storage = new FakeStorage();
        storage.saveEntered = new CountDownLatch(1);
        storage.releaseSave = new CountDownLatch(1);
        CheckpointIoDispatcher dispatcher = new CheckpointIoDispatcher(storage);
        List<CheckpointSaveResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch savesDone = new CountDownLatch(2);

        dispatcher.save(checkpoint(), result -> { results.add(result); savesDone.countDown(); });
        await(storage.saveEntered);
        dispatcher.save(checkpoint(), result -> { results.add(result); savesDone.countDown(); });
        storage.releaseSave.countDown();

        await(savesDone);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, storage.writes.get());
        assertEquals(2, storage.saved.size());
        assertEquals(CheckpointSaveResult.Status.SAVED, results.get(0).status());
        assertEquals(CheckpointSaveResult.Status.SAVED, results.get(1).status());
        dispatcher.close();
    }

    @Test public void storageFailureIsReportedAsFailed() throws Exception {
        FakeStorage storage = new FakeStorage();
        storage.failSave = true;
        CheckpointIoDispatcher dispatcher = new CheckpointIoDispatcher(storage);
        CountDownLatch done = new CountDownLatch(1);
        List<CheckpointSaveResult> results = new ArrayList<>();

        dispatcher.save(checkpoint(), result -> { results.add(result); done.countDown(); });
        await(done);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, results.size());
        assertEquals(CheckpointSaveResult.Status.FAILED, results.get(0).status());
        assertNotNull(results.get(0).error());
        dispatcher.close();
    }

    @Test public void saveAfterCloseIsRejectedWithoutTouchingStorage() {
        FakeStorage storage = new FakeStorage();
        CheckpointIoDispatcher dispatcher = new CheckpointIoDispatcher(storage);
        dispatcher.close();
        try {
            dispatcher.save(checkpoint(), result -> fail("callback must not be invoked"));
            fail("save after close must throw");
        } catch (IllegalStateException expected) {
            assertEquals(0, storage.writes.get());
        }
    }
}
