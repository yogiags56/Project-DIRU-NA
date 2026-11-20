# DEV-003CI — Legacy Checkpoint Compatibility Policy

## Scope and compatibility boundary

This milestone defines an explicit adapter policy for the current native model's pre-envelope `CrossTableLifecycleCoordinator.Snapshot` API. It does not define or parse a historical on-disk save format from the original Diamond Rush JAR. No byte-level legacy save format has been proven by this work.

The outer checkpoint envelope is schema version 1. The inner `ObjectRecordTable.Snapshot` retains its own independent schema version. Both versions must be supported; a recognized outer version does not override an unknown or invalid inner snapshot.

## Policy

1. New checkpoints use `CrossTableCheckpoint.CURRENT_SCHEMA_VERSION` (currently 1).
2. Unknown outer schema versions are rejected before destination mutation.
3. Unknown inner object-table schema versions are rejected by the object-table restore validator.
4. A legacy coordinator snapshot with no tile links can be adapted with an empty packed-metadata map.
5. A legacy snapshot containing tile links requires the caller to supply the exact packed metadata words keyed by `TileGroup`.
6. The adapter does not synthesize object IDs, fill missing groups, or guess opaque metadata bits.
7. The exact key set and each packed object ID must agree with the snapshot's authoritative tile-link map.
8. Restore remains candidate-first and atomic with respect to the destination coordinator.
9. Historical serialized save files remain unsupported until their byte layout, version markers, and migration rules are established from primary evidence.

## Implementation

Added `CrossTableCheckpoint.fromLegacySnapshot(snapshot, packedMetadataWords)`.

- Null snapshot or missing object snapshot is rejected.
- Null tile-link map is rejected.
- Null packed metadata is accepted only when the legacy snapshot has no tile links.
- A nonempty tile-link map requires explicit metadata.
- Supplied metadata is copied into an immutable map by the checkpoint constructor.
- Final consistency validation remains centralized in `restoreInto`.

This is a model API adapter, not a legacy binary parser.

## Regression coverage added

- Empty legacy snapshot adapts and restores.
- Legacy snapshot with tile links and no packed metadata is rejected by the adapter.
- Legacy snapshot with explicit matching metadata adapts and restores.
- Empty-world checkpoint with empty metadata restores.
- Duplicate object-record slot entries in a malformed snapshot are rejected atomically.
- Unknown inner object snapshot schema is rejected without mutating the destination.

Existing schema, defensive-copy, mismatch, missing-metadata, and unsupported-outer-schema tests remain in place.

## CI

The DEV-003CI implementation and test commits trigger GitHub Actions. Record the final outcomes below only after the relevant workflow runs complete:

- Adapter implementation: `1c70469022e1bdbbd90abae436d26a5fb0625650`
- Adapter/regression tests: `f293585b376c6abcdcf4f6d5f5ffa33f47ccc88e`
- Duplicate-slot fixture correction: `c237ae5b0aa47cd33be36f859c5ae32595fe141f`

CI status at documentation authoring: pending.

## Next milestone — DEV-003CJ: Checkpoint Serialization Contract

1. Inspect existing project persistence interfaces and determine whether a canonical serializer already exists.
2. Define deterministic field ordering and stable encoding only where supported by project architecture.
3. Add round-trip tests for the envelope, nested snapshots, packed metadata words, and empty-world state.
4. Define explicit behavior for truncated, malformed, and unknown-version serialized inputs.
5. Keep binary compatibility with the original game's save format out of scope until original-format evidence is obtained.