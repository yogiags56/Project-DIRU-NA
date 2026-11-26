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
}
