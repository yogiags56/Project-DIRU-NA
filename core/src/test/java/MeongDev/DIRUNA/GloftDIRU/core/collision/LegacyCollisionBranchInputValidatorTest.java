package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import static org.junit.Assert.*;

public final class LegacyCollisionBranchInputValidatorTest {
    private static LegacyCollisionNeighborhoodSnapshot snapshot(
            LegacyCollisionNeighborhoodSnapshot.Role... roles) {
        Map<LegacyCollisionNeighborhoodSnapshot.Role,
                LegacyCollisionNeighborhoodSnapshot.CellSample> samples =
                new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                        LegacyCollisionNeighborhoodSnapshot.CellSample>(
                        LegacyCollisionNeighborhoodSnapshot.Role.class);
        for (LegacyCollisionNeighborhoodSnapshot.Role role : roles) {
            samples.put(role, new LegacyCollisionNeighborhoodSnapshot.CellSample(
                    2, 3, 0, -1, 0, false, 0));
        }
        return new LegacyCollisionNeighborhoodSnapshot(samples, null, null);
    }

    private static EnumSet<LegacyCollisionBranchInputValidator.ContextField> common() {
        return EnumSet.of(
                LegacyCollisionBranchInputValidator.ContextField.PROBE_MODE,
                LegacyCollisionBranchInputValidator.ContextField.A_W_INT);
    }

    @Test public void stageBoundsNeedsNoCellSamplesButNeedsControlContext() {
        LegacyCollisionBranchInputValidator.Validation v =
                LegacyCollisionBranchInputValidator.validate(
                        LegacyCollisionBranchInputValidator.Branch.STAGE_BOUNDS,
                        snapshot(), common());
        assertTrue(v.isComplete());
    }

    @Test public void foregroundType2RequiresInventoryCounterAndTarget() {
        LegacyCollisionBranchInputValidator.Validation v =
                LegacyCollisionBranchInputValidator.validate(
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_2,
                        snapshot(LegacyCollisionNeighborhoodSnapshot.Role.TARGET), common());
        assertFalse(v.isComplete());
        assertTrue(v.getMissingContext().contains(
                LegacyCollisionBranchInputValidator.ContextField.I_BYTE_ARR_9));
    }

    @Test public void type48RejectsMissingGeometrySamples() {
        LegacyCollisionBranchInputValidator.Validation v =
                LegacyCollisionBranchInputValidator.validate(
                        LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_48,
                        snapshot(LegacyCollisionNeighborhoodSnapshot.Role.TARGET,
                                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS),
                        common());
        assertFalse(v.isComplete());
        assertEquals(5, v.getMissingSamples().size());
    }

    @Test public void emptyLayerRequiresBothVerticalNeighborsAndCounters() {
        LegacyCollisionBranchInputValidator.Validation v =
                LegacyCollisionBranchInputValidator.validate(
                        LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_EMPTY,
                        snapshot(LegacyCollisionNeighborhoodSnapshot.Role.TARGET,
                                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE_PLAYER_ROW,
                                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE,
                                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_BELOW),
                        common());
        assertFalse(v.isComplete());
        assertTrue(v.getMissingContext().contains(
                LegacyCollisionBranchInputValidator.ContextField.A_N_INT));
        assertTrue(v.getMissingContext().contains(
                LegacyCollisionBranchInputValidator.ContextField.A_P_INT));
    }

    @Test public void type19RequiresAnchorAndBothOrderedScans() {
        LegacyCollisionNeighborhoodSnapshot base =
                snapshot(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
        LegacyCollisionBranchInputValidator.Validation v =
                LegacyCollisionBranchInputValidator.validate(
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_19,
                        base, common());
        assertFalse(v.isComplete());
        assertTrue(v.getMissingSamples().contains(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_RIGHT));
    }

    @Test public void type19AcceptsExplicitScanAndForegroundRun() {
        LegacyCollisionNeighborhoodSnapshot.CellSample anchor =
                new LegacyCollisionNeighborhoodSnapshot.CellSample(5, 1, 0, 39, 7, false, 0);
        ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample> scan =
                new ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample>();
        scan.add(anchor);
        ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample> run =
                new ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample>();
        run.add(new LegacyCollisionNeighborhoodSnapshot.CellSample(2, 3, 19, -1, 0, false, 0));
        LegacyCollisionNeighborhoodSnapshot n =
                new LegacyCollisionNeighborhoodSnapshot(
                        new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                                LegacyCollisionNeighborhoodSnapshot.CellSample>(
                                LegacyCollisionNeighborhoodSnapshot.Role.class),
                        anchor, scan, run);
        LegacyCollisionBranchInputValidator.Validation v =
                LegacyCollisionBranchInputValidator.validate(
                        LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_19,
                        n, common().plus(
                                LegacyCollisionBranchInputValidator.ContextField.L_BOOLEAN));
        assertTrue(v.isComplete());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullBranch() {
        LegacyCollisionBranchInputValidator.validate(null, snapshot(), common());
    }
}
