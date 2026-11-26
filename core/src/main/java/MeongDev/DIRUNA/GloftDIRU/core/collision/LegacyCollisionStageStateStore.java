package MeongDev.DIRUNA.GloftDIRU.core.collision;

import MeongDev.DIRUNA.GloftDIRU.core.stage.StageGrid;
import java.util.HashMap;
import java.util.Map;

/**
 * Single-owner copy-on-write store for collision transactions.
 * Readers observe one immutable root; commits publish a fully prepared replacement root.
 */
public final class LegacyCollisionStageStateStore implements LegacyCollisionMutationTarget {
    private static final class Root {
        final int[] player, background, foreground, metadata, auxiliary, foregroundCache;
        final Map<String,Integer> state;
        final long version;
        Root(int[] player,int[] background,int[] foreground,int[] metadata,int[] auxiliary,
             int[] foregroundCache,Map<String,Integer> state,long version) {
            this.player=player; this.background=background; this.foreground=foreground;
            this.metadata=metadata; this.auxiliary=auxiliary; this.foregroundCache=foregroundCache;
            this.state=state; this.version=version;
        }
    }

    private final int width,height;
    private final Thread ownerThread;
    private volatile Root root;

    /** Convenience empty-state constructor for isolated tests and prototypes. */
    public LegacyCollisionStageStateStore(StageGrid stage) {
        this(new LegacyCollisionStageStateSeed(stage,
                new int[Math.multiplyExact(stage.width(),stage.height())],
                new int[Math.multiplyExact(stage.width(),stage.height())],
                new int[Math.multiplyExact(stage.width(),stage.height())],
                0,0,0,0,0,0,0,0));
    }

    /** Production constructor: imports all decoded world and local-session state. */
    public LegacyCollisionStageStateStore(LegacyCollisionStageStateSeed seed) {
        if(seed==null) throw new IllegalArgumentException("seed is required");
        StageGrid stage=seed.stage();
        width=stage.width(); height=stage.height(); ownerThread=Thread.currentThread();
        Map<String,Integer> state=new HashMap<String,Integer>();
        state.put(stateKey(LegacyCollisionStateIntent.Channel.PLAYER,LegacyCollisionStateIntent.Field.TILE_X,false,0,0),seed.tileX());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.PLAYER,LegacyCollisionStateIntent.Field.TILE_Y,false,0,0),seed.tileY());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.PLAYER,LegacyCollisionStateIntent.Field.PENDING_DIRECTION,false,0,0),seed.pendingDirection());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.PLAYER,LegacyCollisionStateIntent.Field.ACTIVE_DIRECTION,false,0,0),seed.activeDirection());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.PLAYER,LegacyCollisionStateIntent.Field.INTERPOLATION_PROGRESS,false,0,0),seed.interpolationProgress());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.ANIMATION,LegacyCollisionStateIntent.Field.ANIMATION_ID,false,0,0),seed.animationId());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.ANIMATION,LegacyCollisionStateIntent.Field.ANIMATION_FRAME,false,0,0),seed.animationFrame());
        state.put(stateKey(LegacyCollisionStateIntent.Channel.MOVEMENT_FLAGS,LegacyCollisionStateIntent.Field.BLOCKED_FLAG,false,0,0),seed.blockedFlag());
        root=new Root(stage.copyPlayerLayer(),stage.copyBackgroundLayer(),stage.copyForegroundLayer(),
                seed.copyMetadata(),seed.copyAuxiliary(),seed.copyForegroundCache(),state,0L);
    }

    @Override public int width(){return width;}
    @Override public int height(){return height;}
    @Override public boolean isOwnerThread(){return Thread.currentThread()==ownerThread;}

    @Override public int readGrid(LegacyCollisionMutationIntent.Layer layer,int x,int y) {
        checkCell(x,y);
        Root r=root;
        return grid(r,layer)[index(x,y)];
    }

    @Override public int readState(LegacyCollisionStateIntent.Channel channel,
            LegacyCollisionStateIntent.Field field,boolean coordinateScoped,int x,int y) {
        if(channel==null || field==null) throw new IllegalArgumentException("state key is required");
        if(coordinateScoped) checkCell(x,y);
        Root r=root;
        if(channel==LegacyCollisionStateIntent.Channel.FOREGROUND_CACHE) {
            if(!coordinateScoped || field!=LegacyCollisionStateIntent.Field.CACHE_VALUE)
                throw new IllegalArgumentException("foreground cache requires a coordinate-scoped cache field");
            return r.foregroundCache[index(x,y)];
        }
        return value(r.state,stateKey(channel,field,coordinateScoped,x,y));
    }

    /** Copy-on-write all-or-nothing commit. The root reference changes only after all writes are prepared. */
    @Override public boolean commitAtomically(LegacyCollisionMutationPlan plan) {
        if(plan==null) throw new IllegalArgumentException("plan is required");
        if(!isOwnerThread()) throw new IllegalStateException("collision store may only commit on its owner thread");
        if(plan.isProbeMode()) return plan.isEmpty();

        Root before=root;
        for(LegacyCollisionMutationIntent i:plan.getIntents()) {
            if(!contains(i.getX(),i.getY()) || grid(before,i.getLayer())[index(i.getX(),i.getY())]!=i.getExpectedValue())
                return false;
        }
        for(LegacyCollisionStateIntent i:plan.getStateIntents()) {
            if(i.isCoordinateScoped() && !contains(i.getX(),i.getY())) return false;
            if(value(before.state,stateKey(i.getChannel(),i.getField(),i.isCoordinateScoped(),i.getX(),i.getY()))
                    !=i.getExpectedValue()) return false;
        }

        int[] player=before.player.clone(), background=before.background.clone();
        int[] foreground=before.foreground.clone(), metadata=before.metadata.clone();
        int[] auxiliary=before.auxiliary.clone(), cache=before.foregroundCache.clone();
        Map<String,Integer> state=new HashMap<String,Integer>(before.state);
        for(LegacyCollisionMutationIntent i:plan.getIntents())
            grid(player,background,foreground,metadata,auxiliary,i.getLayer())[index(i.getX(),i.getY())]=i.getNewValue();
        for(LegacyCollisionStateIntent i:plan.getStateIntents()) {
            String key=stateKey(i.getChannel(),i.getField(),i.isCoordinateScoped(),i.getX(),i.getY());
            if(i.getChannel()==LegacyCollisionStateIntent.Channel.FOREGROUND_CACHE)
                cache[index(i.getX(),i.getY())]=i.getNewValue();
            else state.put(key,i.getNewValue());
        }
        root=new Root(player,background,foreground,metadata,auxiliary,cache,state,before.version+1L);
        return true;
    }

    /** Returns a detached immutable StageGrid representing the currently published world layers. */
    public StageGrid stageSnapshot() {
        Root r=root;
        return new StageGrid(width,height,r.player,r.background,r.foreground);
    }
    public int foregroundCacheAt(int x,int y){checkCell(x,y);return root.foregroundCache[index(x,y)];}
    public long version(){return root.version;}
    public int readStateValue(LegacyCollisionStateIntent.Channel channel,
            LegacyCollisionStateIntent.Field field) {
        return value(root.state,stateKey(channel,field,false,0,0));
    }

    private int[] grid(Root r,LegacyCollisionMutationIntent.Layer layer) {
        switch(layer) {
            case PLAYER:return r.player;
            case BACKGROUND:return r.background;
            case FOREGROUND:return r.foreground;
            case PACKED_METADATA:return r.metadata;
            case AUXILIARY:return r.auxiliary;
            default:throw new IllegalArgumentException("unsupported grid layer");
        }
    }
    private static int[] grid(int[] player,int[] background,int[] foreground,int[] metadata,
                              int[] auxiliary,LegacyCollisionMutationIntent.Layer layer) {
        switch(layer) {
            case PLAYER:return player;
            case BACKGROUND:return background;
            case FOREGROUND:return foreground;
            case PACKED_METADATA:return metadata;
            case AUXILIARY:return auxiliary;
            default:throw new IllegalArgumentException("unsupported grid layer");
        }
    }
    private boolean contains(int x,int y){return x>=0 && y>=0 && x<width && y<height;}
    private void checkCell(int x,int y){if(!contains(x,y))throw new IndexOutOfBoundsException("stage coordinate out of bounds: "+x+","+y);}
    private int index(int x,int y){return x+y*width;}
    private static int value(Map<String,Integer> map,String key){Integer v=map.get(key);return v==null?0:v.intValue();}
    private static String stateKey(LegacyCollisionStateIntent.Channel c,LegacyCollisionStateIntent.Field f,
                                   boolean scoped,int x,int y) {
        return c.name()+":"+f.name()+(scoped?":"+x+":"+y:"");
    }
}
