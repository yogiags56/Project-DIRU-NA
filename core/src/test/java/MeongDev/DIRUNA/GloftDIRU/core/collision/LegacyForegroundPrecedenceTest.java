package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundPrecedenceTest {
    @Test public void foregroundSevenUsesPackedNibbleThreshold() {
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                LegacyForegroundPrecedence.decide(0x0017,false).getPlayerLayerFlow());
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                LegacyForegroundPrecedence.decide(0x0027,false).getPlayerLayerFlow());
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                LegacyForegroundPrecedence.decide(0x01F7,true).getPlayerLayerFlow());
    }

    @Test public void foregroundThreeAlwaysSkipsPlayerLayerWithDifferentCollisionFlag() {
        LegacyForegroundPrecedence.Decision low =
                LegacyForegroundPrecedence.decide(0x0003,false);
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                low.getPlayerLayerFlow());
        assertTrue(low.foregroundSetsCollisionTrue());
        assertFalse(low.foregroundSetsCollisionFalse());

        LegacyForegroundPrecedence.Decision high =
                LegacyForegroundPrecedence.decide(0x0303,true);
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                high.getPlayerLayerFlow());
        assertFalse(high.foregroundSetsCollisionTrue());
        assertTrue(high.foregroundSetsCollisionFalse());
    }

    @Test public void ordinaryAndSideEffectForegroundBranchesContinueToPlayerLayer() {
        int[] types={0,1,2,4,5,6,19,28,255};
        for(int type:types) {
            for(boolean probe:new boolean[]{false,true}) {
                assertEquals("foreground="+type+", probe="+probe,
                        LegacyForegroundPrecedence.PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                        LegacyForegroundPrecedence.decide(type,probe).getPlayerLayerFlow());
            }
        }
    }

    @Test public void typeSevenThresholdUsesBitsFourThroughSevenNotWholeHighByte() {
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                LegacyForegroundPrecedence.decide(0x00F7,false).getPlayerLayerFlow());
        assertEquals(LegacyForegroundPrecedence.PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                LegacyForegroundPrecedence.decide(0x0107,false).getPlayerLayerFlow());
    }
}
