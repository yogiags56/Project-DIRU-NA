package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Bit operations matching the original Java int/long promotion order. */
public final class LegacyPackedFieldCodec {
    private LegacyPackedFieldCodec(){}
    public static int read(long word,int offset,int width) {
        validate(offset,width);
        long mask=(1L<<width)-1L;
        return (int)((word>>>offset)&mask);
    }
    public static long write(long word,int value,int offset,int width) {
        validate(offset,width);
        int current=read(word,offset,width);
        // Source: word ^ ((long) current << offset) | (long) (value << offset).
        // Clearing is a long shift; setting intentionally overflows as an int.
        long cleared=word ^ ((long)current << offset);
        return cleared | (long)(value << offset);
    }
    private static void validate(int offset,int width) {
        if(offset<0 || width<1 || width>31 || offset+width>64)
            throw new IllegalArgumentException("invalid packed field");
    }
}