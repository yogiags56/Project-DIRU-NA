package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyCollisionNeighborhoodCaptureTest {
    private static final class FakeGrid implements LegacyCollisionNeighborhoodCapture.Grid {
        private final int width, height;
        private final int[][] foreground, player, metadata, auxiliary;
        private final boolean auxiliaryPresent;
        FakeGrid(int width, int height, boolean auxiliaryPresent) {
            this.width=width; this.height=height; this.auxiliaryPresent=auxiliaryPresent;
            foreground=new int[width][height]; player=new int[width][height];
            metadata=new int[width][height]; auxiliary=new int[width][height];
            for(int x=0;x<width;x++) for(int y=0;y<height;y++) player[x][y]=-1;
        }
        public int width(){return width;} public int height(){return height;}
        public int foregroundRawAt(int x,int y){return foreground[x][y];}
        public int playerLayerRawAt(int x,int y){return player[x][y];}
        public int packedMetadataRawAt(int x,int y){return metadata[x][y];}
        public boolean hasAuxiliaryGrid(){return auxiliaryPresent;}
        public int auxiliaryRawAt(int x,int y){return auxiliary[x][y];}
    }
    private static LegacyCollisionNeighborhoodCapture capture(FakeGrid g) {
        return new LegacyCollisionNeighborhoodCapture(g.width(),g.height());
    }

    @Test public void capturesOnlyTargetForSimpleForegroundBranch() {
        FakeGrid g=new FakeGrid(10,10,false);
        g.foreground[4][5]=0x1302;
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_2,g,3,5,1,0);
        assertTrue(r.isComplete());
        assertEquals(0x1302,r.getSnapshot().get(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET).getForegroundRaw());
        assertFalse(r.getSnapshot().contains(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS));
    }

    @Test public void capturesType48GeometryUsingTargetMetadataBit() {
        FakeGrid g=new FakeGrid(12,12,false);
        g.metadata[4][4]=0x8;
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_48,g,3,4,1,0);
        assertTrue(r.isComplete());
        assertEquals(5,r.getSnapshot().get(
                LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_DESTINATION_A).getX());
        assertEquals(5,r.getSnapshot().get(
                LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_DESTINATION_B).getY());
    }

    @Test public void rejectsOutOfBoundsTwoStepBeforeGridRead() {
        FakeGrid g=new FakeGrid(5,5,false);
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_48,g,3,2,1,0);
        assertEquals(LegacyCollisionNeighborhoodCapture.Status.OUT_OF_BOUNDS,r.getStatus());
        assertNull(r.getSnapshot());
    }

    @Test public void capturesType19AnchorScanAndDownwardTerminator() {
        FakeGrid g=new FakeGrid(12,12,false);
        g.foreground[2][3]=19;
        g.foreground[2][4]=19;
        g.foreground[2][5]=0;
        g.player[5][3]=39;
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_19,g,1,3,1,0);
        assertTrue(r.isComplete());
        assertEquals(39,r.getSnapshot().getType19Anchor().getPlayerLayerRaw());
        assertEquals(2,r.getSnapshot().getType19ForegroundRun().size());
        assertEquals(0,r.getSnapshot().getType19ForegroundRun().get(1).getForegroundRaw());
    }

    @Test public void type19MissingAnchorFailsAtTopEdge() {
        FakeGrid g=new FakeGrid(12,6,false);
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_19,g,1,0,1,0);
        assertEquals(LegacyCollisionNeighborhoodCapture.Status.TYPE19_ANCHOR_NOT_FOUND,
                r.getStatus());
        assertNull(r.getSnapshot());
    }

    @Test public void type19UnterminatedForegroundRunFailsAtBottomEdge() {
        FakeGrid g=new FakeGrid(12,8,false);
        g.player[5][2]=39;
        g.foreground[2][3]=19;
        g.foreground[2][4]=19;
        g.foreground[2][5]=19;
        g.foreground[2][6]=19;
        g.foreground[2][7]=19;
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_19,g,1,2,1,0);
        assertEquals(LegacyCollisionNeighborhoodCapture.Status.OUT_OF_BOUNDS,r.getStatus());
    }

    @Test public void capturesAuxiliaryGridPresenceSeparatelyFromValue() {
        FakeGrid g=new FakeGrid(8,8,true);
        g.auxiliary[3][3]=0;
        LegacyCollisionNeighborhoodCapture.Result r=capture(g).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,g,2,3,1,0);
        LegacyCollisionNeighborhoodSnapshot.CellSample sample =
                r.getSnapshot().get(LegacyCollisionNeighborhoodSnapshot.Role.TARGET);
        assertTrue(sample.isAuxiliaryGridPresent());
        assertEquals(0,sample.getAuxiliaryRaw());
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsGridDimensionsThatDifferFromDeclaredBounds() {
        new LegacyCollisionNeighborhoodCapture(9,9).capture(
                LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_7,
                new FakeGrid(8,8,false),2,2,1,0);
    }
}
