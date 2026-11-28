package MeongDev.DIRUNA.GloftDIRU.core.collision;

import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;

/**
 * Read-only bridge from the Native stage layers to the source-oriented collision
 * neighborhood capturer. Packed metadata is mandatory and supplied explicitly;
 * this class never guesses it from the background layer. The auxiliary layer is
 * optional and its absence remains observable to branch validation.
 */
public final class LegacyCollisionGridView implements LegacyCollisionNeighborhoodCapture.Grid {
    private final StageGrid stage;
    private final int[] packedMetadata;
    private final int[] auxiliary;

    public LegacyCollisionGridView(StageGrid stage, int[] packedMetadata, int[] auxiliary) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        int expected = checkedCellCount(stage.width(), stage.height());
        if (packedMetadata == null || packedMetadata.length != expected) {
            throw new IllegalArgumentException("packed metadata layer must match stage dimensions");
        }
        if (auxiliary != null && auxiliary.length != expected) {
            throw new IllegalArgumentException("auxiliary layer must match stage dimensions when present");
        }
        this.stage = stage;
        this.packedMetadata = packedMetadata.clone();
        this.auxiliary = auxiliary == null ? null : auxiliary.clone();
    }

    private static int checkedCellCount(int width, int height) {
        long count = (long) width * (long) height;
        if (count <= 0 || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid stage cell count");
        }
        return (int) count;
    }

    @Override public int width() { return stage.width(); }
    @Override public int height() { return stage.height(); }
    @Override public int foregroundRawAt(int x, int y) { return stage.foregroundAt(x, y); }
    @Override public int playerLayerRawAt(int x, int y) { return stage.playerAt(x, y); }
    @Override public int packedMetadataRawAt(int x, int y) { return packedMetadata[index(x, y)]; }
    @Override public boolean hasAuxiliaryGrid() { return auxiliary != null; }
    @Override public int auxiliaryRawAt(int x, int y) {
        if (auxiliary == null) throw new IllegalStateException("auxiliary grid is absent");
        return auxiliary[index(x, y)];
    }

    private int index(int x, int y) {
        if (!stage.contains(x, y)) {
            throw new IndexOutOfBoundsException("collision sample outside stage: " + x + "," + y);
        }
        return x + y * stage.width();
    }
}
