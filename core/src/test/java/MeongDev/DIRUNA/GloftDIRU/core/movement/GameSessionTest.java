package MeongDev.DIRUNA.GloftDIRU.core.movement;

import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyInputMasks;
import MeongDev.DIRUNA.GloftDIRU.core.input.LegacyInputDispatcher;
import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class GameSessionTest {
    private static StageGrid stage() {
        return new StageGrid(3,3,new int[9],new int[9],new int[9]);
    }
    private static LegacyInputDispatcher.Context context() {
        return new LegacyInputDispatcher.Context(false,0,0,false,1,false,false,false,0);
    }
    @Test public void missingCollisionPolicyFailsClosedWithoutCoordinateMutation() {
        GameSession session=new GameSession(stage(),1,1,null);
        GameSession.TickResult result=session.tick(LegacyInputMasks.RIGHT,context());
        assertEquals(GameSession.TickKind.UNSUPPORTED,result.kind());
        assertEquals(1,session.renderSnapshot().player().tileX());
        assertEquals(1,session.renderSnapshot().player().tileY());
        assertEquals(0,session.renderSnapshot().player().interpolationProgress());
    }
    @Test public void blockedPolicyLeavesPlayerAndStageUnchanged() {
        StageGrid grid=stage();
        GameSession session=new GameSession(grid,1,1,(s,fx,fy,tx,ty)->GameSession.MoveDecision.BLOCKED);
        assertEquals(GameSession.TickKind.BLOCKED,session.tick(LegacyInputMasks.RIGHT,context()).kind());
        assertEquals(1,session.renderSnapshot().player().tileX());
        assertEquals(0,session.renderSnapshot().stage().playerAt(2,1));
    }
    @Test public void acceptedMoveCommitsTileAndStartsInterpolationAtomically() {
        GameSession session=new GameSession(stage(),1,1,(s,fx,fy,tx,ty)->GameSession.MoveDecision.ACCEPTED);
        assertEquals(GameSession.TickKind.MOVEMENT_STARTED,session.tick(LegacyInputMasks.RIGHT,context()).kind());
        PlayerMotionState player=session.renderSnapshot().player();
        assertEquals(2,player.tileX());
        assertEquals(1,player.tileY());
        assertEquals(18,player.interpolationProgress());
        assertEquals(2,player.activeDirection());
    }
    @Test public void activeInterpolationAdvancesBeforeNewInputIsDispatched() {
        GameSession session=new GameSession(stage(),1,1,(s,fx,fy,tx,ty)->GameSession.MoveDecision.ACCEPTED);
        session.tick(LegacyInputMasks.RIGHT,context());
        assertEquals(GameSession.TickKind.MOVEMENT_ADVANCED,session.tick(LegacyInputMasks.DOWN,context()).kind());
        PlayerMotionState player=session.renderSnapshot().player();
        assertEquals(2,player.tileX());
        assertEquals(1,player.tileY());
        assertEquals(12,player.interpolationProgress());
    }
    @Test public void edgeMoveIsBlockedBeforePolicyIsCalled() {
        final boolean[] called={false};
        GameSession session=new GameSession(stage(),2,1,(s,fx,fy,tx,ty)->{called[0]=true;return GameSession.MoveDecision.ACCEPTED;});
        assertEquals(GameSession.TickKind.BLOCKED,session.tick(LegacyInputMasks.RIGHT,context()).kind());
        assertFalse(called[0]);
        assertEquals(2,session.renderSnapshot().player().tileX());
    }

    @Test public void stableViewIsAvailableAtSettledMovementBoundary() {
        GameSession session=new GameSession(stage(),1,1,null);
        GameplayRenderSnapshot stable=session.captureStableView();
        assertNotNull(stable);
        assertEquals(1,stable.player().tileX());
        assertEquals(1,stable.player().tileY());
        assertEquals(0,stable.player().interpolationProgress());
    }

    @Test public void stableViewRejectsActiveInterpolationUntilMovementSettles() {
        GameSession session=new GameSession(stage(),1,1,
                (s,fx,fy,tx,ty)->GameSession.MoveDecision.ACCEPTED);
        session.tick(LegacyInputMasks.RIGHT,context());
        try {
            session.captureStableView();
            fail("capture must reject active movement interpolation");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("interpolation"));
        }
        session.tick(0,context());
        session.tick(0,context());
        session.tick(0,context());
        GameplayRenderSnapshot stable=session.captureStableView();
        assertEquals(2,stable.player().tileX());
        assertEquals(0,stable.player().interpolationProgress());
    }

    @Test public void stableViewEnforcesGameSessionOwnerThread() throws Exception {
        GameSession session=new GameSession(stage(),1,1,null);
        final Throwable[] failure={null};
        Thread other=new Thread(()->{
            try { session.captureStableView(); }
            catch(Throwable t) { failure[0]=t; }
        });
        other.start(); other.join();
        assertTrue(failure[0] instanceof IllegalStateException);
    }

    @Test public void ownerThreadRestrictionRejectsCrossThreadMutation() throws Exception {
        GameSession session=new GameSession(stage(),1,1,(s,fx,fy,tx,ty)->GameSession.MoveDecision.ACCEPTED);
        final Throwable[] failure={null};
        Thread other=new Thread(()->{try{session.tick(0,context());}catch(Throwable t){failure[0]=t;}});
        other.start(); other.join();
        assertTrue(failure[0] instanceof IllegalStateException);
    }
}
