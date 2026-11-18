package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundClassifierTest {
    @Test public void classifiesEveryExplicitForegroundCase() {
        int[] raw={19,1,7,4,28,5,2,6,3};
        LegacyForegroundClassifier.Kind[] expected={
            LegacyForegroundClassifier.Kind.SCRIPTED_TYPE_19,
            LegacyForegroundClassifier.Kind.TYPE_1,
            LegacyForegroundClassifier.Kind.TYPE_7,
            LegacyForegroundClassifier.Kind.CHECKPOINT_TYPE_4,
            LegacyForegroundClassifier.Kind.TYPE_28,
            LegacyForegroundClassifier.Kind.TYPE_5,
            LegacyForegroundClassifier.Kind.TYPE_2,
            LegacyForegroundClassifier.Kind.TYPE_6,
            LegacyForegroundClassifier.Kind.TYPE_3
        };
        for(int i=0;i<raw.length;i++) assertEquals(expected[i],
                LegacyForegroundClassifier.classify(raw[i]));
    }
    @Test public void masksHighBitsBeforeClassification() {
        assertEquals(LegacyForegroundClassifier.Kind.SCRIPTED_TYPE_19,
                LegacyForegroundClassifier.classify(0xAB13));
        assertEquals(LegacyForegroundClassifier.Kind.OTHER,
                LegacyForegroundClassifier.classify(0xAB0E));
    }
    @Test public void exposesUnsignedForegroundType() {
        assertEquals(19,LegacyForegroundClassifier.unsignedType(0xFFFFFF13));
        assertEquals(255,LegacyForegroundClassifier.unsignedType(-1));
    }
}
