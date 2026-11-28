package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded decoder for the script-record wire stream in a selected demo payload.
 * It parses, but never executes or normalizes, legacy script records.
 *
 * The leading u16 is the number of top-level logical records. Opcode 0 contains
 * a one-level child list. Nested opcode-0 groups are rejected until the legacy
 * loader's single-level temporary-group behavior is proven otherwise.
 */
public final class DemoScriptRecordDecoder {
    private static final int MAX_SIGNED_SHORT_COUNT = Short.MAX_VALUE;

    public static final class Record {
        private final int opcode;
        private final long[] operands;
        private final byte[] data;
        private final List<Record> children;
        private final byte[] wireBytes;
        private final int offset;

        private Record(int opcode, long[] operands, byte[] data, List<Record> children,
                       byte[] wireBytes, int offset) {
            this.opcode = opcode;
            this.operands = operands.clone();
            this.data = data.clone();
            this.children = Collections.unmodifiableList(new ArrayList<Record>(children));
            this.wireBytes = wireBytes.clone();
            this.offset = offset;
        }
        public int opcode() { return opcode; }
        public long[] operands() { return operands.clone(); }
        public byte[] copyData() { return data.clone(); }
        public List<Record> children() { return children; }
        public byte[] copyWireBytes() { return wireBytes.clone(); }
        public int offset() { return offset; }
        public boolean isGroup() { return opcode == 0; }
    }

    public static final class DecodedScript {
        private final List<Record> records;
        private final int declaredRecordCount;
        private final byte[] trailing; // always empty on successful parse
        private DecodedScript(int count, List<Record> records) {
            declaredRecordCount = count;
            this.records = Collections.unmodifiableList(new ArrayList<Record>(records));
            trailing = new byte[0];
        }
        public int declaredRecordCount() { return declaredRecordCount; }
        public List<Record> records() { return records; }
        public byte[] copyTrailingBytes() { return trailing.clone(); }
    }

    private final byte[] input;
    private int cursor;

    private DemoScriptRecordDecoder(byte[] input) { this.input = input.clone(); }

    public static DecodedScript decode(byte[] scriptBytes) {
        if (scriptBytes == null) throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "script bytes are required");
        DemoScriptRecordDecoder decoder = new DemoScriptRecordDecoder(scriptBytes);
        decoder.require(0, 2, DemoParseException.Code.TRUNCATED_HEADER, "missing top-level record count");
        int count = decoder.u16();
        if (count > MAX_SIGNED_SHORT_COUNT)
            throw error(DemoParseException.Code.INVALID_COUNT, 0, "top-level record count exceeds legacy signed-short range");
        List<Record> records = new ArrayList<Record>(count);
        for (int i = 0; i < count; i++) records.add(decoder.readRecord(false));
        if (decoder.cursor != decoder.input.length)
            throw error(DemoParseException.Code.TRAILING_BYTES, decoder.cursor, "bytes remain after declared top-level records");
        return new DecodedScript(count, records);
    }

    private Record readRecord(boolean insideGroup) {
        int start = cursor;
        require(cursor, 1, DemoParseException.Code.TRUNCATED_HEADER, "missing opcode byte");
        int opcode = u8();
        long[] values;
        byte[] data = new byte[0];
        List<Record> children = Collections.emptyList();
        switch (opcode) {
            case 0: {
                if (insideGroup)
                    throw error(DemoParseException.Code.NESTED_GROUP, start, "nested opcode-0 group is not supported by verified source contract");
                require(cursor, 1, DemoParseException.Code.TRUNCATED_PAYLOAD, "missing group child count");
                int childCount = u8();
                List<Record> parsed = new ArrayList<Record>(childCount);
                for (int i = 0; i < childCount; i++) parsed.add(readRecord(true));
                children = parsed;
                values = new long[]{childCount};
                break;
            }
            case 1:
                values = new long[]{u16(), u16(), u16()};
                break;
            case 2: {
                long mode = u8();
                long value = u16();
                int length = u16();
                data = bytes(length, "opcode 2 text");
                values = new long[]{mode, value, length};
                break;
            }
            case 4: {
                values = new long[7];
                for (int i = 0; i < values.length; i++) values[i] = u16();
                break;
            }
            case 5:
                values = new long[]{u16(), u16(), u8()};
                break;
            case 6:
                values = new long[]{u32()};
                break;
            case 7: case 14: case 15:
                values = new long[0];
                break;
            // Opcode 8 falls through into opcode 9 in the legacy executor and
            // therefore carries the same three-u16 callback operands.
            case 8: case 9:
                values = new long[]{u16(), u16(), u16()};
                break;
            case 10:
                values = new long[]{u8()};
                break;
            case 11: case 12:
                values = new long[]{u16(), u16()};
                break;
            case 13:
                values = new long[]{u16(), u16(), u16()};
                break;
            case 16: case 17:
                values = new long[]{u16(), u8()};
                break;
            case 18:
                values = new long[]{u8(), u24()};
                break;
            case 25:
                values = new long[]{u16(), u16(), u8(), u8()};
                break;
            case 26:
                values = new long[]{u16(), u16(), u32()};
                break;
            case 27: {
                int length = u16();
                data = bytes(length, "opcode 27 byte payload");
                values = new long[]{length};
                break;
            }
            default:
                throw error(DemoParseException.Code.UNKNOWN_OPCODE, start, "unsupported script opcode " + opcode);
        }
        byte[] wire = new byte[cursor - start];
        System.arraycopy(input, start, wire, 0, wire.length);
        return new Record(opcode, values, data, children, wire, start);
    }

    private int u8() {
        require(cursor, 1, DemoParseException.Code.TRUNCATED_PAYLOAD, "truncated 8-bit operand");
        return input[cursor++] & 0xFF;
    }
    private int u16() {
        require(cursor, 2, DemoParseException.Code.TRUNCATED_PAYLOAD, "truncated 16-bit operand");
        int value = (input[cursor] & 0xFF) | ((input[cursor + 1] & 0xFF) << 8);
        cursor += 2;
        return value;
    }
    private long u24() {
        require(cursor, 3, DemoParseException.Code.TRUNCATED_PAYLOAD, "truncated 24-bit operand");
        long value = ((long) input[cursor] & 0xFF)
                | (((long) input[cursor + 1] & 0xFF) << 8)
                | (((long) input[cursor + 2] & 0xFF) << 16);
        cursor += 3;
        return value;
    }
    private long u32() {
        require(cursor, 4, DemoParseException.Code.TRUNCATED_PAYLOAD, "truncated 32-bit operand");
        long value = ((long) input[cursor] & 0xFF)
                | (((long) input[cursor + 1] & 0xFF) << 8)
                | (((long) input[cursor + 2] & 0xFF) << 16)
                | (((long) input[cursor + 3] & 0xFF) << 24);
        cursor += 4;
        return value;
    }
    private byte[] bytes(int length, String field) {
        require(cursor, length, DemoParseException.Code.TRUNCATED_PAYLOAD, "truncated " + field);
        byte[] result = new byte[length];
        System.arraycopy(input, cursor, result, 0, length);
        cursor += length;
        return result;
    }
    private void require(int offset, int length, DemoParseException.Code code, String message) {
        if (offset < 0 || length < 0 || offset > input.length || length > input.length - offset)
            throw error(code, Math.max(0, Math.min(offset, input.length)), message);
    }
    private static DemoParseException error(DemoParseException.Code code, int offset, String message) {
        return new DemoParseException(code, offset, message);
    }
}
