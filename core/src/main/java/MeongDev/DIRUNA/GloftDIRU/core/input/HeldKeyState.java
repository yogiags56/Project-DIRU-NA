package MeongDev.DIRUNA.GloftDIRU.core.input;

/**
 * Platform-independent held-key bitset matching the source game's capture model.
 *
 * Key press and release callbacks update this state; gameplay queries test whether
 * any bit in a requested mask is currently held. This class does not map Android
 * key codes or touch coordinates to legacy key bits.
 */
public final class HeldKeyState {
    private int pressedMask;

    public void press(int keyMask) {
        pressedMask |= keyMask;
    }

    public void release(int keyMask) {
        pressedMask &= ~keyMask;
    }

    public boolean isAnyPressed(int queryMask) {
        return (pressedMask & queryMask) != 0;
    }

    public int snapshotMask() {
        return pressedMask;
    }

    public void clear() {
        pressedMask = 0;
    }
}
