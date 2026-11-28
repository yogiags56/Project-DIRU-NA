package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.Arrays;

/** Reads the sprite-ID prefix of a selected demo payload without decoding script records. */
public final class DemoPayloadHeaderReader {
    private static final int HEADER_PREFIX_SIZE = 2;
    private static final int MAX_LEGACY_SIGNED_ID = Short.MAX_VALUE;

    public static final class Header {
        private final int declaredSpriteCount;
        private final int[] spriteIdsInSourceOrder;
        private final int[] sortedUniqueSpriteIds;
        private final byte[] scriptRecordBytes;
        private final int scriptRecordOffset;

        private Header(int count, int[] ids, byte[] scriptBytes, int offset) {
            declaredSpriteCount = count;
            spriteIdsInSourceOrder = ids.clone();
            int[] sorted = ids.clone();
            Arrays.sort(sorted);
            int uniqueCount = 0;
            for (int id : sorted) if (uniqueCount == 0 || sorted[uniqueCount - 1] != id) sorted[uniqueCount++] = id;
            sortedUniqueSpriteIds = Arrays.copyOf(sorted, uniqueCount);
            scriptRecordBytes = scriptBytes.clone();
            scriptRecordOffset = offset;
        }
        public int declaredSpriteCount() { return declaredSpriteCount; }
        public int[] spriteIdsInSourceOrder() { return spriteIdsInSourceOrder.clone(); }
        /** Sorted and deduplicated IDs for safe archive/cache requests. */
        public int[] sortedUniqueSpriteIds() { return sortedUniqueSpriteIds.clone(); }
        /** Bytes after the ID list; interpretation is deliberately deferred. */
        public byte[] copyScriptRecordBytes() { return scriptRecordBytes.clone(); }
        public int scriptRecordOffset() { return scriptRecordOffset; }
    }

    private DemoPayloadHeaderReader() { }

    public static Header parse(byte[] payload) {
        if (payload == null) throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "payload is required");
        require(payload.length, 0, HEADER_PREFIX_SIZE, DemoParseException.Code.TRUNCATED_HEADER, "missing sprite count");
        int count = u16(payload, 0);
        long idsLength = (long) count * 2L;
        if (idsLength > payload.length - (long) HEADER_PREFIX_SIZE)
            throw error(DemoParseException.Code.TRUNCATED_PAYLOAD, HEADER_PREFIX_SIZE, "sprite ID list exceeds payload boundary");
        int[] ids = new int[count];
        int offset = HEADER_PREFIX_SIZE;
        for (int i = 0; i < count; i++, offset += 2) {
            int id = u16(payload, offset);
            // Legacy code casts IDs to signed short and uses them as sprite-cache indices.
            if (id > MAX_LEGACY_SIGNED_ID)
                throw error(DemoParseException.Code.INVALID_ARGUMENT, offset, "sprite ID exceeds legacy signed-short index range");
            ids[i] = id;
        }
        return new Header(count, ids, Arrays.copyOfRange(payload, offset, payload.length), offset);
    }

    private static int u16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }
    private static void require(int limit, int offset, int length, DemoParseException.Code code, String message) {
        if (offset < 0 || length < 0 || offset > limit || length > limit - offset)
            throw error(code, Math.max(0, Math.min(offset, limit)), message);
    }
    private static DemoParseException error(DemoParseException.Code code, int offset, String message) {
        return new DemoParseException(code, offset, message);
    }
}
