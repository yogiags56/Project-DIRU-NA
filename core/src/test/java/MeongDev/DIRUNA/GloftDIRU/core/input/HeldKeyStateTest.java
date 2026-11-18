package MeongDev.DIRUNA.GloftDIRU.core.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HeldKeyStateTest {
    @Test
    public void startsWithNoHeldKeys() {
        HeldKeyState state = new HeldKeyState();

        assertEquals(0, state.snapshotMask());
        assertFalse(state.isAnyPressed(1));
    }

    @Test
    public void pressCombinesHeldBits() {
        HeldKeyState state = new HeldKeyState();

        state.press(0b0001);
        state.press(0b0100);

        assertEquals(0b0101, state.snapshotMask());
    }

    @Test
    public void releaseClearsOnlyRequestedBits() {
        HeldKeyState state = new HeldKeyState();
        state.press(0b1111);

        state.release(0b0101);

        assertEquals(0b1010, state.snapshotMask());
    }

    @Test
    public void queryMatchesWhenAnyRequestedBitIsHeld() {
        HeldKeyState state = new HeldKeyState();
        state.press(0b0100);

        assertTrue(state.isAnyPressed(0b1100));
        assertFalse(state.isAnyPressed(0b0011));
    }

    @Test
    public void clearRemovesAllHeldBits() {
        HeldKeyState state = new HeldKeyState();
        state.press(0b1010);

        state.clear();

        assertEquals(0, state.snapshotMask());
        assertFalse(state.isAnyPressed(-1));
    }

    @Test
    public void zeroQueryNeverMatches() {
        HeldKeyState state = new HeldKeyState();
        state.press(-1);

        assertFalse(state.isAnyPressed(0));
    }
}
