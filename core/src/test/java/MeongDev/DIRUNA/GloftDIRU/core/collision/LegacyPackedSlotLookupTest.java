package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPackedSlotLookupTest {
    private long[] table() {
        long[] words=new long[15];
        words[2]=LegacyPackedFieldCodec.write(0L,3,0,4);
        words[2]=LegacyPackedFieldCodec.write(words[2],12,13,7);
        words[2]=LegacyPackedFieldCodec.write(words[2],21,20,7);
        words[5]=LegacyPackedFieldCodec.write(0L,1,0,4);
        words[5]=LegacyPackedFieldCodec.write(words[5],12,13,7);
        words[5]=LegacyPackedFieldCodec.write(words[5],21,20,7);
        return words;
    }
    @Test public void coordinateLookupReturnsFirstMatchIncludingInactiveMarker() {
        assertEquals(2,LegacyPackedSlotLookup.findFirstByCoordinates(table(),12,21));
    }
    @Test public void missingCoordinatesReturnMinusOne() {
        assertEquals(-1,LegacyPackedSlotLookup.findFirstByCoordinates(table(),1,2));
    }
    @Test public void invalidatesOnlyMarkerThreeAndReturnsCopy() {
        long[] original=table();
        long[] result=LegacyPackedSlotLookup.invalidateIfMarkerThree(original,2);
        assertEquals(0L,result[2]);
        assertNotEquals(0L,original[2]);
        assertNotEquals(0L,result[5]);
    }
    @Test public void leavesNonThreeMarkerUntouched() {
        long[] original=table();
        long[] result=LegacyPackedSlotLookup.invalidateIfMarkerThree(original,5);
        assertEquals(original[5],result[5]);
    }
}