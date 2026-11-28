package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable proposal for one collision state transaction; never applies writes. */
public final class LegacyCollisionMutationPlan {
    private final boolean probeMode;
    private final List<LegacyCollisionMutationIntent> gridIntents;
    private final List<LegacyCollisionStateIntent> stateIntents;

    private LegacyCollisionMutationPlan(boolean probeMode,
            List<LegacyCollisionMutationIntent> grid,
            List<LegacyCollisionStateIntent> state) {
        this.probeMode=probeMode;
        this.gridIntents=Collections.unmodifiableList(new ArrayList<LegacyCollisionMutationIntent>(grid));
        this.stateIntents=Collections.unmodifiableList(new ArrayList<LegacyCollisionStateIntent>(state));
    }
    public boolean isProbeMode(){return probeMode;}
    public List<LegacyCollisionMutationIntent> getIntents(){return gridIntents;}
    public List<LegacyCollisionStateIntent> getStateIntents(){return stateIntents;}
    public boolean isEmpty(){return gridIntents.isEmpty() && stateIntents.isEmpty();}
    public static Builder builder(boolean probeMode){return new Builder(probeMode);}

    public static final class Builder {
        private final boolean probeMode;
        private final List<LegacyCollisionMutationIntent> grid=new ArrayList<LegacyCollisionMutationIntent>();
        private final List<LegacyCollisionStateIntent> state=new ArrayList<LegacyCollisionStateIntent>();
        private final Set<String> destinations=new HashSet<String>();
        private boolean built;
        private Builder(boolean probeMode){this.probeMode=probeMode;}

        public Builder add(LegacyCollisionMutationIntent intent) {
            ensureOpen();
            if(intent==null) throw new IllegalArgumentException("intent is required");
            if(probeMode) throw new IllegalStateException("probe mode cannot contain world mutations");
            String key="GRID:"+intent.getLayer().name()+":"+intent.getX()+":"+intent.getY();
            reserve(key);
            grid.add(intent);
            return this;
        }

        public Builder addState(LegacyCollisionStateIntent intent) {
            ensureOpen();
            if(intent==null) throw new IllegalArgumentException("state intent is required");
            if(probeMode) throw new IllegalStateException("probe mode cannot contain state mutations");
            String key=intent.isCoordinateScoped()
                    ? "STATE:"+intent.getChannel()+":"+intent.getField()+":"+intent.getX()+":"+intent.getY()
                    : "STATE:"+intent.getChannel()+":"+intent.getField();
            reserve(key);
            state.add(intent);
            return this;
        }

        private void reserve(String key) {
            if(!destinations.add(key)) throw new IllegalArgumentException("duplicate write destination: "+key);
        }
        private void ensureOpen() {
            if(built) throw new IllegalStateException("plan builder already consumed");
        }
        public LegacyCollisionMutationPlan build() {
            ensureOpen();
            built=true;
            return new LegacyCollisionMutationPlan(probeMode,grid,state);
        }
    }
}
