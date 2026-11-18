package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Pure transcription of the legacy 15-entry packed bLongArr allocator. */
public final class LegacyPackedSlotAllocator {
    public static final int SLOT_COUNT=15;
    public static final class Result {
        private final int slotIndex; private final long[] words;
        private Result(int slotIndex,long[] words){this.slotIndex=slotIndex;this.words=words.clone();}
        public int getSlotIndex(){return slotIndex;}
        public boolean wasAllocated(){return slotIndex>=0;}
        public long[] getWords(){return words.clone();}
    }
    private LegacyPackedSlotAllocator(){}
    public static Result allocate(long[] currentWords,byte anchorTile,byte anchorX,
                                  byte anchorY,byte typeByte){
        if(currentWords==null || currentWords.length!=SLOT_COUNT)
            throw new IllegalArgumentException("exactly 15 slot words are required");
        long[] next=currentWords.clone(); int slot=-1;
        for(int i=0;i<SLOT_COUNT;i++){
            if(LegacyPackedFieldCodec.read(next[i],0,4)==0){slot=i;break;}
        }
        if(slot<0)return new Result(-1,next);
        long word=next[slot];
        word=LegacyPackedFieldCodec.write(word,1,0,4);
        word=LegacyPackedFieldCodec.write(word,typeByte,4,2);
        word=LegacyPackedFieldCodec.write(word,anchorTile,6,7);
        word=LegacyPackedFieldCodec.write(word,anchorTile,27,7);
        word=LegacyPackedFieldCodec.write(word,anchorX,13,7);
        word=LegacyPackedFieldCodec.write(word,anchorY,20,7);
        next[slot]=word;
        return new Result(slot,next);
    }
}