package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

/**
 * Explicit codec for the legacy three-bit object-record -> auxiliary-slot link.
 *
 * Safe encoding rejects auxiliary slots that cannot be represented. The
 * legacy encoder is deliberately named and isolated because it truncates
 * values to the low three bits.
 */
public final class AuxiliaryLinkCodec {
    public static final int LEGACY_LINK_WIDTH = 3;
    public static final int LEGACY_LINK_CAPACITY = 1 << LEGACY_LINK_WIDTH;
    public static final int AUXILIARY_SLOT_COUNT = 15;

    public enum Status { OK, INVALID_SLOT, UNREPRESENTABLE_SLOT }

    public record EncodeResult(Status status, int encodedValue) {
        public boolean succeeded() { return status == Status.OK; }
    }

    private AuxiliaryLinkCodec() { }

    /** Safe native path: only indices 0..7 can be represented without loss. */
    public static EncodeResult encodeSafe(int zeroBasedSlot) {
        if (zeroBasedSlot < 0 || zeroBasedSlot >= AUXILIARY_SLOT_COUNT)
            return new EncodeResult(Status.INVALID_SLOT, -1);
        if (zeroBasedSlot >= LEGACY_LINK_CAPACITY)
            return new EncodeResult(Status.UNREPRESENTABLE_SLOT, -1);
        return new EncodeResult(Status.OK, zeroBasedSlot);
    }

    /** Decodes the raw three-bit field. Every possible input is normalized to 0..7. */
    public static int decode(int rawFieldValue) {
        return rawFieldValue & (LEGACY_LINK_CAPACITY - 1);
    }

    /**
     * Reproduces low-three-bit truncation explicitly for forensic compatibility
     * tests only. Never use this method in the safe native allocation path.
     */
    public static EncodeResult encodeLegacyTruncating(int zeroBasedSlot) {
        if (zeroBasedSlot < 0 || zeroBasedSlot >= AUXILIARY_SLOT_COUNT)
            return new EncodeResult(Status.INVALID_SLOT, -1);
        return new EncodeResult(Status.OK, zeroBasedSlot & (LEGACY_LINK_CAPACITY - 1));
    }

    /** Returns true when decoding the encoded field recovers the original slot. */
    public static boolean roundTripsExactly(int zeroBasedSlot) {
        EncodeResult result = encodeSafe(zeroBasedSlot);
        return result.succeeded() && decode(result.encodedValue()) == zeroBasedSlot;
    }
}
