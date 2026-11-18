package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Pure representation of probe-flag effects explicitly visible in the
 * pinned legacy aBoolean(dx,dy,probe) helper.
 *
 * This policy deliberately does not return a movement/passability result.
 */
public final class LegacyCollisionProbeSemantics {
    private LegacyCollisionProbeSemantics() { }

    /** The legacy type-19 scripted foreground branch is skipped in probe mode. */
    public static boolean shouldRunType19Script(boolean probeMode) {
        return !probeMode;
    }

    /** Player-layer 34/35 copies the probe flag into the local collision result. */
    public static boolean layer34Or35CollisionValue(boolean probeMode) {
        return probeMode;
    }

    /** The legacy type-1 foreground branch mutates timers/counters only outside probe mode. */
    public static boolean shouldApplyType1ForegroundEffects(boolean probeMode) {
        return !probeMode;
    }

    /** Checkpoint progression/save flags are updated only outside probe mode. */
    public static boolean shouldApplyCheckpointEffects(boolean probeMode) {
        return !probeMode;
    }

    /** Type-10 marker initialization requires both xByte==3 and non-probe execution. */
    public static boolean shouldInitializeType10Marker(boolean probeMode, int xByte,
                                                        int auxiliaryRaw) {
        return !probeMode && xByte == 3 && auxiliaryRaw <= 0;
    }

    /** Type-28/type-5 player-layer state changes are suppressed in probe mode. */
    public static boolean shouldApplyType28Or5Effects(boolean probeMode) {
        return !probeMode;
    }
}
