package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundType156EvaluatorTest {
    @Test public void typeOneAlwaysSkipsPlayerLayerButEffectsAreProbeGated() {
        LegacyForegroundType156Evaluator.Decision normal =
                LegacyForegroundType156Evaluator.evaluate(1,false);
        assertEquals(LegacyForegroundType156Evaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                normal.getPlayerLayerFlow());
        assertTrue(normal.shouldResetTimer120());
        assertTrue(normal.shouldIncrementCounter());
        assertTrue(normal.shouldInvokeType1Clear());
        LegacyForegroundType156Evaluator.Decision probe =
                LegacyForegroundType156Evaluator.evaluate(1,true);
        assertEquals(normal.getPlayerLayerFlow(),probe.getPlayerLayerFlow());
        assertFalse(probe.shouldResetTimer120());
        assertFalse(probe.shouldIncrementCounter());
        assertFalse(probe.shouldInvokeType1Clear());
    }
    @Test public void typeFiveAlwaysSetsCollisionAndOnlyNormalModeChangesState() {
        LegacyForegroundType156Evaluator.Decision normal =
                LegacyForegroundType156Evaluator.evaluate(5,false);
        assertEquals(LegacyForegroundType156Evaluator.CollisionEffect.SET_TRUE,
                normal.getCollisionEffect());
        assertTrue(normal.shouldSetLocalXBoolean());
        assertTrue(normal.shouldSetAtBoolean());
        LegacyForegroundType156Evaluator.Decision probe =
                LegacyForegroundType156Evaluator.evaluate(5,true);
        assertEquals(LegacyForegroundType156Evaluator.CollisionEffect.SET_TRUE,
                probe.getCollisionEffect());
        assertFalse(probe.shouldSetLocalXBoolean());
        assertFalse(probe.shouldSetAtBoolean());
    }
    @Test public void typeSixSkipsPlayerLayerWithoutChangingCollisionOrState() {
        LegacyForegroundType156Evaluator.Decision d =
                LegacyForegroundType156Evaluator.evaluate(6,false);
        assertEquals(LegacyForegroundType156Evaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                d.getPlayerLayerFlow());
        assertEquals(LegacyForegroundType156Evaluator.CollisionEffect.UNCHANGED,
                d.getCollisionEffect());
        assertFalse(d.shouldResetTimer120());
        assertFalse(d.shouldSetAtBoolean());
    }
    @Test public void unrelatedTypesRemainNotHandled() {
        for(int type:new int[]{0,2,3,4,7,19,28,255})
            assertFalse(LegacyForegroundType156Evaluator.evaluate(type,false).isHandled());
    }
}
