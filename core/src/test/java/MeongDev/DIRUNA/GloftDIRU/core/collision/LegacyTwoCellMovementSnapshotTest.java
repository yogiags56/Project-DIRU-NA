package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyTwoCellMovementSnapshotTest {
    private static LegacyTwoCellMovementSnapshot sample(
            int x,int y,int dx,int dy,int k,int d,int e) {
        return new LegacyTwoCellMovementSnapshot(x,y,dx,dy,
                8,0x1007,19,0x23,3,2,k,d,e,
                false,true,false);
    }

    @Test public void targetAndAdjacentCoordinatesMatchDistinctLegacyExpressions() {
        LegacyTwoCellMovementSnapshot right=sample(10,20,1,0,0,12,18);
        assertEquals(12,right.getTargetX());
        assertEquals(20,right.getTargetY());
        assertEquals(11,right.getAdjacentPlayerX());
        assertEquals(21,right.getAdjacentPlayerY());

        LegacyTwoCellMovementSnapshot left=sample(10,20,-1,0,0,8,18);
        assertEquals(8,left.getTargetX());
        assertEquals(20,left.getTargetY());
        assertEquals(9,left.getAdjacentPlayerX());
        assertEquals(21,left.getAdjacentPlayerY());
    }

    @Test public void verticalMovementKeepsSourceAdjacentColumnAndOffsetsTargetTwice() {
        LegacyTwoCellMovementSnapshot up=sample(10,20,0,-1,0,10,18);
        assertEquals(10,up.getTargetX());
        assertEquals(18,up.getTargetY());
        assertEquals(10,up.getAdjacentPlayerX());
        assertEquals(21,up.getAdjacentPlayerY());
    }

    @Test public void earlyReturnRequiresExactKByteAndBothCoordinatePredicates() {
        assertTrue(sample(10,20,1,0,4,12,20).isEarlyReturnConditionMet());
        assertFalse(sample(10,20,1,0,3,12,20).isEarlyReturnConditionMet());
        assertFalse(sample(10,20,1,0,4,13,20).isEarlyReturnConditionMet());
        assertFalse(sample(10,20,1,0,4,12,21).isEarlyReturnConditionMet());
    }
    @Test public void snapshotOverloadFeedsTheExactDecodedSamplesToDecision() {
        LegacyTwoCellMovementSnapshot accepted = new LegacyTwoCellMovementSnapshot(
                10,20,1,0,8,0x1007,19,0x23,0,0,0,0,0,
                false,true,false);
        LegacyTwoCellMovementEvaluator.Decision push =
                LegacyTwoCellMovementEvaluator.evaluate(accepted);
        assertTrue(push.isPushAccepted());
        assertTrue(push.isWorldPushRequested());

        LegacyTwoCellMovementSnapshot blocked = new LegacyTwoCellMovementSnapshot(
                10,20,1,0,8,0x0007,19,0x0000,0,0,0,0,0,
                false,true,false);
        assertFalse(LegacyTwoCellMovementEvaluator.evaluate(blocked).isPushAccepted());
    }

    @Test public void snapshotExposesSourceHorizontalPushWriteCoordinates() {
        LegacyTwoCellMovementSnapshot right=sample(10,20,1,0,0,12,18);
        assertEquals(12,right.getPushDestinationX());
        assertEquals(20,right.getPushDestinationY());
        assertEquals(11,right.getPlayerCurrentCellX());
        assertEquals(20,right.getPlayerCurrentCellY());
    }

    @Test public void snapshotRebuildsDirectionBitsBeforeAnimationSelection() {
        LegacyTwoCellMovementSnapshot right=new LegacyTwoCellMovementSnapshot(
                10,20,1,0,8,0,0,0,0,0,4,12,20,false,true,false);
        LegacyTwoCellMovementSnapshot left=new LegacyTwoCellMovementSnapshot(
                10,20,-1,0,8,0,0,0,0,0,4,8,20,false,true,false);
        LegacyTwoCellMovementSnapshot up=new LegacyTwoCellMovementSnapshot(
                10,20,0,-1,8,0,0,0,0,0,4,10,18,false,true,false);
        LegacyTwoCellMovementSnapshot down=new LegacyTwoCellMovementSnapshot(
                10,20,0,1,8,0,0,0,0,0,4,10,22,false,true,false);
        assertEquals(6,right.getResolvedMovementStateBits());
        assertEquals(4,left.getResolvedMovementStateBits());
        assertEquals(1,up.getResolvedMovementStateBits());
        assertEquals(3,down.getResolvedMovementStateBits());
        assertEquals(8,LegacyTwoCellMovementEvaluator.evaluate(right).getEarlyReturnAnimation());
        assertEquals(9,LegacyTwoCellMovementEvaluator.evaluate(left).getEarlyReturnAnimation());
    }

}
