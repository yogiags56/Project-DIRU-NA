package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionProbeSemanticsTest {
    @Test public void type19ScriptRunsOnlyOutsideProbeMode() {
        assertTrue(LegacyCollisionProbeSemantics.shouldRunType19Script(false));
        assertFalse(LegacyCollisionProbeSemantics.shouldRunType19Script(true));
    }

    @Test public void layer34And35CollisionValueIsTheProbeFlag() {
        assertFalse(LegacyCollisionProbeSemantics.layer34Or35CollisionValue(false));
        assertTrue(LegacyCollisionProbeSemantics.layer34Or35CollisionValue(true));
    }

    @Test public void type1AndCheckpointEffectsAreSuppressedDuringProbe() {
        assertTrue(LegacyCollisionProbeSemantics.shouldApplyType1ForegroundEffects(false));
        assertFalse(LegacyCollisionProbeSemantics.shouldApplyType1ForegroundEffects(true));
        assertTrue(LegacyCollisionProbeSemantics.shouldApplyCheckpointEffects(false));
        assertFalse(LegacyCollisionProbeSemantics.shouldApplyCheckpointEffects(true));
    }

    @Test public void type10MarkerRequiresAllThreeConditions() {
        assertTrue(LegacyCollisionProbeSemantics.shouldInitializeType10Marker(false,3,0));
        assertFalse(LegacyCollisionProbeSemantics.shouldInitializeType10Marker(true,3,0));
        assertFalse(LegacyCollisionProbeSemantics.shouldInitializeType10Marker(false,2,0));
        assertFalse(LegacyCollisionProbeSemantics.shouldInitializeType10Marker(false,3,1));
    }

    @Test public void type28And5EffectsAreSuppressedDuringProbe() {
        assertTrue(LegacyCollisionProbeSemantics.shouldApplyType28Or5Effects(false));
        assertFalse(LegacyCollisionProbeSemantics.shouldApplyType28Or5Effects(true));
    }
}
