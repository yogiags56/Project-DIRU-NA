package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoPayloadScriptDecoderTest {
    private static byte[] payload(int... bytes) {
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) out[i] = (byte) bytes[i];
        return out;
    }

    @Test public void composesSpriteHeaderWireDecoderAndNormalizer() {
        DemoPayloadScriptDecoder.DecodedPayload decoded =
                DemoPayloadScriptDecoder.decode(payload(
                        2,0, 9,0, 3,0,
                        2,0,
                        7,
                        10, 255
                ));
        assertEquals(2, decoded.declaredSpriteCount());
        assertArrayEquals(new int[]{9,3}, decoded.spriteIdsInSourceOrder());
        assertArrayEquals(new int[]{3,9}, decoded.sortedUniqueSpriteIds());
        assertEquals(2, decoded.scriptRecordCount());
        assertEquals(7, decoded.wireRecordOpcode(0));
        assertEquals(2, decoded.wireRecordOffset(0));
        assertArrayEquals(new byte[]{7}, decoded.copyWireRecord(0));
        assertArrayEquals(new byte[]{7,0}, decoded.copyNormalizedRecord(0));
        assertArrayEquals(new byte[]{10,(byte)255}, decoded.copyWireRecord(1));
        assertArrayEquals(new byte[]{10,0,(byte)255}, decoded.copyNormalizedRecord(1));
    }

    @Test public void composesOneLevelGroupWithoutExecutingIt() {
        DemoPayloadScriptDecoder.DecodedPayload decoded =
                DemoPayloadScriptDecoder.decode(payload(0,0, 1,0, 0,1,7));
        assertEquals(1, decoded.scriptRecordCount());
        assertEquals(0, decoded.wireRecordOpcode(0));
        assertArrayEquals(new byte[]{0,1,2,0,0,0,7,0},
                decoded.copyNormalizedRecord(0));
    }

    @Test public void rejectsMalformedScriptAfterValidSpriteHeader() {
        try {
            DemoPayloadScriptDecoder.decode(payload(1,0, 4,0, 1,0, 1,1,0));
            fail("expected truncated script record");
        } catch (DemoParseException e) {
            assertEquals(DemoParseException.Code.TRUNCATED_PAYLOAD, e.code());
        }
    }

    @Test public void decodedPayloadReturnsDetachedArrays() {
        DemoPayloadScriptDecoder.DecodedPayload decoded =
                DemoPayloadScriptDecoder.decode(payload(0,0, 1,0, 7));
        int[] ids = decoded.sortedUniqueSpriteIds();
        byte[] wire = decoded.copyWireRecord(0);
        byte[] normalized = decoded.copyNormalizedRecord(0);
        if (ids.length > 0) ids[0] = 99;
        wire[0] = 99;
        normalized[0] = 99;
        assertArrayEquals(new int[0], decoded.sortedUniqueSpriteIds());
        assertArrayEquals(new byte[]{7}, decoded.copyWireRecord(0));
        assertArrayEquals(new byte[]{7,0}, decoded.copyNormalizedRecord(0));
    }
}
