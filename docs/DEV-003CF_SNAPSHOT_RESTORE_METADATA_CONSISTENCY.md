# DEV-003CF — Snapshot/Restore Metadata Consistency

## CI finding and correction from DEV-003CE

The `Android Build and Core Tests` workflow failed for both DEV-003CE commits `fbe1677275c8623376376fc3b6c57600b471baf6` and `1d3488b53dc52180737cba2ca349e68c9cc680fe`. The job reached `Run core JVM tests` and failed; Android debug APK assembly was skipped.

Review of the added `packedMetadataRoundTripsAndClearsEachGroupIndependently` test identified an invalid assertion: after clearing one group, it asserted that every other group's object ID was nonzero. The test initialized the packed word with a pattern that can contain zero object-ID fields, so nonzero was not a valid invariant. The assertion was corrected to compare each neighboring nine-bit group before and after clearing, which directly verifies preservation without assuming its content.

Correction commit: `947200f97cd5e17183e096dc6466cf9a7c45d8ac`.

## Snapshot/restore implementation audit

`ObjectRecordTable.snapshot()` serializes occupied records, generation counters, tile-link map, and auxiliary entries. Per-record reverse-reference sets are deliberately not serialized.

`ObjectRecordTable.restore()` builds a temporary candidate via `fromSnapshot()`, validates schema, generation array size and positivity, record slot uniqueness and generation consistency, record payload/type consistency, Type-7 link restrictions, tile-link ID range (1..7), and existence of each linked record. It rebuilds each record's reverse-reference set from the tile-link map and only copies candidate state into the live table after validation succeeds.

This design makes restore atomic with respect to validation failure and ensures reverse references are derived from one authoritative link map.

## New snapshot/restore regression tests

Three tests were added to `ObjectRecordTableTest`:

1. `snapshotRestorePreservesPackedMetadataLinkAndClearedGroups`: creates records linked through separate metadata groups, clears/unlinks one group, snapshots/restores the object table, then verifies the cleared group remains unlinked, the other groups resolve, and their reference counts are rebuilt.

2. `snapshotRestoreKeepsReleasedSlotGenerationAndOldHandleStale`: releases a record, reuses its slot, snapshots/restores, and verifies the old generation-bearing handle remains stale while the replacement handle resolves.

3. `restoreRejectsMissingRecordForTileLinkAtomically`: corrupts a snapshot so a tile link points to a missing object slot; restore must return `INVALID_SNAPSHOT` and leave the pre-existing live table unchanged.

## Important scope distinction

The `ObjectRecordTable` snapshot includes its own tile-link map and auxiliary table, but it does not serialize an external packed tile metadata word or the cross-table association sidecar held by `CrossTableLifecycleCoordinator`. The coordinator has separate snapshot/restore validation and tests. This milestone validates the object-record table's snapshot contract; it does not claim a unified world-state snapshot across all game subsystems.

Packed metadata words remain an external serialized input. Callers must persist/restore them alongside the corresponding table snapshot and use guarded lookup to detect disagreement.

## CI status

The correction commit and DEV-003CF implementation/test commits each trigger the repository workflow. Their workflow outcomes must be checked before declaring the tests passed. The earlier DEV-003CE CI failure is recorded above; no APK build success is claimed for those failed runs.

## Next milestone — DEV-003CG: Cross-Table Checkpoint Integration

1. Exercise one checkpoint containing ObjectRecordTable and CrossTableLifecycleCoordinator snapshots together.
2. Validate packed metadata words against restored tile-link map entries for all three groups.
3. Test corrupted packed metadata, missing record, invalid auxiliary association, and stale generation as atomic restore failures.
4. Ensure failed restore leaves both object table and coordinator state unchanged.
5. Run the full core JVM suite and Android debug APK assembly; record exact workflow outcomes.

No gameplay wiring or original shipping-JAR parity is claimed.