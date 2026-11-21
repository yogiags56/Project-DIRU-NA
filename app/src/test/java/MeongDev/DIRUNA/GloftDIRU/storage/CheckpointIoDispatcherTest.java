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
import java.util.function.BooleanSupplier;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableLifecycleCoordinator;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public final class CheckpointIoDispatcherTest {
    private static final long TIMEOUT_SECONDS = 15;

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
        volatile CountDownLatch loadFinished;
        volatile CountDownLatch saveFinished;
        volatile boolean failSave;
        volatile boolean failLoad;

        @Override public void save(CrossTableCheckpoint value) throws IOException {
            int number = writes.incrementAndGet();
            if (saveEntered != null) saveEntered.countDown();
            try {
                awaitIfPresent(releaseSave);
                if (failSave) throw new IOException("synthetic write failure");
                saved.add(value);
            } finally {
                if (saveFinished != null) saveFinished.countDown();
            }
        }

        @Override public AndroidCheckpointLoadResult loadCandidate() {
            if (loadEntered != null) loadEntered.countDown();
            try {
                awaitIfPresent(releaseLoad);
                if (failLoad) throw new IllegalStateException("synthetic load failure");
                return AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.MISSING, "test missing");
            } finally {
                if (loadFinished != null) loadFinished.countDown();
            }
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

    private static void drainMainLooperUntil(BooleanSupplier completed) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!completed.getAsBoolean() && System.nanoTime() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue("timed out waiting for main-looper callback", completed.getAsBoolean());
    }

    @Test public void queuedOlderSaveIsSupersededAndNewestSaveIsWritten() throws Exception {
        FakeStorage storage = new FakeStorage();
        storage.loadEntered = new CountDownLatch(1);
        storage.releaseLoad = new CountDownLatch(1);
        CheckpointIoDispatcher dispatcher = new CheckpointIoDispatcher(storage);
        storage.loadFinished = new CountDownLatch(1);
        dispatcher.load(result -> { });
        await(storage.loadEntered);

        List<CheckpointSaveResult> results = Collections.synchronizedList(new ArrayList<>());
        storage.saveFinished = new CountDownLatch(2);
        dispatcher.save(checkpoint(), result -> { results.add(result); });
        dispatcher.save(checkpoint(), result -> { results.add(result); });

        storage.releaseLoad.countDown();
        await(storage.loadFinished);
        await(storage.saveFinished);
        drainMainLooperUntil(() -> results.size() == 2);

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
        storage.saveFinished = new CountDownLatch(2);

        dispatcher.save(checkpoint(), result -> { results.add(result); });
        await(storage.saveEntered);
        dispatcher.save(checkpoint(), result -> { results.add(result); });
        storage.releaseSave.countDown();

        await(storage.saveFinished);
        drainMainLooperUntil(() -> results.size() == 2);
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
        storage.saveFinished = new CountDownLatch(1);
        List<CheckpointSaveResult> results = new ArrayList<>();

        dispatcher.save(checkpoint(), result -> results.add(result));
        await(storage.saveFinished);
        drainMainLooperUntil(() -> results.size() == 1);

        assertEquals(1, results.size());
        assertEquals(CheckpointSaveResult.Status.FAILED, results.get(0).status());
        assertNotNull(results.get(0).failure());
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
