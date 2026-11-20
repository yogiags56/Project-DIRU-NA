package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static org.junit.Assert.*;
import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.AuxiliaryEntryStateMachine.*;

public final class AuxiliaryEntryStateMachineTest {
    private static long entry(int state, int direction, int movement, int x, int y, long unknown) {
        return (unknown & ~0x7FFFFFFL)
                | ((long) y << 20) | ((long) x << 13) | ((long) movement << 6)
                | ((long) direction << 4) | state;
    }

    @Test
    public void stateOneTransitionsToThreeOnlyWhenMovementIsZeroAndLinkMatches() {
        long raw = entry(1, 2, 0, 44, 57, 0x6A00000000000000L);
        TransitionResult result = markReady(raw, 5, 5);
        assertEquals(Status.OK, result.status());
        assertEquals(3, state(result.rawEntry()));
        assertEquals(2, direction(result.rawEntry()));
        assertEquals(44, x(result.rawEntry()));
        assertEquals(57, y(result.rawEntry()));
    }

    @Test
    public void transitionPreservesAllUnknownAndKnownNonStateBits() {
        long raw = 0xA5C0000000000000L | entry(1, 3, 0, 127, 126, 0L);
        TransitionResult result = markReady(raw, 7, 7);
        assertEquals(Status.OK, result.status());
        assertEquals(raw & ~0xFL, result.rawEntry() & ~0xFL);
    }

    @Test
    public void nonzeroMovementCannotEnterReadyState() {
        long raw = entry(1, 1, 1, 2, 3, 0L);
        TransitionResult result = markReady(raw, 1, 1);
        assertEquals(Status.MOVEMENT_NOT_READY, result.status());
        assertEquals(raw, result.rawEntry());
    }

    @Test
    public void wrongStateAndMismatchedLinkAreRejectedAtomically() {
        long allocated = entry(2, 0, 0, 4, 5, 0L);
        assertEquals(Status.WRONG_STATE, markReady(allocated, 2, 2).status());
        long stateOne = entry(1, 0, 0, 4, 5, 0L);
        TransitionResult mismatch = markReady(stateOne, 2, 3);
        assertEquals(Status.LINK_MISMATCH, mismatch.status());
        assertEquals(stateOne, mismatch.rawEntry());
    }

    @Test
    public void unrepresentableAndInvalidSlotsAreRejectedWithoutMutation() {
        long raw = entry(1, 0, 0, 1, 1, 0L);
        assertEquals(Status.UNREPRESENTABLE_LINK, markReady(raw, 8, 0).status());
        assertEquals(Status.INVALID_SLOT, markReady(raw, 15, 7).status());
        assertEquals(Status.INVALID_SLOT, markReady(raw, -1, 7).status());
        assertEquals(1, state(raw));
    }

    @Test
    public void consumingReadyEntryReturnsFieldsAndClearsWholeWord() {
        long raw = entry(3, 2, 19, 61, 72, 0xD500000000000000L);
        ConsumeResult result = consume(raw);
        assertEquals(Status.OK, result.status());
        assertEquals(0L, result.remainingRawEntry());
        assertEquals(61, result.x());
        assertEquals(72, result.y());
        assertEquals(2, result.direction());
    }

    @Test
    public void nonReadyEntryCannotBeConsumedOrCleared() {
        long raw = entry(1, 3, 0, 7, 8, 0x9900000000000000L);
        ConsumeResult result = consume(raw);
        assertEquals(Status.WRONG_STATE, result.status());
        assertEquals(raw, result.remainingRawEntry());
    }
}
