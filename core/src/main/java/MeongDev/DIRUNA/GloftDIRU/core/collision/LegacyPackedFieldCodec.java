package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Bit operations matching the original Java int/long promotion order. */
public final class LegacyPackedFieldCodec {
    private LegacyPackedFieldCodec(){}
    public static int read(long word,int offset,int width) {
        if(offset<0 || width<1 || width>31 || offset+width>64)
            throw new IllegalArgumentException("invalid packed field");
        long mask=(1L<<width)-1L;
        return (int)((word>>>offset)&mask);
    }
    public static long write(long word,int value,int offset,int width) {
        if(offset<0 || width<1 || width>31 || offset+width>64)
            throw new IllegalArgumentException("invalid packed field");
        int mask=(1<<width)-1;
        int current=read(word,offset,width);
        // The legacy helper performs both shifts as 32-bit int operations,
        // then widens their possibly overflowed/sign-extended results to long.
        long cleared=word ^ (long)(current<<offset);
        return cleared | (long)(value<<offset);
    }
}