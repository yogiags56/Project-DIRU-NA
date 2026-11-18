package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPackedFieldCodecTest {
    @Test public void ordinaryFieldsRoundTrip() {
        long word=0L;
        word=LegacyPackedFieldCodec.write(word,1,0,4);
        word=LegacyPackedFieldCodec.write(word,2,4,2);
        word=LegacyPackedFieldCodec.write(word,42,6,7);
        word=LegacyPackedFieldCodec.write(word,5,13,7);
        word=LegacyPackedFieldCodec.write(word,9,20,7);
        assertEquals(1,LegacyPackedFieldCodec.read(word,0,4));
        assertEquals(2,LegacyPackedFieldCodec.read(word,4,2));
        assertEquals(42,LegacyPackedFieldCodec.read(word,6,7));
        assertEquals(5,LegacyPackedFieldCodec.read(word,13,7));
        assertEquals(9,LegacyPackedFieldCodec.read(word,20,7));
    }
    @Test public void offset27UsesIntOverflowButLongClear() {
        long word=LegacyPackedFieldCodec.write(0L,42,27,7);
        assertEquals(10,LegacyPackedFieldCodec.read(word,27,7));
        long changed=LegacyPackedFieldCodec.write(word,1,27,7);
        assertEquals(1,LegacyPackedFieldCodec.read(changed,27,7));
        assertEquals(0L,changed & (0x7FL<<34));
    }
    @Test public void negativeByteValuePreservesLegacySignExtension() {
        long word=LegacyPackedFieldCodec.write(0L,(byte)0xFF,4,2);
        assertEquals(3,LegacyPackedFieldCodec.read(word,4,2));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsFieldOutsideWord(){LegacyPackedFieldCodec.read(0L,63,2);}
}