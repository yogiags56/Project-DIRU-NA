package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyForegroundType2EvaluatorTest {
    @Test public void highZeroWithOneInventoryItemSelectsSevenAndZero() {
        LegacyForegroundType2Evaluator.Decision d =
                LegacyForegroundType2Evaluator.evaluate(0x000002, false, 1, 99, 99);
        assertTrue(d.shouldSetAuxiliaryBranchFlag());
        assertTrue(d.shouldUpdateADInt());
        assertEquals(7,d.getNextADInt());
        assertTrue(d.shouldUpdateAEInt());
        assertEquals(0,d.getNextAEInt());
    }
    @Test public void highZeroWithoutItemSelectsThirteenAndOne() {
        LegacyForegroundType2Evaluator.Decision d =
                LegacyForegroundType2Evaluator.evaluate(0x000002, false, 0, 99, 99);
        assertEquals(13,d.getNextADInt());
        assertEquals(1,d.getNextAEInt());
    }
    @Test public void highOneRequiresTwoInventoryItems() {
        LegacyForegroundType2Evaluator.Decision enough =
                LegacyForegroundType2Evaluator.evaluate(0x000102, false, 2, 99, 99);
        assertEquals(7,enough.getNextADInt());
        assertEquals(1,enough.getNextAEInt());
        LegacyForegroundType2Evaluator.Decision insufficient =
                LegacyForegroundType2Evaluator.evaluate(0x000102, false, 1, 99, 99);
        assertEquals(13,insufficient.getNextADInt());
        assertEquals(1,insufficient.getNextAEInt());
    }
    @Test public void signedHighByteDefaultPreservesExistingCounters() {
        LegacyForegroundType2Evaluator.Decision d =
                LegacyForegroundType2Evaluator.evaluate(0xFFFFFE02, false, 99, 17, 23);
        assertTrue(d.shouldSetAuxiliaryBranchFlag());
        assertFalse(d.shouldUpdateADInt());
        assertEquals(17,d.getNextADInt());
        assertFalse(d.shouldUpdateAEInt());
        assertEquals(23,d.getNextAEInt());
    }
    @Test public void probeSuppressesAllTypeTwoEffectsButStillSkipsPlayerLayer() {
        LegacyForegroundType2Evaluator.Decision d =
                LegacyForegroundType2Evaluator.evaluate(0x000002, true, 99, 17, 23);
        assertFalse(d.shouldSetAuxiliaryBranchFlag());
        assertFalse(d.shouldUpdateADInt());
        assertFalse(d.shouldUpdateAEInt());
        assertEquals(LegacyForegroundType2Evaluator.PlayerLayerFlow.SKIP_PLAYER_LAYER,
                d.getPlayerLayerFlow());
    }
    @Test public void unrelatedForegroundIsNotHandled() {
        assertFalse(LegacyForegroundType2Evaluator.evaluate(1,false,0,0,0).isHandled());
    }
}
