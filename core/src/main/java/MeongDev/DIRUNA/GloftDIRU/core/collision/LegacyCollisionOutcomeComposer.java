package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Composes an observed helper trace into the established result vocabulary.
 *
 * Values not represented by LegacyCollisionTrace are explicit caller inputs;
 * this class deliberately does not infer player-layer visitation or reinterpret
 * b2 as a semantic passability/collision decision.
 */
public final class LegacyCollisionOutcomeComposer {
    private LegacyCollisionOutcomeComposer() { }

    public static LegacyCollisionEvaluationResult compose(
            LegacyCollisionTrace trace,
            boolean localCollisionFlag,
            boolean playerLayerEvaluated) {
        if (trace == null) {
            throw new IllegalArgumentException("trace is required");
        }

        LegacyCollisionEvaluationResult.Completion completion =
                trace.isHelperReturnedNormally()
                        ? LegacyCollisionEvaluationResult.Completion.NORMAL_RETURN
                        : LegacyCollisionEvaluationResult.Completion.EARLY_RETURN;

        return new LegacyCollisionEvaluationResult(
                completion,
                trace.isReturnFlagB2(),
                localCollisionFlag,
                trace.isInteractionFlagB3(),
                playerLayerEvaluated,
                !trace.isPlayerSnapshotWrittenBack());
    }
}
