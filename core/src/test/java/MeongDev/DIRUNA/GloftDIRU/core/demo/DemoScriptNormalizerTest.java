package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoScriptNormalizerTest {
    private static byte[] script(int count, int... body) {
        byte[] out = new byte[2 + body.length];
        out[0] = (byte) count;
        out[1] = (byte) (count >>> 8);
        for (int i = 0; i < body.length; i++) out[i + 2] = (byte) body[i];
        return out;
    }
    private static byte[] normalize(int count, int... body) {
        return DemoScriptNormalizer.normalize(DemoScriptRecordDecoder.decode(script(count, body))).copyRecord(0);
    }

    @Test public void normalizesOpcode1CoordinatesAndSentinels() {
        assertArrayEquals(new byte[]{1,0, 48,0, 72,0, 5,0, 0x10,0x27, 0x10,0x27},
                normalize(1, 1, 2,0, 3,0, 5,0));
    }

    @Test public void normalizesVariableTextOpcode2() {
        assertArrayEquals(new byte[]{2,0, 2,0, 0x10,0x27, 0x34,0x12, 7,0,0, 65,66},
                normalize(1, 2, 7, 0x34,0x12, 2,0, 65,66));
    }

    @Test public void normalizesOpcode18WithLegacyOverlapping24BitWrite() {
        assertArrayEquals(new byte[]{18,0, 5,0x33,0x22,0x11,0,0},
                normalize(1, 18, 5, 0x33,0x22,0x11));
    }

    @Test public void normalizesOpcode27WithSentinelAndPayload() {
        assertArrayEquals(new byte[]{27,0, 2,0, 0x10,0x27, 0,0, 9,8},
                normalize(1, 27, 2,0, 9,8));
    }

    @Test public void packsGroupWithLittleEndianChildLengthTable() {
        byte[] expected = new byte[]{0,2, 2,0,0,0, 3,0,0,0, 7,0, 10,0,(byte)255};
        assertArrayEquals(expected, normalize(1, 0, 2, 7, 10, 255));
    }

    @Test public void rejectsLegacySignedLengthOverflowDuringNormalization() {
        byte[] oversized = new byte[2 + 3 + 32768];
        oversized[0] = 1;
        oversized[2] = 27;
        oversized[3] = 0;
        oversized[4] = (byte) 0x80;
        DemoScriptRecordDecoder.DecodedScript decoded =
                DemoScriptRecordDecoder.decode(oversized);
        try {
            DemoScriptNormalizer.normalize(decoded);
            fail("expected invalid legacy signed length");
        } catch (DemoParseException e) {
            assertEquals(DemoParseException.Code.INVALID_LENGTH, e.code());
        }
    }

    @Test public void normalizedRecordsAreDefensivelyCopied() {
        DemoScriptNormalizer.NormalizedScript normalized =
                DemoScriptNormalizer.normalize(DemoScriptRecordDecoder.decode(script(1, 7)));
        byte[] first = normalized.copyRecord(0);
        first[0] = 99;
        assertArrayEquals(new byte[]{7,0}, normalized.copyRecord(0));
    }
}
