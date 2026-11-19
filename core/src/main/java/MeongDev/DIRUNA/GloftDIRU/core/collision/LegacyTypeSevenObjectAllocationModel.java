package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Isolated transcription of the type-7 branch of the six-argument aLongArr allocator.
 * Models the nine-bit tile-group clear and its render-state marker alongside
 * the type-7 object record. It deliberately does not write offset 54.
 */
public final class LegacyTypeSevenObjectAllocationModel {
    public static final class Result {
        private final int recordId;
        private final long[] objectWords;
        private final int[][] tileMetadata;
        private final byte[][] renderState;
        private Result(int id, long[] words, int[][] metadata, byte[][] render) {
            recordId = id; objectWords = copy(words); tileMetadata = copy(metadata); renderState = copy(render);
        }
        public int getRecordId() { return recordId; }
        public long[] getObjectWords() { return copy(objectWords); }
        public int[][] getTileMetadata() { return copy(tileMetadata); }
        public byte[][] getRenderState() { return copy(renderState); }
    }
    private LegacyTypeSevenObjectAllocationModel() {}
    public static Result allocate(long[] words, int[][] metadata, byte[][] render,
                                  int x, int y, int group, int direction) {
        if (words == null || words.length != 15 || metadata == null || render == null
                || x < 0 || x >= metadata.length || x >= render.length
                || y < 0 || y >= metadata[x].length || y >= render[x].length
                || group < 0 || group > 3) {
            throw new IllegalArgumentException("invalid type-7 allocation state");
        }
        long[] nextWords = words.clone();
        int[][] nextMetadata = copy(metadata);
        byte[][] nextRender = copy(render);
        int id = 1;
        while (id <= 15 && ((nextWords[id - 1] >>> 28) & 7L) != 0L) id++;
        if (id > 15) return new Result(-1, nextWords, nextMetadata, nextRender);
        nextWords[id - 1] = 0L;
        int shift = group * 9;
        nextMetadata[x][y] ^= ((nextMetadata[x][y] >>> shift) & 0x1FF) << shift;
        nextRender[x][y] = 24;
        long word = nextWords[id - 1];
        word = replace(word, 7, 28, 3);
        word = replace(word, x, 31, 6);
        word = replace(word, y, 37, 6);
        word = replace(word, group, 43, 2);
        word = replace(word, direction < 0 ? 2 : direction, 45, 2);
        nextWords[id - 1] = word;
        return new Result(id, nextWords, nextMetadata, nextRender);
    }
    private static long replace(long word, long value, int offset, int width) {
        long mask = (1L << width) - 1L;
        long old = (word >>> offset) & mask;
        return (word ^ (old << offset)) | (value << offset);
    }
    private static int[][] copy(int[][] source) {
        int[][] out = new int[source.length][];
        for (int i = 0; i < source.length; i++) out[i] = source[i].clone();
        return out;
    }
    private static byte[][] copy(byte[][] source) {
        byte[][] out = new byte[source.length][];
        for (int i = 0; i < source.length; i++) out[i] = source[i].clone();
        return out;
    }
    private static long[] copy(long[] source) { return source.clone(); }
}