package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Source-derived read requirements for the legacy demo interpreter switch.
 *
 * Offsets are relative to the current child/leaf start. A descriptor describes
 * the full fall-through path until a source-level break/return or switch end;
 * it does not claim that every conditional path performs every read.
 */
public final class DemoOpcodeOperandDescriptor {
    public enum ReadKind { U8, U16_LE, U32_LE }

    public static final class OperandRead {
        private final int opcodeLabel;
        private final int offset;
        private final int width;
        private final ReadKind kind;
        private final String purpose;

        private OperandRead(int opcodeLabel, int offset, ReadKind kind, String purpose) {
            this.opcodeLabel = opcodeLabel;
            this.offset = offset;
            this.kind = kind;
            this.width = kind == ReadKind.U8 ? 1 : kind == ReadKind.U16_LE ? 2 : 4;
            this.purpose = purpose;
        }
        public int opcodeLabel() { return opcodeLabel; }
        public int offset() { return offset; }
        public int width() { return width; }
        public ReadKind kind() { return kind; }
        public String purpose() { return purpose; }
        public int endExclusive() { return offset + width; }
    }

    private static final Map<Integer, DemoOpcodeOperandDescriptor> TABLE = buildTable();

    private final int entryOpcode;
    private final List<Integer> switchLabels;
    private final List<OperandRead> reads;
    private final int minimumBackingSpan;

    private DemoOpcodeOperandDescriptor(int entryOpcode, List<Integer> labels,
                                         List<OperandRead> reads) {
        this.entryOpcode = entryOpcode;
        this.switchLabels = Collections.unmodifiableList(labels);
        this.reads = Collections.unmodifiableList(reads);
        int span = 2;
        for (OperandRead read : reads) span = Math.max(span, read.endExclusive());
        this.minimumBackingSpan = span;
    }

    public int entryOpcode() { return entryOpcode; }
    public List<Integer> switchLabels() { return switchLabels; }
    public List<OperandRead> reads() { return reads; }
    public int minimumBackingSpan() { return minimumBackingSpan; }

    public static DemoOpcodeOperandDescriptor forOpcode(int opcode) {
        return TABLE.get(opcode);
    }

    public static Map<Integer, DemoOpcodeOperandDescriptor> all() {
        return TABLE;
    }

    private static Map<Integer, DemoOpcodeOperandDescriptor> buildTable() {
        Map<Integer, DemoOpcodeOperandDescriptor> map =
                new LinkedHashMap<Integer, DemoOpcodeOperandDescriptor>();

        // case 1 reads coordinates/limits at +2,+4,+6,+8,+10.
        add(map, 1, labels(1), reads(
                r(1,2,ReadKind.U16_LE,"x sentinel/value"),
                r(1,4,ReadKind.U16_LE,"y sentinel/value"),
                r(1,6,ReadKind.U16_LE,"duration/limit"),
                r(1,8,ReadKind.U16_LE,"x target"),
                r(1,10,ReadKind.U16_LE,"y target")));

        // case 4 reads its frame limit at +14.
        add(map, 4, labels(4), reads(r(4,14,ReadKind.U16_LE,"frame limit")));
        add(map, 6, labels(6), reads(r(6,2,ReadKind.U32_LE,"frame limit")));

        // 11 falls through 10 and 13.
        add(map, 11, labels(11,10,13), reads(
                r(11,2,ReadKind.U16_LE,"animation/frame value"),
                r(11,4,ReadKind.U16_LE,"animation index"),
                r(10,2,ReadKind.U8,"animation state"),
                r(13,6,ReadKind.U16_LE,"duration/limit"),
                r(13,8,ReadKind.U16_LE,"x sentinel/value"),
                r(13,10,ReadKind.U16_LE,"y sentinel/value")));

        // 16/17/18 fall through 14/15 and then 12. Dynamic +2/+4 and +5/+7
        // reads are represented as the union of source branches.
        add(map, 16, labels(16,14,12), reads(
                r(16,2,ReadKind.U16_LE,"frame/target"),
                r(16,4,ReadKind.U16_LE,"alternate frame/target"),
                r(16,4,ReadKind.U8,"frame counter (dynamic branch at +4)"),
                r(16,5,ReadKind.U8,"completion flag"),
                r(16,2,ReadKind.U16_LE,"completion frame"),
                r(12,2,ReadKind.U16_LE,"x coordinate"),
                r(12,4,ReadKind.U16_LE,"y coordinate")));
        add(map, 17, labels(17,15,12), reads(
                r(17,2,ReadKind.U16_LE,"frame/target"),
                r(17,4,ReadKind.U16_LE,"alternate frame/target"),
                r(17,4,ReadKind.U8,"frame counter (dynamic branch at +4)"),
                r(17,5,ReadKind.U8,"completion flag"),
                r(17,2,ReadKind.U16_LE,"completion frame"),
                r(12,2,ReadKind.U16_LE,"x coordinate"),
                r(12,4,ReadKind.U16_LE,"y coordinate")));
        add(map, 18, labels(18,15,12), reads(
                r(18,4,ReadKind.U8,"frame counter"),
                r(18,7,ReadKind.U8,"completion flag"),
                r(12,2,ReadKind.U16_LE,"x coordinate"),
                r(12,4,ReadKind.U16_LE,"y coordinate")));

        // 25 falls through 26, 5, 8, 9, 27, then 2.
        add(map, 25, labels(25,26,5,8,9,27,2), reads(
                r(25,2,ReadKind.U16_LE,"foreground x"),
                r(25,4,ReadKind.U16_LE,"foreground y"),
                r(25,6,ReadKind.U8,"foreground tile high/low byte"),
                r(25,7,ReadKind.U8,"foreground tile high/low byte"),
                r(26,2,ReadKind.U16_LE,"collision x"),
                r(26,4,ReadKind.U16_LE,"collision y"),
                r(26,6,ReadKind.U32_LE,"collision value"),
                r(5,2,ReadKind.U16_LE,"stage x"),
                r(5,4,ReadKind.U16_LE,"stage y"),
                r(5,6,ReadKind.U8,"stage operation"),
                r(9,2,ReadKind.U16_LE,"stage x"),
                r(9,4,ReadKind.U16_LE,"stage y"),
                r(9,6,ReadKind.U16_LE,"stage value"),
                r(27,7,ReadKind.U8,"completion flag"),
                r(2,4,ReadKind.U16_LE,"text position sentinel"),
                r(2,10,ReadKind.U8,"text completion flag")));

        // Standalone entries into labels that are reached by fall-through.
        add(map, 26, labels(26,5,8,9,27,2), reads(
                r(26,2,ReadKind.U16_LE,"collision x"),
                r(26,4,ReadKind.U16_LE,"collision y"),
                r(26,6,ReadKind.U32_LE,"collision value"),
                r(5,2,ReadKind.U16_LE,"stage x"),
                r(5,4,ReadKind.U16_LE,"stage y"),
                r(5,6,ReadKind.U8,"stage operation"),
                r(9,2,ReadKind.U16_LE,"stage x"),
                r(9,4,ReadKind.U16_LE,"stage y"),
                r(9,6,ReadKind.U16_LE,"stage value"),
                r(27,7,ReadKind.U8,"completion flag")));
        add(map, 5, labels(5,8,9,27,2), reads(
                r(5,2,ReadKind.U16_LE,"stage x"),
                r(5,4,ReadKind.U16_LE,"stage y"),
                r(5,6,ReadKind.U8,"stage operation"),
                r(9,2,ReadKind.U16_LE,"stage x"),
                r(9,4,ReadKind.U16_LE,"stage y"),
                r(9,6,ReadKind.U16_LE,"stage value"),
                r(27,7,ReadKind.U8,"completion flag")));
        add(map, 8, labels(8,9,27,2), reads(
                r(9,2,ReadKind.U16_LE,"stage x"),
                r(9,4,ReadKind.U16_LE,"stage y"),
                r(9,6,ReadKind.U16_LE,"stage value"),
                r(27,7,ReadKind.U8,"completion flag")));
        add(map, 9, labels(9,27,2), reads(
                r(9,2,ReadKind.U16_LE,"stage x"),
                r(9,4,ReadKind.U16_LE,"stage y"),
                r(9,6,ReadKind.U16_LE,"stage value"),
                r(27,7,ReadKind.U8,"completion flag")));
        add(map, 27, labels(27,2), reads(
                r(27,7,ReadKind.U8,"completion flag"),
                r(2,4,ReadKind.U16_LE,"text position sentinel"),
                r(2,10,ReadKind.U8,"text completion flag")));

        // Opcode 2 has text-dependent trailing data; only fixed reads visible
        // in the switch are recorded here.
        add(map, 2, labels(2), reads(
                r(2,4,ReadKind.U16_LE,"text position sentinel"),
                r(2,10,ReadKind.U8,"text completion flag")));

        return Collections.unmodifiableMap(map);
    }

    private static void add(Map<Integer, DemoOpcodeOperandDescriptor> map, int opcode,
                            List<Integer> labels, List<OperandRead> reads) {
        map.put(opcode, new DemoOpcodeOperandDescriptor(opcode, labels, reads));
    }
    private static OperandRead r(int label, int offset, ReadKind kind, String purpose) {
        return new OperandRead(label, offset, kind, purpose);
    }
    private static List<Integer> labels(Integer... values) {
        return Arrays.asList(values);
    }
    private static List<OperandRead> reads(OperandRead... values) {
        return Arrays.asList(values);
    }
}
