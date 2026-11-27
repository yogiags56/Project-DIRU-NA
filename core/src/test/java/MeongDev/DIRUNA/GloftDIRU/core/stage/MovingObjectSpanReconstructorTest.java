package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MovingObjectSpanReconstructorTest {
    private static StageGrid stage(int width, int height, int[] player, int[] metadata) {
        return new StageGrid(width, height, player, metadata, new int[width * height]);
    }

    @Test public void rebuildsRecognizedRightAndLeftCandidatesInRowMajorSlotOrder() {
        int[] player = new int[20];
        int[] metadata = new int[20];
        player[1 + 0 * 5] = 48;
        metadata[1 + 0 * 5] = 0x8;
        player[2 + 0 * 5] = 1;
        metadata[1 + 1 * 5] = 0x10;

        player[3 + 2 * 5] = 48;
        metadata[3 + 2 * 5] = 0x8;
        player[2 + 2 * 5] = 45;
        metadata[3 + 3 * 5] = 0x10;

        MovingObjectSpanReconstructor.Result result =
                MovingObjectSpanReconstructor.rebuild(stage(5, 4, player, metadata));
        MovingObjectSpanTable spans = result.spans();
        assertEquals(2, spans.size());
        assertEquals(new MovingObjectSpanTable.Span((byte) 2, (byte) 2, (byte) 0),
                spans.spanAt(0));
        assertEquals(new MovingObjectSpanTable.Span((byte) 2, (byte) 2, (byte) 2),
                spans.spanAt(1));
        int[] packed = result.copyPackedBackgroundMetadata();
        assertEquals(0x00000008, packed[1]);
        assertEquals(0x01000008, packed[3 + 2 * 5]);
    }

    @Test public void unknownOccupantUsesNegativeRowSentinel() {
        int[] player = new int[12];
        int[] metadata = new int[12];
        player[1] = 48;
        metadata[1] = 0x8;
        player[2] = 9;
        MovingObjectSpanTable spans = MovingObjectSpanReconstructor
                .rebuild(stage(4, 3, player, metadata)).spans();
        assertEquals(new MovingObjectSpanTable.Span((byte) 2, (byte) 2, (byte) -1),
                spans.spanAt(0));
    }

    @Test public void nonFlaggedMovingObjectsDoNotAllocateSpanSlots() {
        int[] player = new int[12];
        int[] metadata = new int[12];
        player[1] = 48;
        MovingObjectSpanTable spans = MovingObjectSpanReconstructor
                .rebuild(stage(4, 3, player, metadata)).spans();
        assertEquals(0, spans.size());
    }

    @Test public void sourceGridIsNotMutatedAndMetadataExportIsDetached() {
        int[] player = new int[12];
        int[] metadata = new int[12];
        player[1] = 48;
        metadata[1] = 0x8;
        player[2] = 1;
        StageGrid source = stage(4, 3, player, metadata);
        MovingObjectSpanReconstructor.Result result =
                MovingObjectSpanReconstructor.rebuild(source);
        assertEquals(0x8, source.backgroundAt(1, 0));
        int[] exported = result.copyPackedBackgroundMetadata();
        exported[1] = 99;
        assertEquals(0x8, result.copyPackedBackgroundMetadata()[1]);
    }

    @Test public void rejectsMissingBelowCellAndOutOfBoundsCandidate() {
        int[] player = new int[8];
        int[] metadata = new int[8];
        player[1 + 1 * 4] = 48;
        metadata[1 + 1 * 4] = 0x8;
        try {
            MovingObjectSpanReconstructor.rebuild(stage(4, 2, player, metadata));
            fail("missing below cell must be rejected");
        } catch (IllegalArgumentException expected) { }

        int[] edgePlayer = new int[12];
        int[] edgeMetadata = new int[12];
        edgePlayer[3] = 48;
        edgeMetadata[3] = 0x8;
        try {
            MovingObjectSpanReconstructor.rebuild(stage(4, 3, edgePlayer, edgeMetadata));
            fail("candidate outside stage must be rejected");
        } catch (IllegalArgumentException expected) { }
    }
}
