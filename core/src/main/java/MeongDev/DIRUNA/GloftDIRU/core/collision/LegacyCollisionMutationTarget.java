package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Owner-thread adapter required by the collision applier.
 * Implementations must make commitAtomically all-or-nothing and must not retain
 * or mutate the supplied plan.
 */
public interface LegacyCollisionMutationTarget {
    int width();
    int height();
    boolean isOwnerThread();
    int readGrid(LegacyCollisionMutationIntent.Layer layer,int x,int y);
    int readState(LegacyCollisionStateIntent.Channel channel,
                  LegacyCollisionStateIntent.Field field,boolean coordinateScoped,int x,int y);
    boolean commitAtomically(LegacyCollisionMutationPlan plan);
}
