package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Pure transcription of the pinned legacy eBoolean(x,y) cell predicate.
 * This evaluates only the raw player-layer and unsigned foreground type;
 * it does not inspect bounds, mutate state, or imply that the full move succeeds.
 */
public final class LegacyEmptyCellPredicate {
    private LegacyEmptyCellPredicate() { }

    public static boolean isLegacyEmpty(int playerLayerRaw, int foregroundRaw) {
        int foregroundType = foregroundRaw & 0xFF;
        return playerLayerRaw == -1
                && foregroundType != 14
                && foregroundType != 33
                && foregroundType != 5
                && foregroundType != 28;
    }
}
