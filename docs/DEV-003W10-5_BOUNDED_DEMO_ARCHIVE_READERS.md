# DEV-003W10.5 — Bounded demo archive readers

## Implemented scope

The core module now contains a parser-only implementation under
`MeongDev.DIRUNA.GloftDIRU.core.demo`:

- `DemoParseException`: stable error codes and failure offsets.
- `DemoChunkReader`: reads the tagged entries from an already-extracted `demo.f` chunk-zero byte array.
- `DemoSpriteArchiveReader`: parses and indexes `demoSpr.bin` sprite records, rejecting duplicate IDs and reporting missing requested IDs.

The implementation is platform-independent Java 17 and has no Android, image-decoder, stage, renderer, or interpreter dependencies.

## Source-derived compatibility

The parser follows the pinned source audit in `DIRU-NA-AUDIT-015_DEMO_ARCHIVE_BYTE_LAYOUT.md`:
- unsigned little-endian 16-bit and 32-bit fields;
- two-byte opaque preamble for the inner `demo.f` tagged stream;
- first matching tag wins;
- sprite records retain archive insertion order;
- requested sprite IDs are returned in ascending numeric order.

## Safety properties

- Fixed-size headers are validated before field reads.
- 32-bit unsigned lengths are held in `long` until checked against remaining bytes.
- No allocation is performed from an unchecked declared length.
- Every payload is copied into parser-owned storage.
- Returned payloads and requested-sprite maps contain detached copies.
- Duplicate sprite IDs, missing requested IDs, truncated headers, truncated payloads, and trailing bytes fail explicitly.
- The parser does not decode sprite image formats or mutate global cache state.

## Synthetic tests added

`DemoArchiveReaderTest` covers:
- first matching tag and unknown-tag skipping;
- absent tags;
- truncated chunk headers and bodies;
- unsigned sprite IDs and sorted/deduplicated requests;
- duplicate and missing sprite IDs;
- truncated sprite headers and bodies;
- trailing bytes;
- defensive copies and invalid arguments.

## Verification status

Source and test files are committed to the development branch. The GitHub connector did not return an associated Actions run for the latest commit, so this milestone is **not yet CI-verified**. Do not treat the parser as build-validated until the repository's Android/core workflow reports green.

## Deferred

- full resource-directory `readChunk` wrapper implementation;
- selected demo payload sprite-ID header decoder;
- script-record and nested-group decoder;
- image decoding and shared cache ownership;
- interpreter, rendering, stage mutation, and gameplay integration.

The resource-directory wrapper is deliberately deferred because W10.5's public parser contract consumes an already-extracted inner chunk. Keeping these layers separate prevents the inner tagged stream from being confused with the outer resource directory.
