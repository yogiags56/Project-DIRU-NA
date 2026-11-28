package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure, source-derived player-layer classification used by step-9 Pass B. */
public final class LegacyPlayerLayerClassifierTest {
    @Test public void mapsOnlyTheObservedPlayerCodes() {
        assertEquals(34,LegacyPlayerLayerClassifier.classify(1));
        assertEquals(35,LegacyPlayerLayerClassifier.classify(45));
        assertEquals(37,LegacyPlayerLayerClassifier.classify(19));
        assertEquals(37,LegacyPlayerLayerClassifier.classify(43));
        assertEquals(36,LegacyPlayerLayerClassifier.classify(46));
        assertEquals(39,LegacyPlayerLayerClassifier.classify(49));
    }

    @Test public void rejectsNegativeAndUnmappedCodes() {
        assertEquals(-1,LegacyPlayerLayerClassifier.classify(-1));
        assertEquals(-1,LegacyPlayerLayerClassifier.classify(0));
        assertEquals(-1,LegacyPlayerLayerClassifier.classify(48));
        assertEquals(-1,LegacyPlayerLayerClassifier.classify(79));
        assertEquals(-1,LegacyPlayerLayerClassifier.classify(255));
    }
}
