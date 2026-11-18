package MeongDev.DIRUNA.GloftDIRU.core.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class LegacyInputMasksTest {
    @Test
    public void directionMasksMatchAuditedSourceValues() {
        assertEquals(4097, LegacyInputMasks.UP);
        assertEquals(65544, LegacyInputMasks.RIGHT);
        assertEquals(262146, LegacyInputMasks.DOWN);
        assertEquals(16388, LegacyInputMasks.LEFT);
    }

    @Test
    public void eachDirectionMaskIsDisjointFromTheOthers() {
        int[] masks = {
                LegacyInputMasks.UP,
                LegacyInputMasks.RIGHT,
                LegacyInputMasks.DOWN,
                LegacyInputMasks.LEFT
        };

        for (int i = 0; i < masks.length; i++) {
            for (int j = i + 1; j < masks.length; j++) {
                assertEquals(0, masks[i] & masks[j]);
            }
        }
    }

    @Test
    public void heldStateCanBeQueriedUsingLegacyDirectionMask() {
        HeldKeyState state = new HeldKeyState();
        state.press(LegacyInputMasks.LEFT);

        assertTrue(state.isAnyPressed(LegacyInputMasks.LEFT));
        assertFalse(state.isAnyPressed(LegacyInputMasks.RIGHT));
    }
}
