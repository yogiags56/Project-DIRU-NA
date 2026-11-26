package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionMutationPlanTest {
    private static LegacyCollisionMutationIntent intent(
            LegacyCollisionMutationIntent.Layer layer,int x,int y,int before,int after) {
        return new LegacyCollisionMutationIntent(layer,x,y,before,after);
    }

    @Test public void planIsImmutableAndPreservesOrderedIntents() {
        LegacyCollisionMutationPlan.Builder builder=LegacyCollisionMutationPlan.builder(false);
        builder.add(intent(LegacyCollisionMutationIntent.Layer.PLAYER,4,5,-1,48));
        builder.add(intent(LegacyCollisionMutationIntent.Layer.PLAYER,5,5,48,-1));
        LegacyCollisionMutationPlan plan=builder.build();
        assertEquals(2,plan.getIntents().size());
        assertEquals(4,plan.getIntents().get(0).getX());
        assertEquals(48,plan.getIntents().get(0).getNewValue());
        try { plan.getIntents().clear(); fail("list must be immutable"); }
        catch(UnsupportedOperationException expected) { }
    }

    @Test(expected=IllegalStateException.class)
    public void probeModeCannotAcceptWorldWrites() {
        LegacyCollisionMutationPlan.builder(true).add(
                intent(LegacyCollisionMutationIntent.Layer.PLAYER,4,5,-1,48));
    }

    @Test(expected=IllegalArgumentException.class)
    public void conflictingOrRepeatedDestinationIsRejected() {
        LegacyCollisionMutationPlan.builder(false)
                .add(intent(LegacyCollisionMutationIntent.Layer.PLAYER,4,5,-1,48))
                .add(intent(LegacyCollisionMutationIntent.Layer.PLAYER,4,5,48,-1));
    }

    @Test public void sameCoordinateOnDifferentLayersIsDistinct() {
        LegacyCollisionMutationPlan plan=LegacyCollisionMutationPlan.builder(false)
                .add(intent(LegacyCollisionMutationIntent.Layer.PLAYER,4,5,-1,48))
                .add(intent(LegacyCollisionMutationIntent.Layer.FOREGROUND,4,5,7,0))
                .build();
        assertEquals(2,plan.getIntents().size());
    }

    @Test(expected=IllegalStateException.class)
    public void builderCannotBeReusedAfterBuild() {
        LegacyCollisionMutationPlan.Builder b=LegacyCollisionMutationPlan.builder(false);
        b.build();
        b.add(intent(LegacyCollisionMutationIntent.Layer.PLAYER,1,1,0,1));
    }
}
