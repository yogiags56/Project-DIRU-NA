
package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Coordinate lookup and explicit slot invalidation for the legacy 15-slot table. */
public final class LegacyPackedSlotLookup {
    private LegacyPackedSlotLookup(){}
    public static int findFirstByCoordinates(long[] words,int x,int y) {
        validate(words);
        for(int i=0;i<LegacyPackedSlotAllocator.SLOT_COUNT;i++) {
            if(LegacyPackedFieldCodec.read(words[i],13,7)==x
                    && LegacyPackedFieldCodec.read(words[i],20,7)==y) return i;
        }
        return -1;
    }
    /** Mirrors the source call-site guard: only marker 3 is invalidated. */
    public static long[] invalidateIfMarkerThree(long[] words,int slot) {
        validate(words);
        if(slot<0 || slot>=LegacyPackedSlotAllocator.SLOT_COUNT)
            throw new IllegalArgumentException("slot index out of range");
        long[] copy=words.clone();
        if(LegacyPackedFieldCodec.read(copy[slot],0,4)==3) copy[slot]=0L;
        return copy;
    }
    private static void validate(long[] words) {
        if(words==null || words.length!=LegacyPackedSlotAllocator.SLOT_COUNT)
            throw new IllegalArgumentException("exactly 15 slot words are required");
    }
}