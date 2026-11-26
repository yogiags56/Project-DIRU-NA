package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundModelParityTest {
    @Test public void typeThreeFlowAndFlagAgreeAcrossSignedHighByteBoundaries() {
        int[] highBytes = {
                Integer.MIN_VALUE, -65536, -257, -1, 0, 1, 2, 3, 4, 127, 255
        };
        for (int high : highBytes) {
            int raw = (high << 8) | 3;
            LegacyForegroundBranchEvaluator.Decision branch =
                    LegacyForegroundBranchEvaluator.evaluate(raw);
            LegacyForegroundPrecedence.Decision precedence =
                    LegacyForegroundPrecedence.decide(raw, false);

            LegacyForegroundPrecedence.PlayerLayerFlow expectedFlow =
                    branch.getPlayerLayerFlow()
                            == LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                            ? LegacyForegroundPrecedence.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                            : LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER;
            assertEquals("raw=" + raw, expectedFlow, precedence.getPlayerLayerFlow());
            assertEquals("raw=" + raw,
                    branch.getCollisionFlagEffect()
                            == LegacyForegroundBranchEvaluator.CollisionFlagEffect.SET_TRUE,
                    precedence.foregroundSetsCollisionTrue());
            assertEquals("raw=" + raw,
                    branch.getCollisionFlagEffect()
                            == LegacyForegroundBranchEvaluator.CollisionFlagEffect.SET_FALSE,
                    precedence.foregroundSetsCollisionFalse());
        }
    }

    @Test public void typeSevenFlowAgreesAcrossPackedNibbleBoundaries() {
        int[] highBytes = {0x00, 0x10, 0x1F, 0x20, 0x2F, 0x70, 0xF0};
        for (int high : highBytes) {
            int raw = (high << 8) | 7;
            LegacyForegroundBranchEvaluator.Decision branch =
                    LegacyForegroundBranchEvaluator.evaluate(raw);
            LegacyForegroundPrecedence.Decision precedence =
                    LegacyForegroundPrecedence.decide(raw, true);
            LegacyForegroundPrecedence.PlayerLayerFlow expectedFlow =
                    branch.getPlayerLayerFlow()
                            == LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                            ? LegacyForegroundPrecedence.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                            : LegacyForegroundPrecedence.PlayerLayerFlow.SKIP_PLAYER_LAYER;
            assertEquals("raw=0x" + Integer.toHexString(raw),
                    expectedFlow, precedence.getPlayerLayerFlow());
        }
    }
}
