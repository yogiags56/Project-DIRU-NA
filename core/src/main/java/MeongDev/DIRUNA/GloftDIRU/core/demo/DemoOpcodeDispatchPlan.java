package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Source-backed description of the legacy switch labels reached by one record.
 *
 * This class intentionally plans control flow only. It does not read operands,
 * mutate runtime state, call stage adapters, update the camera, or render.
 */
public final class DemoOpcodeDispatchPlan {
    private static final int[] SKIP_ALLOW_LIST = {5, 9, 10, 25, 26};

    private final int entryOpcode;
    private final boolean skipped;
    private final List<Integer> reachedLabels;

    private DemoOpcodeDispatchPlan(int entryOpcode, boolean skipped, List<Integer> labels) {
        this.entryOpcode = entryOpcode;
        this.skipped = skipped;
        this.reachedLabels = Collections.unmodifiableList(new ArrayList<Integer>(labels));
    }

    public static DemoOpcodeDispatchPlan forRecord(
            DemoInterpreterRuntimeState.RecordState record, boolean skipRequested) {
        if (record == null) throw new IllegalArgumentException("record is required");
        if (record.isGroup()) throw new IllegalArgumentException("dispatch plans are created for leaf records");
        return forOpcode(record.opcode(), skipRequested);
    }

    public static DemoOpcodeDispatchPlan forOpcode(int opcode, boolean skipRequested) {
        if (opcode < 1 || opcode > 27)
            throw new IllegalArgumentException("unsupported opcode " + opcode);
        if (opcode == 3 || opcode == 19 || opcode == 20 || opcode == 21
                || opcode == 22 || opcode == 23 || opcode == 24)
            throw new IllegalArgumentException("opcode has no verified decoder/runtime contract: " + opcode);

        if (skipRequested && !isSkipAllowed(opcode))
            return new DemoOpcodeDispatchPlan(opcode, true, Collections.<Integer>emptyList());

        List<Integer> labels = new ArrayList<Integer>();
        switch (opcode) {
            case 16: case 17: case 18:
                labels.add(opcode);
                labels.add(14);
                labels.add(12);
                break;
            case 14: case 15:
                labels.add(opcode);
                labels.add(12);
                break;
            case 11:
                labels.add(11);
                labels.add(10);
                labels.add(13);
                break;
            case 10:
                labels.add(10);
                labels.add(13);
                break;
            case 13: case 1: case 4: case 6:
                labels.add(opcode);
                break;
            case 25:
                add(labels, 25, 26, 5, 8, 9, 27, 2);
                break;
            case 26:
                add(labels, 26, 5, 8, 9, 27, 2);
                break;
            case 5:
                add(labels, 5, 8, 9, 27, 2);
                break;
            case 8:
                add(labels, 8, 9, 27, 2);
                break;
            case 9:
                add(labels, 9, 27, 2);
                break;
            case 27:
                add(labels, 27, 2);
                break;
            case 7:
                add(labels, 7, 27, 2);
                break;
            case 2:
                labels.add(2);
                break;
            default:
                throw new IllegalArgumentException("opcode has no verified dispatch plan: " + opcode);
        }
        return new DemoOpcodeDispatchPlan(opcode, false, labels);
    }

    public int entryOpcode() { return entryOpcode; }
    public boolean isSkipped() { return skipped; }
    public List<Integer> reachedLabels() { return reachedLabels; }

    public static boolean isSkipAllowed(int opcode) {
        for (int allowed : SKIP_ALLOW_LIST) if (allowed == opcode) return true;
        return false;
    }

    private static void add(List<Integer> target, int... values) {
        for (int value : values) target.add(value);
    }
}
