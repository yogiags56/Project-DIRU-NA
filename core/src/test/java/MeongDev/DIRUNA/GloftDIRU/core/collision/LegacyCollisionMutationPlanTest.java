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
    @Test public void planCarriesPlayerAnimationFlagAndCacheStateIntents() {
        LegacyCollisionMutationPlan plan=LegacyCollisionMutationPlan.builder(false)
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.PENDING_DIRECTION,2,0))
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.ANIMATION,
                        LegacyCollisionStateIntent.Field.ANIMATION_ID,8,18))
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.MOVEMENT_FLAGS,
                        LegacyCollisionStateIntent.Field.BLOCKED_FLAG,0,1))
                .addState(LegacyCollisionStateIntent.cell(
                        LegacyCollisionStateIntent.Channel.FOREGROUND_CACHE,
                        LegacyCollisionStateIntent.Field.CACHE_VALUE,7,9,0,35))
                .build();
        assertEquals(4,plan.getStateIntents().size());
        assertEquals(LegacyCollisionStateIntent.Channel.ANIMATION,
                plan.getStateIntents().get(1).getChannel());
        assertTrue(plan.getStateIntents().get(3).isCoordinateScoped());
        assertEquals(7,plan.getStateIntents().get(3).getX());
    }

    @Test(expected=IllegalStateException.class)
    public void probeModeCannotAcceptStateWrites() {
        LegacyCollisionMutationPlan.builder(true).addState(
                LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.TILE_X,4,5));
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidChannelFieldCombinationIsRejected() {
        LegacyCollisionStateIntent.scalar(
                LegacyCollisionStateIntent.Channel.ANIMATION,
                LegacyCollisionStateIntent.Field.TILE_X,0,1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void repeatedStateDestinationIsRejected() {
        LegacyCollisionMutationPlan.builder(false)
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.TILE_X,4,5))
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.TILE_X,5,6));
    }

}
