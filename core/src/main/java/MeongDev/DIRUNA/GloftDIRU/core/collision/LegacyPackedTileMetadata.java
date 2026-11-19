package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Models the legacy six-argument tile metadata setter.
 *
 * <p>The packed int array and paired byte-state array correspond to the
 * original eIntArrArr and cByteArrArr fields. The byte-state writes are
 * intentionally part of the same operation as the packed-field update.</p>
 */
public final class LegacyPackedTileMetadata {
    private static final byte MARKED_VALUE = 24;

    private final int[][] packedWords;
    private final byte[][] adjacentStates;

    public LegacyPackedTileMetadata(int[][] packedWords, byte[][] adjacentStates) {
        if (packedWords == null || adjacentStates == null) {
            throw new IllegalArgumentException("tile metadata arrays must not be null");
        }
        this.packedWords = packedWords;
        this.adjacentStates = adjacentStates;
    }

    /**
     * Reads one field using the source's 9-bit group stride and int shifts.
     */
    public int read(int row, int column, int group, int bitOffset, int width) {
        int shift = validateField(group, bitOffset, width);
        validateCell(row, column);
        return readPackedField(packedWords[row][column], shift, width);
    }

    /**
     * Reproduces the six source arguments:
     * (row, column, group, value, bitOffset, width).
     *
     * <p>Successful writes preserve the original int expression exactly:
     * (word ^ (current << shift)) | (value << shift). In particular, the
     * value is not pre-masked and both shifts remain 32-bit int operations.
     * The same cell and its next column in the auxiliary byte array are then
     * assigned 24.</p>
     */
    public void write(
            int row,
            int column,
            int group,
            int value,
            int bitOffset,
            int width) {
        int shift = validateField(group, bitOffset, width);
        validateCell(row, column);

        int word = packedWords[row][column];
        int current = readPackedField(word, shift, width);

        // Keep Java int shift/overflow semantics from the original helper.
        packedWords[row][column] = (word ^ (current << shift)) | (value << shift);
        adjacentStates[row][column] = MARKED_VALUE;
        adjacentStates[row][column + 1] = MARKED_VALUE;
    }

    private void validateCell(int row, int column) {
        if (row < 0 || row >= packedWords.length
                || row >= adjacentStates.length
                || packedWords[row] == null
                || adjacentStates[row] == null
                || column < 0
                || column >= packedWords[row].length
                || column + 1 >= adjacentStates[row].length) {
            throw new IllegalArgumentException("tile cell or adjacent byte-state cell is out of range");
        }
    }

    private static int validateField(int group, int bitOffset, int width) {
        if (group < 0 || group > 2 || bitOffset < 0 || bitOffset > 8
                || width < 1 || width > 9) {
            throw new IllegalArgumentException("invalid packed tile field");
        }
        int shift = group * 9 + bitOffset;
        if (shift + width > Integer.SIZE) {
            throw new IllegalArgumentException("packed tile field exceeds 32 bits");
        }
        return shift;
    }

    private static int readPackedField(int word, int shift, int width) {
        return (word >>> shift) & ~(-1 << width);
    }
}
