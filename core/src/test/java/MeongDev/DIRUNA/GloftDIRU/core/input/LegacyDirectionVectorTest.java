package MeongDev.DIRUNA.GloftDIRU.core.input;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class LegacyDirectionVectorTest {
    @Test public void upMovesOneTileTowardNegativeY() {
        assertVector(LegacyDirectionResolver.UP, 0, -1);
    }

    @Test public void rightMovesOneTileTowardPositiveX() {
        assertVector(LegacyDirectionResolver.RIGHT, 1, 0);
    }

    @Test public void downMovesOneTileTowardPositiveY() {
        assertVector(LegacyDirectionResolver.DOWN, 0, 1);
    }

    @Test public void leftMovesOneTileTowardNegativeX() {
        assertVector(LegacyDirectionResolver.LEFT, -1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noneIsNotAValidMovementVector() {
        LegacyDirectionVector.tileDeltaX(LegacyDirectionResolver.NONE);
    }

    private static void assertVector(int direction, int expectedX, int expectedY) {
        assertEquals(expectedX, LegacyDirectionVector.tileDeltaX(direction));
        assertEquals(expectedY, LegacyDirectionVector.tileDeltaY(direction));
    }
}
