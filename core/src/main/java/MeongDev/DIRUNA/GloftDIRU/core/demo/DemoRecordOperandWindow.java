package MeongDev.DIRUNA.GloftDIRU.core.demo;

/**
 * Read-only, bounds-checked view over one normalized record's operand bytes.
 *
 * Offsets passed to read methods are absolute offsets in the normalized record
 * (header bytes are at offsets 0 and 1). This window never reads neighboring
 * records and never exposes its backing array.
 */
public final class DemoRecordOperandWindow {
    public static final class OperandBoundsException extends RuntimeException {
        private final int offset;
        private final int length;
        private final int recordLength;

        private OperandBoundsException(int offset, int length, int recordLength) {
            super("operand range [" + offset + ", " + ((long) offset + length)
                    + ") outside normalized record length " + recordLength);
            this.offset = offset;
            this.length = length;
            this.recordLength = recordLength;
        }
        public int offset() { return offset; }
        public int length() { return length; }
        public int recordLength() { return recordLength; }
    }

    private final byte[] record;

    public DemoRecordOperandWindow(DemoInterpreterRuntimeState.RecordState state) {
        if (state == null) throw new IllegalArgumentException("record state is required");
        if (state.isGroup()) throw new IllegalArgumentException("group records do not have leaf operands");
        this.record = state.copyNormalizedBytes();
    }

    /** Construct a detached window from a normalized leaf record. */
    public static DemoRecordOperandWindow fromNormalizedBytes(byte[] normalizedRecord) {
        if (normalizedRecord == null) throw new IllegalArgumentException("normalized record is required");
        if (normalizedRecord.length < 2)
            throw new IllegalArgumentException("normalized record must contain a two-byte header");
        if ((normalizedRecord[0] & 0xFF) == 0)
            throw new IllegalArgumentException("group records do not have leaf operands");
        if (normalizedRecord[1] != 0)
            throw new IllegalArgumentException("normalized leaf reserved byte must be zero");
        return new DemoRecordOperandWindow(normalizedRecord);
    }

    private DemoRecordOperandWindow(byte[] normalizedRecord) {
        this.record = normalizedRecord.clone();
    }

    public int opcode() { return record[0] & 0xFF; }
    public int recordLength() { return record.length; }

    public int readU8(int offset) {
        require(offset, 1);
        return record[offset] & 0xFF;
    }

    public int readU16(int offset) {
        require(offset, 2);
        return (record[offset] & 0xFF) | ((record[offset + 1] & 0xFF) << 8);
    }

    public short readI16(int offset) {
        return (short) readU16(offset);
    }

    public long readU24(int offset) {
        require(offset, 3);
        return ((long) record[offset] & 0xFF)
                | (((long) record[offset + 1] & 0xFF) << 8)
                | (((long) record[offset + 2] & 0xFF) << 16);
    }

    public long readU32(int offset) {
        require(offset, 4);
        return ((long) record[offset] & 0xFF)
                | (((long) record[offset + 1] & 0xFF) << 8)
                | (((long) record[offset + 2] & 0xFF) << 16)
                | (((long) record[offset + 3] & 0xFF) << 24);
    }

    public byte[] copyBytes(int offset, int length) {
        require(offset, length);
        byte[] copy = new byte[length];
        System.arraycopy(record, offset, copy, 0, length);
        return copy;
    }

    private void require(int offset, int length) {
        if (offset < 0 || length < 0 || offset > record.length
                || length > record.length - offset) {
            throw new OperandBoundsException(offset, length, record.length);
        }
    }
}
