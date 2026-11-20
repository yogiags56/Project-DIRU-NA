package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static org.junit.Assert.*;
import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.AuxiliaryLinkCodec.*;

public final class AuxiliaryLinkCodecTest {
    @Test
    public void safeEncoderAcceptsExactlyTheEightRepresentableSlots() {
        for (int slot = 0; slot <= 7; slot++) {
            EncodeResult result = encodeSafe(slot);
            assertEquals(Status.OK, result.status());
            assertEquals(slot, result.encodedValue());
            assertEquals(slot, decode(result.encodedValue()));
            assertTrue(roundTripsExactly(slot));
        }
    }

    @Test
    public void safeEncoderRejectsSlotsEightThroughFourteenWithoutAliasing() {
        for (int slot = 8; slot <= 14; slot++) {
            EncodeResult result = encodeSafe(slot);
            assertEquals(Status.UNREPRESENTABLE_SLOT, result.status());
            assertEquals(-1, result.encodedValue());
            assertFalse(roundTripsExactly(slot));
        }
    }

    @Test
    public void legacyTruncationIsExplicitAndDocumentsAliasing() {
        for (int slot = 0; slot < 15; slot++) {
            EncodeResult result = encodeLegacyTruncating(slot);
            assertEquals(Status.OK, result.status());
            assertEquals(slot & 7, result.encodedValue());
            assertEquals(slot & 7, decode(result.encodedValue()));
        }
        assertEquals(0, encodeLegacyTruncating(8).encodedValue());
        assertEquals(1, encodeLegacyTruncating(9).encodedValue());
        assertEquals(6, encodeLegacyTruncating(14).encodedValue());
    }

    @Test
    public void invalidSlotIndicesAreRejectedByBothEncoders() {
        for (int slot : new int[]{-1, 15, 16, Integer.MAX_VALUE}) {
            assertEquals(Status.INVALID_SLOT, encodeSafe(slot).status());
            assertEquals(Status.INVALID_SLOT, encodeLegacyTruncating(slot).status());
        }
    }

    @Test
    public void decoderMasksToThreeBits() {
        assertEquals(0, decode(0));
        assertEquals(7, decode(7));
        assertEquals(0, decode(8));
        assertEquals(7, decode(15));
        assertEquals(7, decode(-1));
    }
}
