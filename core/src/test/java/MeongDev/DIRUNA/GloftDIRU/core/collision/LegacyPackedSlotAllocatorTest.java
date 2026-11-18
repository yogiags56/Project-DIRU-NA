package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPackedSlotAllocatorTest {
    private long[] empty(){return new long[15];}
    @Test public void allocatesFirstEmptySlotAndWritesLegacyPackedFields(){
        long[] input=empty();
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(input,(byte)42,(byte)5,(byte)9,(byte)2);
        assertEquals(0,r.getSlotIndex());
        long w=r.getWords()[0];
        assertEquals(1,LegacyPackedFieldCodec.read(w,0,4));
        assertEquals(2,LegacyPackedFieldCodec.read(w,4,2));
        assertEquals(42,LegacyPackedFieldCodec.read(w,6,7));
        assertEquals(5,LegacyPackedFieldCodec.read(w,13,7));
        assertEquals(9,LegacyPackedFieldCodec.read(w,20,7));
        assertEquals(10,LegacyPackedFieldCodec.read(w,27,7));
        assertEquals(0L,input[0]);
    }
    @Test public void skipsOccupiedSlots(){
        long[] words=empty(); for(int i=0;i<4;i++)words[i]=1L;
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(words,(byte)3,(byte)4,(byte)5,(byte)1);
        assertEquals(4,r.getSlotIndex());
    }
    @Test public void fullTableReturnsMinusOneUnchanged(){
        long[] words=empty(); for(int i=0;i<15;i++)words[i]=1L;
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(words,(byte)1,(byte)2,(byte)3,(byte)0);
        assertEquals(-1,r.getSlotIndex()); assertArrayEquals(words,r.getWords());
    }
    @Test public void preservesUnrelatedBitsUsingLegacyIntShiftSemantics(){
        long[] words=empty(); words[0]=0x123456789ABCDE00L; long before=words[0];
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(words,(byte)7,(byte)8,(byte)9,(byte)1);
        long expected=before;
        expected=LegacyPackedFieldCodec.write(expected,1,0,4);
        expected=LegacyPackedFieldCodec.write(expected,1,4,2);
        expected=LegacyPackedFieldCodec.write(expected,7,6,7);
        expected=LegacyPackedFieldCodec.write(expected,7,27,7);
        expected=LegacyPackedFieldCodec.write(expected,8,13,7);
        expected=LegacyPackedFieldCodec.write(expected,9,20,7);
        assertEquals(expected,r.getWords()[0]);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsWrongSlotCount(){LegacyPackedSlotAllocator.allocate(new long[14],(byte)1,(byte)1,(byte)1,(byte)1);}
}