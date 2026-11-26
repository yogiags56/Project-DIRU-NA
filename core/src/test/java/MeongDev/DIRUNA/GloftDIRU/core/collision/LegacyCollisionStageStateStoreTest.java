package MeongDev.DIRUNA.GloftDIRU.core.collision;

import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionStageStateStoreTest {
    private static StageGrid stage() {
        return new StageGrid(2,2,new int[]{1,2,3,4},new int[]{5,6,7,8},new int[]{9,10,11,12});
    }

    @Test public void commitsGridPlayerStateAndCacheAsOnePublishedRoot() {
        LegacyCollisionStageStateStore store=new LegacyCollisionStageStateStore(stage());
        StageGrid before=store.stageSnapshot();
        LegacyCollisionMutationPlan plan=LegacyCollisionMutationPlan.builder(false)
                .add(new LegacyCollisionMutationIntent(
                        LegacyCollisionMutationIntent.Layer.PLAYER,1,0,2,48))
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.TILE_X,0,1))
                .addState(LegacyCollisionStateIntent.cell(
                        LegacyCollisionStateIntent.Channel.FOREGROUND_CACHE,
                        LegacyCollisionStateIntent.Field.CACHE_VALUE,0,1,0,35))
                .build();

        assertEquals(LegacyCollisionMutationApplier.Result.APPLIED,
                LegacyCollisionMutationApplier.apply(plan,store));
        assertEquals(1L,store.version());
        assertEquals(48,store.stageSnapshot().playerAt(1,0));
        assertEquals(2,before.playerAt(1,0));
        assertEquals(1,store.readStateValue(LegacyCollisionStateIntent.Channel.PLAYER,
                LegacyCollisionStateIntent.Field.TILE_X));
        assertEquals(35,store.foregroundCacheAt(0,1));
        assertEquals(35,store.readState(LegacyCollisionStateIntent.Channel.FOREGROUND_CACHE,
                LegacyCollisionStateIntent.Field.CACHE_VALUE,true,0,1));
    }

    @Test public void staleStateRejectsEntireCommitWithoutPublishingGridWrite() {
        LegacyCollisionStageStateStore store=new LegacyCollisionStageStateStore(stage());
        LegacyCollisionMutationPlan plan=LegacyCollisionMutationPlan.builder(false)
                .add(new LegacyCollisionMutationIntent(
                        LegacyCollisionMutationIntent.Layer.PLAYER,1,0,2,48))
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.TILE_X,99,1))
                .build();
        assertEquals(LegacyCollisionMutationApplier.Result.EXPECTED_VALUE_MISMATCH,
                LegacyCollisionMutationApplier.apply(plan,store));
        assertEquals(2,store.stageSnapshot().playerAt(1,0));
        assertEquals(0L,store.version());
    }

    @Test public void invalidCacheCoordinatesRejectBeforeCommit() {
        LegacyCollisionStageStateStore store=new LegacyCollisionStageStateStore(stage());
        LegacyCollisionMutationPlan plan=LegacyCollisionMutationPlan.builder(false)
                .addState(LegacyCollisionStateIntent.cell(
                        LegacyCollisionStateIntent.Channel.FOREGROUND_CACHE,
                        LegacyCollisionStateIntent.Field.CACHE_VALUE,2,0,0,7))
                .build();
        assertEquals(LegacyCollisionMutationApplier.Result.OUT_OF_BOUNDS,
                LegacyCollisionMutationApplier.apply(plan,store));
        assertEquals(0L,store.version());
    }

    @Test public void probePlanNeverPublishesANewRoot() {
        LegacyCollisionStageStateStore store=new LegacyCollisionStageStateStore(stage());
        assertEquals(LegacyCollisionMutationApplier.Result.PROBE_NO_OP,
                LegacyCollisionMutationApplier.apply(
                        LegacyCollisionMutationPlan.builder(true).build(),store));
        assertEquals(0L,store.version());
    }

    @Test public void commitRejectsWrongThread() throws Exception {
        final LegacyCollisionStageStateStore store=new LegacyCollisionStageStateStore(stage());
        final LegacyCollisionMutationPlan plan=LegacyCollisionMutationPlan.builder(false)
                .add(new LegacyCollisionMutationIntent(
                        LegacyCollisionMutationIntent.Layer.PLAYER,1,0,2,48))
                .build();
        final Throwable[] failure=new Throwable[1];
        Thread worker=new Thread(new Runnable(){public void run(){
            try { store.commitAtomically(plan); } catch(Throwable t) { failure[0]=t; }
        }});
        worker.start(); worker.join();
        assertTrue(failure[0] instanceof IllegalStateException);
        assertEquals(0L,store.version());
    }
}
