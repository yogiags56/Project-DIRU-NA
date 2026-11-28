package MeongDev.DIRUNA.GloftDIRU.core.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable lifecycle state layered over immutable normalized demo records.
 * Runtime flags and counters never modify the normalized byte buffers.
 *
 * This is a state container only: it does not execute opcodes, mutate a stage,
 * render, or schedule time-based text reveal.
 */
public final class DemoInterpreterRuntimeState {
    public static final class RecordState {
        private final byte[] normalizedBytes;
        private final int opcode;
        private final List<RecordState> children;
        private boolean completed;
        private int revealCounter;
        private int scrollPosition;

        private RecordState(byte[] bytes) {
            if (bytes == null || bytes.length < 2)
                throw new IllegalArgumentException("normalized record must contain a two-byte header");
            normalizedBytes = bytes.clone();
            opcode = normalizedBytes[0] & 0xFF;
            // For groups byte 1 is the child count, not a reserved byte.
            if (opcode != 0 && (normalizedBytes[1] & 0xFF) != 0)
                throw new IllegalArgumentException("normalized record reserved byte must be zero");
            if (opcode == 0) {
                children = Collections.unmodifiableList(parseGroupChildren(normalizedBytes));
            } else {
                children = Collections.emptyList();
            }
        }

        public int opcode() { return opcode; }
        public byte[] copyNormalizedBytes() { return normalizedBytes.clone(); }
        public List<RecordState> children() { return children; }
        public boolean isGroup() { return opcode == 0; }
        public boolean isCompleted() {
            if (!isGroup()) return completed;
            for (RecordState child : children) if (!child.isCompleted()) return false;
            return true;
        }
        public int revealCounter() { return revealCounter; }
        public int scrollPosition() { return scrollPosition; }

        public void setCompleted(boolean value) {
            if (isGroup()) throw new IllegalStateException("group completion is derived from its children");
            completed = value;
        }
        public void setRevealCounter(int value) {
            if (value < 0) throw new IllegalArgumentException("reveal counter must be non-negative");
            revealCounter = value;
        }
        public void setScrollPosition(int value) {
            if (value < 0) throw new IllegalArgumentException("scroll position must be non-negative");
            scrollPosition = value;
        }
        private void reset() {
            completed = false;
            revealCounter = 0;
            scrollPosition = 0;
            for (RecordState child : children) child.reset();
        }
    }

    private final List<RecordState> buffers;
    private int selectedBufferIndex;
    private int tick;
    private boolean skipRequested;

    public DemoInterpreterRuntimeState(DemoScriptNormalizer.NormalizedScript script) {
        if (script == null) throw new IllegalArgumentException("normalized script is required");
        List<RecordState> parsed = new ArrayList<RecordState>(script.recordCount());
        for (byte[] bytes : script.copyRecords()) parsed.add(new RecordState(bytes));
        buffers = Collections.unmodifiableList(parsed);
        selectedBufferIndex = buffers.isEmpty() ? -1 : 0;
    }

    public int bufferCount() { return buffers.size(); }
    public List<RecordState> buffers() { return buffers; }
    public int selectedBufferIndex() { return selectedBufferIndex; }
    public int tick() { return tick; }
    public boolean skipRequested() { return skipRequested; }

    /** Returns false and moves selection to -1 when there is no next buffer. */
    public boolean selectNextBuffer() {
        if (selectedBufferIndex < 0) return false;
        if (selectedBufferIndex + 1 >= buffers.size()) {
            selectedBufferIndex = -1;
            return false;
        }
        selectedBufferIndex++;
        tick = 0;
        return true;
    }

    /** Advances the interpreter frame counter exactly once; never selects a buffer. */
    public void advanceTick() {
        if (tick == Integer.MAX_VALUE) throw new IllegalStateException("tick counter overflow");
        tick++;
    }

    public void setSkipRequested(boolean value) { skipRequested = value; }

    /** Resets every buffer and child, then selects the first buffer. */
    public void reset() {
        for (RecordState buffer : buffers) buffer.reset();
        selectedBufferIndex = buffers.isEmpty() ? -1 : 0;
        tick = 0;
        skipRequested = false;
    }

    private static List<RecordState> parseGroupChildren(byte[] bytes) {
        if (bytes.length < 2) throw new IllegalArgumentException("truncated normalized group header");
        int count = bytes[1] & 0xFF;
        long tableEndLong = 2L + count * 4L;
        if (tableEndLong > bytes.length)
            throw new IllegalArgumentException("truncated normalized group length table");
        int tableEnd = (int) tableEndLong;
        long payloadLength = 0;
        int[] lengths = new int[count];
        for (int i = 0; i < count; i++) {
            int offset = 2 + i * 4;
            long length = ((long) bytes[offset] & 0xFF)
                    | (((long) bytes[offset + 1] & 0xFF) << 8)
                    | (((long) bytes[offset + 2] & 0xFF) << 16)
                    | (((long) bytes[offset + 3] & 0xFF) << 24);
            if (length < 2 || length > Integer.MAX_VALUE)
                throw new IllegalArgumentException("invalid normalized group child length");
            lengths[i] = (int) length;
            payloadLength += length;
            if (payloadLength > Integer.MAX_VALUE)
                throw new IllegalArgumentException("normalized group payload is too large");
        }
        if (tableEndLong + payloadLength != bytes.length)
            throw new IllegalArgumentException("normalized group child lengths do not match payload size");
        List<RecordState> result = new ArrayList<RecordState>(count);
        int cursor = tableEnd;
        for (int length : lengths) {
            byte[] child = new byte[length];
            System.arraycopy(bytes, cursor, child, 0, length);
            if ((child[0] & 0xFF) == 0)
                throw new IllegalArgumentException("nested normalized groups are not supported");
            result.add(new RecordState(child));
            cursor += length;
        }
        return result;
    }
}
