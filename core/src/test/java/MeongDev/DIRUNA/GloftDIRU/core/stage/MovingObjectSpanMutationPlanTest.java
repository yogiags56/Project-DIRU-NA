package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MovingObjectSpanMutationPlanTest {
    private static StageGrid stage(int width, int height, int[] player, int[] metadata) {
        return new StageGrid(width, height, player, metadata, new int[width * height]);
    }

    @Test public void plansRecognizedObjectConversionMetadataMarkersAndEvent() {
        int width = 6, height = 5, x = 2, y = 2;
        int[] player = new int[width * height];
        int[] metadata = new int[width * height];
        player[x + y * width] = 43;
        metadata[x + y * width] = 0x00ABCDEF;
        StageGrid source = stage(width, height, player, metadata);

        MovingObjectSpanMutationPlan plan = MovingObjectSpanMutationPlan.plan(
                source, new MovingObjectSpanTable(new byte[]{2, 2, 2}));

        assertEquals(1, plan.cellMutations().size());
        assertEquals(new MovingObjectSpanMutationPlan.CellMutation(
                2, 2, 43, 0x00ABCDEF, 9,
                (0x00ABCDEF & 0xF03FFFFF) | (37 << 22) | 0x10000000),
                plan.cellMutations().get(0));
        assertEquals(9, plan.markerWrites().size());
        assertEquals(new MovingObjectSpanMutationPlan.MarkerWrite(1, 1, 48),
                plan.markerWrites().get(0));
        assertEquals(new MovingObjectSpanMutationPlan.MarkerWrite(3, 3, 48),
                plan.markerWrites().get(8));
        assertEquals(new MovingObjectSpanMutationPlan.InteractionEvent(2, 2, 1),
                plan.interactionEvents().get(0));
        assertEquals(43, source.playerAt(2, 2));
        assertEquals(0x00ABCDEF, source.backgroundAt(2, 2));
    }

    @Test public void ignoresUnrecognizedAndInactiveSpanCells() {
        int[] player = new int[25];
        player[2 + 2 * 5] = 8;
        MovingObjectSpanMutationPlan plan = MovingObjectSpanMutationPlan.plan(
                stage(5, 5, player, new int[25]),
                new MovingObjectSpanTable(new byte[]{2, 2, 2, 1, 1, -1}));
        assertTrue(plan.cellMutations().isEmpty());
        assertTrue(plan.markerWrites().isEmpty());
        assertTrue(plan.interactionEvents().isEmpty());
    }

    @Test public void plansOnlyRecognizedCellsWithinInclusiveSpan() {
        int[] player = new int[35];
        player[2 + 2 * 7] = 1;
        player[3 + 2 * 7] = 8;
        player[4 + 2 * 7] = 49;
        MovingObjectSpanMutationPlan plan = MovingObjectSpanMutationPlan.plan(
                stage(7, 5, player, new int[35]),
                new MovingObjectSpanTable(new byte[]{2, 4, 2}));
        assertEquals(2, plan.cellMutations().size());
        assertEquals(18, plan.markerWrites().size());
        assertEquals(2, plan.interactionEvents().size());
    }

    @Test public void rejectsMarkerRegionOutsideStage() {
        int[] player = new int[25];
        player[0 + 2 * 5] = 1;
        try {
            MovingObjectSpanMutationPlan.plan(stage(5, 5, player, new int[25]),
                    new MovingObjectSpanTable(new byte[]{0, 0, 2}));
            fail("incomplete marker region must be rejected");
        } catch (IllegalArgumentException expected) { }
    }
    @Test public void overlappingSpansPlanAnObjectOnlyOnceLikeSequentialLegacyWrites() {
        int[] player = new int[36];
        player[2 + 2 * 6] = 43;
        StageGrid source = stage(6, 6, player, new int[36]);
        MovingObjectSpanMutationPlan plan = MovingObjectSpanMutationPlan.plan(source,
                new MovingObjectSpanTable(new byte[]{1, 3, 2, 2, 4, 2}));
        assertEquals(1, plan.cellMutations().size());
        assertEquals(1, plan.interactionEvents().size());
    }

    @Test public void transactionPublishesStageMarkersAndRingBufferTogether() {
        int[] player = new int[36];
        int[] metadata = new int[36];
        player[2 + 2 * 6] = 43;
        StageGrid source = stage(6, 6, player, metadata);
        MovingObjectSpanMutationPlan plan = MovingObjectSpanMutationPlan.plan(source,
                new MovingObjectSpanTable(new byte[]{2, 2, 2}));
        MovingObjectSpanMutationTransaction.Result result =
                MovingObjectSpanMutationTransaction.apply(source, new byte[36],
                        new MovingObjectSpanMutationTransaction.QueueState(
                                new byte[8], new byte[8], new byte[8], new byte[8], 7), plan);

        assertEquals(43, source.playerAt(2, 2));
        assertEquals(9, result.stage().playerAt(2, 2));
        assertEquals(48, result.copyMarkerLayer()[1 + 1 * 6]);
        assertEquals(1, result.queue().nextIndex());
        assertEquals(2, result.queue().copyX()[7]);
        assertEquals(2, result.queue().copyY()[7]);
        assertEquals(1, result.queue().copyType()[7]);
        assertEquals(0, result.queue().copyStatus()[7]);
    }

    @Test public void stalePreconditionRejectsWholeTransactionWithoutMutatingInputs() {
        int[] player = new int[36];
        player[2 + 2 * 6] = 43;
        StageGrid source = stage(6, 6, player, new int[36]);
        MovingObjectSpanMutationPlan plan = MovingObjectSpanMutationPlan.plan(source,
                new MovingObjectSpanTable(new byte[]{2, 2, 2}));
        byte[] markers = new byte[36];
        try {
            MovingObjectSpanMutationTransaction.apply(
                    stage(6, 6, new int[36], new int[36]), markers,
                    MovingObjectSpanMutationTransaction.QueueState.empty(), plan);
            fail("stale cell precondition must reject transaction");
        } catch (IllegalStateException expected) { }
        assertEquals(43, source.playerAt(2, 2));
        assertEquals(0, markers[1 + 1 * 6]);
    }

}
