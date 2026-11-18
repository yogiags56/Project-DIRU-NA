package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyEmptyCellPredicateTest {
    @Test public void acceptsEmptyPlayerLayerWithOrdinaryForeground() {
        assertTrue(LegacyEmptyCellPredicate.isLegacyEmpty(-1, 0));
        assertTrue(LegacyEmptyCellPredicate.isLegacyEmpty(-1, 0x1201));
        assertTrue(LegacyEmptyCellPredicate.isLegacyEmpty(-1, 255));
    }

    @Test public void rejectsEachExcludedForegroundType() {
        int[] excluded = {14, 33, 5, 28};
        for (int foreground : excluded) {
            assertFalse("foreground type " + foreground,
                    LegacyEmptyCellPredicate.isLegacyEmpty(-1, foreground));
            assertFalse("high bits must not change low-byte type " + foreground,
                    LegacyEmptyCellPredicate.isLegacyEmpty(-1, foreground | 0x1200));
        }
    }

    @Test public void rejectsNonEmptyPlayerLayerRegardlessOfForeground() {
        int[] layers = {0, 1, 14, 28, 39, 48, 127};
        for (int layer : layers) {
            assertFalse("player layer " + layer,
                    LegacyEmptyCellPredicate.isLegacyEmpty(layer, 0));
            assertFalse("player layer " + layer,
                    LegacyEmptyCellPredicate.isLegacyEmpty(layer, 14));
        }
    }

    @Test public void treatsForegroundAsUnsignedLowByte() {
        assertTrue(LegacyEmptyCellPredicate.isLegacyEmpty(-1, 0xFFFFFF00));
        assertFalse(LegacyEmptyCellPredicate.isLegacyEmpty(-1, 0xFFFFFF1C));
    }
}
