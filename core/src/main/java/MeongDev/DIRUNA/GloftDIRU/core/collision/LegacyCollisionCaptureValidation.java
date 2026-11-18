package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Combines a bounds-checked neighborhood capture with branch-input validation.
 * A consumer receives a snapshot only when both capture and context validation
 * are complete. This class does not evaluate collision outcomes.
 */
public final class LegacyCollisionCaptureValidation {
    public enum Status {
        VALID,
        CAPTURE_INCOMPLETE,
        INPUT_INCOMPLETE
    }

    public static final class Result {
        private final Status status;
        private final LegacyCollisionNeighborhoodSnapshot snapshot;
        private final LegacyCollisionNeighborhoodCapture.Status captureStatus;
        private final LegacyCollisionCoordinateResolver.Coordinate failedCoordinate;
        private final Set<LegacyCollisionNeighborhoodSnapshot.Role> missingSamples;
        private final Set<LegacyCollisionBranchInputValidator.ContextField> missingContext;

        private Result(Status status, LegacyCollisionNeighborhoodSnapshot snapshot,
                       LegacyCollisionNeighborhoodCapture.Status captureStatus,
                       LegacyCollisionCoordinateResolver.Coordinate failedCoordinate,
                       Set<LegacyCollisionNeighborhoodSnapshot.Role> missingSamples,
                       Set<LegacyCollisionBranchInputValidator.ContextField> missingContext) {
            this.status=status;
            this.snapshot=snapshot;
            this.captureStatus=captureStatus;
            this.failedCoordinate=failedCoordinate;
            this.missingSamples=immutableEnumSet(missingSamples,
                    LegacyCollisionNeighborhoodSnapshot.Role.class);
            this.missingContext=immutableEnumSet(missingContext,
                    LegacyCollisionBranchInputValidator.ContextField.class);
        }

        private static <E extends Enum<E>> Set<E> immutableEnumSet(Set<E> values, Class<E> type) {
            if (values == null || values.isEmpty()) return Collections.unmodifiableSet(
                    EnumSet.noneOf(type));
            return Collections.unmodifiableSet(EnumSet.copyOf(values));
        }

        public Status getStatus(){return status;}
        public boolean isValid(){return status==Status.VALID;}
        public LegacyCollisionNeighborhoodSnapshot getSnapshot(){return snapshot;}
        public LegacyCollisionNeighborhoodCapture.Status getCaptureStatus(){return captureStatus;}
        public LegacyCollisionCoordinateResolver.Coordinate getFailedCoordinate(){
            return failedCoordinate;
        }
        public Set<LegacyCollisionNeighborhoodSnapshot.Role> getMissingSamples(){
            return missingSamples;
        }
        public Set<LegacyCollisionBranchInputValidator.ContextField> getMissingContext(){
            return missingContext;
        }
    }

    private LegacyCollisionCaptureValidation(){}

    public static Result validate(
            LegacyCollisionNeighborhoodCapture.Result capture,
            LegacyCollisionBranchInputValidator.Branch branch,
            Set<LegacyCollisionBranchInputValidator.ContextField> availableContext) {
        if(capture==null || branch==null || availableContext==null){
            throw new IllegalArgumentException("capture, branch and context are required");
        }
        if(!capture.isComplete()){
            return new Result(Status.CAPTURE_INCOMPLETE,null,capture.getStatus(),
                    capture.getFailedCoordinate(),null,null);
        }
        LegacyCollisionBranchInputValidator.Validation validation =
                LegacyCollisionBranchInputValidator.validate(
                        branch,capture.getSnapshot(),availableContext);
        if(!validation.isComplete()){
            return new Result(Status.INPUT_INCOMPLETE,null,capture.getStatus(),null,
                    validation.getMissingSamples(),validation.getMissingContext());
        }
        return new Result(Status.VALID,capture.getSnapshot(),capture.getStatus(),
                null,null,null);
    }
}
