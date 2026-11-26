package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable collision write-set. This is a validated plan, not an applier:
 * it never mutates StageGrid or gameplay state.
 */
public final class LegacyCollisionMutationPlan {
    private final boolean probeMode;
    private final List<LegacyCollisionMutationIntent> intents;
    private LegacyCollisionMutationPlan(boolean probeMode,List<LegacyCollisionMutationIntent> intents) {
        this.probeMode=probeMode;
        this.intents=Collections.unmodifiableList(new ArrayList<LegacyCollisionMutationIntent>(intents));
    }
    public boolean isProbeMode(){return probeMode;}
    public List<LegacyCollisionMutationIntent> getIntents(){return intents;}
    public boolean isEmpty(){return intents.isEmpty();}

    public static Builder builder(boolean probeMode){return new Builder(probeMode);}
    public static final class Builder {
        private final boolean probeMode;
        private final List<LegacyCollisionMutationIntent> intents=new ArrayList<LegacyCollisionMutationIntent>();
        private final Set<String> destinations=new HashSet<String>();
        private boolean built;
        private Builder(boolean probeMode){this.probeMode=probeMode;}
        public Builder add(LegacyCollisionMutationIntent intent) {
            if(built) throw new IllegalStateException("plan builder already consumed");
            if(intent==null) throw new IllegalArgumentException("intent is required");
            if(probeMode) throw new IllegalStateException("probe mode cannot contain world mutations");
            String key=intent.getLayer().name()+":"+intent.getX()+":"+intent.getY();
            if(!destinations.add(key))
                throw new IllegalArgumentException("duplicate write destination: "+key);
            intents.add(intent);
            return this;
        }
        public LegacyCollisionMutationPlan build() {
            if(built) throw new IllegalStateException("plan builder already consumed");
            built=true;
            return new LegacyCollisionMutationPlan(probeMode,intents);
        }
    }
}
