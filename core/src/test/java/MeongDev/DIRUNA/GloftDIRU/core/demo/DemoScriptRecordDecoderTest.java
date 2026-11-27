package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoScriptRecordDecoderTest {
    private static byte[] script(int count, int... body) {
        byte[] out = new byte[2 + body.length];
        out[0] = (byte) count; out[1] = (byte) (count >>> 8);
        for (int i = 0; i < body.length; i++) out[i + 2] = (byte) body[i];
        return out;
    }
    private static DemoParseException expect(DemoParseException.Code code, Runnable action) {
        try { action.run(); fail("expected " + code); }
        catch (DemoParseException e) { assertEquals(code, e.code()); return e; }
        return null;
    }

    @Test public void decodesAllObservedFixedAndVariableOpcodeShapes() {
        byte[] input = script(17,
            1, 1,0, 2,0, 3,0,
            2, 7, 8,0, 2,0, 65,66,
            4, 1,0,2,0,3,0,4,0,5,0,6,0,7,0,
            5, 1,0,2,0,3,
            6, 1,2,3,4,
            7,
            9, 1,0,2,0,3,0,
            10, 255,
            11, 1,0,2,0,
            12, 3,0,4,0,
            13, 1,0,2,0,3,0,
            14, 15,
            16, 1,0, 2,
            17, 3,0, 4,
            18, 5, 255,255,255,
            25, 1,0,2,0,3,4,
            26, 5,0,6,0,1,2,3,4,
            27, 2,0, 9,8
        );
        DemoScriptRecordDecoder.DecodedScript decoded = DemoScriptRecordDecoder.decode(input);
        assertEquals(17, decoded.records().size());
        assertEquals(0xFFFFFFL, decoded.records().get(15).operands()[1]);
        assertArrayEquals(new byte[]{65,66}, decoded.records().get(1).copyData());
        assertArrayEquals(new byte[]{9,8}, decoded.records().get(16).copyData());
    }

    @Test public void parsesOneLevelGroupAndPreservesWireBytes() {
        DemoScriptRecordDecoder.DecodedScript decoded =
            DemoScriptRecordDecoder.decode(script(1, 0, 2, 7, 14));
        DemoScriptRecordDecoder.Record group = decoded.records().get(0);
        assertTrue(group.isGroup());
        assertEquals(2, group.children().size());
        assertEquals(7, group.children().get(0).opcode());
        assertEquals(14, group.children().get(1).opcode());
        assertArrayEquals(new byte[]{0,2,7,14}, group.copyWireBytes());
    }

    @Test public void acceptsZeroRecords() {
        assertEquals(0, DemoScriptRecordDecoder.decode(script(0)).records().size());
    }

    @Test public void rejectsUnknownOpcodeAndNestedGroup() {
        expect(DemoParseException.Code.UNKNOWN_OPCODE,
            () -> DemoScriptRecordDecoder.decode(script(1, 3)));
        expect(DemoParseException.Code.NESTED_GROUP,
            () -> DemoScriptRecordDecoder.decode(script(1, 0, 1, 0, 0)));
    }

    @Test public void rejectsTruncatedOperandsAndVariablePayloads() {
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD,
            () -> DemoScriptRecordDecoder.decode(script(1, 1, 0)));
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD,
            () -> DemoScriptRecordDecoder.decode(script(1, 2, 1, 0, 2, 0, 65)));
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD,
            () -> DemoScriptRecordDecoder.decode(script(1, 27, 2, 0, 65)));
        expect(DemoParseException.Code.TRUNCATED_PAYLOAD,
            () -> DemoScriptRecordDecoder.decode(script(1, 18, 1, 1, 2)));
    }

    @Test public void rejectsInvalidCountsAndTrailingBytes() {
        expect(DemoParseException.Code.INVALID_COUNT,
            () -> DemoScriptRecordDecoder.decode(new byte[]{0, (byte) 0x80}));
        expect(DemoParseException.Code.TRAILING_BYTES,
            () -> DemoScriptRecordDecoder.decode(script(0, 7)));
        expect(DemoParseException.Code.TRUNCATED_HEADER,
            () -> DemoScriptRecordDecoder.decode(new byte[]{1}));
    }

    @Test public void recordsAreDefensivelyCopied() {
        byte[] input = script(1, 2, 1, 3, 0, 1, 0, 65);
        DemoScriptRecordDecoder.Record record =
            DemoScriptRecordDecoder.decode(input).records().get(0);
        input[2] = 99;
        byte[] data = record.copyData(); data[0] = 0;
        long[] operands = record.operands(); operands[0] = 99;
        assertEquals(2, record.opcode());
        assertArrayEquals(new byte[]{65}, record.copyData());
        assertEquals(1L, record.operands()[0]);
    }
}
