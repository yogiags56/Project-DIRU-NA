package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Pure classifier transcribed from the pinned legacy step-9 Pass B helper.
 * Returns the legacy entity code, or -1 when the player-layer value is unmapped.
 */
public final class LegacyPlayerLayerClassifier {
    private LegacyPlayerLayerClassifier() { }

    public static int classify(int playerLayerCode) {
        switch (playerLayerCode) {
            case 1: return 34;
            case 45: return 35;
            case 19:
            case 43: return 37;
            case 46: return 36;
            case 49: return 39;
            default: return -1;
        }
    }
}
