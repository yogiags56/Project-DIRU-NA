package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable execution inputs plus an append-only dry-run trace. */
public final class DemoExecutionContext {
    private final DemoLegacyBufferLayout.RecordSlot slot;
    private final DemoOpcodeOperandDescriptor descriptor;
    private final DemoOpcodeDispatchPlan plan;
    private final DemoOperandAccessReader operands;
    private final List<Integer> visitedLabels = new ArrayList<Integer>();

    public DemoExecutionContext(DemoLegacyBufferLayout.RecordSlot slot,
                                DemoOpcodeOperandDescriptor descriptor,
                                DemoOpcodeDispatchPlan plan) {
        if (slot == null || descriptor == null || plan == null)
            throw new IllegalArgumentException("execution inputs are required");
        if (slot.opcode() != plan.entryOpcode()
                || slot.opcode() != descriptor.entryOpcode())
            throw new IllegalArgumentException("execution input opcode mismatch");
        this.slot = slot;
        this.descriptor = descriptor;
        this.plan = plan;
        this.operands = new DemoOperandAccessReader(slot, descriptor);
    }

    public int opcode() { return slot.opcode(); }
    public DemoOpcodeOperandDescriptor descriptor() { return descriptor; }
    public DemoOpcodeDispatchPlan plan() { return plan; }
    public DemoOperandAccessReader operands() { return operands; }

    void visitSwitchLabel(int label) {
        if (!plan.reachedLabels().contains(label))
            throw new IllegalArgumentException("label is not part of this dispatch plan: " + label);
        visitedLabels.add(label);
    }

    public List<Integer> visitedLabels() {
        return Collections.unmodifiableList(new ArrayList<Integer>(visitedLabels));
    }
}
