package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Strict parser for the legacy world stage stream.
 *
 * Format: opaque one-byte prefix, one-byte stage count, then for each stage
 * little-endian u16 width/u16 height and three width*height byte layers:
 * player/object, background, foreground. Layer bytes are widened with Java
 * signed-byte semantics, matching assignment to the original byte[][] arrays.
 */
public final class WorldStageStreamParser {
    public static final class Limits {
        public final int maxWidth,maxHeight,maxCells;
        public Limits(int maxWidth,int maxHeight,int maxCells) {
            if(maxWidth<=0 || maxHeight<=0 || maxCells<=0)
                throw new IllegalArgumentException("limits must be positive");
            this.maxWidth=maxWidth;this.maxHeight=maxHeight;this.maxCells=maxCells;
        }
        public static Limits conservativeDefault(){return new Limits(512,512,262144);}
    }

    public static final class ParsedStage {
        private final int prefix,stageCount,stageIndex;
        private final StageGrid grid;
        private ParsedStage(int prefix,int stageCount,int stageIndex,StageGrid grid) {
            this.prefix=prefix;this.stageCount=stageCount;this.stageIndex=stageIndex;this.grid=grid;
        }
        public int prefix(){return prefix;}
        public int stageCount(){return stageCount;}
        public int stageIndex(){return stageIndex;}
        public StageGrid grid(){return new StageGrid(grid.width(),grid.height(),
                grid.copyPlayerLayer(),grid.copyBackgroundLayer(),grid.copyForegroundLayer());}
    }

    private final Limits limits;
    public WorldStageStreamParser(){this(Limits.conservativeDefault());}
    public WorldStageStreamParser(Limits limits) {
        if(limits==null) throw new IllegalArgumentException("limits are required");
        this.limits=limits;
    }

    public ParsedStage parse(byte[] bytes,int selectedStageIndex) {
        if(bytes==null) throw new IllegalArgumentException("stage stream is required");
        Cursor in=new Cursor(bytes);
        int prefix=in.u8();
        int count=in.u8();
        if(count==0) throw new IllegalArgumentException("stage stream contains no stage records");
        if(selectedStageIndex<0 || selectedStageIndex>=count)
            throw new IllegalArgumentException("selected stage index is outside stage directory");
        StageGrid selected=null;
        for(int stage=0;stage<count;stage++) {
            int width=in.u16le(),height=in.u16le();
            if(width==0 || height==0 || width>limits.maxWidth || height>limits.maxHeight)
                throw new IllegalArgumentException("stage dimensions are invalid or exceed configured limits");
            long cellsLong=(long)width*(long)height;
            if(cellsLong>limits.maxCells || cellsLong>Integer.MAX_VALUE)
                throw new IllegalArgumentException("stage cell count exceeds configured limits");
            int cells=(int)cellsLong;
            long layerBytes=3L*cells;
            if(layerBytes>in.remaining())
                throw new IllegalArgumentException("truncated stage layer payload");
            if(stage==selectedStageIndex) {
                int[] player=readSignedLayer(in,cells);
                int[] background=readSignedLayer(in,cells);
                int[] foreground=readSignedLayer(in,cells);
                selected=new StageGrid(width,height,player,background,foreground);
            } else {
                in.skipFully((int)layerBytes);
            }
        }
        if(in.remaining()!=0) throw new IllegalArgumentException("unexpected trailing bytes in stage stream");
        return new ParsedStage(prefix,count,selectedStageIndex,selected);
    }

    private static int[] readSignedLayer(Cursor in,int cells) {
        int[] layer=new int[cells];
        for(int i=0;i<cells;i++) layer[i]=(byte)in.u8();
        return layer;
    }

    private static final class Cursor {
        private final byte[] bytes; private int position;
        Cursor(byte[] bytes){this.bytes=bytes;}
        int remaining(){return bytes.length-position;}
        int u8(){if(remaining()<1) throw new IllegalArgumentException("truncated stage stream");return bytes[position++]&0xFF;}
        int u16le(){int lo=u8(),hi=u8();return lo|(hi<<8);}
        void skipFully(int count){if(count<0 || count>remaining())throw new IllegalArgumentException("truncated skipped stage payload");position+=count;}
    }
}
