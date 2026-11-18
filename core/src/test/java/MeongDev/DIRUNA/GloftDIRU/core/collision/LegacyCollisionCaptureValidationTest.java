package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import java.util.EnumSet;
import static org.junit.Assert.*;

public final class LegacyCollisionCaptureValidationTest {
    private static final class Grid implements LegacyCollisionNeighborhoodCapture.Grid {
        public int width(){return 8;} public int height(){return 8;}
        public int foregroundRawAt(int x,int y){return 0;}
        public int playerLayerRawAt(int x,int y){return -1;}
        public int packedMetadataRawAt(int x,int y){return 0;}
        public boolean hasAuxiliaryGrid(){return false;}
        public int auxiliaryRawAt(int x,int y){throw new AssertionError("no auxiliary grid");}
    }
    private static LegacyCollisionNeighborhoodCapture.Result capture(int px,int py) {
        return new LegacyCollisionNeighborhoodCapture(8,8).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                new Grid(),px,py,1,0);
    }

    @Test public void exposesSnapshotOnlyAfterCaptureAndContextAreValid() {
        EnumSet<LegacyCollisionBranchInputValidator.ContextField> context =
                EnumSet.of(LegacyCollisionBranchInputValidator.ContextField.PROBE_MODE,
                        LegacyCollisionBranchInputValidator.ContextField.A_W_INT);
        LegacyCollisionCaptureValidation.Result r =
                LegacyCollisionCaptureValidation.validate(capture(2,2),
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,context);
        assertTrue(r.isValid());
        assertNotNull(r.getSnapshot());
        assertTrue(r.getMissingSamples().isEmpty());
        assertTrue(r.getMissingContext().isEmpty());
    }

    @Test public void withholdsSnapshotWhenContextIsMissing() {
        LegacyCollisionCaptureValidation.Result r =
                LegacyCollisionCaptureValidation.validate(capture(2,2),
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                        EnumSet.noneOf(LegacyCollisionBranchInputValidator.ContextField.class));
        assertEquals(LegacyCollisionCaptureValidation.Status.INPUT_INCOMPLETE,r.getStatus());
        assertNull(r.getSnapshot());
        assertTrue(r.getMissingContext().contains(
                LegacyCollisionBranchInputValidator.ContextField.PROBE_MODE));
        assertTrue(r.getMissingContext().contains(
                LegacyCollisionBranchInputValidator.ContextField.A_W_INT));
    }

    @Test public void propagatesOutOfBoundsCaptureWithoutSnapshot() {
        LegacyCollisionCaptureValidation.Result r =
                LegacyCollisionCaptureValidation.validate(capture(-1,2),
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                        EnumSet.of(LegacyCollisionBranchInputValidator.ContextField.PROBE_MODE,
                                LegacyCollisionBranchInputValidator.ContextField.A_W_INT));
        assertEquals(LegacyCollisionCaptureValidation.Status.CAPTURE_INCOMPLETE,r.getStatus());
        assertEquals(LegacyCollisionNeighborhoodCapture.Status.OUT_OF_BOUNDS,
                r.getCaptureStatus());
        assertNull(r.getSnapshot());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void missingContextSetIsImmutable() {
        LegacyCollisionCaptureValidation.Result r =
                LegacyCollisionCaptureValidation.validate(capture(2,2),
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                        EnumSet.noneOf(LegacyCollisionBranchInputValidator.ContextField.class));
        r.getMissingContext().clear();
    }
}
