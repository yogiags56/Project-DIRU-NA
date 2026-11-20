package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

/**
 * Bit-exact helper for the legacy aLongArr auxiliary-link field.
 *
 * Object-record slot IDs are one-based (1..15) and are NOT this field.
 * The auxiliary link itself is a three-bit, zero-based value at bits 54..56.
 * Type-7 creation follows a separate source branch that does not write this
 * field; callers must represent that as NO_LINK rather than interpreting zero.
 */
public final class LegacyObjectRecordLinkCodec {
    public static final int LINK_BIT_OFFSET = 54;
    public static final int LINK_BIT_WIDTH = 3;
    public static final long LINK_MASK = 0x7L << LINK_BIT_OFFSET;

    public enum Status { OK, INVALID_TYPE, INVALID_SLOT, UNREPRESENTABLE_SLOT, TYPE7_HAS_NO_LINK }

    public record ReadResult(Status status, int auxiliarySlot) {
        public boolean succeeded() { return status == Status.OK; }
    }

    public record WriteResult(Status status, long rawRecord) {
        public boolean succeeded() { return status == Status.OK; }
    }

    private LegacyObjectRecordLinkCodec() { }

    /** Reads bits 54..56 without assigning semantics to other packed fields. */
    public static int readRawField(long rawRecord) {
        return (int) ((rawRecord >>> LINK_BIT_OFFSET) & 0x7L);
    }

    /** Reads a semantic link; Type-7 records are explicitly linkless. */
    public static ReadResult read(long rawRecord, ObjectRecordTable.RecordType type) {
        if (type == null) return new ReadResult(Status.INVALID_TYPE, -1);
        if (type == ObjectRecordTable.RecordType.TYPE_7)
            return new ReadResult(Status.TYPE7_HAS_NO_LINK, -1);
        return new ReadResult(Status.OK, readRawField(rawRecord));
    }

    /** Writes the three-bit field while preserving every other bit. */
    public static WriteResult write(long rawRecord, ObjectRecordTable.RecordType type,
                                    int zeroBasedAuxiliarySlot) {
        if (type == null) return new WriteResult(Status.INVALID_TYPE, rawRecord);
        if (type == ObjectRecordTable.RecordType.TYPE_7)
            return new WriteResult(Status.TYPE7_HAS_NO_LINK, rawRecord);
        AuxiliaryLinkCodec.EncodeResult encoded =
                AuxiliaryLinkCodec.encodeSafe(zeroBasedAuxiliarySlot);
        if (encoded.status() == AuxiliaryLinkCodec.Status.INVALID_SLOT)
            return new WriteResult(Status.INVALID_SLOT, rawRecord);
        if (encoded.status() == AuxiliaryLinkCodec.Status.UNREPRESENTABLE_SLOT)
            return new WriteResult(Status.UNREPRESENTABLE_SLOT, rawRecord);
        long updated = (rawRecord & ~LINK_MASK)
                | (((long) encoded.encodedValue()) << LINK_BIT_OFFSET);
        return new WriteResult(Status.OK, updated);
    }

    /** Clears only the link field; useful for explicit reclassification policies. */
    public static long clearField(long rawRecord) {
        return rawRecord & ~LINK_MASK;
    }
}
