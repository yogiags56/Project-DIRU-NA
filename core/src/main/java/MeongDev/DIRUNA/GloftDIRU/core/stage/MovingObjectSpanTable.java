package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Immutable read-only view of the legacy three-byte moving-object span table.
 *
 * The source runtime stores records as [x0, x1, y] byte triples. This type
 * preserves the signed byte values and defensive-copy boundary without
 * claiming to rebuild, update, or persist the legacy table. The table is
 * derived from stage layers and packed slot metadata during stage setup and
 * restore.
 */
public final class MovingObjectSpanTable {
    public record Span(byte firstX, byte secondX, byte rowY) { }

    private final byte[] triples;

    public MovingObjectSpanTable(byte[] triples) {
        if (triples == null) {
            throw new IllegalArgumentException("span triples are required");
        }
        if (triples.length % 3 != 0) {
            throw new IllegalArgumentException("span table length must be divisible by three");
        }
        this.triples = triples.clone();
    }

    /** Number of fixed-width three-byte records. */
    public int size() {
        return triples.length / 3;
    }

    /** Returns the raw signed-byte fields for one zero-based slot. */
    public Span spanAt(int slot) {
        if (slot < 0 || slot >= size()) {
            throw new IndexOutOfBoundsException("span slot out of range: " + slot);
        }
        int offset = slot * 3;
        return new Span(triples[offset], triples[offset + 1], triples[offset + 2]);
    }

    /** Returns a detached copy of the exact legacy triple representation. */
    public byte[] copyRawTriples() {
        return triples.clone();
    }
}
