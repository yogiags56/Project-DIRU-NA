package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MovingObjectSpanUpdaterTest {
    private static StageGrid stage(int width, int height, int[] player) {
        return new StageGrid(width, height, player, new int[width * height],
                new int[width * height]);
    }

    @Test public void recomputesBothEndpointsAcrossTraversableCells() {
        int width = 8, height = 4, row = 2;
        int[] player = new int[width * height];
        player[1 + row * width] = 1;
        player[2 + row * width] = -1;
        player[4 + row * width] = 31;
        player[5 + row * width] = 45;
        player[0 + row * width] = 5;
        player[6 + row * width] = 5;

        MovingObjectSpanTable input = new MovingObjectSpanTable(
                new byte[]{3, 3, 2});
        MovingObjectSpanTable output = MovingObjectSpanUpdater.update(
                stage(width, height, player), input);

        assertEquals(new MovingObjectSpanTable.Span((byte) 1, (byte) 5, (byte) 2),
                output.spanAt(0));
        assertEquals(new MovingObjectSpanTable.Span((byte) 3, (byte) 3, (byte) 2),
                input.spanAt(0));
    }

    @Test public void preservesInactiveSentinelAndZeroRowRecords() {
        MovingObjectSpanTable input = new MovingObjectSpanTable(
                new byte[]{2, 4, -1, 3, 5, 0});
        MovingObjectSpanTable output = MovingObjectSpanUpdater.update(
                stage(8, 4, new int[32]), input);
        assertEquals(input.spanAt(0), output.spanAt(0));
        assertEquals(input.spanAt(1), output.spanAt(1));
    }

    @Test public void stopsAtNonTraversableOccupants() {
        int width = 7, height = 3, row = 1;
        int[] player = new int[width * height];
        player[2 + row * width] = 5;
        player[4 + row * width] = 5;
        MovingObjectSpanTable output = MovingObjectSpanUpdater.update(
                stage(width, height, player),
                new MovingObjectSpanTable(new byte[]{3, 3, 1}));
        assertEquals(new MovingObjectSpanTable.Span((byte) 3, (byte) 3, (byte) 1),
                output.spanAt(0));
    }

    @Test public void rejectsInvalidActiveRowsAndEndpoints() {
        try {
            MovingObjectSpanUpdater.update(stage(4, 3, new int[12]),
                    new MovingObjectSpanTable(new byte[]{1, 1, 3}));
            fail("row outside grid must be rejected");
        } catch (IllegalArgumentException expected) { }

        try {
            MovingObjectSpanUpdater.update(stage(4, 3, new int[12]),
                    new MovingObjectSpanTable(new byte[]{-1, 1, 1}));
            fail("negative active endpoint must be rejected");
        } catch (IllegalArgumentException expected) { }
    }
}
