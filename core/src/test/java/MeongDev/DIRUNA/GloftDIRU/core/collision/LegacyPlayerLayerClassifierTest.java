package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPlayerLayerClassifierTest {
    @Test public void classifiesEmptyAndProbeDependentValues() {
        assertEquals(LegacyPlayerLayerClassifier.Kind.EMPTY_CELL,
                LegacyPlayerLayerClassifier.classify(-1));
        assertEquals(LegacyPlayerLayerClassifier.Kind.PROBE_DEPENDENT_34_35,
                LegacyPlayerLayerClassifier.classify(34));
        assertEquals(LegacyPlayerLayerClassifier.Kind.PROBE_DEPENDENT_34_35,
                LegacyPlayerLayerClassifier.classify(35));
    }
    @Test public void classifiesNonBlockingAndExplicitBlockingSets() {
        assertEquals(LegacyPlayerLayerClassifier.Kind.NON_BLOCKING_31_49,
                LegacyPlayerLayerClassifier.classify(31));
        assertEquals(LegacyPlayerLayerClassifier.Kind.NON_BLOCKING_31_49,
                LegacyPlayerLayerClassifier.classify(49));
        int[] blocking={1,2,4,5,6,7,11,14,19,24,26,27,33,40,41,42,43,45,50,51,52,53};
        for(int value:blocking) assertEquals("layer "+value,
                LegacyPlayerLayerClassifier.Kind.BLOCKING_EXPLICIT_SET,
                LegacyPlayerLayerClassifier.classify(value));
    }
    @Test public void preservesDedicatedSpecialBranches() {
        assertEquals(LegacyPlayerLayerClassifier.Kind.TYPE_10_MODE_DEPENDENT,
                LegacyPlayerLayerClassifier.classify(10));
        for(int value:new int[]{0,8,9,47}) assertEquals(
                LegacyPlayerLayerClassifier.Kind.TWO_STEP_SET_0_8_9_47,
                LegacyPlayerLayerClassifier.classify(value));
        assertEquals(LegacyPlayerLayerClassifier.Kind.SPECIAL_OBJECT_48,
                LegacyPlayerLayerClassifier.classify(48));
        assertEquals(LegacyPlayerLayerClassifier.Kind.TYPE_28_SPECIAL,
                LegacyPlayerLayerClassifier.classify(28));
    }
    @Test public void unknownValuesRemainExplicitlyUnknown() {
        assertEquals(LegacyPlayerLayerClassifier.Kind.OTHER_DEFAULT,
                LegacyPlayerLayerClassifier.classify(127));
        assertEquals(LegacyPlayerLayerClassifier.Kind.OTHER_DEFAULT,
                LegacyPlayerLayerClassifier.classify(-2));
    }
}
