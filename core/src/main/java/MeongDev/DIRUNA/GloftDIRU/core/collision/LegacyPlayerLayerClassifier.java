package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Labels the player-layer cases handled explicitly by the pinned legacy helper.
 * Classification is not a movement/passability decision; some labels depend
 * on probe mode or are handled by dedicated multi-cell logic.
 */
public final class LegacyPlayerLayerClassifier {
    public enum Kind {
        EMPTY_CELL, PROBE_DEPENDENT_34_35, NON_BLOCKING_31_49,
        BLOCKING_EXPLICIT_SET, TYPE_10_MODE_DEPENDENT,
        TWO_STEP_SET_0_8_9_47, SPECIAL_OBJECT_48, TYPE_28_SPECIAL,
        OTHER_DEFAULT
    }
    private LegacyPlayerLayerClassifier() { }

    public static Kind classify(int raw) {
        switch (raw) {
            case -1: return Kind.EMPTY_CELL;
            case 34:
            case 35: return Kind.PROBE_DEPENDENT_34_35;
            case 31:
            case 49: return Kind.NON_BLOCKING_31_49;
            case 1: case 2: case 4: case 5: case 6: case 7:
            case 11: case 14: case 19: case 24: case 26: case 27:
            case 33: case 40: case 41: case 42: case 43: case 45:
            case 50: case 51: case 52: case 53:
                return Kind.BLOCKING_EXPLICIT_SET;
            case 10: return Kind.TYPE_10_MODE_DEPENDENT;
            case 0: case 8: case 9: case 47:
                return Kind.TWO_STEP_SET_0_8_9_47;
            case 48: return Kind.SPECIAL_OBJECT_48;
            case 28: return Kind.TYPE_28_SPECIAL;
            default: return Kind.OTHER_DEFAULT;
        }
    }
}
