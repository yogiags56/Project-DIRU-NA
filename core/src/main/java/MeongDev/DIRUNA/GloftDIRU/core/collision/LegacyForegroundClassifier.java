package MeongDev.DIRUNA.GloftDIRU.core.collision;

/** Source-grounded classifier for foreground low-byte values in the legacy helper. */
public final class LegacyForegroundClassifier {
    public enum Kind {
        SCRIPTED_TYPE_19, TYPE_1, TYPE_7, CHECKPOINT_TYPE_4,
        TYPE_28, TYPE_5, TYPE_2, TYPE_6, TYPE_3, OTHER
    }
    private LegacyForegroundClassifier() { }
    public static int unsignedType(int raw) { return raw & 0xFF; }
    public static Kind classify(int raw) {
        switch (unsignedType(raw)) {
            case 19: return Kind.SCRIPTED_TYPE_19;
            case 1: return Kind.TYPE_1;
            case 7: return Kind.TYPE_7;
            case 4: return Kind.CHECKPOINT_TYPE_4;
            case 28: return Kind.TYPE_28;
            case 5: return Kind.TYPE_5;
            case 2: return Kind.TYPE_2;
            case 6: return Kind.TYPE_6;
            case 3: return Kind.TYPE_3;
            default: return Kind.OTHER;
        }
    }
}
