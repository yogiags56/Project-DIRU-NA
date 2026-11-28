package MeongDev.DIRUNA.GloftDIRU.core.stage;

/** Applies a detached moving-span plan to private copies and publishes one immutable result. */
public final class MovingObjectSpanMutationTransaction {
    private static final int QUEUE_CAPACITY = 8;
    private MovingObjectSpanMutationTransaction() { }

    /** Snapshot of the legacy x/y/type/status interaction ring buffer. */
    public static final class QueueState {
        private final byte[] x, y, type, status;
        private final int nextIndex;

        public QueueState(byte[] x, byte[] y, byte[] type, byte[] status, int nextIndex) {
            if (x == null || y == null || type == null || status == null
                    || x.length != QUEUE_CAPACITY || y.length != QUEUE_CAPACITY
                    || type.length != QUEUE_CAPACITY || status.length != QUEUE_CAPACITY
                    || nextIndex < 0 || nextIndex >= QUEUE_CAPACITY) {
                throw new IllegalArgumentException("invalid eight-slot interaction queue state");
            }
            this.x=x.clone(); this.y=y.clone(); this.type=type.clone(); this.status=status.clone();
            this.nextIndex=nextIndex;
        }
        public static QueueState empty() {
            return new QueueState(new byte[8],new byte[8],new byte[8],new byte[8],0);
        }
        public byte[] copyX(){return x.clone();}
        public byte[] copyY(){return y.clone();}
        public byte[] copyType(){return type.clone();}
        public byte[] copyStatus(){return status.clone();}
        public int nextIndex(){return nextIndex;}
    }

    public static final class Result {
        private final StageGrid stage;
        private final byte[] markerLayer;
        private final QueueState queue;
        private Result(StageGrid stage, byte[] markers, QueueState queue) {
            this.stage=stage; this.markerLayer=markers.clone(); this.queue=queue;
        }
        public StageGrid stage(){return stage;}
        public byte[] copyMarkerLayer(){return markerLayer.clone();}
        public QueueState queue(){return queue;}
    }

    public static Result apply(StageGrid source, byte[] sourceMarkers, QueueState sourceQueue,
                               MovingObjectSpanMutationPlan plan) {
        if(source==null||sourceMarkers==null||sourceQueue==null||plan==null)
            throw new IllegalArgumentException("stage, marker layer, queue, and plan are required");
        int count=Math.multiplyExact(source.width(),source.height());
        if(sourceMarkers.length!=count)
            throw new IllegalArgumentException("marker layer length must equal stage cell count");
        if(source.width()>128||source.height()>128)
            throw new IllegalArgumentException("stage dimensions exceed signed-byte coordinates");

        int[] player=source.copyPlayerLayer();
        int[] background=source.copyBackgroundLayer();
        int[] foreground=source.copyForegroundLayer();
        byte[] markers=sourceMarkers.clone();
        byte[] qx=sourceQueue.copyX(), qy=sourceQueue.copyY();
        byte[] qt=sourceQueue.copyType(), qs=sourceQueue.copyStatus();
        int next=sourceQueue.nextIndex();

        // Preflight every operation before changing any private working copy.
        for(MovingObjectSpanMutationPlan.CellMutation m:plan.cellMutations()) {
            if(!source.contains(m.x(),m.y()))
                throw new IllegalArgumentException("cell mutation outside stage");
            int i=m.x()+m.y()*source.width();
            if(player[i]!=m.expectedPlayerCode()||background[i]!=m.expectedBackgroundMetadata())
                throw new IllegalStateException("stale moving-span mutation precondition at "+m.x()+","+m.y());
        }
        for(MovingObjectSpanMutationPlan.MarkerWrite m:plan.markerWrites())
            if(!source.contains(m.x(),m.y())) throw new IllegalArgumentException("marker write outside stage");
        for(MovingObjectSpanMutationPlan.InteractionEvent e:plan.interactionEvents())
            if(!source.contains(e.x(),e.y())) throw new IllegalArgumentException("interaction event outside stage");

        for(MovingObjectSpanMutationPlan.CellMutation m:plan.cellMutations()) {
            int i=m.x()+m.y()*source.width();
            player[i]=m.playerCode(); background[i]=m.backgroundMetadata();
        }
        for(MovingObjectSpanMutationPlan.MarkerWrite m:plan.markerWrites())
            markers[m.x()+m.y()*source.width()]=(byte)m.value();
        for(MovingObjectSpanMutationPlan.InteractionEvent e:plan.interactionEvents()) {
            qx[next]=(byte)e.x(); qy[next]=(byte)e.y(); qt[next]=(byte)e.type(); qs[next]=0;
            next=(next+1)&0x7;
        }
        StageGrid committed=new StageGrid(source.width(),source.height(),player,background,foreground);
        return new Result(committed,markers,new QueueState(qx,qy,qt,qs,next));
    }
}
