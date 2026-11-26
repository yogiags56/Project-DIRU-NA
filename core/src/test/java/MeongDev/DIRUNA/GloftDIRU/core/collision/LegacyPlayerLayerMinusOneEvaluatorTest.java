package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPlayerLayerMinusOneEvaluatorTest {
    private static LegacyPlayerLayerMinusOneSnapshot sample(
            int x,int y,int dx,int dy,int nearLayer,int nearMeta,
            int aboveLayer,int aboveMeta,int belowLayer,int belowMeta,
            int a,int b,boolean probe) {
        return new LegacyPlayerLayerMinusOneSnapshot(
                x,y,dx,dy,20,20,nearLayer,nearMeta,
                aboveLayer,aboveMeta,belowLayer,belowMeta,a,b,probe);
    }

    @Test public void everyStageEdgeBlocksAndClearsMovementBlockBit() {
        LegacyPlayerLayerMinusOneSnapshot[] edges={
            sample(0,5,1,0,0,0,0,0,0,0,0,0,false),
            sample(5,0,0,1,0,0,0,0,0,0,0,0,false),
            sample(18,5,1,0,0,0,0,0,0,0,0,0,false),
            sample(5,18,0,1,0,0,0,0,0,0,0,0,0,false)
        };
        for(LegacyPlayerLayerMinusOneSnapshot s:edges){
            LegacyPlayerLayerMinusOneEvaluator.Decision d=
                    LegacyPlayerLayerMinusOneEvaluator.evaluate(s,false);
            assertTrue(d.getReturnFlag());
            assertTrue(d.isEdgeBlocked());
            assertTrue(d.shouldClearMovementBlock());
            assertFalse(d.shouldClearLocalDirection());
        }
    }

    @Test public void horizontalNearBelowEmptyCellWithDirectionThreeAllowsDoorException() {
        LegacyPlayerLayerMinusOneSnapshot s=sample(5,5,1,0,0,3,0,0,0,0,0,0,false);
        LegacyPlayerLayerMinusOneEvaluator.Decision d=
                LegacyPlayerLayerMinusOneEvaluator.evaluate(s,false);
        assertFalse(d.getReturnFlag());
        assertTrue(d.isDoorException());
        assertTrue(d.shouldClearLocalDirection());
        assertTrue(d.shouldClearMovementBlock());
    }

    @Test public void nearBelowExceptionRequiresHorizontalMovementEmptyLayerAndExactLowBits() {
        assertFalse(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,0,1,0,3,0,0,0,0,0,0,false),false).isDoorException());
        assertFalse(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,1,0,1,3,0,0,0,0,0,0,false),false).isDoorException());
        assertFalse(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,1,0,0,2,0,0,0,0,0,0,false),false).isDoorException());
    }

    @Test public void doorCounterARequiresDoor28WithBitEightClear() {
        assertTrue(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,0,1,0,0,28,0,0,0,24,0,false),false).isDoorException());
        assertFalse(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,0,1,0,0,28,8,0,0,24,0,false),false).isDoorException());
        assertFalse(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,0,1,0,0,28,0,0,0,23,0,false),false).isDoorException());
    }

    @Test public void doorCounterBNeedsDoor28ButDoesNotCheckBitEight() {
        assertTrue(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,0,1,0,0,0,8,28,8,0,24,false),false).isDoorException());
        assertFalse(LegacyPlayerLayerMinusOneEvaluator.evaluate(
                sample(5,5,0,1,0,0,0,0,0,0,0,24,false),false).isDoorException());
    }

    @Test public void probeSuppressesEveryDoorException() {
        LegacyPlayerLayerMinusOneSnapshot s=sample(5,5,1,0,0,3,28,0,0,0,24,24,true);
        LegacyPlayerLayerMinusOneEvaluator.Decision d=
                LegacyPlayerLayerMinusOneEvaluator.evaluate(s,false);
        assertTrue(d.getReturnFlag());
        assertFalse(d.isDoorException());
        assertFalse(d.shouldClearLocalDirection());
        assertTrue(d.shouldClearMovementBlock());
    }
}
