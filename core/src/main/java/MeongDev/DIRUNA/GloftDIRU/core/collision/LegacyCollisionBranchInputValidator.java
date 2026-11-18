package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Checks whether a branch fixture carries the source inputs needed to inspect
 * that branch. It does not evaluate collision outcomes.
 */
public final class LegacyCollisionBranchInputValidator {
    public enum Branch {
        STAGE_BOUNDS,
        FOREGROUND_TYPE_19,
        FOREGROUND_TYPE_2,
        FOREGROUND_TYPE_4,
        FOREGROUND_TYPE_7,
        FOREGROUND_TYPE_5_OR_28,
        PLAYER_LAYER_TYPE_10,
        PLAYER_LAYER_TYPE_0_8_9_47,
        PLAYER_LAYER_TYPE_48,
        PLAYER_LAYER_EMPTY,
        PLAYER_LAYER_OTHER
    }

    public enum ContextField {
        PROBE_MODE,
        A_W_INT,
        A_X_INT,
        A_N_INT,
        A_P_INT,
        A_C_INT,
        A_S_INT,
        AR_INT,
        K_BYTE,
        X_BYTE,
        I_BYTE_ARR_9,
        I_BYTE_ARR_10,
        L_BOOLEAN,
        SPRITE_ANIMATOR_FRAME_DATA,
        PLAYER_B_INT
    }

    public static final class Validation {
        private final Set<LegacyCollisionNeighborhoodSnapshot.Role> missingSamples;
        private final Set<ContextField> missingContext;

        private Validation(Set<LegacyCollisionNeighborhoodSnapshot.Role> missingSamples,
                           Set<ContextField> missingContext) {
            this.missingSamples = Collections.unmodifiableSet(
                    missingSamples.isEmpty()
                            ? EnumSet.noneOf(LegacyCollisionNeighborhoodSnapshot.Role.class)
                            : EnumSet.copyOf(missingSamples));
            this.missingContext = Collections.unmodifiableSet(
                    missingContext.isEmpty()
                            ? EnumSet.noneOf(ContextField.class)
                            : EnumSet.copyOf(missingContext));
        }

        public boolean isComplete() {
            return missingSamples.isEmpty() && missingContext.isEmpty();
        }
        public Set<LegacyCollisionNeighborhoodSnapshot.Role> getMissingSamples() {
            return missingSamples;
        }
        public Set<ContextField> getMissingContext() { return missingContext; }
    }

    private LegacyCollisionBranchInputValidator() { }

    public static Validation validate(
            Branch branch,
            LegacyCollisionNeighborhoodSnapshot neighborhood,
            Set<ContextField> availableContext) {
        if (branch == null || neighborhood == null || availableContext == null) {
            throw new IllegalArgumentException("branch, neighborhood and context are required");
        }

        EnumSet<LegacyCollisionNeighborhoodSnapshot.Role> requiredSamples =
                EnumSet.noneOf(LegacyCollisionNeighborhoodSnapshot.Role.class);
        EnumSet<ContextField> requiredContext = EnumSet.of(
                ContextField.PROBE_MODE, ContextField.A_W_INT);

        switch (branch) {
            case STAGE_BOUNDS:
                break;
            case FOREGROUND_TYPE_19:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredContext.add(ContextField.L_BOOLEAN);
                break;
            case FOREGROUND_TYPE_2:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredContext.add(ContextField.I_BYTE_ARR_9);
                break;
            case FOREGROUND_TYPE_4:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredContext.add(ContextField.A_C_INT);
                requiredContext.add(ContextField.A_S_INT);
                break;
            case FOREGROUND_TYPE_7:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                break;
            case FOREGROUND_TYPE_5_OR_28:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                break;
            case PLAYER_LAYER_TYPE_10:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredContext.add(ContextField.X_BYTE);
                break;
            case PLAYER_LAYER_TYPE_0_8_9_47:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE_PLAYER_ROW);
                requiredContext.add(ContextField.A_X_INT);
                requiredContext.add(ContextField.K_BYTE);
                requiredContext.add(ContextField.AR_INT);
                requiredContext.add(ContextField.SPRITE_ANIMATOR_FRAME_DATA);
                requiredContext.add(ContextField.PLAYER_B_INT);
                break;
            case PLAYER_LAYER_TYPE_48:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_DESTINATION_A);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_DESTINATION_B);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SOURCE_CLEAR_A);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SOURCE_CLEAR_B);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SUPPORT);
                requiredContext.add(ContextField.A_X_INT);
                break;
            case PLAYER_LAYER_EMPTY:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE_PLAYER_ROW);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE);
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_BELOW);
                requiredContext.add(ContextField.A_N_INT);
                requiredContext.add(ContextField.A_P_INT);
                break;
            case PLAYER_LAYER_OTHER:
                requiredSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
                break;
            default:
                throw new IllegalArgumentException("Unsupported collision branch: " + branch);
        }

        EnumSet<LegacyCollisionNeighborhoodSnapshot.Role> missingSamples =
                EnumSet.noneOf(LegacyCollisionNeighborhoodSnapshot.Role.class);
        for (LegacyCollisionNeighborhoodSnapshot.Role role : requiredSamples) {
            if (!neighborhood.contains(role)) {
                missingSamples.add(role);
            }
        }
        if (branch == Branch.FOREGROUND_TYPE_19
                && (neighborhood.getType19Anchor() == null
                || neighborhood.getType19Scan().isEmpty()
                || neighborhood.getType19ForegroundRun().isEmpty())) {
            missingSamples.add(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_RIGHT);
        }

        EnumSet<ContextField> missingContext = EnumSet.copyOf(requiredContext);
        missingContext.removeAll(availableContext);
        return new Validation(missingSamples, missingContext);
    }
}
