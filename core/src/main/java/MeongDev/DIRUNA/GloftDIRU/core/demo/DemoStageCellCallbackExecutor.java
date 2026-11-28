package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Opt-in executor for the source-audited immediate stage-cell callback effects
 * of entry opcodes 5, 8, and 9.
 *
 * This is deliberately not a complete interpreter: downstream opcode 27/2
 * completion and dialogue behavior is outside this executor.
 */
public final class DemoStageCellCallbackExecutor {
    public static final class Result {
        private final int entryOpcode;
        private final List<Integer> callbackLabels;

        private Result(int entryOpcode, List<Integer> labels) {
            this.entryOpcode = entryOpcode;
            this.callbackLabels = Collections.unmodifiableList(
                    new ArrayList<Integer>(labels));
        }

        public int entryOpcode() { return entryOpcode; }

        /** Switch labels that invoked the audited stage-cell callback, in source order. */
        public List<Integer> callbackLabels() { return callbackLabels; }
    }

    private final DemoStageMutationAdapter stage;

    public DemoStageCellCallbackExecutor(DemoStageMutationAdapter stage) {
        if (stage == null) throw new IllegalArgumentException("stage adapter is required");
        this.stage = stage;
    }

    /**
     * Executes only immediate callback writes. Operand reads are gated by the
     * entry opcode descriptor and preserve signed-short / signed-byte casts.
     */
    public Result execute(DemoLegacyBufferLayout.RecordSlot slot) {
        if (slot == null) throw new IllegalArgumentException("record slot is required");
        int opcode = slot.opcode();
        if (opcode != 5 && opcode != 8 && opcode != 9)
            throw new IllegalArgumentException("stage-cell callback executor does not support opcode " + opcode);

        DemoOpcodeOperandDescriptor descriptor = DemoOpcodeOperandDescriptor.forOpcode(opcode);
        if (descriptor == null)
            throw new IllegalArgumentException("opcode has no audited operand descriptor: " + opcode);

        DemoOperandAccessReader operands = new DemoOperandAccessReader(slot, descriptor);
        int x = (short) operands.readU16LE(2);
        int y = (short) operands.readU16LE(4);
        List<Integer> callbacks = new ArrayList<Integer>(2);

        if (opcode == 5) {
            byte playerLayer = (byte) operands.readU8(6);
            stage.applyCallback(x, y, playerLayer, 0);
            callbacks.add(5);
        }

        // Opcode 8 falls through to label 9; opcode 5 also reaches label 9
        // after its first callback. Opcode 9 enters this callback directly.
        int backgroundValue = (short) operands.readU16LE(6);
        stage.applyCallback(x, y, (byte) 0, backgroundValue);
        callbacks.add(9);

        return new Result(opcode, callbacks);
    }
}
