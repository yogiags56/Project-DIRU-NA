package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoPayloadHeaderReaderTest {
    @Test public void parsesCountIdsAndPreservesFollowingScriptBytes() {
        byte[] payload = {3, 0, 9, 0, 2, 0, 9, 0, 0x41, 0x42};
        DemoPayloadHeaderReader.Header header = DemoPayloadHeaderReader.parse(payload);
        assertEquals(3, header.declaredSpriteCount());
        assertArrayEquals(new int[]{9, 2, 9}, header.spriteIdsInSourceOrder());
        assertArrayEquals(new int[]{2, 9}, header.sortedUniqueSpriteIds());
        assertEquals(8, header.scriptRecordOffset());
        assertArrayEquals(new byte[]{0x41, 0x42}, header.copyScriptRecordBytes());
    }

    @Test public void acceptsZeroIdsAndEmptyScriptTail() {
        DemoPayloadHeaderReader.Header header = DemoPayloadHeaderReader.parse(new byte[]{0, 0});
        assertEquals(0, header.declaredSpriteCount());
        assertArrayEquals(new int[0], header.sortedUniqueSpriteIds());
        assertArrayEquals(new byte[0], header.copyScriptRecordBytes());
    }

    @Test public void rejectsTruncatedCountAndIdList() {
        expect(DemoParseException.Code.TRUNCATED_HEADER, () -> DemoPayloadHeaderReader.parse(new byte[]{1}));
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD, () -> DemoPayloadHeaderReader.parse(new byte[]{2, 0, 7, 0}));
    }

    @Test public void rejectsIdsOutsideLegacyPositiveShortRange() {
        expect(DemoParseException.Code.INVALID_ARGUMENT,
                () -> DemoPayloadHeaderReader.parse(new byte[]{1, 0, 0, (byte) 0x80}));
    }

    @Test public void returnsDefensiveCopies() {
        DemoPayloadHeaderReader.Header header = DemoPayloadHeaderReader.parse(new byte[]{1, 0, 3, 0, 5});
        int[] ids = header.spriteIdsInSourceOrder(); ids[0] = 99;
        byte[] tail = header.copyScriptRecordBytes(); tail[0] = 0;
        assertArrayEquals(new int[]{3}, header.spriteIdsInSourceOrder());
        assertArrayEquals(new byte[]{5}, header.copyScriptRecordBytes());
    }

    private static void expect(DemoParseException.Code code, Runnable action) {
        try { action.run(); fail("expected " + code); }
        catch (DemoParseException e) { assertEquals(code, e.code()); }
    }
}
