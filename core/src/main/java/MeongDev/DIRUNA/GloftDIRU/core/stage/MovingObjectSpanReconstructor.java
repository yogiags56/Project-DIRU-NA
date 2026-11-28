package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Rebuilds the legacy three-byte moving-object span projection from an
 * immutable stage grid.
 *
 * Source-derived rules:
 * - visit cells in row-major order (y, then x);
 * - select player-layer code 48 with background metadata bit 0x8 set;
 * - choose candidateX from the metadata bit 0x10 in the cell below;
 * - store [candidateX, candidateX, y] when the candidate is one of the
 *   recognized occupants, otherwise [candidateX, candidateX, -1];
 * - pack the zero-based span slot into the high byte of the source cell's
 *   background metadata, preserving its low 24 bits.
 *
 * This is a detached reconstruction. It never mutates the supplied StageGrid.
 */
public final class MovingObjectSpanReconstructor {
    private MovingObjectSpanReconstructor() { }

    public static final class Result {
        private final MovingObjectSpanTable spans;
        private final int[] packedBackgroundMetadata;

        private Result(MovingObjectSpanTable spans, int[] metadata) {
            this.spans = spans;
            this.packedBackgroundMetadata = metadata.clone();
        }

        public MovingObjectSpanTable spans() { return spans; }
        public int[] copyPackedBackgroundMetadata() {
            return packedBackgroundMetadata.clone();
        }
    }

    public static Result rebuild(StageGrid stage) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        int width = stage.width();
        int height = stage.height();
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (stage.playerAt(x, y) == 48
                        && (stage.backgroundAt(x, y) & 0x8) != 0) {
                    if (y + 1 >= height) {
                        throw new IllegalArgumentException(
                                "moving-object span source requires a cell below at " + x + "," + y);
                    }
                    int below = stage.backgroundAt(x, y + 1);
                    int candidateX = x + ((below & 0x10) == 0 ? 1 : -1);
                    if (!stage.contains(candidateX, y)) {
                        throw new IllegalArgumentException(
                                "moving-object span candidate is outside stage at " + candidateX + "," + y);
                    }
                    count++;
                }
            }
        }

        byte[] triples = new byte[Math.multiplyExact(count, 3)];
        int[] metadata = stage.copyBackgroundLayer();
        int slot = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (stage.playerAt(x, y) != 48
                        || (stage.backgroundAt(x, y) & 0x8) == 0) {
                    continue;
                }
                int below = stage.backgroundAt(x, y + 1);
                int candidateX = x + ((below & 0x10) == 0 ? 1 : -1);
                int occupant = stage.playerAt(candidateX, y);
                triples[slot * 3] = (byte) candidateX;
                triples[slot * 3 + 1] = (byte) candidateX;
                triples[slot * 3 + 2] = (byte) (isRecognizedOccupant(occupant) ? y : -1);
                int index = x + y * width;
                metadata[index] = (metadata[index] & 0x00FFFFFF) | (slot << 24);
                slot++;
            }
        }
        return new Result(new MovingObjectSpanTable(triples), metadata);
    }

    private static boolean isRecognizedOccupant(int code) {
        return code == 1 || code == 45 || code == 19 || code == 43
                || code == 46 || code == 49;
    }
}
