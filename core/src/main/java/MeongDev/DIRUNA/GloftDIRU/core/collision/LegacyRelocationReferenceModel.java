package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Isolated model of the record-table effects in the source mVoid(x,y) path.
 *
 * <p>This intentionally does not emulate player-layer arrays, eIntArrArr,
 * rendering callbacks, or the complete game loop. It models only the
 * bLongArr coordinate lookup/type-2 mark and the non-type-7 aLongArr
 * allocation that stores the bLongArr slot in bits 54..56.</p>
 */
public final class LegacyRelocationReferenceModel {
    public static final class Result {
        private final int matchedBSlot;
        private final int allocatedARecordId;
        private final long[] bWords;
        private final long[] aWords;

        private Result(int matchedBSlot, int allocatedARecordId,
                       long[] bWords, long[] aWords) {
            this.matchedBSlot = matchedBSlot;
            this.allocatedARecordId = allocatedARecordId;
            this.bWords = bWords.clone();
            this.aWords = aWords.clone();
        }

        public boolean didMatch() { return matchedBSlot >= 0; }
        public int getMatchedBSlot() { return matchedBSlot; }
        public int getAllocatedARecordId() { return allocatedARecordId; }
        public long[] getBWords() { return bWords.clone(); }
        public long[] getAWords() { return aWords.clone(); }
    }

    private LegacyRelocationReferenceModel() {}

    /**
     * Models mVoid(x,y) after its coordinate lookup succeeds.
     * The aLongArr record uses x, y+1, group 0, direction 0, and the
     * found zero-based bLongArr index as its offset-54 three-bit value.
     */
    public static Result relocate(long[] bLongArrWords, long[] aLongArrWords,
                                   int x, int y) {
        if (bLongArrWords == null
                || bLongArrWords.length != LegacyByteIndexedLongRecordCodec.SLOT_COUNT
                || aLongArrWords == null
                || aLongArrWords.length != LegacyPackedLongRecordCodec.SLOT_COUNT) {
            throw new IllegalArgumentException("both legacy tables must contain 15 words");
        }

        long[] nextB = bLongArrWords.clone();
        long[] nextA = aLongArrWords.clone();
        LegacyByteIndexedLongRecordCodec bTable =
                new LegacyByteIndexedLongRecordCodec(nextB);
        int bSlot = bTable.findFirstByCoordinates(x, y);
        if (bSlot < 0) {
            return new Result(-1, -1, nextB, nextA);
        }

        // mVoid marks the matching bLongArr record type as 2 before allocation.
        bTable.write(bSlot, (byte) 2, 0, 4);

        LegacyPackedLongRecordCodec aCodec =
                new LegacyPackedLongRecordCodec(nextA);
        LegacyPackedLongRecordCodec.ObjectLayout aObjects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(aCodec);
        int aRecordId = aObjects.allocateTypeOne(x, y + 1, 0, 0, bSlot);

        return new Result(bSlot, aRecordId, nextB, nextA);
    }
}
