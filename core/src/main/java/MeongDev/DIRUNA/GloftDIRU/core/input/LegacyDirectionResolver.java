package MeongDev.DIRUNA.GloftDIRU.core.input;

/**
 * Resolves the normal in-stage directional input using the source branch order.
 *
 * This resolver is only for the ordinary movement branch (aTInt <= 0 in the
 * audited source). Special movement, modal gates, center/action keys, and
 * contextual actions are handled by their own state-specific paths.
 */
public final class LegacyDirectionResolver {
    public static final int NONE = 0;
    public static final int UP = 1;
    public static final int RIGHT = 2;
    public static final int DOWN = 3;
    public static final int LEFT = 4;

    private LegacyDirectionResolver() {
    }

    /**
     * Applies the source's ordered if/else-if checks:
     * up, down, left, then right.
     *
     * @return the legacy direction code, or NONE if no direction is held
     */
    public static int resolveNormalStageDirection(HeldKeyState keys) {
        if (keys.isAnyPressed(LegacyInputMasks.UP)) {
            return UP;
        } else if (keys.isAnyPressed(LegacyInputMasks.DOWN)) {
            return DOWN;
        } else if (keys.isAnyPressed(LegacyInputMasks.LEFT)) {
            return LEFT;
        } else if (keys.isAnyPressed(LegacyInputMasks.RIGHT)) {
            return RIGHT;
        }
        return NONE;
    }
}
