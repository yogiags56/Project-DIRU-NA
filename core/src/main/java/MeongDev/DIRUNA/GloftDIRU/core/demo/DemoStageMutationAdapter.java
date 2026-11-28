package MeongDev.DIRUNA.GloftDIRU.core.demo;

/**
 * Bounds-checked boundary for the immediate stage-cell writes performed by
 * legacy demo callbacks. Coordinates follow the source's [x][y] array order.
 *
 * This class intentionally knows nothing about Android, rendering, entities,
 * camera state, or interpreter scheduling.
 */
public final class DemoStageMutationAdapter {
    public interface StageGrid {
        int width();
        int height();
        void setPlayerLayer(int x, int y, byte value);
        void setBackground(int x, int y, int value);
        void setForeground(int x, int y, short value);
    }

    public static final class StageBoundsException extends RuntimeException {
        private final int x;
        private final int y;
        private StageBoundsException(int x, int y, int width, int height) {
            super("stage coordinate [" + x + "][" + y + "] outside " + width + "x" + height);
            this.x = x;
            this.y = y;
        }
        public int x() { return x; }
        public int y() { return y; }
    }

    private final StageGrid stage;

    public DemoStageMutationAdapter(StageGrid stage) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        if (stage.width() < 0 || stage.height() < 0)
            throw new IllegalArgumentException("stage dimensions must be non-negative");
        this.stage = stage;
    }

    /** Legacy callback shared by opcode 5, 8, and 9. */
    public void applyCallback(int x, int y, byte playerLayer, int backgroundValue) {
        requireCell(x, y);
        // Preserve source order: player-layer write precedes background write.
        stage.setPlayerLayer(x, y, playerLayer);
        stage.setBackground(x, y, backgroundValue);
    }

    /** Opcode 25's direct foreground-grid write. */
    public void setForegroundCell(int x, int y, short value) {
        requireCell(x, y);
        stage.setForeground(x, y, value);
    }

    /** Opcode 26's direct background-grid write. */
    public void setBackgroundCell(int x, int y, int value) {
        requireCell(x, y);
        stage.setBackground(x, y, value);
    }

    private void requireCell(int x, int y) {
        int width = stage.width();
        int height = stage.height();
        if (x < 0 || y < 0 || x >= width || y >= height)
            throw new StageBoundsException(x, y, width, height);
    }
}
