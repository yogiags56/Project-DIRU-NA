# DEV-003CH — Checkpoint Schema, Integrity & Compatibility

## Scope

This milestone introduces an outer checkpoint schema version and strengthens tests for schema rejection and packed-metadata immutability. The cross-group guarded lookup test initially failed during DEV-003CG validation. The test fixture was updated to derive packed object IDs from the serialized tile-link map, and diagnostic assertions were added for both metadata groups. The latest complete workflow then passed.

## Implemented

`CrossTableCheckpoint` now exposes `CURRENT_SCHEMA_VERSION = 1`, stores the envelope schema version, retains the existing constructor as a schema-1 convenience overload, and rejects unsupported schema versions before attempting restore.

The packed metadata map is copied into a new `HashMap` and exposed through an unmodifiable view. A null map is normalized to an empty map; this can only restore successfully when the object snapshot has no tile links because exact key-set equality is required.

## Added tests

- unsupported schema version is rejected without mutating a populated destination;
- source-map mutation after checkpoint construction does not alter the checkpoint;
- exposed packed metadata map cannot be mutated;
- null metadata map is treated as empty and rejected when the snapshot contains tile links.

## CI investigation and final validation

Earlier DEV-003CG workflow runs failed in `CrossTableCheckpointTest.checkpointRestoresAllPackedMetadataLinksAndCoordinatorState`. The test fixture now derives packed IDs directly from the serialized tile-link map, and diagnostic assertions report both metadata-group lookup outcomes. The final diagnostic commit completed successfully:

- Core JVM tests: **success** (305 tests completed in this workflow; no failures reported).
- Android debug APK assembly: **success**.
- APK artifact upload: **success**.

Workflow: `44795118276` — https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44795118276

This confirms the current checkpoint schema and cross-group tests pass CI. It does not prove compatibility with historical serialized checkpoint files or original-game runtime parity.

## Commits

- Outer checkpoint schema version: `73aacad367eac8cf4659278c3abc8d615010044d`
- Schema and defensive-copy tests: `06f857d1d50b6e623ea076f4d9be61b2b51e6575`
- Derive test metadata from serialized tile links: `db995214550e8b88e674361e76b6dfcafbb4ea0e`
- Guarded lookup diagnostics: `a144e6fe2a7d6ed93a4d8e7df599947c576703c1`
- Second-group diagnostics: `b2d3c4b839d1537f2f4eee2fa0ac7f65bb5662ec`

## Next milestone — DEV-003CI: Legacy Checkpoint Compatibility Policy

1. Define the canonical serialized envelope fields and schema migration rules.
2. Decide whether pre-envelope snapshots are rejected, upgraded through an explicit adapter, or accepted only when packed metadata is supplied separately.
3. Test unknown future schema versions, missing envelope fields, and empty-world snapshots.
4. Preserve strict atomic restore and avoid guessing missing packed metadata.
5. Document that passing native model tests is not proof of compatibility with the original game's save format.