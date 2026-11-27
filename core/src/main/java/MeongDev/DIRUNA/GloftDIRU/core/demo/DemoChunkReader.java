package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.Arrays;

/**
 * Bounded reader for the tagged entries inside an already-extracted demo.f chunk zero.
 * All integer fields are unsigned little-endian values. The two-byte archive preamble
 * is retained as an opaque field because the legacy loader skips it.
 */
public final class DemoChunkReader {
    private static final int PREAMBLE_SIZE = 2;
    private static final int ENTRY_HEADER_SIZE = 8;

    public static final class Entry {
        private final int tag;
        private final int recordCount;
        private final byte[] payload;
        private final int headerOffset;

        private Entry(int tag, int recordCount, byte[] payload, int headerOffset) {
            this.tag = tag;
            this.recordCount = recordCount;
            this.payload = payload.clone();
            this.headerOffset = headerOffset;
        }
        public int tag() { return tag; }
        public int recordCount() { return recordCount; }
        public int payloadLength() { return payload.length; }
        public int headerOffset() { return headerOffset; }
        public byte[] copyPayload() { return payload.clone(); }
    }

    private DemoChunkReader() { }

    /** Returns the first matching tag, matching the legacy c.aVoid(int) selection rule. */
    public static Entry findFirst(byte[] archive, int requestedTag) {
        if (archive == null) throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "archive is required");
        if (requestedTag < 0 || requestedTag > 0xFFFF)
            throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "tag must be an unsigned 16-bit value");
        if (archive.length < PREAMBLE_SIZE)
            throw error(DemoParseException.Code.TRUNCATED_HEADER, archive.length, "missing two-byte archive preamble");

        int offset = PREAMBLE_SIZE;
        while (offset < archive.length) {
            int headerOffset = offset;
            require(archive.length, offset, ENTRY_HEADER_SIZE, DemoParseException.Code.TRUNCATED_HEADER,
                    "incomplete tagged-entry header");
            int tag = u16(archive, offset);
            int count = u16(archive, offset + 2);
            long declaredLength = u32(archive, offset + 4);
            offset += ENTRY_HEADER_SIZE;

            if (declaredLength > archive.length - (long) offset)
                throw error(DemoParseException.Code.TRUNCATED_PAYLOAD, headerOffset + 4,
                        "tagged-entry payload exceeds archive boundary");
            int length = (int) declaredLength;
            if (tag == requestedTag)
                return new Entry(tag, count, Arrays.copyOfRange(archive, offset, offset + length), headerOffset);
            offset += length;
        }
        return null;
    }

    private static int u16(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }
    private static long u32(byte[] b, int p) {
        return ((long) b[p] & 0xFF)
                | (((long) b[p + 1] & 0xFF) << 8)
                | (((long) b[p + 2] & 0xFF) << 16)
                | (((long) b[p + 3] & 0xFF) << 24);
    }
    private static void require(int limit, int offset, int length, DemoParseException.Code code, String message) {
        if (offset < 0 || length < 0 || offset > limit || length > limit - offset)
            throw error(code, Math.max(0, Math.min(offset, limit)), message);
    }
    private static DemoParseException error(DemoParseException.Code code, int offset, String message) {
        return new DemoParseException(code, offset, message);
    }
}
