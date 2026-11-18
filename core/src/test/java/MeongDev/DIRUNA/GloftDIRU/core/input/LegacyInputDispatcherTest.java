package MeongDev.DIRUNA.GloftDIRU.core.input;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyInputDispatcherTest {
    // Synthetic single-bit fixtures: 0x10 matches CENTER; 0x20 matches CONFIRM
    // without also matching CENTER. These are not Android or handset key codes.
    private static final int CENTER_SAMPLE = 0x10;
    private static final int CONFIRM_ONLY_SAMPLE = 0x20;
    private static LegacyInputDispatcher.Context normalContext() {
        return new LegacyInputDispatcher.Context(false, 0, 0,
                false, 1, false, false, false, 0);
    }

    private static LegacyInputDispatcher.Result dispatch(int mask,
            LegacyInputDispatcher.Context context, HeldKeyState state) {
        state.press(mask);
        return LegacyInputDispatcher.dispatch(state, context);
    }

    @Test public void stateLockClearsInputAndBlocksDispatch() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(LegacyInputMasks.RIGHT,
                new LegacyInputDispatcher.Context(false, 1, 0,
                        false, 1, false, false, false, 0), keys);
        assertEquals(LegacyInputDispatcher.Kind.BLOCKED, result.kind());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void clearBeforeDispatchHappensBeforeClIntGate() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(CONFIRM_ONLY_SAMPLE,
                new LegacyInputDispatcher.Context(true, 1, 0,
                        false, 1, false, false, false, 0), keys);
        assertEquals(LegacyInputDispatcher.Kind.BLOCKED, result.kind());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void confirmBypassesOnlyTheClIntGate() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(CONFIRM_ONLY_SAMPLE,
                new LegacyInputDispatcher.Context(false, 1, 0,
                        false, 1, false, false, false, 0), keys);
        assertEquals(LegacyInputDispatcher.Kind.NO_INPUT, result.kind());
        assertTrue(keys.isAnyPressed(LegacyInputMasks.CONFIRM));
    }

    @Test public void nonzeroAyCounterBlocksInput() {
        assertBlocked(new LegacyInputDispatcher.Context(false, 0, 1,
                false, 1, false, false, false, 0));
    }

    @Test public void scriptedExitBlocksInput() {
        assertBlocked(new LegacyInputDispatcher.Context(false, 0, 0,
                true, 1, false, false, false, 0));
    }

    @Test public void zeroPlayerStatusBlocksInput() {
        assertBlocked(new LegacyInputDispatcher.Context(false, 0, 0,
                false, 0, false, false, false, 0));
    }

    @Test public void deathAnimationBlocksInput() {
        assertBlocked(new LegacyInputDispatcher.Context(false, 0, 0,
                false, 1, true, false, false, 0));
    }

    @Test public void specialActionFlagBlocksInput() {
        assertBlocked(new LegacyInputDispatcher.Context(false, 0, 0,
                false, 1, false, true, false, 0));
    }

    @Test public void interactionPrefersCenterOverConfirmAndAlwaysClears() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(
                CENTER_SAMPLE | CONFIRM_ONLY_SAMPLE,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, true, 0), keys);
        assertEquals(LegacyInputDispatcher.Kind.INTERACTION_CENTER, result.kind());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void interactionConfirmIsDistinctFromCenterAction() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(CONFIRM_ONLY_SAMPLE,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, true, 0), keys);
        assertEquals(LegacyInputDispatcher.Kind.INTERACTION_CONFIRM, result.kind());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void interactionIdleStillClearsHeldInput() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = LegacyInputDispatcher.dispatch(keys,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, true, 0));
        assertEquals(LegacyInputDispatcher.Kind.INTERACTION_IDLE, result.kind());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void specialMovementUsesItsOwnDirectionPriorityAndOffset() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(
                LegacyInputMasks.UP | LegacyInputMasks.RIGHT,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, false, 3), keys);
        assertEquals(LegacyInputDispatcher.Kind.SPECIAL_MOVEMENT, result.kind());
        assertEquals(LegacyDirectionResolver.UP, result.direction());
        assertEquals(0, result.offsetX());
        assertEquals(-5, result.offsetY());
        assertTrue(result.consumesSpecialMovementTick());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void specialMovementCenterPrecedesConfirmAndConsumesTick() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(
                LegacyInputMasks.CENTER | LegacyInputMasks.CONFIRM,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, false, 2), keys);
        assertEquals(LegacyInputDispatcher.Kind.SPECIAL_CENTER_CONSUMED, result.kind());
        assertTrue(result.consumesSpecialMovementTick());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void specialMovementConfirmIsSeparateAction() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(CONFIRM_ONLY_SAMPLE,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, false, 2), keys);
        assertEquals(LegacyInputDispatcher.Kind.SPECIAL_CONFIRM_ACTION, result.kind());
        assertTrue(result.consumesSpecialMovementTick());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void specialMovementWaitPreservesInputAndDoesNotConsumeTick() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(0,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, false, 2), keys);
        assertEquals(LegacyInputDispatcher.Kind.SPECIAL_WAIT, result.kind());
        assertFalse(result.consumesSpecialMovementTick());
    }

    @Test public void specialMovementRightProducesPositiveHorizontalOffset() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(LegacyInputMasks.RIGHT,
                new LegacyInputDispatcher.Context(false, 0, 0,
                        false, 1, false, false, false, 2), keys);
        assertEquals(5, result.offsetX());
        assertEquals(0, result.offsetY());
    }

    @Test public void normalDirectionLeavesHeldInputForLaterGameUpdate() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(LegacyInputMasks.LEFT,
                normalContext(), keys);
        assertEquals(LegacyInputDispatcher.Kind.NORMAL_DIRECTION, result.kind());
        assertEquals(LegacyDirectionResolver.LEFT, result.direction());
        assertTrue(keys.isAnyPressed(LegacyInputMasks.LEFT));
    }

    @Test public void normalCenterActionClearsInput() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(CENTER_SAMPLE,
                normalContext(), keys);
        assertEquals(LegacyInputDispatcher.Kind.CENTER_ACTION, result.kind());
        assertEquals(0, keys.snapshotMask());
    }

    @Test public void broadConfirmIsNotNormalCenterAction() {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(CONFIRM_ONLY_SAMPLE,
                normalContext(), keys);
        assertEquals(LegacyInputDispatcher.Kind.NO_INPUT, result.kind());
        assertTrue(keys.isAnyPressed(LegacyInputMasks.CONFIRM));
    }

    private static void assertBlocked(LegacyInputDispatcher.Context context) {
        HeldKeyState keys = new HeldKeyState();
        LegacyInputDispatcher.Result result = dispatch(LegacyInputMasks.RIGHT,
                context, keys);
        assertEquals(LegacyInputDispatcher.Kind.BLOCKED, result.kind());
        assertEquals(0, keys.snapshotMask());
    }
}
