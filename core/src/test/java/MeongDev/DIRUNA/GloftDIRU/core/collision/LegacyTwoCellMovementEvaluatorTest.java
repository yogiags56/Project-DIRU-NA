package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyTwoCellMovementEvaluatorTest {
    private static LegacyTwoCellMovementEvaluator.Decision evaluate(
            boolean early, int counter, boolean probe, boolean direction,
            boolean positive, boolean passable, int fgLow, int fgHigh,
            int adjacentPlayer, int adjacentForeground, boolean incoming) {
        return LegacyTwoCellMovementEvaluator.evaluate(early,counter,probe,direction,
                positive,passable,fgLow,fgHigh,adjacentPlayer,adjacentForeground,0,incoming);
    }

    @Test public void earlyReturnHappensBeforeCounterDecrementAndSuppressesEffects() {
        LegacyTwoCellMovementEvaluator.Decision d =
                evaluate(true,4,false,true,true,true,0,0,0,0,true);
        assertTrue(d.isEarlyReturn());
        assertFalse(d.getReturnFlag());
        assertEquals(4,d.getCounterAfter());
        assertEquals(9,d.getEarlyReturnAnimation());
        assertFalse(d.isWorldPushRequested());
        assertFalse(d.shouldSetMovementBlock());
        assertFalse(d.shouldClearLocalDirection());
        LegacyTwoCellMovementEvaluator.Decision alternate = LegacyTwoCellMovementEvaluator.evaluate(
                true,4,false,true,true,true,0,0,0,0,2,true);
        assertEquals(8,alternate.getEarlyReturnAnimation());
    }

    @Test public void acceptedPushReturnsTrueAndRequestsMutationOnlyOutsideProbe() {
        LegacyTwoCellMovementEvaluator.Decision normal =
                evaluate(false,0,false,true,true,true,0,0,0,0,false);
        assertTrue(normal.isPushAccepted());
        assertTrue(normal.getReturnFlag());
        assertEquals(-1,normal.getCounterAfter());
        assertTrue(normal.isWorldPushRequested());
        assertTrue(normal.shouldSetMovementBlock());
        assertTrue(normal.isPushTowardPositiveDirection());

        LegacyTwoCellMovementEvaluator.Decision probe =
                evaluate(false,0,true,true,false,true,0,0,0,0,false);
        assertTrue(probe.isPushAccepted());
        assertTrue(probe.getReturnFlag());
        assertFalse(probe.isWorldPushRequested());
        assertTrue(probe.shouldSetMovementBlock());
    }

    @Test public void foregroundTypeSevenRequiresHighNibbleExceptionToPermitPush() {
        assertFalse(evaluate(false,0,false,true,true,true,7,0,0,0,false).isPushAccepted());
        assertTrue(evaluate(false,0,false,true,true,true,7,0x10,0,0,false).isPushAccepted());
    }

    @Test public void adjacentPlayerBlockersRequireForegroundLowByte23() {
        for(int player:new int[]{19,45,49,43}) {
            assertFalse(evaluate(false,0,false,true,true,true,0,0,player,0,false).isPushAccepted());
            assertTrue(evaluate(false,0,false,true,true,true,0,0,player,0x23,false).isPushAccepted());
        }
    }

    @Test public void failedPushClearsLocalDirectionAndPlayerMetadataLowBits() {
        LegacyTwoCellMovementEvaluator.Decision d =
                evaluate(false,0,false,true,true,false,0,0,0,0,true);
        assertFalse(d.isPushAccepted());
        assertTrue(d.getReturnFlag());
        assertTrue(d.shouldSetMovementBlock());
        assertTrue(d.shouldClearLocalDirection());
        assertTrue(d.shouldClearPlayerMetadataLowBits());
        assertFalse(d.isWorldPushRequested());
    }

    @Test public void zeroDirectionAndCounterGatePreventPush() {
        assertFalse(evaluate(false,0,false,false,true,true,0,0,0,0,false).isPushAccepted());
        assertFalse(evaluate(false,1,false,true,true,true,0,0,0,0,false).isPushAccepted());
        assertTrue(evaluate(false,1,true,true,true,true,0,0,0,0,false).isPushAccepted());
    }
}
