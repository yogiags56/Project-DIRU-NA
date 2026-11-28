package MeongDev.DIRUNA.GloftDIRU.core.demo;

/**
 * Sequential, non-mutating traversal of normalized top-level records and
 * one-level packed groups. Each successful step records a dry-run trace and
 * advances the session cursor; unsupported records remain current on failure.
 */
public final class DemoExecutionSession {
    private final DemoInterpreterRuntimeState runtime;
    private final DemoOpcodeDryRunEngine engine;
    private int childIndex;

    public DemoExecutionSession(DemoInterpreterRuntimeState runtime,
                                DemoOpcodeDryRunEngine engine) {
        if (runtime == null || engine == null)
            throw new IllegalArgumentException("runtime and engine are required");
        this.runtime = runtime;
        this.engine = engine;
        this.childIndex = 0;
        normalizeCursor();
    }

    public boolean hasCurrentRecord() {
        return currentSlot() != null;
    }

    public int currentOpcode() {
        DemoLegacyBufferLayout.RecordSlot slot = currentSlot();
        if (slot == null) throw new IllegalStateException("execution session is exhausted");
        return slot.opcode();
    }

    public int currentBufferIndex() { return runtime.selectedBufferIndex(); }
    public int currentRecordIndex() { return childIndex; }
    public int tick() { return runtime.tick(); }

    /**
     * Dry-runs the current record. Cursor and tick advance only after successful
     * dispatch. No stage state, record bytes, completion flag, or graphics state
     * is mutated.
     */
    public DemoOpcodeDryRunEngine.Result step() {
        DemoLegacyBufferLayout.RecordSlot slot = currentSlot();
        if (slot == null) throw new IllegalStateException("execution session is exhausted");
        DemoOpcodeDryRunEngine.Result result = engine.execute(slot, runtime.skipRequested());
        runtime.advanceTick();
        childIndex++;
        normalizeCursor();
        return result;
    }

    public void setSkipRequested(boolean value) {
        runtime.setSkipRequested(value);
    }

    /** Resets runtime counters and restarts traversal at the first record. */
    public void reset() {
        runtime.reset();
        childIndex = 0;
        normalizeCursor();
    }

    private DemoLegacyBufferLayout.RecordSlot currentSlot() {
        int bufferIndex = runtime.selectedBufferIndex();
        if (bufferIndex < 0 || bufferIndex >= runtime.bufferCount()) return null;
        DemoInterpreterRuntimeState.RecordState record = runtime.buffers().get(bufferIndex);
        if (!record.isGroup()) {
            return childIndex == 0
                    ? DemoLegacyBufferLayout.topLevelLeaf(record.copyNormalizedBytes()).slot(0)
                    : null;
        }
        if (childIndex >= record.children().size()) return null;
        DemoLegacyBufferLayout layout =
                DemoLegacyBufferLayout.packedGroup(record.copyNormalizedBytes());
        return layout.slot(childIndex);
    }

    private void normalizeCursor() {
        while (runtime.selectedBufferIndex() >= 0
                && runtime.selectedBufferIndex() < runtime.bufferCount()) {
            DemoInterpreterRuntimeState.RecordState record =
                    runtime.buffers().get(runtime.selectedBufferIndex());
            int count = record.isGroup() ? record.children().size() : 1;
            if (childIndex < count) return;
            if (!runtime.selectNextBuffer()) return;
            childIndex = 0;
        }
    }
}
