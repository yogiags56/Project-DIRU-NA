package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionInputSnapshotTest {
    private static LegacyCollisionInputSnapshot sample() {
        return new LegacyCollisionInputSnapshot(
                4, 7, 20, 30, 1, 0, false,
                0x1002, 2, 0, 12, 0, 3,
                false, true, 5, 0, 3, 4,
                0x0402, 9, 0x1234, 0x0001, 8);
    }

    @Test public void preservesRawSourceInputsWithoutNormalizingPackedValues() {
        LegacyCollisionInputSnapshot s = sample();
        assertEquals(0x1002, s.getPackedPlayerState());
        assertEquals(0x0402, s.getForegroundCellValue());
        assertEquals(0x1234, s.getPlayerCellMetadata());
        assertEquals(9, s.getPlayerLayerCellValue());
    }

    @Test public void computesTargetCoordinatesFromImmutableOriginAndDelta() {
        LegacyCollisionInputSnapshot s = sample();
        assertEquals(5, s.getTargetX());
        assertEquals(7, s.getTargetY());
        assertEquals(4, s.getPlayerX());
        assertEquals(7, s.getPlayerY());
    }

    @Test public void retainsProbeAndOptionalAuxiliaryGridPresenceSeparately() {
        LegacyCollisionInputSnapshot s = sample();
        assertFalse(s.isProbeMode());
        assertTrue(s.isAuxiliaryGridPresent());
        assertEquals(5, s.getAuxiliaryTargetValue());
    }

    @Test public void allowsAbsentAuxiliaryGridWithoutInventingAValue() {
        LegacyCollisionInputSnapshot s = new LegacyCollisionInputSnapshot(
                0, 0, 4, 4, 0, -1, true,
                0, 0, 0, 0, 0, 0, false, false,
                0, 0, 0, 0, 0, -1, 0, 0, -1);
        assertFalse(s.isAuxiliaryGridPresent());
        assertEquals(0, s.getAuxiliaryTargetValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroStageWidth() {
        new LegacyCollisionInputSnapshot(
                0, 0, 0, 4, 0, 0, false,
                0, 0, 0, 0, 0, 0, false, false,
                0, 0, 0, 0, 0, -1, 0, 0, -1);
    }
}
