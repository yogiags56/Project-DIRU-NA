package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyStagePreprocessingResultTest {
    private static StageGrid stage() {
        return new StageGrid(2,2,new int[]{1,2,3,4},new int[]{5,6,7,8},new int[]{9,10,11,12});
    }

    @Test public void defensivelyCopiesAllLayersAndRuntimeGrids() {
        int[] background={5,6,7,8};
        byte[] b={1,2,3,4}, c={24,48,0,127};
        LegacyStagePreprocessingResult result=
                new LegacyStagePreprocessingResult(stage(),background,b,c);
        background[0]=99;b[0]=99;c[0]=99;
        assertEquals(5,result.backgroundRuntimeAt(0,0));
        assertEquals(1,result.bByteAt(0,0));
        assertEquals(24,result.cByteAt(0,0));
        int[] copy=result.copyBackgroundRuntime();copy[0]=77;
        assertEquals(5,result.backgroundRuntimeAt(0,0));
        StageGrid transformed=result.transformedStage();
        assertEquals(9,transformed.foregroundAt(0,0));
    }

    @Test public void rejectsWrongRuntimeGridLengths() {
        try {
            new LegacyStagePreprocessingResult(stage(),new int[3],new byte[4],new byte[4]);
            fail();
        } catch(IllegalArgumentException expected) { }
        try {
            new LegacyStagePreprocessingResult(stage(),new int[4],new byte[4],new byte[3]);
            fail();
        } catch(IllegalArgumentException expected) { }
    }

    @Test public void boundsChecksRuntimeGridAccess() {
        LegacyStagePreprocessingResult result=
                new LegacyStagePreprocessingResult(stage(),new int[4],new byte[4],new byte[4]);
        try { result.cByteAt(2,0);fail(); }
        catch(IndexOutOfBoundsException expected) { }
    }
}
