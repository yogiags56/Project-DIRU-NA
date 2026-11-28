package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPlayerLayer48EvaluatorTest {
    private static LegacyPlayerLayer48Snapshot sample(
            int metadata,int counter,boolean probe,int a,int b,int guard) {
        return new LegacyPlayerLayer48Snapshot(
                10,20,1,0,counter,metadata,48,
                a,b,guard,0x12345678,0xABCDEF01,probe);
    }

    @Test public void metadataBitEightSelectsTheTwoVerticalOffsets() {
        LegacyPlayerLayer48Snapshot normal=sample(0,0,false,-1,-1,1);
        assertEquals(19,normal.getDestinationAY());
        assertEquals(20,normal.getDestinationBY());
        assertEquals(19,normal.getSourceAY());
        assertEquals(20,normal.getSourceBY());
        assertEquals(21,normal.getGuardY());

        LegacyPlayerLayer48Snapshot flipped=sample(8,0,false,-1,-1,1);
        assertEquals(20,flipped.getDestinationAY());
        assertEquals(21,flipped.getDestinationBY());
        assertEquals(20,flipped.getSourceAY());
        assertEquals(21,flipped.getSourceBY());
        assertEquals(22,flipped.getGuardY());
    }

    @Test public void acceptedPushProducesFourCellWritesAndMetadataIntents() {
        LegacyPlayerLayer48Snapshot s=sample(0,0,false,-1,-1,1);
        LegacyPlayerLayer48Evaluator.Decision d=LegacyPlayerLayer48Evaluator.evaluate(s);
        assertTrue(d.isAccepted());
        assertTrue(d.isWorldMutationRequested());
        assertTrue(d.shouldWriteDestinationA());
        assertTrue(d.shouldWriteDestinationB());
        assertTrue(d.shouldClearSourceA());
        assertTrue(d.shouldClearSourceB());
        assertTrue(d.shouldWriteMetadataA());
        assertTrue(d.shouldWriteMetadataB());
        assertTrue(d.shouldWriteAnimationByte());
        assertTrue(d.shouldWriteForegroundCache());
        assertEquals(0xABCDEF02,d.getMetadataBValue());
        assertEquals(2,d.getDirectionBits());
    }

    @Test public void probeAcceptsButSuppressesEveryWorldWrite() {
        LegacyPlayerLayer48Snapshot s=sample(0,0,true,-1,-1,1);
        LegacyPlayerLayer48Evaluator.Decision d=LegacyPlayerLayer48Evaluator.evaluate(s);
        assertTrue(d.isAccepted());
        assertFalse(d.isWorldMutationRequested());
        assertFalse(d.shouldWriteDestinationA());
        assertFalse(d.shouldWriteDestinationB());
        assertFalse(d.shouldClearSourceA());
        assertFalse(d.shouldClearSourceB());
        assertFalse(d.shouldWriteMetadataA());
        assertFalse(d.shouldWriteMetadataB());
        assertFalse(d.shouldWriteAnimationByte());
        assertFalse(d.shouldWriteForegroundCache());
        assertTrue(d.shouldSetMovementBlock());
    }

    @Test public void counterAndDirectionAndBothDestinationCellsGatePush() {
        assertFalse(LegacyPlayerLayer48Evaluator.evaluate(sample(0,1,false,-1,-1,1)).isAccepted());
        assertFalse(LegacyPlayerLayer48Evaluator.evaluate(sample(0,0,false,0,-1,1)).isAccepted());
        assertFalse(LegacyPlayerLayer48Evaluator.evaluate(sample(0,0,false,-1,0,1)).isAccepted());
        assertFalse(LegacyPlayerLayer48Evaluator.evaluate(sample(0,0,false,-1,-1,-1)).isAccepted());
        assertTrue(LegacyPlayerLayer48Evaluator.evaluate(sample(0,1,true,-1,-1,1)).isAccepted());
    }

    @Test public void rejectedPushRequestsLocalDirectionClearAndPreservesNoWorldWrites() {
        LegacyPlayerLayer48Evaluator.Decision d =
                LegacyPlayerLayer48Evaluator.evaluate(sample(0,0,false,-1,0,1));
        assertFalse(d.isAccepted());
        assertFalse(d.isWorldMutationRequested());
        assertTrue(d.shouldSetMovementBlock());
        assertTrue(d.shouldClearLocalDirection());
        assertFalse(d.shouldWriteMetadataA());
    }

    @Test public void leftwardPushUsesDirectionFourAndMetadataMask() {
        LegacyPlayerLayer48Snapshot s=new LegacyPlayerLayer48Snapshot(
                10,20,-1,0,0,0,48,-1,-1,1,
                0x12345678,0xABCDEF01,false);
        LegacyPlayerLayer48Evaluator.Decision d=LegacyPlayerLayer48Evaluator.evaluate(s);
        assertTrue(d.isAccepted());
        assertEquals(4,d.getDirectionBits());
        assertEquals(0xABCDEF04,d.getMetadataBValue());
        assertEquals(8,s.getDestinationAX());
        assertEquals(8,s.getDestinationBX());
        assertEquals(9,s.getSourceAX());
        assertEquals(9,s.getSourceBX());
    }
}
