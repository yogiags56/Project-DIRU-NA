package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundType428EvaluatorTest {
    @Test public void checkpointAtOrAboveProgressRequestsSaveAndUpdatesValues() {
        LegacyForegroundType428Evaluator.Decision equal =
                LegacyForegroundType428Evaluator.evaluate(0x000004, false, 4, 100);
        assertTrue(equal.shouldRequestSaveCheckpoint());
        assertTrue(equal.shouldUpdateCheckpointProgress());
        assertEquals(5,equal.getNextCheckpointProgress());
        assertTrue(equal.shouldUpdateCheckpointTimer());
        assertEquals(113,equal.getNextCheckpointTimer());
        assertEquals(LegacyForegroundType428Evaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                equal.getPlayerLayerFlow());
    }

    @Test public void checkpointBelowProgressDoesNotRequestAnyWrite() {
        LegacyForegroundType428Evaluator.Decision d =
                LegacyForegroundType428Evaluator.evaluate(0x000304, false, 4, 100);
        assertFalse(d.shouldRequestSaveCheckpoint());
        assertFalse(d.shouldUpdateCheckpointProgress());
        assertEquals(4,d.getNextCheckpointProgress());
        assertFalse(d.shouldUpdateCheckpointTimer());
    }

    @Test public void checkpointProbeSuppressesThresholdEffects() {
        LegacyForegroundType428Evaluator.Decision d =
                LegacyForegroundType428Evaluator.evaluate(0x000A04, true, 4, 100);
        assertFalse(d.shouldRequestSaveCheckpoint());
        assertFalse(d.shouldUpdateCheckpointProgress());
        assertFalse(d.shouldUpdateCheckpointTimer());
    }

    @Test public void checkpointUsesSignedPackedHighTwentyFourBits() {
        LegacyForegroundType428Evaluator.Decision d =
                LegacyForegroundType428Evaluator.evaluate(0xFFFFFF04, false, -2, 7);
        assertTrue(d.shouldRequestSaveCheckpoint());
        assertEquals(0,d.getNextCheckpointProgress());
        assertEquals(20,d.getNextCheckpointTimer());
    }

    @Test public void typeTwentyEightSetsLocalFlagOnlyOutsideProbeMode() {
        LegacyForegroundType428Evaluator.Decision normal =
                LegacyForegroundType428Evaluator.evaluate(28,false,3,9);
        assertEquals(LegacyForegroundType428Evaluator.LocalXBooleanEffect.SET_TRUE,
                normal.getLocalXBooleanEffect());
        assertEquals(LegacyForegroundType428Evaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                normal.getPlayerLayerFlow());
        LegacyForegroundType428Evaluator.Decision probe =
                LegacyForegroundType428Evaluator.evaluate(28,true,3,9);
        assertEquals(LegacyForegroundType428Evaluator.LocalXBooleanEffect.UNCHANGED,
                probe.getLocalXBooleanEffect());
    }

    @Test public void unrelatedTypesRemainNotHandled() {
        for(int type:new int[]{0,1,2,3,5,6,7,19,255})
            assertFalse(LegacyForegroundType428Evaluator.evaluate(type,false,0,0).isHandled());
    }
}
