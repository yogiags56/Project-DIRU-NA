package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Validated, platform-independent stage grid with distinct source layers.
 * Values are stored as decoded ints so signed-byte widening from the legacy
 * loader can be preserved by the decoder rather than silently normalized here.
 * The constructor defensively copies every layer; this type is immutable.
 */
public final class StageGrid {
    private final int width;
    private final int height;
    private final int[] playerLayer;
    private final int[] backgroundLayer;
    private final int[] foregroundLayer;

    public StageGrid(int width, int height, int[] playerLayer,
                     int[] backgroundLayer, int[] foregroundLayer) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("stage dimensions must be positive");
        }
        final long cellCount = (long) width * (long) height;
        if (cellCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("stage dimensions overflow Java array indexing");
        }
        int expected = (int) cellCount;
        requireLayer("player", playerLayer, expected);
        requireLayer("background", backgroundLayer, expected);
        requireLayer("foreground", foregroundLayer, expected);
        this.width = width;
        this.height = height;
        this.playerLayer = playerLayer.clone();
        this.backgroundLayer = backgroundLayer.clone();
        this.foregroundLayer = foregroundLayer.clone();
    }

    private static void requireLayer(String name, int[] layer, int expected) {
        if (layer == null || layer.length != expected) {
            throw new IllegalArgumentException(name + " layer length must equal width * height");
        }
    }

    public int width() { return width; }
    public int height() { return height; }
    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
    public int playerAt(int x, int y) { return playerLayer[index(x, y)]; }
    public int backgroundAt(int x, int y) { return backgroundLayer[index(x, y)]; }
    public int foregroundAt(int x, int y) { return foregroundLayer[index(x, y)]; }
    public int[] copyPlayerLayer() { return playerLayer.clone(); }
    public int[] copyBackgroundLayer() { return backgroundLayer.clone(); }
    public int[] copyForegroundLayer() { return foregroundLayer.clone(); }

    private int index(int x, int y) {
        if (!contains(x, y)) {
            throw new IndexOutOfBoundsException("stage coordinate out of bounds: " + x + "," + y);
        }
        return x + y * width;
    }
}
