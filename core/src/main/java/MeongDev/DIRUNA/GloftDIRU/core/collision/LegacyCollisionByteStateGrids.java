package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Raw per-cell byte-state grids from the legacy runtime.
 * Values remain bytes (including signed values); no timer semantics are inferred here.
 */
public final class LegacyCollisionByteStateGrids {
    private final int width,height;
    private final byte[] bByteArrArr,cByteArrArr;

    public LegacyCollisionByteStateGrids(int width,int height,byte[] bByteArrArr,byte[] cByteArrArr) {
        if(width<=0 || height<=0) throw new IllegalArgumentException("dimensions must be positive");
        int count=Math.multiplyExact(width,height);
        if(bByteArrArr==null || bByteArrArr.length!=count)
            throw new IllegalArgumentException("bByteArrArr length must equal stage cell count");
        if(cByteArrArr==null || cByteArrArr.length!=count)
            throw new IllegalArgumentException("cByteArrArr length must equal stage cell count");
        this.width=width;this.height=height;
        this.bByteArrArr=bByteArrArr.clone();this.cByteArrArr=cByteArrArr.clone();
    }
    public int width(){return width;} public int height(){return height;}
    public byte bByteAt(int x,int y){return bByteArrArr[index(x,y)];}
    public byte cByteAt(int x,int y){return cByteArrArr[index(x,y)];}
    public byte[] copyBByteArrArr(){return bByteArrArr.clone();}
    public byte[] copyCByteArrArr(){return cByteArrArr.clone();}
    private int index(int x,int y) {
        if(x<0 || y<0 || x>=width || y>=height)
            throw new IndexOutOfBoundsException("stage coordinate out of bounds: "+x+","+y);
        return x+y*width;
    }
}
