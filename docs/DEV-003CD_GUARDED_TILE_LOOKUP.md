# DEV-003CD — Guarded Tile Lookup & Call-Site Contract

## Scope

This milestone adds a guarded lookup path that accepts the packed nine-bit tile metadata word rather than relying only on the in-memory tile-link map.

## Implementation

`ObjectRecordTable.lookup(TileGroup, int rawMetadataWord)` now:
1. Reads the selected group's three-bit object ID through `PackedTileMetadataCodec.readObjectReference()`.
2. Returns `NO_LINK` for raw ID zero before any object-array indexing.
3. Rejects invalid metadata groups.
4. Requires the decoded nonzero ID to match the authoritative tile-link map entry.
5. Resolves only IDs 1..7 and then returns the live object handle and type.

A new `TILE_METADATA_MISMATCH` status distinguishes a packed metadata/map disagreement from a missing link. The previous `lookup(TileGroup)` remains available for model-only operations and tests; callers that possess serialized or live packed metadata should use the guarded overload.

## Regression tests

Four tests were added:
- matching nonzero packed ID resolves the expected live record;
- zero packed ID returns `NO_LINK` without attempting an array access;
- a packed ID that disagrees with the tile-link map is rejected;
- a stale packed ID after unlinking is rejected as a metadata/map mismatch.

## Source-evidence boundary

The pinned decompiled source has a one-based object accessor that subtracts one before indexing the 15-entry object table. The native guarded API ensures zero is never used as an index. The source's complete control-flow proof for every legacy zero-ID caller is still incomplete, and no original shipping JAR/class artifact is present in the repository.

This API therefore defines a safe native lookup contract; it does not claim every legacy zero field has been proven to mean empty in every original game context.

## Commits

- Guarded lookup implementation: `8480b3b5fa2f2902d1a47e9920c645ef14341e29`
- Four regression tests: `94a03953da60c12f9e7bd53105bdc4a1af5ae0d8`

## Validation

CI workflows are triggered by the implementation and test commits. Their completion status must be checked independently; this document does not claim JVM tests or Android APK assembly have passed.

No gameplay integration, original-JAR parity, or runtime trace is claimed.

## Next milestone — DEV-003CE: Metadata Round-Trip & Stale-Reference Integration Tests

1. Exercise writeObjectId -> guarded lookup -> clearGroup -> guarded lookup as a single lifecycle.
2. Verify each of the three metadata groups independently while preserving neighboring groups.
3. Verify stale record handles and generation reuse cannot be reached through old packed metadata.
4. Add restore/snapshot tests for packed metadata and link-map consistency.
5. Review every native lookup call site and migrate any semantic lookup that still uses the map-only overload.
6. Run core JVM tests and Android debug APK assembly, recording exact results.