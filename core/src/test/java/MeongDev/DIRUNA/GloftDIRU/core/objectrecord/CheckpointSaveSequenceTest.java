package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CheckpointSaveSequenceTest {
    @Test public void newerQueuedRequestSupersedesOlderRequest() {
        CheckpointSaveSequence sequence = new CheckpointSaveSequence();
        long first = sequence.request();
        assertFalse(sequence.isSuperseded(first));

        long second = sequence.request();
        assertTrue(sequence.isSuperseded(first));
        assertFalse(sequence.isSuperseded(second));
        assertEquals(second, sequence.latestRequested());
    }

    @Test public void inFlightRequestIsNotRetroactivelyCancelledBySequenceGate() {
        CheckpointSaveSequence sequence = new CheckpointSaveSequence();
        long inFlight = sequence.request();
        // The caller checks once immediately before entering storage.save().
        assertFalse(sequence.isSuperseded(inFlight));

        long later = sequence.request();
        // The gate marks only the old sequence as stale for workers that have
        // not begun writing; it cannot and does not interrupt the in-flight I/O.
        assertTrue(sequence.isSuperseded(inFlight));
        assertFalse(sequence.isSuperseded(later));
    }

    @Test public void unknownSequenceIsRejected() {
        CheckpointSaveSequence sequence = new CheckpointSaveSequence();
        try {
            sequence.isSuperseded(1);
            fail("unknown sequence must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, sequence.latestRequested());
        }
    }

    @Test public void sequenceIsMonotonicAcrossRequests() {
        CheckpointSaveSequence sequence = new CheckpointSaveSequence();
        assertEquals(1L, sequence.request());
        assertEquals(2L, sequence.request());
        assertEquals(3L, sequence.request());
    }
}
