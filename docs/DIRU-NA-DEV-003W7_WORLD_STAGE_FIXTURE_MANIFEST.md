# DIRU-NA DEV-003W7 — World stage fixture manifest

## Evidence baseline

Dimensions are transcribed from `DIRU-NA-AUDIT-029_WORLD_STAGE_STREAM_BINARY_INVENTORY.md`, whose static parser audit inspected the pinned candidate resources at `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

The three resource identities are intentionally explicit:
- Angkor / `w0.bin`: 14 records.
- Scotland resource / Bavaria UI / `w1.bin`: 13 records.
- Tibet resource / Siberia UI / `w2.bin`: 14 records.

The names retain both resource and UI labels because the audit found a mismatch in naming. No map-node or title mapping is inferred from stage index.

## Implementation

Added `WorldStageFixtureManifest` in `:core`:
- Enumerates all 41 audited stage records.
- Exposes world, zero-based stage index, width, height and checked cells-per-layer.
- Returns detached entry objects and rejects null worlds or invalid indices.
- Contains metadata only; no original binary payloads are copied into the Native project.

Added `WorldStageFixtureManifestTest`:
- Verifies stage counts 14/13/14.
- Checks selected boundary dimensions (Angkor index 5: 30×75; Tibet index 9: 104×17).
- Verifies the total 50,453 cells per layer.
- Checks invalid world/index handling.

## Integrity/hash status

Per-stage SHA-256 values are deliberately not fabricated. The current audit records full-resource byte sizes and parsing outcomes, but the per-stage payload hashes have not been independently captured in this implementation session. The manifest therefore does not expose a hash field until it can be computed from verified, legally usable source bytes with an explicit layer/record byte-range convention.

## Unresolved catalog relationships

- Angkor has 14 stage records but 13 map nodes/title entries.
- Scotland/Bavaria has 13 records, map nodes and title entries.
- Tibet/Siberia has 14 records/map nodes but 15 title mapping entries.
- These remain separate catalogs. No synthetic stage, title or map node is added.

## Validation boundary

This milestone adds static dimensions and JVM tests. It does not prove stage resource runtime loading, title-to-stage mapping, gameplay initialization, or collision state semantics. The source candidate is static evidence and has not been runtime-validated against a retail JAR/device here.

## Next

DEV-003W8: model the verified stage preprocessing boundary as a distinct immutable result type. First trace and encode only fields whose allocation, write order, and ownership are directly evidenced. Preserve unknowns as unresolved; do not manufacture production collision seeds or connect the parser to GameSession yet.
