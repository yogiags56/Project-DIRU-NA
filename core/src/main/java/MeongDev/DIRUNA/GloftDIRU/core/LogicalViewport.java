package MeongDev.DIRUNA.GloftDIRU.core;

/**
 * Immutable logical game-space dimensions.
 *
 * This type records the project's initial 240x320 portrait coordinate space.
 * It does not define device scaling, camera behavior, or aspect-ratio adaptation.
 */
public record LogicalViewport(int width, int height) {
    public static final LogicalViewport INITIAL_PORTRAIT =
            new LogicalViewport(240, 320);

    public LogicalViewport {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
    }
}
