package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyType19ScriptScanPlannerTest {
    private static final class Grid implements LegacyType19ScriptScanPlanner.Grid {
        final int w,h; final int[][] player,foreground;
        Grid(int w,int h){this.w=w;this.h=h;player=new int[w][h];foreground=new int[w][h];
            for(int x=0;x<w;x++) for(int y=0;y<h;y++){player[x][y]=-1;foreground[x][y]=0;}}
        public int width(){return w;} public int height(){return h;}
        public int playerLayerRawAt(int x,int y){return player[x][y];}
        public int foregroundRawAt(int x,int y){return foreground[x][y];}
    }
    @Test public void plansAnchorAndOnlyRowsAfterAnchorForClearing(){
        Grid g=new Grid(10,8); g.player[5][3]=39; g.foreground[2][4]=19; g.foreground[2][5]=0x1213;
        LegacyType19ScriptScanPlanner.Plan p=LegacyType19ScriptScanPlanner.plan(2,5,19,false,g);
        assertTrue(p.isReady()); assertEquals(5,p.getAnchorX()); assertEquals(3,p.getAnchorY());
        assertEquals(1,p.getForegroundRowsToClear().size()); assertEquals(Integer.valueOf(4),p.getForegroundRowsToClear().get(0));
    }
    @Test public void probeModeDoesNotScanOrPlanMutation(){
        Grid g=new Grid(10,8);
        assertEquals(LegacyType19ScriptScanPlanner.Status.PROBE_SKIPPED,
            LegacyType19ScriptScanPlanner.plan(2,5,19,true,g).getStatus());
    }
    @Test public void rejectsAnchorColumnOutsideStage(){
        Grid g=new Grid(5,8);
        assertEquals(LegacyType19ScriptScanPlanner.Status.ANCHOR_COLUMN_OUT_OF_BOUNDS,
            LegacyType19ScriptScanPlanner.plan(2,5,19,false,g).getStatus());
    }
    @Test public void reportsMissingPlayerLayerAnchor(){
        Grid g=new Grid(10,8);
        assertEquals(LegacyType19ScriptScanPlanner.Status.ANCHOR_NOT_FOUND,
            LegacyType19ScriptScanPlanner.plan(2,5,19,false,g).getStatus());
    }
    @Test public void refusesRunThatReachesStageBottomWithoutTerminator(){
        Grid g=new Grid(10,5); g.player[5][2]=39; g.foreground[2][3]=19; g.foreground[2][4]=19;
        LegacyType19ScriptScanPlanner.Plan p=LegacyType19ScriptScanPlanner.plan(2,4,19,false,g);
        assertEquals(LegacyType19ScriptScanPlanner.Status.RUN_NOT_TERMINATED,p.getStatus());
        assertTrue(p.getForegroundRowsToClear().isEmpty());
    }
    @Test public void rejectsOtherForegroundTypes(){
        Grid g=new Grid(10,8);
        assertEquals(LegacyType19ScriptScanPlanner.Status.NOT_TYPE_19,
            LegacyType19ScriptScanPlanner.plan(2,5,0x1314,false,g).getStatus());
    }
}