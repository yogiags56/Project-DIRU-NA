# DEV-003W10.10 — Selected demo payload decode pipeline

## Goal

Provide one deterministic entry point for the audited selected demo payload structure. Compose the existing payload-header parser, bounded wire-record decoder, and legacy-layout normalizer without adding interpreter or gameplay behavior.

## Pipeline

1. DemoPayloadHeaderReader validates the unsigned little-endian sprite count and complete sprite-ID list, retaining source order and exposing sorted/deduplicated lookup IDs.
2. DemoScriptRecordDecoder parses the remaining bytes as the u16 top-level logical-record count followed by exactly that many bounded wire records.
3. DemoScriptNormalizer converts immutable wire records to detached legacy runtime-layout byte arrays, preserving source-derived sentinels, coordinate scaling, group offset tables, and the opcode-18 overlapping write.

DemoPayloadScriptDecoder composes these steps and exposes one immutable-by-copy result. Wire bytes and normalized bytes remain separately inspectable; original wire offsets and opcodes are retained.

## Safety boundary

- Each stage validates its input before the next stage runs.
- Malformed headers, unknown opcodes, truncated operands, unsupported nested groups, invalid legacy signed lengths, and trailing bytes fail closed with DemoParseException.
- No sprite archive is opened and no image/cache entry is created.
- No script opcode is executed or ticked.
- No stage, collision, renderer, UI, or Android state is mutated.
- Returned arrays are defensive copies.
- Source-order sprite IDs are preserved separately from sorted unique archive lookup IDs.

## Synthetic verification

DemoPayloadScriptDecoderTest covers a complete selected payload with multiple sprite IDs, deterministic ID ordering, wire and normalized record inspection, a one-level group, malformed script bytes after a valid header, and defensive-copy behavior.

These are synthetic contract tests. They do not establish parity against a retail demo binary or validate sprite-cache ownership.

## CI gate

Run the full repository workflow on the branch head: core JVM tests, Android storage tests, and Android debug APK assembly. A green run validates compilation and synthetic tests only; it does not establish a playable game or retail-runtime compatibility.

## Deferred

- outer resource-directory readChunk wrapper;
- demoSpr.bin cache ownership/reference-counting and image decoding;
- interpreter reset, selection, tick, completion, and render behavior;
- stage mutation bridge and gameplay integration;
- original-runtime and retail-binary parity validation.
