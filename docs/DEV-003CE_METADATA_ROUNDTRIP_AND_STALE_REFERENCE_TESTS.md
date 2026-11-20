# DEV-003CE — Metadata Round-Trip & Stale-Reference Integration Tests

## Scope

This milestone exercises packed tile metadata and the object-table model together. It is a test-only change: no production implementation was modified.

## CI prerequisite: DEV-003CD

The GitHub Actions workflow `Android Build and Core Tests` completed successfully for all three DEV-003CD commits:
- guarded lookup implementation `8480b3b5fa2f2902d1a47e9920c645ef14341e29` — success;
- guarded lookup regression tests `94a03953da60c12f9e7bd53105bdc4a1af5ae0d8` — success;
- DEV-003CD documentation `6d3bdcbc` — success.

## Added integration-style tests

Three tests were added to `ObjectRecordTableTest`:

1. `packedMetadataRoundTripsAndClearsEachGroupIndependently`: allocates and links a record for each of the three metadata groups, writes the one-based slot ID into the packed word, resolves it through guarded lookup, clears each nine-bit group in turn, confirms lookup becomes `NO_LINK`, unlinks the model reference, and verifies neighboring groups remain populated.

2. `packedReferenceAfterUnlinkIsRejectedBeforeSlotReuse`: retains old packed bits, unlinks and releases the referenced record, then verifies the old bits are rejected as `TILE_METADATA_MISMATCH` while the map has no matching link.

3. `legacyPackedIdCannotDistinguishSameSlotAfterReuse`: demonstrates a fundamental legacy limitation. After a slot is released and reused, and the replacement is linked at the same tile, the old three-bit packed slot ID is indistinguishable from the new link. Guarded lookup therefore resolves the replacement record. The legacy metadata contains no generation counter.

## Important lifecycle conclusion

Generation-bearing `ObjectHandle` values protect native callers from stale references. They do not make the original three-bit packed tile ID generation-aware. A stale packed ID can be rejected when no matching link exists, but it cannot be distinguished from a new object that reuses the same slot and is relinked at the same tile.

Do not claim that a three-bit legacy field alone prevents ABA/slot-reuse aliasing. Native integration must either keep the authoritative link map synchronized with packed metadata and preserve the original lifecycle ordering, or introduce a separate generation-aware ownership mechanism outside the legacy field. Do not alter serialized legacy bits to store a generation.

## Test and build status

The DEV-003CE test commit triggers GitHub Actions. Results are not yet available at the time this document is written. No successful test or APK build is claimed for this new commit until its workflow finishes.

## Commit

- Tests: `fbe1677275c8623376376fc3b6c57600b471baf6`

## Next milestone — DEV-003CF: Snapshot/Restore Metadata Consistency

1. Verify snapshots preserve the tile-link map and packed metadata group semantics together.
2. Verify restore rebuilds reverse references and rejects invalid slot IDs, missing records, or duplicate references.
3. Add tests for a cleared group alongside two still-live groups across snapshot/restore.
4. Test slot release/reuse across snapshot/restore while old handles remain stale.
5. Clarify that generation safety is native-handle safety, not legacy packed-ID safety.
6. Run and record the complete core JVM suite and Android debug APK assembly.