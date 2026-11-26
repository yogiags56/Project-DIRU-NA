package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPlayerLayerEvaluatorTest {
    @Test public void probeDependentValuesReturnProbeMode() {
        for(int value:new int[]{34,35}) {
            assertEquals(false, LegacyPlayerLayerEvaluator.evaluate(
                    value,false,0,0,false).getReturnFlag());
            assertEquals(true, LegacyPlayerLayerEvaluator.evaluate(
                    value,true,0,0,false).getReturnFlag());
        }
    }
    @Test public void explicitBlockingAndNonBlockingSetsAreExact() {
        for(int value:new int[]{1,2,4,5,6,7,11,14,19,24,26,27,33,40,41,42,43,45,50,51,52,53})
            assertTrue("blocking "+value, LegacyPlayerLayerEvaluator.evaluate(
                    value,false,0,0,false).getReturnFlag());
        for(int value:new int[]{31,49})
            assertFalse("nonblocking "+value, LegacyPlayerLayerEvaluator.evaluate(
                    value,false,0,0,true).getReturnFlag());
    }
    @Test public void typeTenIsModeDependentAndProbeSuppressesMetadataIntent() {
        LegacyPlayerLayerEvaluator.Decision active = LegacyPlayerLayerEvaluator.evaluate(
                10,false,3,0,false);
        assertTrue(active.getReturnFlag());
        assertEquals(LegacyPlayerLayerEvaluator.Effect.REQUEST_PLAYER_METADATA_ONE,
                active.getEffect());

        LegacyPlayerLayerEvaluator.Decision probe = LegacyPlayerLayerEvaluator.evaluate(
                10,true,3,0,false);
        assertTrue(probe.getReturnFlag());
        assertEquals(LegacyPlayerLayerEvaluator.Effect.NONE,probe.getEffect());

        LegacyPlayerLayerEvaluator.Decision inactive = LegacyPlayerLayerEvaluator.evaluate(
                10,false,2,0,false);
        assertFalse(inactive.getReturnFlag());
        assertEquals(LegacyPlayerLayerEvaluator.Effect.NONE,inactive.getEffect());
    }
    @Test public void typeTwentyEightRequestsDirectionClearWithoutInventingReturnValue() {
        LegacyPlayerLayerEvaluator.Decision result = LegacyPlayerLayerEvaluator.evaluate(
                28,false,0,0,true);
        assertTrue(result.getReturnFlag());
        assertEquals(LegacyPlayerLayerEvaluator.Effect.CLEAR_ACTIVE_DIRECTION,
                result.getEffect());
    }
    @Test public void complexAndUnknownBranchesRemainNotHandled() {
        for(int value:new int[]{-1,0,8,9,47,48,127,-2}) {
            LegacyPlayerLayerEvaluator.Decision result = LegacyPlayerLayerEvaluator.evaluate(
                    value,false,0,0,false);
            assertFalse("value "+value,result.isHandled());
            assertFalse(result.getReturnFlag());
        }
    }
}
