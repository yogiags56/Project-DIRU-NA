package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundBranchEvaluatorTest {
    @Test public void typeSevenUsesOnlyMetadataNibbleFourThroughSeven() {
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                LegacyForegroundBranchEvaluator.evaluate(0x0007).getPlayerLayerFlow());
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                LegacyForegroundBranchEvaluator.evaluate(0x1007).getPlayerLayerFlow());
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                LegacyForegroundBranchEvaluator.evaluate(0x2007).getPlayerLayerFlow());
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER,
                LegacyForegroundBranchEvaluator.evaluate(0xF007).getPlayerLayerFlow());
    }

    @Test public void typeSevenDoesNotModifyLocalCollisionFlag() {
        assertEquals(LegacyForegroundBranchEvaluator.CollisionFlagEffect.UNCHANGED,
                LegacyForegroundBranchEvaluator.evaluate(0x0007).getCollisionFlagEffect());
        assertEquals(LegacyForegroundBranchEvaluator.CollisionFlagEffect.UNCHANGED,
                LegacyForegroundBranchEvaluator.evaluate(0x2007).getCollisionFlagEffect());
    }

    @Test public void typeThreeUsesSignedHighByteThresholdAndAlwaysSkipsPlayerLayer() {
        LegacyForegroundBranchEvaluator.Decision below =
                LegacyForegroundBranchEvaluator.evaluate(0x0003);
        assertEquals(LegacyForegroundBranchEvaluator.CollisionFlagEffect.SET_TRUE,
                below.getCollisionFlagEffect());
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                below.getPlayerLayerFlow());

        LegacyForegroundBranchEvaluator.Decision at =
                LegacyForegroundBranchEvaluator.evaluate(0x0303);
        assertEquals(LegacyForegroundBranchEvaluator.CollisionFlagEffect.SET_FALSE,
                at.getCollisionFlagEffect());
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                at.getPlayerLayerFlow());

        LegacyForegroundBranchEvaluator.Decision negative =
                LegacyForegroundBranchEvaluator.evaluate(0xFFFFFF03);
        assertEquals(LegacyForegroundBranchEvaluator.CollisionFlagEffect.SET_TRUE,
                negative.getCollisionFlagEffect());
    }

    @Test public void unrelatedForegroundTypesAreNotClaimedByThisEvaluator() {
        for(int type:new int[]{0,1,2,4,5,6,19,28,255}) {
            assertFalse(LegacyForegroundBranchEvaluator.evaluate(type).isHandled());
        }
    }
}
