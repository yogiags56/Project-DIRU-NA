package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionMutationApplierTest {
    private static final class Target implements LegacyCollisionMutationTarget {
        int gridValue=7, stateValue=2;
        int commits;
        boolean owner=true, commitAccepted=true;
        LegacyCollisionMutationPlan committed;
        public int width(){return 4;}
        public int height(){return 4;}
        public boolean isOwnerThread(){return owner;}
        public int readGrid(LegacyCollisionMutationIntent.Layer l,int x,int y){return gridValue;}
        public int readState(LegacyCollisionStateIntent.Channel c,
                LegacyCollisionStateIntent.Field f,boolean scoped,int x,int y){return stateValue;}
        public boolean commitAtomically(LegacyCollisionMutationPlan p){
            commits++; committed=p; return commitAccepted;
        }
    }
    private static LegacyCollisionMutationPlan plan(int x,int expectedGrid,int expectedState) {
        return LegacyCollisionMutationPlan.builder(false)
                .add(new LegacyCollisionMutationIntent(
                        LegacyCollisionMutationIntent.Layer.FOREGROUND,x,1,expectedGrid,9))
                .addState(LegacyCollisionStateIntent.scalar(
                        LegacyCollisionStateIntent.Channel.PLAYER,
                        LegacyCollisionStateIntent.Field.TILE_X,expectedState,3))
                .build();
    }

    @Test public void preflightSuccessCallsSingleAtomicCommit() {
        Target t=new Target();
        assertEquals(LegacyCollisionMutationApplier.Result.APPLIED,
                LegacyCollisionMutationApplier.apply(plan(1,7,2),t));
        assertEquals(1,t.commits);
        assertNotNull(t.committed);
    }

    @Test public void outOfBoundsRejectsBeforeCommit() {
        Target t=new Target();
        assertEquals(LegacyCollisionMutationApplier.Result.OUT_OF_BOUNDS,
                LegacyCollisionMutationApplier.apply(plan(4,7,2),t));
        assertEquals(0,t.commits);
    }

    @Test public void gridExpectedValueMismatchRejectsBeforeCommit() {
        Target t=new Target(); t.gridValue=8;
        assertEquals(LegacyCollisionMutationApplier.Result.EXPECTED_VALUE_MISMATCH,
                LegacyCollisionMutationApplier.apply(plan(1,7,2),t));
        assertEquals(0,t.commits);
    }

    @Test public void stateExpectedValueMismatchRejectsBeforeCommit() {
        Target t=new Target(); t.stateValue=99;
        assertEquals(LegacyCollisionMutationApplier.Result.EXPECTED_VALUE_MISMATCH,
                LegacyCollisionMutationApplier.apply(plan(1,7,2),t));
        assertEquals(0,t.commits);
    }

    @Test public void commitRejectionIsReportedWithoutRetry() {
        Target t=new Target(); t.commitAccepted=false;
        assertEquals(LegacyCollisionMutationApplier.Result.COMMIT_REJECTED,
                LegacyCollisionMutationApplier.apply(plan(1,7,2),t));
        assertEquals(1,t.commits);
    }

    @Test(expected=IllegalStateException.class)
    public void wrongThreadIsRejectedBeforeReads() {
        Target t=new Target(); t.owner=false;
        LegacyCollisionMutationApplier.apply(plan(1,7,2),t);
    }

    @Test public void probeIsNoOpAndNeverCallsTargetCommit() {
        Target t=new Target();
        LegacyCollisionMutationPlan p=LegacyCollisionMutationPlan.builder(true).build();
        assertEquals(LegacyCollisionMutationApplier.Result.PROBE_NO_OP,
                LegacyCollisionMutationApplier.apply(p,t));
        assertEquals(0,t.commits);
    }
}
