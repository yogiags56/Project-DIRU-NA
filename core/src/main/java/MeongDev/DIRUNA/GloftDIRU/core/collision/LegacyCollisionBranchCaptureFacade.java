package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.Set;

/**
 * Single entry point for a branch-scoped collision capture request.
 *
 * The facade performs bounds-checked raw sampling first, then validates the
 * branch's required samples and source context. It exposes a neighborhood
 * snapshot only when both phases succeed; it never evaluates passability or
 * mutates gameplay state.
 */
public final class LegacyCollisionBranchCaptureFacade {
    private final LegacyCollisionNeighborhoodCapture capture;

    public LegacyCollisionBranchCaptureFacade(int stageWidth, int stageHeight) {
        this.capture = new LegacyCollisionNeighborhoodCapture(stageWidth, stageHeight);
    }

    public LegacyCollisionCaptureValidation.Result captureAndValidate(
            LegacyCollisionBranchInputValidator.Branch branch,
            LegacyCollisionNeighborhoodCapture.Grid grid,
            int playerX,
            int playerY,
            int deltaX,
            int deltaY,
            Set<LegacyCollisionBranchInputValidator.ContextField> availableContext) {
        if (branch == null || grid == null || availableContext == null) {
            throw new IllegalArgumentException("branch, grid and available context are required");
        }

        LegacyCollisionNeighborhoodCapture.Result captured = capture.capture(
                branch, grid, playerX, playerY, deltaX, deltaY);
        return LegacyCollisionCaptureValidation.validate(
                captured, branch, availableContext);
    }
}
