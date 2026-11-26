package MeongDev.DIRUNA.GloftDIRU.core.movement;

import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyDirectionResolver;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OrdinaryMovementInterpolationTest {
    @Test public void followsSourceOrdinaryProgressSequence() {
        OrdinaryMovementInterpolation clock = new OrdinaryMovementInterpolation();
        clock.beginAcceptedMove(LegacyDirectionResolver.RIGHT);
        assertEquals(18, clock.progress());
        assertEquals(12, clock.advanceOneUpdate());
        assertEquals(6, clock.advanceOneUpdate());
        assertEquals(0, clock.advanceOneUpdate());
        assertFalse(clock.isActive());
        assertEquals(LegacyDirectionResolver.NONE, clock.direction());
    }

    @Test public void refusesToStartSecondMoveWhileActive() {
        OrdinaryMovementInterpolation clock = new OrdinaryMovementInterpolation();
        clock.beginAcceptedMove(LegacyDirectionResolver.UP);
        try { clock.beginAcceptedMove(LegacyDirectionResolver.DOWN); fail("expected active guard"); }
        catch (IllegalStateException expected) { }
    }

    @Test public void rejectsUnknownDirectionAndResetClearsState() {
        OrdinaryMovementInterpolation clock = new OrdinaryMovementInterpolation();
        try { clock.beginAcceptedMove(0); fail("expected direction validation"); }
        catch (IllegalArgumentException expected) { }
        clock.beginAcceptedMove(LegacyDirectionResolver.LEFT);
        clock.reset();
        assertEquals(0, clock.progress());
        assertEquals(LegacyDirectionResolver.NONE, clock.direction());
    }
}
