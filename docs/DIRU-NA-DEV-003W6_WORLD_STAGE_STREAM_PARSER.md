# DIRU-NA DEV-003W6 — Bounded world stage-stream parser

## Source-backed format

Pinned candidate source: `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

The audited world files contain:
1. One opaque prefix byte (observed value `0x01`; semantic meaning remains unknown).
2. One unsigned stage-count byte.
3. For every stage: unsigned little-endian 16-bit width and height, then three contiguous byte layers in order: player/object, background, foreground.
4. Each layer contains exactly width × height bytes, indexed in source row-major order `x + y * width`.

The pinned binary inventory validated 14 Angkor, 13 Scotland/Bavaria-resource, and 14 Tibet/Siberia-resource records, with exact full-file consumption. The parser does not infer the unresolved stage/title count anomalies.

## Implementation

Added `WorldStageStreamParser` in the platform-independent `:core` module.

- Uses a bounded in-memory cursor with exact byte reads and skips.
- Checks prefix/count availability, selected index, nonzero dimensions, configurable maximum width/height/cell count, overflow-safe cell arithmetic, and remaining payload bytes before allocating selected layers.
- Skips unselected stage payloads only after validating their dimensions and byte extents.
- Requires exact file consumption; trailing data is rejected.
- Preserves Java signed-byte widening into StageGrid integer arrays, matching the audited legacy byte-array layer assignments.
- Returns a detached immutable StageGrid with the original layer order and dimensions.
- Does not treat the opaque prefix as a version enum.

## Tests added

`WorldStageStreamParserTest` covers:
- Selecting a later stage while skipping an earlier record.
- Layer ordering and dimensions.
- Signed-byte widening for 0xFF, 0x80 and 0x7F.
- Invalid selected stage index.
- Truncated selected payload.
- Trailing bytes.
- Configured dimension limits.
- Zero stage count and zero dimensions.

## Deliberate scope boundary

This parser decodes only the three serialized authored layers. It does not claim to produce a fully initialized gameplay collision seed. Runtime arrays `bByteArrArr`, `cByteArrArr`, `eIntArrArr`, derived/transformed foreground values, player/session scalars, and object/auxiliary tables are created or mutated by later stage initialization and mode-dependent paths. They require a separate source-derived preprocessing pass with verified ordering and dependencies.

In particular:
- `bIntArrArr` is the serialized background layer and has runtime mutation semantics.
- `eIntArrArr` is separately allocated packed 32-bit tile metadata; it is not serialized as one of the three byte layers.
- `cByteArrArr` is a runtime countdown/tick-gate byte grid with observed seed and writer sites.
- `bByteArrArr` has separate per-cell state/timer semantics and must not be conflated with cByte state.
- The earlier generic `metadata`, `auxiliary`, and `foregroundCache` slots in the collision store remain provisional abstractions, not evidence that the parser can populate them.

## Next

DEV-003W7: add parser fixture manifest with source-derived per-world/per-stage dimensions and content hashes where the legally available fixture provenance permits; then implement a separate stage-preprocessing result type for the exact verified initialization pass. Do not fabricate collision seed values or activate GameSession collision writes until that pass and its state ownership are established.
