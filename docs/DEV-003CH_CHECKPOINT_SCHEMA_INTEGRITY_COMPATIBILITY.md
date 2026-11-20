# DEV-003CH — Checkpoint Schema, Integrity & Compatibility

## Scope

This milestone introduces an outer checkpoint schema version and strengthens tests for schema rejection and packed-metadata immutability. It remains in validation because the cross-group guarded lookup integration test is still failing.

## Implemented

`CrossTableCheckpoint` now exposes `CURRENT_SCHEMA_VERSION = 1`, stores the envelope schema version, retains the existing constructor as a schema-1 convenience overload, and rejects unsupported schema versions before attempting restore.

The packed metadata map is copied into a new `HashMap` and exposed through an unmodifiable view. A null map is normalized to an empty map; this can only restore successfully when the object snapshot has no tile links because exact key-set equality is required.

## Added tests

- unsupported schema version is rejected without mutating a populated destination;
- source-map mutation after checkpoint construction does not alter the checkpoint;
- exposed packed metadata map cannot be mutated;
- null metadata map is treated as empty and rejected when the snapshot contains tile links.

## CI investigation

DEV-003CG CI failed in `CrossTableCheckpointTest.checkpointRestoresAllPackedMetadataLinksAndCoordinatorState`. The first metadata group guarded lookup passes in the diagnostic run; the second metadata group is the remaining failure. The test now emits the second group's guarded lookup status, tile key, raw word, decoded ID, and map-only handle to identify the exact mismatch. Do not mark DEV-003CH complete until this failure is resolved and the full core test suite plus Android debug APK build pass.

Earlier DEV-003CG runs failed in Core JVM tests, so Android debug APK assembly was skipped. The standalone checkpoint-envelope implementation commit did pass CI, but that does not validate the failing integration test.

## Commits

- Outer checkpoint schema version: `73aacad367eac8cf4659278c3abc8d615010044d`
- Schema and defensive-copy tests: `06f857d1d50b6e623ea076f4d9be61b2b51e6575`
- Derive test metadata from serialized tile links: `db995214550e8b88e674361e76b6dfcafbb4ea0e`
- Guarded lookup diagnostics: `a144e6fe2a7d6ed93a4d8e7df599947c576703c1`
- Second-group diagnostics: `b2d3c4b839d1537f2f4eee2fa0ac7f65bb5662ec`

## Next action — DEV-003CH-FIX

1. Read the latest diagnostic CI output for the second metadata group.
2. Fix the underlying codec, tile-group, or test fixture mismatch based on observed values; do not weaken the assertion.
3. Add explicit round-trip assertions for groups 0, 1, and 2 using distinct nonzero IDs.
4. Re-run all core JVM tests and Android debug APK assembly.
5. Only after a green workflow, proceed to legacy checkpoint compatibility policy.