package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Opt-in ordered stage-cell effects for the audited opcode-25/26 fall-through
 * chain. Script completion writes are reported as effects; source record bytes
 * are never mutated here. Opcode-2 dialogue timing remains outside this class.
 */
public final class DemoStageCompositeCellExecutor {
    public static final class Result {
        private final int entryOpcode;
        private final List<Integer> completedLabels;
        private final boolean dialogueReached;

        private Result(int entryOpcode, List<Integer> completedLabels,
                       boolean dialogueReached) {
            this.entryOpcode = entryOpcode;
            this.completedLabels = Collections.unmodifiableList(
                    new ArrayList<Integer>(completedLabels));
            this.dialogueReached = dialogueReached;
        }

        public int entryOpcode() { return entryOpcode; }

        /** Ordered labels whose legacy completion byte would be set to 1. */
        public List<Integer> completedLabels() { return completedLabels; }

        /** True when opcode 27's early-return gate is clear and opcode 2 is reached. */
        public boolean dialogueReached() { return dialogueReached; }
    }

    private final DemoStageMutationAdapter stage;

    public DemoStageCompositeCellExecutor(DemoStageMutationAdapter stage) {
        if (stage == null) throw new IllegalArgumentException("stage adapter is required");
        this.stage = stage;
    }

    /**
     * Executes the source-audited immediate stage effects for entry opcode 25
     * or 26, including fall-through through 5, 8, 9, and the opcode-27 gate.
     * All operands are read through the entry descriptor. Coordinates retain
     * signed-short interpretation and all stage writes retain source order.
     */
    public Result execute(DemoLegacyBufferLayout.RecordSlot slot) {
        if (slot == null) throw new IllegalArgumentException("record slot is required");
        int opcode = slot.opcode();
        if (opcode != 25 && opcode != 26)
            throw new IllegalArgumentException("composite cell executor supports only opcode 25 or 26");

        DemoOpcodeOperandDescriptor descriptor = DemoOpcodeOperandDescriptor.forOpcode(opcode);
        if (descriptor == null)
            throw new IllegalArgumentException("opcode has no audited operand descriptor: " + opcode);
        DemoOperandAccessReader operands = new DemoOperandAccessReader(slot, descriptor);

        int x = (short) operands.readU16LE(2);
        int y = (short) operands.readU16LE(4);
        List<Integer> completed = new ArrayList<Integer>(5);

        if (opcode == 25) {
            int low = operands.readU8(6);
            int high = operands.readU8(7);
            short foreground = (short) (((byte) high << 8) | low);
            stage.setForegroundCell(x, y, foreground);
            completed.add(25);
        }

        // Both entry paths reach label 26. Its u32 value is interpreted as
        // the same 32-bit bit pattern as the legacy int grid assignment.
        int background = (int) operands.readU32LE(6);
        stage.setBackgroundCell(x, y, background);
        completed.add(26);

        // Fall-through through label 5: byte +6, background value 0.
        byte playerLayer = (byte) operands.readU8(6);
        stage.applyCallback(x, y, playerLayer, 0);
        completed.add(5);

        // Label 8 is empty; label 9 performs the second callback.
        int callbackBackground = (short) operands.readU16LE(6);
        stage.applyCallback(x, y, (byte) 0, callbackBackground);
        completed.add(9);

        // Label 27 checks the raw byte at +7. A nonzero value completes 27
        // and returns before label 2; zero falls through to dialogue handling.
        boolean dialogueReached = operands.readU8(7) == 0;
        if (!dialogueReached) completed.add(27);

        return new Result(opcode, completed, dialogueReached);
    }
}
