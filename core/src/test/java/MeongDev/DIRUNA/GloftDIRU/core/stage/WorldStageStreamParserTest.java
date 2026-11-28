package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WorldStageStreamParserTest {
    private static byte[] stream(int prefix,int[][] dimensions,byte[][][] layers) {
        int size=2;
        for(int i=0;i<dimensions.length;i++) size+=4+3*dimensions[i][0]*dimensions[i][1];
        byte[] out=new byte[size];int p=0;
        out[p++]=(byte)prefix;out[p++]=(byte)dimensions.length;
        for(int i=0;i<dimensions.length;i++) {
            int w=dimensions[i][0],h=dimensions[i][1];
            out[p++]=(byte)w;out[p++]=(byte)(w>>>8);
            out[p++]=(byte)h;out[p++]=(byte)(h>>>8);
            for(int layer=0;layer<3;layer++)
                for(int j=0;j<w*h;j++) out[p++]=layers[i][layer][j];
        }
        return out;
    }
    private static byte[][] layers(byte a,byte b,byte c,int cells) {
        byte[][] result=new byte[3][];
        result[0]=new byte[cells];result[1]=new byte[cells];result[2]=new byte[cells];
        java.util.Arrays.fill(result[0],a);java.util.Arrays.fill(result[1],b);
        java.util.Arrays.fill(result[2],c);return result;
    }

    @Test public void parsesSelectedRecordAndSkipsOtherRecordsExactly() {
        byte[] bytes=stream(1,new int[][]{{2,1},{2,2}},
                new byte[][][]{layers((byte)9,(byte)8,(byte)7,2),
                        layers((byte)1,(byte)2,(byte)3,4)});
        WorldStageStreamParser.ParsedStage parsed=new WorldStageStreamParser().parse(bytes,1);
        assertEquals(1,parsed.prefix());assertEquals(2,parsed.stageCount());
        StageGrid grid=parsed.grid();
        assertEquals(2,grid.width());assertEquals(2,grid.height());
        assertEquals(1,grid.playerAt(0,0));assertEquals(2,grid.backgroundAt(1,1));
        assertEquals(3,grid.foregroundAt(0,1));
    }

    @Test public void preservesSignedByteWidening() {
        byte[][] data=layers((byte)0,(byte)0,(byte)0,1);
        data[0][0]=(byte)0xFF;data[1][0]=(byte)0x80;data[2][0]=(byte)0x7F;
        StageGrid grid=new WorldStageStreamParser().parse(
                stream(1,new int[][]{{1,1}},new byte[][][]{data}),0).grid();
        assertEquals(-1,grid.playerAt(0,0));
        assertEquals(-128,grid.backgroundAt(0,0));
        assertEquals(127,grid.foregroundAt(0,0));
    }

    @Test public void rejectsInvalidSelectedIndex() {
        try { new WorldStageStreamParser().parse(stream(1,new int[][]{{1,1}},
                new byte[][][]{layers((byte)0,(byte)0,(byte)0,1)}),1);fail(); }
        catch(IllegalArgumentException expected) { }
    }

    @Test public void rejectsTruncatedAndTrailingPayloads() {
        byte[] valid=stream(1,new int[][]{{1,1}},
                new byte[][][]{layers((byte)0,(byte)0,(byte)0,1)});
        byte[] shortData=java.util.Arrays.copyOf(valid,valid.length-1);
        try { new WorldStageStreamParser().parse(shortData,0);fail(); }
        catch(IllegalArgumentException expected) { }
        byte[] trailing=java.util.Arrays.copyOf(valid,valid.length+1);
        try { new WorldStageStreamParser().parse(trailing,0);fail(); }
        catch(IllegalArgumentException expected) { }
    }

    @Test public void rejectsDimensionsBeyondConfiguredLimits() {
        byte[] bytes=stream(1,new int[][]{{3,2}},
                new byte[][][]{layers((byte)0,(byte)0,(byte)0,6)});
        try { new WorldStageStreamParser(new WorldStageStreamParser.Limits(2,10,100))
                .parse(bytes,0);fail(); }
        catch(IllegalArgumentException expected) { }
    }

    @Test public void rejectsZeroStageCountAndZeroDimensions() {
        try { new WorldStageStreamParser().parse(new byte[]{1,0},0);fail(); }
        catch(IllegalArgumentException expected) { }
        try { new WorldStageStreamParser().parse(new byte[]{1,1,0,0,1,0},0);fail(); }
        catch(IllegalArgumentException expected) { }
    }
}
