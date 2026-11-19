package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Isolated codec for the legacy 15-word bLongArr table.
 *
 * <p>This table is not aLongArr: it has byte-indexed slots and its own
 * snapshot array. The observed runtime scans coordinate fields and clears
 * matching type-3 records. The generic write operation mirrors the original
 * helper's int-shift semantics, although no direct runtime call to that
 * setter overload was found in the audited source revision.</p>
 */
public final class LegacyByteIndexedLongRecordCodec {
    public static final int SLOT_COUNT = 15;
    private static final int TYPE_OFFSET = 0;
    private static final int TYPE_WIDTH = 4;
    private static final int SUBTYPE_OFFSET = 4;
    private static final int SUBTYPE_WIDTH = 2;
    private static final int X_OFFSET = 13;
    private static final int Y_OFFSET = 20;
    private static final int COORDINATE_WIDTH = 7;

    private final long[] words;

    public LegacyByteIndexedLongRecordCodec(long[] words) {
        if (words == null || words.length != SLOT_COUNT) {
            throw new IllegalArgumentException("bLongArr must contain exactly 15 words");
        }
        this.words = words;
    }

    public long[] backingArray() {
        return words;
    }

    public int read(int slot, int offset, int width) {
        validate(slot, offset, width);
        return (int) ((words[slot] >>> offset) & mask(width));
    }

    /**
     * Mirrors the decompiled setter: the incoming byte is shifted as an int
     * before being widened to long. Do not replace this with a masked long
     * shift; negative/out-of-range byte values have different semantics.
     */
    public void write(int slot, byte value, int offset, int width) {
        validate(slot, offset, width);
        long current = read(slot, offset, width);
        words[slot] = (words[slot] ^ (current << offset))
                | (long) (value << offset);
    }

    public int readType(int slot) {
        return read(slot, TYPE_OFFSET, TYPE_WIDTH);
    }

    public int readSubtype(int slot) {
        return read(slot, SUBTYPE_OFFSET, SUBTYPE_WIDTH);
    }

    public int readX(int slot) {
        return read(slot, X_OFFSET, COORDINATE_WIDTH);
    }

    public int readY(int slot) {
        return read(slot, Y_OFFSET, COORDINATE_WIDTH);
    }

    /** Returns the first matching coordinate pair, irrespective of type. */
    public int findFirstByCoordinates(int x, int y) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (readX(slot) == x && readY(slot) == y) return slot;
        }
        return -1;
    }

    /** Mirrors the runtime's direct whole-word release. */
    public void clear(int slot) {
        validate(slot, 0, 1);
        words[slot] = 0L;
    }

    private static void validate(int slot, int offset, int width) {
        if (slot < 0 || slot >= SLOT_COUNT || offset < 0
                || width < 1 || width > 63 || offset + width > Long.SIZE) {
            throw new IllegalArgumentException("invalid bLongArr field");
        }
    }

    private static long mask(int width) {
        return -1L >>> (Long.SIZE - width);
    }
}
