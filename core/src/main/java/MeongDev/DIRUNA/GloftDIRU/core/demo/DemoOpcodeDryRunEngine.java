package MeongDev.DIRUNA.GloftDIRU.core.demo;

/** Executes only the verified dispatch trace. No operands are consumed and no game state is mutated. */
public final class DemoOpcodeDryRunEngine {
    public static final class Result {
        private final int opcode;
        private final boolean skipped;
        private final java.util.List<Integer> visitedLabels;

        private Result(int opcode, boolean skipped, java.util.List<Integer> labels) {
            this.opcode = opcode;
            this.skipped = skipped;
            this.visitedLabels = java.util.Collections.unmodifiableList(
                    new java.util.ArrayList<Integer>(labels));
        }
        public int opcode() { return opcode; }
        public boolean isSkipped() { return skipped; }
        public java.util.List<Integer> visitedLabels() { return visitedLabels; }
    }

    private final DemoOpcodeHandlerRegistry registry;

    public DemoOpcodeDryRunEngine(DemoOpcodeHandlerRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("handler registry is required");
        this.registry = registry;
    }

    public Result execute(DemoLegacyBufferLayout.RecordSlot slot, boolean skipRequested) {
        if (slot == null) throw new IllegalArgumentException("record slot is required");
        int opcode = slot.opcode();
        DemoOpcodeOperandDescriptor descriptor = DemoOpcodeOperandDescriptor.forOpcode(opcode);
        if (descriptor == null || !registry.supports(opcode))
            throw new IllegalArgumentException("opcode has no audited dry-run handler: " + opcode);
        DemoOpcodeDispatchPlan plan = DemoOpcodeDispatchPlan.forOpcode(opcode, skipRequested);
        DemoExecutionContext context = new DemoExecutionContext(slot, descriptor, plan);
        if (!plan.isSkipped()) {
            DemoOpcodeHandler handler = registry.handlerFor(opcode);
            for (Integer label : plan.reachedLabels()) handler.visit(context, label);
        }
        return new Result(opcode, plan.isSkipped(), context.visitedLabels());
    }
}
