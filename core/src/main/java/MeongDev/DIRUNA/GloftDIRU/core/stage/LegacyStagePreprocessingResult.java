package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Immutable boundary for a completed, source-derived stage preprocessing pass.
 *
 * This type stores distinct authored/transformed layers and runtime grids.
 * It deliberately does not implement preprocessing or infer missing values.
 */
public final class LegacyStagePreprocessingResult {
    private final StageGrid transformedStage;
    private final int[] backgroundRuntime;
    private final byte[] bByteArrArr;
    private final byte[] cByteArrArr;

    public LegacyStagePreprocessingResult(StageGrid transformedStage,
            int[] backgroundRuntime, byte[] bByteArrArr, byte[] cByteArrArr) {
        if (transformedStage == null) throw new IllegalArgumentException("transformed stage is required");
        int cells = Math.multiplyExact(transformedStage.width(), transformedStage.height());
        require("backgroundRuntime", backgroundRuntime, cells);
        require("bByteArrArr", bByteArrArr, cells);
        require("cByteArrArr", cByteArrArr, cells);
        this.transformedStage = copy(transformedStage);
        this.backgroundRuntime = backgroundRuntime.clone();
        this.bByteArrArr = bByteArrArr.clone();
        this.cByteArrArr = cByteArrArr.clone();
    }

    private static StageGrid copy(StageGrid stage) {
        return new StageGrid(stage.width(), stage.height(), stage.copyPlayerLayer(),
                stage.copyBackgroundLayer(), stage.copyForegroundLayer());
    }
    private static void require(String name, int[] values, int cells) {
        if (values == null || values.length != cells)
            throw new IllegalArgumentException(name + " length must equal stage cell count");
    }
    private static void require(String name, byte[] values, int cells) {
        if (values == null || values.length != cells)
            throw new IllegalArgumentException(name + " length must equal stage cell count");
    }

    public int width() { return transformedStage.width(); }
    public int height() { return transformedStage.height(); }
    public StageGrid transformedStage() { return copy(transformedStage); }
    public int backgroundRuntimeAt(int x, int y) { return backgroundRuntime[index(x, y)]; }
    public byte bByteAt(int x, int y) { return bByteArrArr[index(x, y)]; }
    public byte cByteAt(int x, int y) { return cByteArrArr[index(x, y)]; }
    public int[] copyBackgroundRuntime() { return backgroundRuntime.clone(); }
    public byte[] copyBByteArrArr() { return bByteArrArr.clone(); }
    public byte[] copyCByteArrArr() { return cByteArrArr.clone(); }

    private int index(int x, int y) {
        if (x < 0 || y < 0 || x >= width() || y >= height())
            throw new IndexOutOfBoundsException("stage coordinate out of bounds: " + x + "," + y);
        return x + y * width();
    }
}
