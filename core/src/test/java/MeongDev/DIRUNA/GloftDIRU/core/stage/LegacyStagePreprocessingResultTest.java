package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyStagePreprocessingResultTest {
    private static StageGrid stage() {
        return new StageGrid(2,2,new int[]{1,2,3,4},new int[]{5,6,7,8},
                new int[]{9,10,11,12});
    }

    @Test public void detachesEveryInputAndReturnedArray() {
        int[] background={20,21,22,23};
        byte[] b={1,2,3,4},c={24,30,48,0};
        LegacyStagePreprocessingResult result=
                new LegacyStagePreprocessingResult(stage(),background,b,c);
        background[0]=99;b[0]=99;c[0]=99;
        assertEquals(20,result.backgroundRuntimeAt(0,0));
        assertEquals(1,result.bByteAt(0,0));
        assertEquals(24,result.cByteAt(0,0));
        int[] copy=result.copyBackgroundRuntime();copy[0]=88;
        byte[] bCopy=result.copyBByteArrArr();bCopy[0]=88;
        byte[] cCopy=result.copyCByteArrArr();cCopy[0]=88;
        assertEquals(20,result.backgroundRuntimeAt(0,0));
        assertEquals(1,result.bByteAt(0,0));
        assertEquals(24,result.cByteAt(0,0));
        StageGrid detached=result.transformedStage();
        int[] player=detached.copyPlayerLayer();player[0]=77;
        assertEquals(1,result.transformedStage().playerAt(0,0));
    }

    @Test public void validatesAllCellArrayLengths() {
        try { new LegacyStagePreprocessingResult(stage(),new int[3],new byte[4],new byte[4]);fail(); }
        catch(IllegalArgumentException expected) { }
        try { new LegacyStagePreprocessingResult(stage(),new int[4],new byte[3],new byte[4]);fail(); }
        catch(IllegalArgumentException expected) { }
        try { new LegacyStagePreprocessingResult(stage(),new int[4],new byte[4],new byte[3]);fail(); }
        catch(IllegalArgumentException expected) { }
    }

    @Test public void checksCoordinatesAndPreservesSignedBytes() {
        LegacyStagePreprocessingResult result=new LegacyStagePreprocessingResult(
                stage(),new int[4],new byte[]{-1,0,0,0},new byte[]{0,-128,0,0});
        assertEquals(-1,result.bByteAt(0,0));
        assertEquals(-128,result.cByteAt(1,0));
        try { result.backgroundRuntimeAt(2,0);fail(); }
        catch(IndexOutOfBoundsException expected) { }
    }
}
