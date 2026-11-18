package MeongDev.DIRUNA.GloftDIRU.core.input;

/**
 * Direction query masks observed in the pinned Diamond Rush Java ME source.
 *
 * These are query masks, not Android key codes and not pixel velocities.
 * HeldKeyState.isAnyPressed(mask) preserves the source's any-matching-bit test.
 */
public final class LegacyInputMasks {
    public static final int UP = 4097;
    public static final int RIGHT = 65544;
    public static final int DOWN = 262146;
    public static final int LEFT = 16388;

    private LegacyInputMasks() {
    }
}
