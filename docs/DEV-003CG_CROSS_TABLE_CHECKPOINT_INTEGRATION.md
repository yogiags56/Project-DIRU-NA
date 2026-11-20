# DEV-003CG — Cross-Table Checkpoint Integration

## CI prerequisite

DEV-003CF correction and test workflows completed successfully:
- `947200f97cd5e17183e096dc6466cf9a7c45d8ac` — success;
- `1a3e6d3cede8b79b1e8ae648affb024a956745c6` — success;
- `1537d827ed67fcc0d785d969af02ed816f0a0821` — success.

## Design finding

`CrossTableLifecycleCoordinator.Snapshot` already captures its `ObjectRecordTable.Snapshot` and the object-to-auxiliary association sidecar. Its restore path validates association entries against each record's packed auxiliary link and auxiliary state, rebuilds a candidate object table, and only then restores the destination coordinator.

Packed tile metadata words are still external to both snapshots. Therefore a successful coordinator restore alone does not prove that the restored tile-link map agrees with the serialized nine-bit tile metadata words.

## New checkpoint envelope

Added `CrossTableCheckpoint`, which wraps a `CrossTableLifecycleCoordinator.Snapshot` plus an immutable map of `TileGroup -> packed nine-bit metadata word`.

`restoreInto(destination)` performs the following:
1. Rejects null destination or missing snapshot.
2. Requires the packed metadata key set to match the snapshot's tile-link key set exactly.
3. Restores the coordinator snapshot into an isolated candidate.
4. Resolves every packed metadata word through the guarded `ObjectRecordTable.lookup(tile, rawWord)` API.
5. Rejects any missing, extra, zero, invalid, or mismatched packed reference.
6. Mutates the destination coordinator only after all checks succeed.

The envelope preserves the legacy metadata bits unchanged; it does not attempt to embed generation counters into the three-bit legacy object ID.

## Regression tests

Added four `CrossTableCheckpointTest` cases:
- complete checkpoint restores multiple tile groups and their auxiliary associations;
- packed ID mismatch rejects restore and leaves an already-populated destination unchanged;
- missing packed metadata entry rejects restore atomically;
- null destination is rejected.

## Commits

- Checkpoint envelope: `493627b1e2412570211ff33a87f96220c990205e`
- Initial tests: `995b509c96e4da0fff58aaec13e27e5f54141de0`
- Test API correction (decode slot from ObjectHandle value): `3b231972a3f26c47c1a547975d381238d865219f`

## Validation boundary

The DEV-003CF workflows are confirmed successful, including the corrected test run and documentation run. DEV-003CG commits trigger CI; their outcomes must be checked before declaring this milestone validated. No runtime trace against the original shipping JAR or end-to-end game checkpoint integration is claimed.

## Next milestone — DEV-003CH: Checkpoint Schema, Integrity & Compatibility

1. Add schema/version validation for the outer checkpoint envelope.
2. Test duplicate/missing tile keys and malformed packed metadata groups.
3. Test atomic restore when coordinator association validation fails after the outer envelope passes.
4. Verify snapshot defensive-copy behavior for the packed metadata map.
5. Define compatibility policy for older checkpoints that do not contain packed metadata.
6. Run core JVM tests and Android debug APK assembly and record exact CI outcomes.