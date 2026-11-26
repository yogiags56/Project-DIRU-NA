package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundFlowContractTest {
    private static void assertEvaluatesPlayerLayer(
            LegacyForegroundBranchEvaluator.PlayerLayerFlow flow) {
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER, flow);
    }

    private static void assertSkipsPlayerLayer(
            LegacyForegroundBranchEvaluator.PlayerLayerFlow flow) {
        assertEquals(LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER, flow);
    }

    @Test public void labeledExitForegroundBranchesFallThroughToPlayerLayer() {
        assertEvaluatesPlayerLayer(LegacyForegroundBranchEvaluator.evaluate(0x0003).getPlayerLayerFlow());
        assertEvaluatesPlayerLayer(LegacyForegroundType156Evaluator.evaluate(0x0001, false)
                .getPlayerLayerFlow() == LegacyForegroundType156Evaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                ? LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                : LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER);
        assertEvaluatesPlayerLayer(LegacyForegroundType156Evaluator.evaluate(0x0005, false)
                .getPlayerLayerFlow() == LegacyForegroundType156Evaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                ? LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                : LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER);
        assertEvaluatesPlayerLayer(LegacyForegroundType156Evaluator.evaluate(0x0006, true)
                .getPlayerLayerFlow() == LegacyForegroundType156Evaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                ? LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                : LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER);
        assertEvaluatesPlayerLayer(LegacyForegroundType2Evaluator.evaluate(0x0002, true, 0, 0, 0)
                .getPlayerLayerFlow() == LegacyForegroundType2Evaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                ? LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                : LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER);
        assertEvaluatesPlayerLayer(LegacyForegroundType428Evaluator.evaluate(0x0004, false, 0, 0)
                .getPlayerLayerFlow() == LegacyForegroundType428Evaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                ? LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                : LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER);
        assertEvaluatesPlayerLayer(LegacyForegroundType428Evaluator.evaluate(0x001C, false, 0, 0)
                .getPlayerLayerFlow() == LegacyForegroundType428Evaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                ? LegacyForegroundBranchEvaluator.PlayerLayerFlow.EVALUATE_PLAYER_LAYER
                : LegacyForegroundBranchEvaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER);
    }

    @Test public void explicitN10OneBranchesSkipPlayerLayer() {
        assertSkipsPlayerLayer(LegacyForegroundBranchEvaluator.evaluate(0x2007).getPlayerLayerFlow());
        assertSkipsPlayerLayer(LegacyForegroundBranchEvaluator.evaluate(0x0303).getPlayerLayerFlow());
    }
}
