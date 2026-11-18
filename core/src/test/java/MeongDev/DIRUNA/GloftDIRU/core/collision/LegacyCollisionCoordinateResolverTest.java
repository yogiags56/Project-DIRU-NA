package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionCoordinateResolverTest {
    private static LegacyCollisionCoordinateResolver.StageBounds bounds() {
        return new LegacyCollisionCoordinateResolver.StageBounds(20, 30);
    }

    @Test public void resolvesTargetAndTwoStepCoordinates() {
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(5, 7),
                LegacyCollisionCoordinateResolver.target(4, 7, 1, 0, bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(6, 7),
                LegacyCollisionCoordinateResolver.twoSteps(4, 7, 1, 0, bounds()));
    }

    @Test public void resolvesAbovePlayerRowAndTargetVerticalNeighbors() {
        LegacyCollisionCoordinateResolver.Coordinate target =
                LegacyCollisionCoordinateResolver.target(4, 7, 1, 0, bounds());
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(5, 8),
                LegacyCollisionCoordinateResolver.targetAbovePlayerRow(4, 7, 1, bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(5, 6),
                LegacyCollisionCoordinateResolver.targetAbove(target, bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(5, 8),
                LegacyCollisionCoordinateResolver.targetBelow(target, bounds()));
    }

    @Test public void resolvesType19DynamicScanAndForegroundRunSeparately() {
        LegacyCollisionResolverFixture();
    }

    private void LegacyCollisionResolverFixture() {
        LegacyCollisionCoordinateResolver.Coordinate target =
                new LegacyCollisionCoordinateResolver.Coordinate(4, 7);
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(7, 7),
                LegacyCollisionCoordinateResolver.type19AnchorColumn(target, bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(7, 6),
                LegacyCollisionCoordinateResolver.type19ScanNext(
                        new LegacyCollisionCoordinateResolver.Coordinate(7, 7), bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(4, 8),
                LegacyCollisionCoordinateResolver.type19ForegroundRunNext(target, 7, bounds()));
    }

    @Test public void resolvesType48GeometryOffsets() {
        LegacyCollisionCoordinateResolver.Coordinate target =
                new LegacyCollisionCoordinateResolver.Coordinate(4, 7);
        LegacyCollisionCoordinateResolver.Coordinate two =
                new LegacyCollisionCoordinateResolver.Coordinate(6, 7);
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(6, 6),
                LegacyCollisionCoordinateResolver.type48Destination(two, -1, bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(4, 7),
                LegacyCollisionCoordinateResolver.type48SourceClear(target, 0, bounds()));
        assertEquals(new LegacyCollisionCoordinateResolver.Coordinate(4, 9),
                LegacyCollisionCoordinateResolver.type48Support(target, 1, bounds()));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsOutOfStageTargetBeforeArrayRead() {
        LegacyCollisionCoordinateResolver.target(0, 0, -1, 0, bounds());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsOutOfStageType19ScanAtTopEdge() {
        LegacyCollisionCoordinateResolver.type19ScanNext(
                new LegacyCollisionCoordinateResolver.Coordinate(3, 0), bounds());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsOutOfStageType48Support() {
        LegacyCollisionCoordinateResolver.type48Support(
                new LegacyCollisionCoordinateResolver.Coordinate(4, 29), 1, bounds());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsIntegerOverflow() {
        LegacyCollisionCoordinateResolver.checked((long) Integer.MAX_VALUE + 1L, 0, bounds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidStageDimensions() {
        new LegacyCollisionCoordinateResolver.StageBounds(0, 10);
    }
}
