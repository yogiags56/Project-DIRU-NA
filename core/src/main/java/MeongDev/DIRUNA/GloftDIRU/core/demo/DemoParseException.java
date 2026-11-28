package MeongDev.DIRUNA.GloftDIRU.core.demo;

/** Structured failure raised when a demo archive is malformed or incomplete. */
public final class DemoParseException extends IllegalArgumentException {
    public enum Code {
        INVALID_ARGUMENT, TRUNCATED_HEADER, TRUNCATED_PAYLOAD, INVALID_LENGTH,
        INVALID_COUNT, DUPLICATE_ID, MISSING_ID, TRAILING_BYTES,
        UNKNOWN_OPCODE, NESTED_GROUP
    }

    private final Code code;
    private final int offset;

    public DemoParseException(Code code, int offset, String message) {
        super(message + " (offset=" + offset + ", code=" + code + ")");
        if (code == null) throw new IllegalArgumentException("code is required");
        this.code = code;
        this.offset = offset;
    }

    public Code code() { return code; }
    public int offset() { return offset; }
}
