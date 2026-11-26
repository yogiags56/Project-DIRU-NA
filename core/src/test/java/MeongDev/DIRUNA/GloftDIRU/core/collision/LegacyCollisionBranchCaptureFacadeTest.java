package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import java.util.EnumSet;
import static org.junit.Assert.*;

public final class LegacyCollisionBranchCaptureFacadeTest {
    private static final class Grid implements LegacyCollisionNeighborhoodCapture.Grid {
        private final int width;
        private final int height;
        Grid(int width, int height) { this.width = width; this.height = height; }
        public int width() { return width; }
        public int height() { return height; }
        public int foregroundRawAt(int x, int y) { return 0; }
        public int playerLayerRawAt(int x, int y) { return -1; }
        public int packedMetadataRawAt(int x, int y) { return 0; }
        public boolean hasAuxiliaryGrid() { return false; }
        public int auxiliaryRawAt(int x, int y) {
            throw new AssertionError("auxiliary grid is absent");
        }
    }

    private static EnumSet<LegacyCollisionBranchInputValidator.ContextField> completeContext() {
        return EnumSet.of(
                LegacyCollisionBranchInputValidator.ContextField.PROBE_MODE,
                LegacyCollisionBranchInputValidator.ContextField.A_W_INT);
    }

    @Test public void returnsValidatedSnapshotForCompleteBranchRequest() {
        LegacyCollisionBranchCaptureFacade facade =
                new LegacyCollisionBranchCaptureFacade(8, 8);
        LegacyCollisionCaptureValidation.Result result = facade.captureAndValidate(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                new Grid(8, 8), 2, 2, 1, 0, completeContext());

        assertEquals(LegacyCollisionCaptureValidation.Status.VALID, result.getStatus());
        assertNotNull(result.getSnapshot());
        assertTrue(result.getSnapshot().contains(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET));
    }

    @Test public void rejectsOutOfBoundsSamplesWithoutExposingSnapshot() {
        LegacyCollisionBranchCaptureFacade facade =
                new LegacyCollisionBranchCaptureFacade(8, 8);
        LegacyCollisionCaptureValidation.Result result = facade.captureAndValidate(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                new Grid(8, 8), 7, 2, 1, 0, completeContext());

        assertEquals(LegacyCollisionCaptureValidation.Status.CAPTURE_INCOMPLETE,
                result.getStatus());
        assertNull(result.getSnapshot());
        assertEquals(LegacyCollisionNeighborhoodCapture.Status.OUT_OF_BOUNDS,
                result.getCaptureStatus());
    }

    @Test public void rejectsMissingBranchContextWithoutExposingSnapshot() {
        LegacyCollisionBranchCaptureFacade facade =
                new LegacyCollisionBranchCaptureFacade(8, 8);
        LegacyCollisionCaptureValidation.Result result = facade.captureAndValidate(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_2,
                new Grid(8, 8), 2, 2, 1, 0, completeContext());

        assertEquals(LegacyCollisionCaptureValidation.Status.INPUT_INCOMPLETE,
                result.getStatus());
        assertNull(result.getSnapshot());
        assertTrue(result.getMissingContext().contains(
                LegacyCollisionBranchInputValidator.ContextField.I_BYTE_ARR_9));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGridDimensionsThatDoNotMatchStageBounds() {
        new LegacyCollisionBranchCaptureFacade(8, 8).captureAndValidate(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                new Grid(7, 8), 2, 2, 1, 0, completeContext());
    }
}
