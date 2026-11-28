package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes the physical byte-array layout used by the legacy demo loader.
 *
 * Top-level leaves own an isolated byte array. Opcode-0 group children are
 * packed consecutively into one shared group array, with a u32 length table.
 * A RecordSlot retains both its logical record length and its physical backing
 * container so legacy fall-through reads can be audited without unsafe access.
 */
public final class DemoLegacyBufferLayout {
    public static final class BufferBoundsException extends RuntimeException {
        private final int relativeOffset;
        private final int width;
        private final int backingLength;

        private BufferBoundsException(int relativeOffset, int width, int backingLength) {
            super("legacy buffer read [" + relativeOffset + ", "
                    + ((long) relativeOffset + width) + ") exceeds backing container length "
                    + backingLength);
            this.relativeOffset = relativeOffset;
            this.width = width;
            this.backingLength = backingLength;
        }
        public int relativeOffset() { return relativeOffset; }
        public int width() { return width; }
        public int backingLength() { return backingLength; }
    }

    public static final class RecordSlot {
        private final byte[] backing;
        private final int offset;
        private final int logicalLength;
        private final int backingLength;

        private RecordSlot(byte[] backing, int offset, int logicalLength) {
            this.backing = backing;
            this.offset = offset;
            this.logicalLength = logicalLength;
            this.backingLength = backing.length;
        }

        public int offsetInContainer() { return offset; }
        public int logicalLength() { return logicalLength; }
        public int backingLength() { return backingLength; }
        public int opcode() { return backing[offset] & 0xFF; }

        /** True only when the complete requested range remains in this record. */
        public boolean isWithinLogicalRecord(int relativeOffset, int width) {
            return relativeOffset >= 0 && width >= 0
                    && relativeOffset <= logicalLength
                    && width <= logicalLength - relativeOffset;
        }

        /**
         * Reads as the legacy shared byte[] would: may cross a sibling boundary
         * inside a packed group, but can never cross the physical array boundary.
         */
        public int readBackingU8(int relativeOffset) {
            requireBackingRange(relativeOffset, 1);
            return backing[offset + relativeOffset] & 0xFF;
        }

        public int readBackingU16(int relativeOffset) {
            requireBackingRange(relativeOffset, 2);
            int absolute = offset + relativeOffset;
            return (backing[absolute] & 0xFF)
                    | ((backing[absolute + 1] & 0xFF) << 8);
        }

        public long readBackingU32(int relativeOffset) {
            requireBackingRange(relativeOffset, 4);
            int absolute = offset + relativeOffset;
            return ((long) backing[absolute] & 0xFF)
                    | (((long) backing[absolute + 1] & 0xFF) << 8)
                    | (((long) backing[absolute + 2] & 0xFF) << 16)
                    | (((long) backing[absolute + 3] & 0xFF) << 24);
        }

        private void requireBackingRange(int relativeOffset, int width) {
            if (relativeOffset < 0 || width < 0 || relativeOffset > backingLength - offset
                    || width > backingLength - offset - relativeOffset) {
                throw new BufferBoundsException(relativeOffset, width, backingLength);
            }
        }
    }

    private final byte[] backing;
    private final List<RecordSlot> slots;
    private final boolean packedGroup;

    private DemoLegacyBufferLayout(byte[] backing, List<RecordSlot> slots, boolean packedGroup) {
        this.backing = backing;
        this.slots = Collections.unmodifiableList(new ArrayList<RecordSlot>(slots));
        this.packedGroup = packedGroup;
    }

    /** Models one legacy top-level leaf array with no adjacent record bytes. */
    public static DemoLegacyBufferLayout topLevelLeaf(byte[] normalizedLeaf) {
        validateLeaf(normalizedLeaf, 0);
        byte[] copy = normalizedLeaf.clone();
        List<RecordSlot> slots = new ArrayList<RecordSlot>(1);
        slots.add(new RecordSlot(copy, 0, copy.length));
        return new DemoLegacyBufferLayout(copy, slots, false);
    }

    /** Parses one normalized opcode-0 group and retains its shared packed array. */
    public static DemoLegacyBufferLayout packedGroup(byte[] normalizedGroup) {
        if (normalizedGroup == null)
            throw new IllegalArgumentException("normalized group is required");
        if (normalizedGroup.length < 2 || (normalizedGroup[0] & 0xFF) != 0)
            throw new IllegalArgumentException("normalized group header is invalid");

        byte[] copy = normalizedGroup.clone();
        int count = copy[1] & 0xFF;
        long tableEndLong = 2L + (long) count * 4L;
        if (tableEndLong > copy.length)
            throw new IllegalArgumentException("normalized group length table is truncated");
        int tableEnd = (int) tableEndLong;
        int[] lengths = new int[count];
        long totalPayload = 0;
        for (int i = 0; i < count; i++) {
            long length = readU32(copy, 2 + i * 4);
            if (length < 2 || length > Integer.MAX_VALUE)
                throw new IllegalArgumentException("normalized group child length is invalid");
            lengths[i] = (int) length;
            totalPayload += length;
            if (totalPayload > Integer.MAX_VALUE)
                throw new IllegalArgumentException("normalized group payload is too large");
        }
        if (tableEndLong + totalPayload != copy.length)
            throw new IllegalArgumentException("normalized group lengths do not match packed payload");

        List<RecordSlot> slots = new ArrayList<RecordSlot>(count);
        int cursor = tableEnd;
        for (int length : lengths) {
            validateLeaf(copy, cursor);
            if (length > copy.length - cursor)
                throw new IllegalArgumentException("normalized group child exceeds packed payload");
            slots.add(new RecordSlot(copy, cursor, length));
            cursor += length;
        }
        return new DemoLegacyBufferLayout(copy, slots, true);
    }

    public int slotCount() { return slots.size(); }
    public RecordSlot slot(int index) { return slots.get(index); }
    public List<RecordSlot> slots() { return slots; }
    public boolean isPackedGroup() { return packedGroup; }
    public int backingLength() { return backing.length; }

    private static void validateLeaf(byte[] bytes, int offset) {
        if (bytes == null || offset < 0 || offset > bytes.length - 2)
            throw new IllegalArgumentException("normalized leaf header is truncated");
        if ((bytes[offset] & 0xFF) == 0)
            throw new IllegalArgumentException("nested group is not a leaf slot");
        if (bytes[offset + 1] != 0)
            throw new IllegalArgumentException("normalized leaf reserved byte must be zero");
    }

    private static long readU32(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xFF)
                | (((long) bytes[offset + 1] & 0xFF) << 8)
                | (((long) bytes[offset + 2] & 0xFF) << 16)
                | (((long) bytes[offset + 3] & 0xFF) << 24);
    }
}
