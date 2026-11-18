package MeongDev.DIRUNA.GloftDIRU.core.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LegacyDirectionResolverTest {
    @Test
    public void returnsNoneWhenNoDirectionIsHeld() {
        assertEquals(LegacyDirectionResolver.NONE,
                LegacyDirectionResolver.resolveNormalStageDirection(new HeldKeyState()));
    }

    @Test
    public void resolvesEachDirectionIndividually() {
        assertDirection(LegacyInputMasks.UP, LegacyDirectionResolver.UP);
        assertDirection(LegacyInputMasks.DOWN, LegacyDirectionResolver.DOWN);
        assertDirection(LegacyInputMasks.LEFT, LegacyDirectionResolver.LEFT);
        assertDirection(LegacyInputMasks.RIGHT, LegacyDirectionResolver.RIGHT);
    }

    @Test
    public void upHasPriorityOverDown() {
        assertDirection(LegacyInputMasks.UP | LegacyInputMasks.DOWN,
                LegacyDirectionResolver.UP);
    }

    @Test
    public void upHasPriorityOverLeft() {
        assertDirection(LegacyInputMasks.UP | LegacyInputMasks.LEFT,
                LegacyDirectionResolver.UP);
    }

    @Test
    public void upHasPriorityOverRight() {
        assertDirection(LegacyInputMasks.UP | LegacyInputMasks.RIGHT,
                LegacyDirectionResolver.UP);
    }

    @Test
    public void downHasPriorityOverLeft() {
        assertDirection(LegacyInputMasks.DOWN | LegacyInputMasks.LEFT,
                LegacyDirectionResolver.DOWN);
    }

    @Test
    public void downHasPriorityOverRight() {
        assertDirection(LegacyInputMasks.DOWN | LegacyInputMasks.RIGHT,
                LegacyDirectionResolver.DOWN);
    }

    @Test
    public void leftHasPriorityOverRight() {
        assertDirection(LegacyInputMasks.LEFT | LegacyInputMasks.RIGHT,
                LegacyDirectionResolver.LEFT);
    }

    @Test
    public void upWinsWhenAllFourDirectionsAreHeld() {
        int allDirections = LegacyInputMasks.UP
                | LegacyInputMasks.DOWN
                | LegacyInputMasks.LEFT
                | LegacyInputMasks.RIGHT;

        assertDirection(allDirections, LegacyDirectionResolver.UP);
    }

    @Test
    public void resolverDoesNotMutateHeldInputState() {
        HeldKeyState state = new HeldKeyState();
        int held = LegacyInputMasks.DOWN | LegacyInputMasks.RIGHT;
        state.press(held);

        LegacyDirectionResolver.resolveNormalStageDirection(state);

        assertEquals(held, state.snapshotMask());
    }

    @Test
    public void nonDirectionalBitsDoNotCreateDirection() {
        HeldKeyState state = new HeldKeyState();
        state.press(32784 | 32944 | 256);

        assertEquals(LegacyDirectionResolver.NONE,
                LegacyDirectionResolver.resolveNormalStageDirection(state));
    }

    private static void assertDirection(int inputMask, int expectedDirection) {
        HeldKeyState state = new HeldKeyState();
        state.press(inputMask);

        assertEquals(expectedDirection,
                LegacyDirectionResolver.resolveNormalStageDirection(state));
    }
}
