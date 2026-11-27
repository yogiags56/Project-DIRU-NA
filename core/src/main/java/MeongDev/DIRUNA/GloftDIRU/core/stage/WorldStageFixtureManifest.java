package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Source-audited stage dimensions for the three candidate world resources.
 * This is metadata only: it contains no proprietary stage payloads or guessed hashes.
 */
public final class WorldStageFixtureManifest {
    public enum World { ANGKOR, SCOTLAND_RESOURCE_BAVARIA_UI, TIBET_RESOURCE_SIBERIA_UI }
    public static final class Entry {
        private final World world; private final int index,width,height;
        private Entry(World world,int index,int width,int height) {
            this.world=world;this.index=index;this.width=width;this.height=height;
        }
        public World world(){return world;} public int index(){return index;}
        public int width(){return width;} public int height(){return height;}
        public int cellsPerLayer(){return Math.multiplyExact(width,height);}
    }
    private static final int[][] ANGKOR={{26,21},{27,24},{27,26},{40,23},{51,24},{30,75},{26,45},{44,29},{35,14},{50,30},{50,31},{46,31},{46,31},{68,11}};
    private static final int[][] SCOTLAND={{45,24},{40,24},{33,30},{43,30},{45,33},{43,33},{36,43},{53,27},{34,32},{35,25},{37,24},{60,20},{23,60}};
    private static final int[][] TIBET={{60,26},{39,35},{35,42},{38,38},{46,27},{49,28},{55,30},{51,20},{51,37},{104,17},{35,25},{45,28},{35,32},{35,26}};
    private WorldStageFixtureManifest(){}
    public static int stageCount(World world){return dimensions(world).length;}
    public static Entry entry(World world,int index){
        int[][] d=dimensions(world);
        if(index<0 || index>=d.length) throw new IndexOutOfBoundsException("stage index: "+index);
        return new Entry(world,index,d[index][0],d[index][1]);
    }
    public static Entry[] entries(World world){
        int[][] d=dimensions(world);Entry[] out=new Entry[d.length];
        for(int i=0;i<d.length;i++) out[i]=new Entry(world,i,d[i][0],d[i][1]);
        return out;
    }
    private static int[][] dimensions(World world){
        if(world==null) throw new IllegalArgumentException("world is required");
        switch(world){case ANGKOR:return ANGKOR;case SCOTLAND_RESOURCE_BAVARIA_UI:return SCOTLAND;case TIBET_RESOURCE_SIBERIA_UI:return TIBET;default:throw new AssertionError(world);}
    }
}