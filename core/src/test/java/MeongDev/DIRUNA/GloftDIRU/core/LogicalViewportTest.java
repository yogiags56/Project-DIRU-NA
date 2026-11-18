package MeongDev.DIRUNA.GloftDIRU.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class LogicalViewportTest {
    @Test
    public void initialPortraitViewportIs240By320() {
        assertEquals(240, LogicalViewport.INITIAL_PORTRAIT.width());
        assertEquals(320, LogicalViewport.INITIAL_PORTRAIT.height());
    }

    @Test
    public void acceptsOtherPositiveLogicalDimensions() {
        LogicalViewport viewport = new LogicalViewport(320, 240);
        assertEquals(320, viewport.width());
        assertEquals(240, viewport.height());
    }

    @Test
    public void rejectsZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogicalViewport(0, 320));
    }

    @Test
    public void rejectsNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogicalViewport(240, -1));
    }
}
