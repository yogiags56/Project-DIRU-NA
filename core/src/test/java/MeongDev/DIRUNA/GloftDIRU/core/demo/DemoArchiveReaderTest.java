package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import static org.junit.Assert.*;

public class DemoArchiveReaderTest {
    private static void u16(ByteArrayOutputStream out, int v) {
        out.write(v & 255); out.write((v >>> 8) & 255);
    }
    private static void u32(ByteArrayOutputStream out, long v) {
        out.write((int) v & 255); out.write((int) (v >>> 8) & 255);
        out.write((int) (v >>> 16) & 255); out.write((int) (v >>> 24) & 255);
    }
    private static byte[] chunk(int tag, int count, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        u16(out, 0xCAFE);
        u16(out, tag); u16(out, count); u32(out, payload.length);
        out.write(payload, 0, payload.length);
        return out.toByteArray();
    }
    private static byte[] sprites(int[][] records) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        u16(out, records.length);
        for (int[] record : records) {
            u16(out, record[0]); u32(out, record[1]);
            for (int i = 2; i < record.length; i++) out.write(record[i]);
        }
        return out.toByteArray();
    }
    private static DemoParseException expect(DemoParseException.Code code, Runnable action) {
        try { action.run(); fail("expected " + code); }
        catch (DemoParseException e) { assertEquals(code, e.code()); return e; }
        return null;
    }

    @Test public void selectsFirstMatchingTagAndCopiesPayload() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] first = chunk(7, 2, new byte[]{1, 2});
        byte[] second = chunk(7, 9, new byte[]{3});
        out.write(first, 0, first.length);
        out.write(second, 2, second.length - 2); // append second entry without a second preamble
        DemoChunkReader.Entry entry = DemoChunkReader.findFirst(out.toByteArray(), 7);
        assertNotNull(entry);
        assertEquals(2, entry.recordCount());
        assertArrayEquals(new byte[]{1, 2}, entry.copyPayload());
        byte[] leaked = entry.copyPayload(); leaked[0] = 99;
        assertArrayEquals(new byte[]{1, 2}, entry.copyPayload());
    }

    @Test public void skipsUnknownTagsAndReturnsNullWhenAbsent() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] a = chunk(1, 0, new byte[]{8, 9});
        byte[] b = chunk(2, 1, new byte[]{4});
        out.write(a, 0, a.length); out.write(b, 2, b.length - 2);
        assertArrayEquals(new byte[]{4}, DemoChunkReader.findFirst(out.toByteArray(), 2).copyPayload());
        assertNull(DemoChunkReader.findFirst(out.toByteArray(), 3));
    }

    @Test public void rejectsTruncatedChunkHeaderAndBody() {
        expect(DemoParseException.Code.TRUNCATED_HEADER,
                () -> DemoChunkReader.findFirst(new byte[]{0, 0, 1}, 1));
        byte[] bad = chunk(1, 1, new byte[]{5});
        bad[7] = 10; // declared payload length becomes larger than available
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD,
                () -> DemoChunkReader.findFirst(bad, 1));
    }

    @Test public void spriteArchiveSupportsUnsignedIdsAndSortedRequestedCopies() {
        byte[] archive = sprites(new int[][]{{500, 1, 42}, {2, 2, 7, 8}});
        DemoSpriteArchiveReader reader = DemoSpriteArchiveReader.parse(archive);
        assertEquals(2, reader.spriteCount());
        Map<Integer, byte[]> selected = reader.copyRequested(new int[]{500, 2, 500});
        assertEquals(2, selected.keySet().iterator().next().intValue());
        assertArrayEquals(new byte[]{7, 8}, selected.get(2));
        assertArrayEquals(new byte[]{42}, selected.get(500));
        byte[] leaked = selected.get(2); leaked[0] = 0;
        assertArrayEquals(new byte[]{7, 8}, reader.copyPayload(2));
    }

    @Test public void rejectsDuplicateAndMissingSpriteIds() {
        expect(DemoParseException.Code.DUPLICATE_ID,
                () -> DemoSpriteArchiveReader.parse(sprites(new int[][]{{4, 0}, {4, 0}})));
        DemoSpriteArchiveReader reader = DemoSpriteArchiveReader.parse(sprites(new int[][]{{4, 0}}));
        expect(DemoParseException.Code.MISSING_ID, () -> reader.copyPayload(9));
    }

    @Test public void rejectsTruncatedSpriteHeaderAndBodyAndTrailingBytes() {
        expect(DemoParseException.Code.TRUNCATED_HEADER,
                () -> DemoSpriteArchiveReader.parse(new byte[]{1, 0, 4}));
        byte[] badBody = sprites(new int[][]{{1, 1, 7}});
        badBody[4] = 2;
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD,
                () -> DemoSpriteArchiveReader.parse(badBody));
        byte[] trailing = sprites(new int[][]{{1, 0}});
        byte[] extra = java.util.Arrays.copyOf(trailing, trailing.length + 1);
        expect(DemoParseException.Code.TRAILING_BYTES,
                () -> DemoSpriteArchiveReader.parse(extra));
    }

    @Test public void rejectsInvalidArguments() {
        expect(DemoParseException.Code.INVALID_ARGUMENT,
                () -> DemoChunkReader.findFirst(null, 1));
        expect(DemoParseException.Code.INVALID_ARGUMENT,
                () -> DemoChunkReader.findFirst(new byte[]{0, 0}, 65536));
        expect(DemoParseException.Code.INVALID_ARGUMENT,
                () -> DemoSpriteArchiveReader.parse(null));
    }
}
