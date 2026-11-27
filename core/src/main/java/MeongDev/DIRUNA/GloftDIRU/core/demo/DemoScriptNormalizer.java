package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reconstructs the legacy runtime byte-array layout from decoded wire records.
 * This class performs no opcode execution and has no stage or sprite side effects.
 */
public final class DemoScriptNormalizer {
    public static final class NormalizedScript {
        private final List<byte[]> records;
        private NormalizedScript(List<byte[]> records) {
            List<byte[]> copy = new ArrayList<byte[]>(records.size());
            for (byte[] record : records) copy.add(record.clone());
            this.records = Collections.unmodifiableList(copy);
        }
        public int recordCount() { return records.size(); }
        public byte[] copyRecord(int index) { return records.get(index).clone(); }
        public List<byte[]> copyRecords() {
            List<byte[]> copy = new ArrayList<byte[]>(records.size());
            for (byte[] record : records) copy.add(record.clone());
            return Collections.unmodifiableList(copy);
        }
    }

    private DemoScriptNormalizer() { }

    public static NormalizedScript normalize(DemoScriptRecordDecoder.DecodedScript script) {
        if (script == null) throw error(DemoParseException.Code.INVALID_ARGUMENT, 0, "decoded script is required");
        List<byte[]> result = new ArrayList<byte[]>(script.records().size());
        for (DemoScriptRecordDecoder.Record record : script.records()) result.add(normalizeRecord(record));
        return new NormalizedScript(result);
    }

    private static byte[] normalizeRecord(DemoScriptRecordDecoder.Record record) {
        int opcode = record.opcode();
        long[] v = record.operands();
        byte[] data = record.copyData();
        byte[] out;
        switch (opcode) {
            case 0:
                return packGroup(record);
            case 1:
                out = header(opcode, 12);
                putShort(out, 2, (short) ((int) v[0] * 24));
                putShort(out, 4, (short) ((int) v[1] * 24));
                putShort(out, 6, (short) v[2]);
                putShort(out, 8, (short) 10000);
                putShort(out, 10, (short) 10000);
                return out;
            case 2: {
                int length = checkedLegacyLength(v[2], record.offset());
                out = header(opcode, checkedSize(11, length, record.offset()));
                putShort(out, 2, (short) length);
                putShort(out, 4, (short) 10000);
                putShort(out, 6, (short) v[1]);
                out[8] = (byte) v[0];
                out[9] = 0;
                out[10] = 0;
                System.arraycopy(data, 0, out, 11, data.length);
                return out;
            }
            case 4:
                out = header(opcode, 16);
                for (int i = 0; i < 7; i++) putShort(out, 2 + i * 2, (short) v[i]);
                return out;
            case 5:
                out = header(opcode, 7);
                putShort(out, 2, (short) v[0]);
                putShort(out, 4, (short) v[1]);
                out[6] = (byte) v[2];
                return out;
            case 6:
                out = header(opcode, 6);
                putInt(out, 2, (int) v[0]);
                return out;
            case 7: case 14: case 15:
                return header(opcode, 2);
            case 9:
                out = header(opcode, 8);
                for (int i = 0; i < 3; i++) putShort(out, 2 + i * 2, (short) v[i]);
                return out;
            case 10:
                out = header(opcode, 3);
                out[2] = (byte) v[0];
                return out;
            case 11: case 12:
                out = header(opcode, 6);
                putShort(out, 2, (short) v[0]);
                putShort(out, 4, (short) v[1]);
                return out;
            case 13:
                out = header(opcode, 12);
                putShort(out, 2, (short) v[0]);
                putShort(out, 4, (short) v[1]);
                putShort(out, 6, (short) v[2]);
                putShort(out, 8, (short) 10000);
                putShort(out, 10, (short) 10000);
                return out;
            case 16: case 17:
                out = header(opcode, 6);
                putShort(out, 2, (short) v[0]);
                putShort(out, 4, (short) v[1]);
                out[5] = 0;
                return out;
            case 18:
                out = header(opcode, 8);
                putShort(out, 2, (short) v[0]);
                // Legacy source writes the 24-bit operand at byte offset 3, overlapping
                // the second byte of the preceding short, then clears byte 7.
                putInt24Legacy(out, 3, v[1]);
                out[7] = 0;
                return out;
            case 25:
                out = header(opcode, 8);
                putShort(out, 2, (short) v[0]);
                putShort(out, 4, (short) v[1]);
                out[6] = (byte) v[2];
                out[7] = (byte) v[3];
                return out;
            case 26:
                out = header(opcode, 10);
                putShort(out, 2, (short) v[0]);
                putShort(out, 4, (short) v[1]);
                putInt(out, 6, (int) v[2]);
                return out;
            case 27: {
                int length = checkedLegacyLength(v[0], record.offset());
                out = header(opcode, checkedSize(8, length, record.offset()));
                putShort(out, 2, (short) length);
                putShort(out, 4, (short) 10000);
                out[6] = 0;
                out[7] = 0;
                System.arraycopy(data, 0, out, 8, data.length);
                return out;
            }
            default:
                throw error(DemoParseException.Code.UNKNOWN_OPCODE, record.offset(), "no normalized layout for opcode " + opcode);
        }
    }

    private static byte[] packGroup(DemoScriptRecordDecoder.Record group) {
        List<DemoScriptRecordDecoder.Record> children = group.children();
        int count = children.size();
        long payloadLength = 0;
        List<byte[]> normalized = new ArrayList<byte[]>(count);
        for (DemoScriptRecordDecoder.Record child : children) {
            byte[] bytes = normalizeRecord(child);
            payloadLength += bytes.length;
            if (payloadLength > Integer.MAX_VALUE)
                throw error(DemoParseException.Code.INVALID_LENGTH, group.offset(), "group payload exceeds Java array limit");
            normalized.add(bytes);
        }
        long total = 2L + count * 4L + payloadLength;
        if (total > Integer.MAX_VALUE)
            throw error(DemoParseException.Code.INVALID_LENGTH, group.offset(), "group buffer exceeds Java array limit");
        byte[] out = new byte[(int) total];
        out[0] = 0;
        out[1] = (byte) count;
        int dataOffset = 2 + count * 4;
        for (int i = 0; i < count; i++) {
            byte[] child = normalized.get(i);
            putInt(out, 2 + i * 4, child.length);
            System.arraycopy(child, 0, out, dataOffset, child.length);
            dataOffset += child.length;
        }
        return out;
    }

    private static int checkedLegacyLength(long length, int offset) {
        if (length < 0 || length > Short.MAX_VALUE)
            throw error(DemoParseException.Code.INVALID_LENGTH, offset, "variable payload length exceeds legacy signed-short range");
        return (int) length;
    }
    private static int checkedSize(int prefix, int length, int offset) {
        long size = (long) prefix + length;
        if (size > Integer.MAX_VALUE)
            throw error(DemoParseException.Code.INVALID_LENGTH, offset, "normalized record exceeds Java array limit");
        return (int) size;
    }
    private static byte[] header(int opcode, int length) {
        byte[] out = new byte[length];
        out[0] = (byte) opcode;
        out[1] = 0;
        return out;
    }
    private static void putShort(byte[] out, int offset, short value) {
        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >> 8);
    }
    private static void putInt(byte[] out, int offset, int value) {
        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >> 8);
        out[offset + 2] = (byte) (value >> 16);
        out[offset + 3] = (byte) (value >>> 24);
    }
    private static void putInt24Legacy(byte[] out, int offset, long value) {
        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >>> 8);
        out[offset + 2] = (byte) (value >>> 16);
    }
    private static DemoParseException error(DemoParseException.Code code, int offset, String message) {
        return new DemoParseException(code, offset, message);
    }
}
