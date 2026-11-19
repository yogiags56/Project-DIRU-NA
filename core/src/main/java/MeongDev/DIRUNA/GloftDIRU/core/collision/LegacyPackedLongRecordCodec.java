package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Source-compatible codecs for the two observed, mutually overlapping
 * interpretations of the legacy 15-element aLongArr storage.
 *
 * <p>Do not use both layouts for the same live record population. The
 * original source exposes separate allocator/caller paths; this class keeps
 * their bit layouts explicit instead of pretending they are one schema.</p>
 */
public final class LegacyPackedLongRecordCodec {
    public static final int SLOT_COUNT = 15;

    private static final int MOTION_TYPE_OFFSET = 0;
    private static final int OBJECT_TYPE_OFFSET = 28;

    private final long[] words;

    public LegacyPackedLongRecordCodec(long[] words) {
        if (words == null || words.length != SLOT_COUNT) {
            throw new IllegalArgumentException("aLongArr must contain exactly 15 words");
        }
        this.words = words;
    }

    public long[] backingArray() {
        return words;
    }

    /** First observed layout: zero-based slots, type field at bits 0..3. */
    public static final class MotionLayout {
        private final LegacyPackedLongRecordCodec owner;

        private MotionLayout(LegacyPackedLongRecordCodec owner) {
            this.owner = owner;
        }

        public static MotionLayout on(LegacyPackedLongRecordCodec owner) {
            if (owner == null) throw new IllegalArgumentException("owner must not be null");
            return new MotionLayout(owner);
        }

        public int readType(int slot) {
            return owner.read(slot, MOTION_TYPE_OFFSET, 4);
        }

        public int findFirstFreeSlot() {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (readType(slot) == 0) return slot;
            }
            return -1;
        }

        /**
         * Mirrors the four-byte allocator: writes type=1, then subtype,
         * and four 7-bit coordinate/parameter fields. Values are not masked
         * before the source's long shifts.
         */
        public int allocate(byte valueA, byte valueB, byte valueC, byte subtype) {
            int slot = findFirstFreeSlot();
            if (slot < 0) return -1;
            owner.words[slot] = 0L;
            owner.write(slot, 1, 0, 4);
            owner.write(slot, subtype, 4, 2);
            owner.write(slot, valueA, 6, 7);
            owner.write(slot, valueB, 27, 7);
            owner.write(slot, valueC, 13, 7);
            owner.write(slot, valueA, 20, 7);
            return slot;
        }

        public int readSubtype(int slot) { return owner.read(slot, 4, 2); }
        public int readFieldA(int slot) { return owner.read(slot, 6, 7); }
        public int readFieldB(int slot) { return owner.read(slot, 13, 7); }
        public int readFieldC(int slot) { return owner.read(slot, 20, 7); }
        public int readFieldD(int slot) { return owner.read(slot, 27, 7); }
    }

    /** Second observed layout: one-based record IDs, type field at bits 28..30. */
    public static final class ObjectLayout {
        private final LegacyPackedLongRecordCodec owner;

        private ObjectLayout(LegacyPackedLongRecordCodec owner) {
            this.owner = owner;
        }

        public static ObjectLayout on(LegacyPackedLongRecordCodec owner) {
            if (owner == null) throw new IllegalArgumentException("owner must not be null");
            return new ObjectLayout(owner);
        }

        public int readType(int recordId) {
            return owner.read(index(recordId), 28, 3);
        }

        public int findFirstFreeRecordId() {
            for (int id = 1; id <= SLOT_COUNT; id++) {
                if (readType(id) == 0) return id;
            }
            return -1;
        }

        /**
         * Mirrors the type-1 object record writes. The source's six-argument
         * allocator separately handles type 7; this method models type 1.
         */
        public int allocateTypeOne(
                int x, int y, int group, int direction, int mode) {
            int id = findFirstFreeRecordId();
            if (id < 0) return -1;
            int slot = index(id);
            owner.words[slot] = 0L;
            owner.write(slot, 1, 28, 3);
            owner.write(slot, mode, 54, 3);
            owner.write(slot, x, 31, 6);
            owner.write(slot, y, 37, 6);
            owner.write(slot, group, 43, 2);
            owner.write(slot, direction < 0 ? 2 : direction, 45, 2);
            return id;
        }

        public int readX(int id) { return owner.read(index(id), 31, 6); }
        public int readY(int id) { return owner.read(index(id), 37, 6); }
        public int readGroup(int id) { return owner.read(index(id), 43, 2); }
        public int readDirection(int id) { return owner.read(index(id), 45, 2); }
        public int readMode(int id) { return owner.read(index(id), 54, 3); }

        private static int index(int id) {
            if (id < 1 || id > SLOT_COUNT) {
                throw new IllegalArgumentException("record ID must be in range 1..15");
            }
            return id - 1;
        }
    }

    /** Exact 64-bit field read used by the decompiled source helper. */
    public int read(int slot, int offset, int width) {
        validateField(slot, offset, width);
        return (int) ((words[slot] >>> offset) & mask(width));
    }

    /**
     * Exact source-style field replacement:
     * (old ^ (current << offset)) | ((long)value << offset).
     * The incoming value is deliberately not masked.
     */
    public void write(int slot, long value, int offset, int width) {
        validateField(slot, offset, width);
        long current = read(slot, offset, width);
        words[slot] = (words[slot] ^ (current << offset)) | (value << offset);
    }

    private void validateField(int slot, int offset, int width) {
        if (slot < 0 || slot >= SLOT_COUNT || offset < 0
                || width < 1 || width > 63 || offset + width > Long.SIZE) {
            throw new IllegalArgumentException("invalid packed long field");
        }
    }

    private static long mask(int width) {
        return -1L >>> (Long.SIZE - width);
    }
}
