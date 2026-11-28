package MeongDev.DIRUNA.GloftDIRU.core.collision;

import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionGridViewTest {
    private static StageGrid stage() {
        return new StageGrid(3, 2,
                new int[]{-1, 8, 9, 10, 11, 12},
                new int[]{-128, -1, 0, 1, 127, 255},
                new int[]{0, 1, 2, 3, 4, 5});
    }

    @Test public void exposesDistinctRawLayersWithoutInferringMetadataFromBackground() {
        LegacyCollisionGridView view = new LegacyCollisionGridView(stage(),
                new int[]{101, 102, 103, 104, 105, 106}, null);
        assertEquals(2, view.height());
        assertEquals(3, view.width());
        assertEquals(8, view.playerLayerRawAt(1, 0));
        assertEquals(1, view.foregroundRawAt(1, 0));
        assertEquals(102, view.packedMetadataRawAt(1, 0));
        assertFalse(view.hasAuxiliaryGrid());
    }

    @Test public void optionalAuxiliaryLayerPresenceIsExplicitAndDefensivelyCopied() {
        int[] aux = {7, 8, 9, 10, 11, 12};
        LegacyCollisionGridView view = new LegacyCollisionGridView(stage(), new int[6], aux);
        aux[0] = 999;
        assertTrue(view.hasAuxiliaryGrid());
        assertEquals(7, view.auxiliaryRawAt(0, 0));
    }

    @Test public void rejectsMissingOrMismatchedRequiredMetadata() {
        try { new LegacyCollisionGridView(stage(), null, null); fail("metadata is required"); }
        catch (IllegalArgumentException expected) { }
        try { new LegacyCollisionGridView(stage(), new int[5], null); fail("metadata size mismatch"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsMismatchedOptionalAuxiliaryLayer() {
        try { new LegacyCollisionGridView(stage(), new int[6], new int[5]); fail("auxiliary size mismatch"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void missingAuxiliaryCannotBeReadAsInventedZeroes() {
        LegacyCollisionGridView view = new LegacyCollisionGridView(stage(), new int[6], null);
        try { view.auxiliaryRawAt(0, 0); fail("absent layer must not be readable"); }
        catch (IllegalStateException expected) { }
    }

    @Test public void rejectsOutOfBoundsLayerReads() {
        LegacyCollisionGridView view = new LegacyCollisionGridView(stage(), new int[6], null);
        try { view.packedMetadataRawAt(3, 0); fail("out of bounds"); }
        catch (IndexOutOfBoundsException expected) { }
    }
}
