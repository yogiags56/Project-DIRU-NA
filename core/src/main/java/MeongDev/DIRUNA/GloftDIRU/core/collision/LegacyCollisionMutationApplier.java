package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Preflight validator and atomic-commit coordinator for collision mutation plans. */
public final class LegacyCollisionMutationApplier {
    public enum Result { APPLIED, PROBE_NO_OP, OUT_OF_BOUNDS, EXPECTED_VALUE_MISMATCH, COMMIT_REJECTED }
    private LegacyCollisionMutationApplier(){}

    public static Result apply(LegacyCollisionMutationPlan plan,LegacyCollisionMutationTarget target) {
        if(plan==null || target==null) throw new IllegalArgumentException("plan and target are required");
        if(!target.isOwnerThread()) throw new IllegalStateException("collision mutation must run on owner thread");
        if(plan.isProbeMode()) {
            if(!plan.isEmpty()) throw new IllegalArgumentException("probe plan contains mutations");
            return Result.PROBE_NO_OP;
        }
        int width=target.width(),height=target.height();
        if(width<=0 || height<=0) throw new IllegalStateException("target dimensions must be positive");

        for(LegacyCollisionMutationIntent i:plan.getIntents()) {
            if(!inside(i.getX(),i.getY(),width,height)) return Result.OUT_OF_BOUNDS;
            if(target.readGrid(i.getLayer(),i.getX(),i.getY())!=i.getExpectedValue())
                return Result.EXPECTED_VALUE_MISMATCH;
        }
        for(LegacyCollisionStateIntent i:plan.getStateIntents()) {
            if(i.isCoordinateScoped()) {
                if(!inside(i.getX(),i.getY(),width,height)) return Result.OUT_OF_BOUNDS;
            }
            int current=target.readState(i.getChannel(),i.getField(),
                    i.isCoordinateScoped(),i.getX(),i.getY());
            if(current!=i.getExpectedValue()) return Result.EXPECTED_VALUE_MISMATCH;
        }
        return target.commitAtomically(plan) ? Result.APPLIED : Result.COMMIT_REJECTED;
    }

    private static boolean inside(int x,int y,int width,int height) {
        return x>=0 && y>=0 && x<width && y<height;
    }
}
