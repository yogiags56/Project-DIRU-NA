package MeongDev.DIRUNA.GloftDIRU.core.input;

/**
 * Tile-step vectors for the ordinary movement direction codes.
 *
 * The pinned source stores the opposite-sign vector in gByteArr and passes
 * its negation into the collision helper. These methods expose that resulting
 * logical tile delta, not the raw camera interpolation vector.
 */
public final class LegacyDirectionVector {
    private LegacyDirectionVector() {
    }

    public static int tileDeltaX(int direction) {
        switch (direction) {
            case LegacyDirectionResolver.UP:
            case LegacyDirectionResolver.DOWN:
                return 0;
            case LegacyDirectionResolver.RIGHT:
                return 1;
            case LegacyDirectionResolver.LEFT:
                return -1;
            default:
                throw new IllegalArgumentException("Unknown legacy direction: " + direction);
        }
    }

    public static int tileDeltaY(int direction) {
        switch (direction) {
            case LegacyDirectionResolver.UP:
                return -1;
            case LegacyDirectionResolver.RIGHT:
            case LegacyDirectionResolver.LEFT:
                return 0;
            case LegacyDirectionResolver.DOWN:
                return 1;
            default:
                throw new IllegalArgumentException("Unknown legacy direction: " + direction);
        }
    }
}
