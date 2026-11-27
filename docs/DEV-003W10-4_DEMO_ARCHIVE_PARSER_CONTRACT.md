# DEV-003W10.4 — Demo archive parser contract and synthetic binary fixtures

## Purpose and boundary

Define a bounded, deterministic parser foundation for the `demo.f` tagged chunk archive and `demoSpr.bin` sprite archive. This increment is parser-only: it must not execute script records, mutate gameplay stage grids, render sprites, or install resources into a global cache.

Evidence is limited to the pinned decompiled candidate source:
- `kubikaugustyn/DiamondRushSource/src/main/java/c.java`
- `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`

No retail runtime or original binary assets are used by this contract. Tests must use synthetic byte arrays authored specifically for the parser tests.

## 1. Parser responsibilities

Keep the archive layers separate:

1. `DemoChunkReader`: walk `demo.f` tagged, length-delimited chunks; return bounded views for matching tags and safely skip nonmatching chunks.
2. `DemoSpriteArchiveReader`: index `demoSpr.bin` records and return bounded payload views by sprite ID.
3. `DemoPayloadHeaderReader`: read the selected demo payload's sprite-ID list and the remaining script-record payload boundary.
4. `DemoScriptBufferDecoder`: later milestone; decode fixed/variable record buffers and nested groups only after record-layout fixtures are complete.

W10.4 implements only (1) and (2), plus payload-header parsing where the exact header byte order and count semantics have been verified against source. It does not implement (4).

## 2. Structural contracts

### demo.f

The observed logical structure is a sequence of tagged records. A matching record contains:
- a 16-bit tag;
- a 16-bit record count;
- a 32-bit payload byte length;
- exactly that many payload bytes.

The reader must expose the tag, count, payload offset and payload length without copying the entire archive. Unknown tags are skipped using the declared bounded payload length. A matching tag must not cause the reader to read beyond its own payload.

### demoSpr.bin

The observed logical structure is:
- a 16-bit archive sprite count;
- repeated records containing a 16-bit sprite ID, a 32-bit payload length, and exactly that many sprite payload bytes.

The reader must provide deterministic lookup by ID while preserving archive order and duplicate-ID evidence. Do not silently overwrite duplicates in a map. The caller must receive an explicit duplicate-ID result or a documented policy; W10.4 chooses rejection as malformed input because source behavior for duplicate IDs is not established.

### Byte order and signedness gate

Do not infer byte order from the file extension or from the nested script-group format. The archive header/tag/count/length fields and the nested script-group length table are separate formats. Before implementation, record the exact source read primitive and its byte order for each archive field. Preserve raw 16-bit bit patterns; convert IDs/tags to nonnegative integer values only if the source comparison semantics support that conversion. Lengths must be validated before conversion to host allocation sizes.

## 3. Malformed-input policy

All readers are fail-closed and return structured parse outcomes. They must never silently reinterpret malformed data as an empty archive, empty script, absent sprite, or valid zero-length record.

Required outcomes:
- `OK`: complete structure parsed and all declared bounds respected.
- `TRUNCATED_HEADER`: not enough bytes remain for the next fixed-size header.
- `TRUNCATED_PAYLOAD`: declared payload extends beyond the containing archive or chunk.
- `INVALID_LENGTH`: negative/overflowing length or length not representable within the containing buffer.
- `INVALID_COUNT`: count cannot be satisfied by the remaining bytes or violates the source-derived maximum.
- `DUPLICATE_ID`: duplicate sprite ID in one archive.
- `MISSING_ID`: requested sprite ID is not present.
- `TRAILING_BYTES`: archive parser did not consume the complete declared archive.
- `INVALID_ARGUMENT`: null input, invalid range, or invalid requested identifier.

A parser must not allocate memory directly from an untrusted declared length. First validate using overflow-safe arithmetic against the enclosing byte-array range; only then expose a slice or allocate a bounded copy. Zero-length payloads are valid only where the source structure permits them. Count/length disagreement is malformed, not an invitation to guess.

Every parse result must include the failing byte offset and a stable diagnostic code. Avoid embedding raw binary payload contents in diagnostics.

## 4. Bounds and arithmetic invariants

- All offsets and lengths are checked with subtraction-based range validation (`length <= limit - offset`) after validating `offset <= limit`; avoid unchecked `offset + length`.
- 16-bit fields are read only when two bytes remain; 32-bit fields only when four remain.
- Archive iteration must make forward progress for every accepted record.
- A chunk's declared payload is bounded by the archive, and its parser cannot consume bytes belonging to the next chunk.
- Sprite records are bounded by the sprite archive, and one sprite payload cannot overlap the next record.
- A requested sprite ID must resolve to exactly one archive entry.
- No unbounded scan, retry loop, or sentinel-based overread is permitted.
- The parser must not use the source's insertion sort or sequential search as a reason to trust archive ordering; native lookup must still preserve last/first/duplicate semantics explicitly where source evidence requires it.

## 5. Synthetic fixture matrix

### demo.f chunk archive
1. Empty archive.
2. One matching chunk with zero-length payload, if source permits.
3. One matching chunk with a nonempty payload.
4. Multiple tags where unknown chunks are skipped and a later requested tag is found.
5. Duplicate matching tags, verifying source-compatible selection policy at the caller boundary.
6. Truncated tag, count, and length fields independently.
7. Payload length exactly equal to remaining archive bytes.
8. Payload length one byte larger than remaining archive bytes.
9. Maximum accepted payload and one byte above the configured cap.
10. Trailing partial header after an otherwise valid chunk.

### demoSpr.bin
1. Empty archive and zero sprites.
2. One sprite with zero-length payload, if permitted by source.
3. Multiple IDs in ascending, descending, and mixed order.
4. Duplicate IDs (must return `DUPLICATE_ID`).
5. Requested ID absent (`MISSING_ID`).
6. Truncated archive count, ID, length, and sprite body independently.
7. Declared length at exact end-of-buffer and one byte beyond.
8. Length arithmetic overflow and configured maximum boundary.
9. Duplicate requested IDs in the caller list; lookup must be deterministic and must not duplicate cache ownership.
10. Trailing bytes after the declared sprite count.

### Payload-header boundary
1. Zero sprite IDs.
2. One and multiple IDs.
3. Ascending, descending, and duplicate requested IDs.
4. Truncated ID list.
5. Count larger than the remaining header bytes.
6. Payload boundary exactly at the end of the selected chunk.

## 6. Ownership and immutability

- Parser results are immutable views over a privately owned input snapshot, or detached copies; they must never expose a mutable caller-owned backing array.
- The parser does not create image objects or shared sprite-cache entries.
- Sprite payload ownership remains with the archive result until a later cache-loading milestone defines reference counting and teardown.
- Disposing one parsed demo payload must not invalidate another payload or a shared archive snapshot.
- Keep archive parsing independent from script interpreter update, execution, render, and cleanup.

## 7. Acceptance tests

W10.4 is accepted only when:
- source byte order and field signedness are recorded for each archive field;
- every fixture above has a deterministic test;
- malformed input returns the expected structured status and exact failure offset;
- no test depends on original proprietary assets;
- parser output is immutable/detached;
- full core JVM test suite passes;
- Android debug build and Android storage tests remain green;
- no production stage mutation or GameSession integration is introduced.

## 8. Explicitly deferred

- demo script record decoding and nested-group execution;
- script interpreter tick/update/reset/completion behavior;
- render traversal and render-side state mutations;
- shared sprite cache construction, ownership and reference counting;
- stage teardown integration;
- interpreter-to-gameplay mutation bridge;
- retail-runtime parity validation.

## 9. Next

After this contract is reviewed, implement the two bounded archive readers and their synthetic fixtures as a separate code increment. Keep demo record decoding and interpreter execution deferred until the archive parser and shared sprite ownership contracts are proven.
