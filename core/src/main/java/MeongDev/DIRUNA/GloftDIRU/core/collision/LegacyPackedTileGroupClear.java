package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Reproduces the legacy three-argument packed tile group clear operation.
 * This is intentionally separate from LegacyPackedTileMetadata.write:
 * clearing one 9-bit group refreshes only the addressed byte-state cell.
 */
public final class LegacyPackedTileGroupClear {
    private static final byte REFRESH_VALUE = 24;

    private final int[][] packedWords;
    private final byte[][] cellStates;

    public LegacyPackedTileGroupClear(int[][] packedWords, byte[][] cellStates) {
        if (packedWords == null || cellStates == null) {
            throw new IllegalArgumentException("tile arrays must not be null");
        }
        this.packedWords = packedWords;
        this.cellStates = cellStates;
    }

    public void clear(int row, int column, int group) {
        if (group < 0 || group > 2) {
            throw new IllegalArgumentException("group must be in range 0..2");
        }
        if (row < 0 || row >= packedWords.length || row >= cellStates.length
                || packedWords[row] == null || cellStates[row] == null
                || column < 0 || column >= packedWords[row].length
                || column >= cellStates[row].length) {
            throw new IllegalArgumentException("tile cell is out of range");
        }

        int word = packedWords[row][column];
        int shift = group * 9;
        int currentGroup = (word >>> shift) & 0x1FF;
        packedWords[row][column] = word ^ (currentGroup << shift);
        cellStates[row][column] = REFRESH_VALUE;
    }
}
