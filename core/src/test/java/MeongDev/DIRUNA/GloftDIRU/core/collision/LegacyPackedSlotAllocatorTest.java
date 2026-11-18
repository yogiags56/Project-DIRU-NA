package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPackedSlotAllocatorTest {
    private long[] empty(){return new long[15];}
    @Test public void allocatesFirstEmptySlotAndWritesSixPackedFields(){
        long[] input=empty();
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(
                input,(byte)42,(byte)5,(byte)9,(byte)2);
        assertEquals(0,r.getSlotIndex());
        long w=r.getWords()[0];
        assertEquals(1L,w&0xFL);
        assertEquals(2L,(w>>>4)&0x3L);
        assertEquals(42L,(w>>>6)&0x7FL);
        assertEquals(5L,(w>>>13)&0x7FL);
        assertEquals(9L,(w>>>20)&0x7FL);
        assertEquals(42L,(w>>>27)&0x7FL);
        assertEquals(0L,input[0]);
    }
    @Test public void skipsOccupiedSlotsAndSelectsFirstLowNibbleZero(){
        long[] words=empty();
        for(int i=0;i<4;i++)words[i]=0x1L;
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(
                words,(byte)3,(byte)4,(byte)5,(byte)1);
        assertEquals(4,r.getSlotIndex());
        assertEquals(1L,r.getWords()[4]&0xFL);
    }
    @Test public void fullTableReturnsMinusOneWithoutChangingAnyWord(){
        long[] words=empty();
        for(int i=0;i<15;i++)words[i]=0x1L;
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(
                words,(byte)1,(byte)2,(byte)3,(byte)0);
        assertEquals(-1,r.getSlotIndex());
        assertArrayEquals(words,r.getWords());
    }
    @Test public void preservesUnrelatedBitsInSelectedSlot(){
        long[] words=empty();
        words[0]=0x123456789ABCDE00L;
        long before=words[0];
        LegacyPackedSlotAllocator.Result r=LegacyPackedSlotAllocator.allocate(
                words,(byte)7,(byte)8,(byte)9,(byte)1);
        assertEquals(0,r.getSlotIndex());
        long expected=before;
        expected=(expected&~(0xFL<<0))|1L;
        expected=(expected&~(0x3L<<4))|(1L<<4);
        expected=(expected&~(0x7FL<<6))|(7L<<6);
        expected=(expected&~(0x7FL<<13))|(8L<<13);
        expected=(expected&~(0x7FL<<20))|(9L<<20);
        expected=(expected&~(0x7FL<<27))|(7L<<27);
        assertEquals(expected,r.getWords()[0]);
    }
    @Test public void rejectsWrongSlotCount(){
        try{
            LegacyPackedSlotAllocator.allocate(new long[14],(byte)1,(byte)1,(byte)1,(byte)1);
            fail("expected validation");
        }catch(IllegalArgumentException expected){}
    }
}