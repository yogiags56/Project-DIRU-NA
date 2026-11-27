package MeongDev.DIRUNA.GloftDIRU.core.demo;

/** Composes bounded demo payload parsing without executing or mutating gameplay. */
public final class DemoPayloadScriptDecoder {
    public static final class DecodedPayload {
        private final DemoPayloadHeaderReader.Header header;
        private final DemoScriptRecordDecoder.DecodedScript wireScript;
        private final DemoScriptNormalizer.NormalizedScript normalizedScript;

        private DecodedPayload(DemoPayloadHeaderReader.Header header,
                               DemoScriptRecordDecoder.DecodedScript wireScript,
                               DemoScriptNormalizer.NormalizedScript normalizedScript) {
            this.header = header;
            this.wireScript = wireScript;
            this.normalizedScript = normalizedScript;
        }
        public int declaredSpriteCount() { return header.declaredSpriteCount(); }
        public int[] spriteIdsInSourceOrder() { return header.spriteIdsInSourceOrder(); }
        public int[] sortedUniqueSpriteIds() { return header.sortedUniqueSpriteIds(); }
        public int scriptRecordCount() { return wireScript.declaredRecordCount(); }
        public int wireRecordOpcode(int index) { return wireScript.records().get(index).opcode(); }
        public int wireRecordOffset(int index) { return wireScript.records().get(index).offset(); }
        public byte[] copyWireRecord(int index) { return wireScript.records().get(index).copyWireBytes(); }
        public byte[] copyNormalizedRecord(int index) { return normalizedScript.copyRecord(index); }
    }

    private DemoPayloadScriptDecoder() { }

    /** Parses header, wire records, and normalized buffers; malformed input fails closed. */
    public static DecodedPayload decode(byte[] payload) {
        DemoPayloadHeaderReader.Header header = DemoPayloadHeaderReader.parse(payload);
        DemoScriptRecordDecoder.DecodedScript wire =
                DemoScriptRecordDecoder.decode(header.copyScriptRecordBytes());
        DemoScriptNormalizer.NormalizedScript normalized = DemoScriptNormalizer.normalize(wire);
        return new DecodedPayload(header, wire, normalized);
    }
}
