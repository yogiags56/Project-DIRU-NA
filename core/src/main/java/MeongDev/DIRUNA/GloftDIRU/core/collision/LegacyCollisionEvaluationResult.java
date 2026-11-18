package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Immutable record of the distinct result channels visible in legacy
 * aBoolean(dx,dy,probe). This is a data model, not an evaluator.
 */
public final class LegacyCollisionEvaluationResult {
    public enum Completion {
        NORMAL_RETURN,
        EARLY_RETURN
    }

    private final Completion completion;
    private final boolean returnValue;
    private final boolean localCollisionFlag;
    private final boolean auxiliaryBranchFlag;
    private final boolean playerLayerEvaluated;
    private final boolean stateCommitSuppressed;

    public LegacyCollisionEvaluationResult(
            Completion completion,
            boolean returnValue,
            boolean localCollisionFlag,
            boolean auxiliaryBranchFlag,
            boolean playerLayerEvaluated,
            boolean stateCommitSuppressed) {
        if (completion == null) throw new IllegalArgumentException("completion is required");
        if (completion == Completion.EARLY_RETURN && returnValue) {
            throw new IllegalArgumentException("the traced early-return path returns false");
        }
        this.completion=completion;
        this.returnValue=returnValue;
        this.localCollisionFlag=localCollisionFlag;
        this.auxiliaryBranchFlag=auxiliaryBranchFlag;
        this.playerLayerEvaluated=playerLayerEvaluated;
        this.stateCommitSuppressed=stateCommitSuppressed;
    }

    public Completion getCompletion(){return completion;}
    public boolean isEarlyReturn(){return completion==Completion.EARLY_RETURN;}
    public boolean getReturnValue(){return returnValue;}
    public boolean getLocalCollisionFlag(){return localCollisionFlag;}
    public boolean getAuxiliaryBranchFlag(){return auxiliaryBranchFlag;}
    public boolean wasPlayerLayerEvaluated(){return playerLayerEvaluated;}
    public boolean isStateCommitSuppressed(){return stateCommitSuppressed;}
}
