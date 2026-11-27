package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, immutable index of the little-endian demoSpr.bin sprite archive. */
public final class DemoSpriteArchiveReader {
    private final Map<Integer, byte[]> payloads;

    private DemoSpriteArchiveReader(Map<Integer, byte[]> source) {
        Map<Integer, byte[]> copy = new LinkedHashMap<Integer, byte[]>();
        for (Map.Entry<Integer, byte[]> entry : source.entrySet())
            copy.put(entry.getKey(), entry.getValue().clone());
        payloads = Collections.unmodifiableMap(copy);
    }

    public static DemoSpriteArchiveReader parse(byte[] archive) {
        if (archive == null) throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "archive is required");
        require(archive.length, 0, 2, DemoParseException.Code.TRUNCATED_HEADER, "missing sprite count");
        int count = u16(archive, 0);
        int offset = 2;
        Map<Integer, byte[]> entries = new LinkedHashMap<Integer, byte[]>();

        for (int i = 0; i < count; i++) {
            int headerOffset = offset;
            require(archive.length, offset, 6, DemoParseException.Code.TRUNCATED_HEADER,
                    "incomplete sprite record header");
            int id = u16(archive, offset);
            long declaredLength = u32(archive, offset + 2);
            offset += 6;
            if (declaredLength > archive.length - (long) offset)
                throw error(DemoParseException.Code.TRUNCATED_PAYLOAD, headerOffset + 2,
                        "sprite payload exceeds archive boundary");
            if (entries.containsKey(id))
                throw error(DemoParseException.Code.DUPLICATE_ID, headerOffset,
                        "duplicate sprite ID " + id);
            int length = (int) declaredLength;
            byte[] payload = new byte[length];
            System.arraycopy(archive, offset, payload, 0, length);
            entries.put(id, payload);
            offset += length;
        }
        if (offset != archive.length)
            throw error(DemoParseException.Code.TRAILING_BYTES, offset,
                    "bytes remain after declared sprite records");
        return new DemoSpriteArchiveReader(entries);
    }

    public int spriteCount() { return payloads.size(); }
    public boolean contains(int id) { return payloads.containsKey(id); }

    /** Returns a detached payload copy or throws MISSING_ID. */
    public byte[] copyPayload(int id) {
        byte[] payload = payloads.get(id);
        if (payload == null)
            throw error(DemoParseException.Code.MISSING_ID, 0, "sprite ID not found: " + id);
        return payload.clone();
    }

    /** Returns detached copies for the requested IDs, in ascending ID order. */
    public Map<Integer, byte[]> copyRequested(int[] requestedIds) {
        if (requestedIds == null)
            throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "requested IDs are required");
        int[] sorted = requestedIds.clone();
        java.util.Arrays.sort(sorted);
        Map<Integer, byte[]> result = new LinkedHashMap<Integer, byte[]>();
        for (int id : sorted) {
            if (id < 0 || id > 0xFFFF)
                throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "sprite ID must be unsigned 16-bit");
            if (!result.containsKey(id)) result.put(id, copyPayload(id));
        }
        return Collections.unmodifiableMap(result);
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
