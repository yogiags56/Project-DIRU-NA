package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Computes the source-compatible horizontal endpoint projection for each
 * active moving-object span.
 *
 * This mirrors the legacy aByte(start, direction, edge, row) scan used by
 * VVoid: empty cells, code 31, and occupants recognized by dInt are traversable.
 * The updater is deliberately pure; legacy aBoolean() also converts eligible
 * occupants into player-layer code 9 and can damage the hero. Those gameplay
 * mutations are not performed here.
 */
public final class MovingObjectSpanUpdater {
    private MovingObjectSpanUpdater() { }

    public static MovingObjectSpanTable update(StageGrid stage, MovingObjectSpanTable source) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        if (source == null) throw new IllegalArgumentException("span table is required");
        // Legacy span coordinates are signed bytes. A larger grid cannot be
        // represented losslessly by this record format.
        if (stage.width() > 128 || stage.height() > 128) {
            throw new IllegalArgumentException("stage dimensions exceed signed-byte span coordinates");
        }

        byte[] updated = source.copyRawTriples();
        for (int slot = 0; slot < source.size(); slot++) {
            MovingObjectSpanTable.Span span = source.spanAt(slot);
            int row = span.rowY();
            if (row <= 0) {
                continue;
            }
            if (row >= stage.height()) {
                throw new IllegalArgumentException("active span row outside stage: " + row);
            }

            int leftStart = span.firstX();
            int rightStart = span.secondX();
            if (!stage.contains(leftStart, row) || !stage.contains(rightStart, row)) {
                throw new IllegalArgumentException("span endpoint outside stage at row " + row);
            }

            int left = scan(stage, leftStart, -1, 0, row);
            int right = scan(stage, rightStart, 1, stage.width() - 1, row);
            updated[slot * 3] = (byte) left;
            updated[slot * 3 + 1] = (byte) right;
        }
        return new MovingObjectSpanTable(updated);
    }

    private static int scan(StageGrid stage, int start, int direction, int edge, int row) {
        int current = start;
        while ((direction < 0 && current > edge)
                || (direction > 0 && current < edge)) {
            int next = current + direction;
            int occupant = stage.playerAt(next, row);
            if (!isTraversable(occupant)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private static boolean isTraversable(int code) {
        return code < 0 || code == 31 || isRecognizedOccupant(code);
    }

    private static boolean isRecognizedOccupant(int code) {
        return code == 1 || code == 45 || code == 19 || code == 43
                || code == 46 || code == 49;
    }
}
