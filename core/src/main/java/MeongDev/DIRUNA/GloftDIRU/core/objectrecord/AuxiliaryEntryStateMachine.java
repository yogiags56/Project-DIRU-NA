package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

/**
 * Conservative pure model of source-proven bLongArr transitions.
 * Unknown packed bits are preserved during state changes and are discarded
 * only by the source-observed whole-entry consume operation.
 */
public final class AuxiliaryEntryStateMachine {
    private static final long STATE_MASK = 0xFL;
    private static final long DIRECTION_MASK = 0x3L << 4;
    private static final long MOVEMENT_MASK = 0x7FL << 6;
    private static final long X_MASK = 0x7FL << 13;
    private static final long Y_MASK = 0x7FL << 20;

    public static final int STATE_EMPTY = 0;
    public static final int STATE_ALLOCATED = 1;
    public static final int STATE_READY = 3;

    public enum Status {
        OK, INVALID_SLOT, UNREPRESENTABLE_LINK, LINK_MISMATCH,
        WRONG_STATE, MOVEMENT_NOT_READY
    }

    public record TransitionResult(Status status, long rawEntry) {
        public boolean succeeded() { return status == Status.OK; }
    }

    public record ConsumeResult(Status status, long remainingRawEntry,
                                int x, int y, int direction) {
        public boolean succeeded() { return status == Status.OK; }
    }

    private AuxiliaryEntryStateMachine() { }

    public static int state(long raw) { return (int) (raw & STATE_MASK); }
    public static int direction(long raw) { return (int) ((raw & DIRECTION_MASK) >>> 4); }
    public static int movementValue(long raw) { return (int) ((raw & MOVEMENT_MASK) >>> 6); }
    public static int x(long raw) { return (int) ((raw & X_MASK) >>> 13); }
    public static int y(long raw) { return (int) ((raw & Y_MASK) >>> 20); }

    /**
     * Applies the source-observed state-1 -> state-3 transition.
     * The object record's three-bit link must resolve to the supplied slot,
     * and the source-proven movement field (bits 6..12) must be zero.
     */
    public static TransitionResult markReady(long rawEntry, int zeroBasedSlot,
                                             int objectRecordRawLink) {
        if (zeroBasedSlot < 0 || zeroBasedSlot >= AuxiliaryLinkCodec.AUXILIARY_SLOT_COUNT)
            return new TransitionResult(Status.INVALID_SLOT, rawEntry);
        AuxiliaryLinkCodec.EncodeResult safe = AuxiliaryLinkCodec.encodeSafe(zeroBasedSlot);
        if (!safe.succeeded())
            return new TransitionResult(Status.UNREPRESENTABLE_LINK, rawEntry);
        if (AuxiliaryLinkCodec.decode(objectRecordRawLink) != zeroBasedSlot)
            return new TransitionResult(Status.LINK_MISMATCH, rawEntry);
        if (state(rawEntry) != STATE_ALLOCATED)
            return new TransitionResult(Status.WRONG_STATE, rawEntry);
        if (movementValue(rawEntry) != 0)
            return new TransitionResult(Status.MOVEMENT_NOT_READY, rawEntry);

        long updated = (rawEntry & ~STATE_MASK) | STATE_READY;
        return new TransitionResult(Status.OK, updated);
    }

    /** Consumes a state-3 entry and clears the entire 64-bit word, as observed in source. */
    public static ConsumeResult consume(long rawEntry) {
        if (state(rawEntry) != STATE_READY)
            return new ConsumeResult(Status.WRONG_STATE, rawEntry, -1, -1, -1);
        return new ConsumeResult(Status.OK, 0L, x(rawEntry), y(rawEntry), direction(rawEntry));
    }
}
