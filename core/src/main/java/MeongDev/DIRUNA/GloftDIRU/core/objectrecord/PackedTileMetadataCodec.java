package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

/**
 * Bit-exact codec for one legacy eIntArrArr tile word.
 *
 * Decompiled source evidence shows three 9-bit groups packed into an int.
 * Within each group, bits 0..2 hold the object-record ID, bits 3..6 are
 * a four-bit field, and bits 7..8 are a two-bit field. This class keeps
 * the latter fields semantically opaque until every consumer is audited.
 */
public final class PackedTileMetadataCodec {
    public static final int GROUP_COUNT = 3;
    public static final int BITS_PER_GROUP = 9;
    public static final int OBJECT_ID_OFFSET = 0;
    public static final int OBJECT_ID_WIDTH = 3;
    public static final int MIDDLE_FIELD_OFFSET = 3;
    public static final int MIDDLE_FIELD_WIDTH = 4;
    public static final int UPPER_FIELD_OFFSET = 7;
    public static final int UPPER_FIELD_WIDTH = 2;
    private static final int GROUP_MASK = (1 << BITS_PER_GROUP) - 1;

    public enum Status { OK, NO_OBJECT_REFERENCE, INVALID_GROUP, INVALID_FIELD, VALUE_OUT_OF_RANGE }

    public record ReadResult(Status status, int value) {
        public boolean succeeded() { return status == Status.OK; }
    }

    public record WriteResult(Status status, int rawWord) {
        public boolean succeeded() { return status == Status.OK; }
    }

    private PackedTileMetadataCodec() { }

    public static ReadResult readField(int rawWord, int group, int fieldOffset, int width) {
        if (!validGroup(group)) return new ReadResult(Status.INVALID_GROUP, 0);
        if (!validField(fieldOffset, width))
            return new ReadResult(Status.INVALID_FIELD, 0);
        int shift = group * BITS_PER_GROUP + fieldOffset;
        int mask = (1 << width) - 1;
        return new ReadResult(Status.OK, (rawWord >>> shift) & mask);
    }

    public static WriteResult writeField(int rawWord, int group, int fieldOffset,
                                         int width, int value) {
        if (!validGroup(group)) return new WriteResult(Status.INVALID_GROUP, rawWord);
        if (!validField(fieldOffset, width))
            return new WriteResult(Status.INVALID_FIELD, rawWord);
        int mask = (1 << width) - 1;
        if (value < 0 || (value & ~mask) != 0)
            return new WriteResult(Status.VALUE_OUT_OF_RANGE, rawWord);
        int shift = group * BITS_PER_GROUP + fieldOffset;
        int shiftedMask = mask << shift;
        int updated = (rawWord & ~shiftedMask) | (value << shift);
        return new WriteResult(Status.OK, updated);
    }

    public static ReadResult readObjectId(int rawWord, int group) {
        return readField(rawWord, group, OBJECT_ID_OFFSET, OBJECT_ID_WIDTH);
    }

    /**
     * Safe native interpretation for tile lookups: zero means no resolvable
     * tile reference. Use readObjectId() when the raw legacy bits themselves
     * must be preserved or inspected; do not use zero as an array index.
     */
    public static ReadResult readObjectReference(int rawWord, int group) {
        ReadResult raw = readObjectId(rawWord, group);
        if (!raw.succeeded()) return raw;
        if (raw.value() == 0) return new ReadResult(Status.NO_OBJECT_REFERENCE, 0);
        return raw;
    }

    public static WriteResult writeObjectId(int rawWord, int group, int objectId) {
        return writeField(rawWord, group, OBJECT_ID_OFFSET, OBJECT_ID_WIDTH, objectId);
    }

    /** Mirrors cVoid(x,y,group): clear all nine bits of only the selected group. */
    public static WriteResult clearGroup(int rawWord, int group) {
        if (!validGroup(group)) return new WriteResult(Status.INVALID_GROUP, rawWord);
        int shift = group * BITS_PER_GROUP;
        return new WriteResult(Status.OK, rawWord & ~(GROUP_MASK << shift));
    }

    private static boolean validGroup(int group) {
        return group >= 0 && group < GROUP_COUNT;
    }

    private static boolean validField(int fieldOffset, int width) {
        return fieldOffset >= 0 && width > 0
                && fieldOffset + width <= BITS_PER_GROUP;
    }
}