package MeongDev.DIRUNA.GloftDIRU.core.stage;

/**
 * Applies a detached moving-span plan to private working copies and publishes
 * one immutable result only after every precondition and queue shape is valid.
 */
public final class MovingObjectSpanMutationTransaction {
    private static final int QUEUE_CAPACITY = 8;

    private MovingObjectSpanMutationTransaction() { }

    public static final class QueueState {
        private final byte[] x;
        private final byte[] y;
        private final byte[] type;
        private final byte[] status;
        private final int nextIndex;

        public QueueState(byte[] x, byte[] y, byte[] type, byte[] status, int nextIndex) {
            if (x == null || y == null || type == null || status == null
                    || x.length != QUEUE_CAPACITY || y.length != QUEUE_CAPACITY
                    || type.length != QUEUE_CAPACITY || status.length != QUEUE_CAPACITY
                    || nextIndex < 0 || nextIndex >= QUEUE_CAPACITY) {
                throw new IllegalArgumentException("invalid eight-slot interaction queue state");
            }
            this.x = x.clone();
            this.y = y.clone();
            this.type = type.clone();
            this.status = status.clone();
            this.nextIndex = nextIndex;
        }

        public static QueueState empty() {
            return new QueueState(new byte[8], new byte[8], new byte[8], new byte[8], 0);
        }
        public byte[] copyX() { return x.clone(); }
        public byte[] copyY() { return y.clone(); }
        public byte[] copyType() { return type.clone(); }
        public byte[] copyStatus() { return status.clone(); }
        public int nextIndex() { return nextIndex; }
    }

    public static final class Result {
        private final StageGrid stage;
        private final int[] markerLayer;
        private final QueueState queue;

        private Result(StageGrid stage, int[] markerLayer, QueueState queue) {
            this.stage = stage;
            this.markerLayer = markerLayer.clone();
            this.queue = queue;
        }
        public StageGrid stage() { return stage; }
        public int[] copyMarkerLayer() { return markerLayer.clone(); }
        public QueueState queue() { return queue; }
    }

    public static Result apply(StageGrid source, int[] sourceMarkers, QueueState sourceQueue,
                               MovingObjectSpanMutationPlan plan) {
        if (source == null || sourceMarkers == null || sourceQueue == null || plan == null) {
            throw new IllegalArgumentException("stage, marker layer, queue, and plan are required");
        }
        int cells = Math.multiplyExact(source.width(), source.height());
        if (sourceMarkers.length != cells) {
            throw new IllegalArgumentException("marker layer length must equal stage cell count");
        }
        if (source.width() > 128 || source.height() > 128) {
            throw new IllegalArgumentException("stage dimensions exceed signed-byte queue coordinates");
        }

        int[] player = source.copyPlayerLayer();
        int[] background = source.copyBackgroundLayer();
        int[] foreground = source.copyForegroundLayer();
        int[] markers = sourceMarkers.clone();
        byte[] qx = sourceQueue.copyX();
        byte[] qy = sourceQueue.copyY();
        byte[] qt = sourceQueue.copyType();
        byte[] qs = sourceQueue.copyStatus();
        int next = sourceQueue.nextIndex();

        // Validate all cell preconditions before applying any planned write.
        for (MovingObjectSpanMutationPlan.CellMutation mutation : plan.cellMutations()) {
            if (!source.contains(mutation.x(), mutation.y())) {
                throw new IllegalArgumentException("cell mutation outside stage");
            }
            int index = mutation.x() + mutation.y() * source.width();
            if (player[index] != mutation.expectedPlayerCode()
                    || background[index] != mutation.expectedBackgroundMetadata()) {
                throw new IllegalStateException("stale moving-span mutation precondition at "
                        + mutation.x() + "," + mutation.y());
            }
        }
        for (MovingObjectSpanMutationPlan.MarkerWrite marker : plan.markerWrites()) {
            if (!source.contains(marker.x(), marker.y())) {
                throw new IllegalArgumentException("marker write outside stage");
            }
        }
        for (MovingObjectSpanMutationPlan.InteractionEvent event : plan.interactionEvents()) {
            if (!source.contains(event.x(), event.y())) {
                throw new IllegalArgumentException("interaction event outside stage");
            }
        }

        for (MovingObjectSpanMutationPlan.CellMutation mutation : plan.cellMutations()) {
            int index = mutation.x() + mutation.y() * source.width();
            player[index] = mutation.playerCode();
            background[index] = mutation.backgroundMetadata();
        }
        for (MovingObjectSpanMutationPlan.MarkerWrite marker : plan.markerWrites()) {
            markers[marker.x() + marker.y() * source.width()] = marker.value();
        }
        for (MovingObjectSpanMutationPlan.InteractionEvent event : plan.interactionEvents()) {
            qx[next] = (byte) event.x();
            qy[next] = (byte) event.y();
            qt[next] = (byte) event.type();
            qs[next] = 0;
            next = (next + 1) & 0x7;
        }

        StageGrid committed = new StageGrid(source.width(), source.height(),
                player, background, foreground);
        return new Result(committed, markers, new QueueState(qx, qy, qt, qs, next));
    }
}
