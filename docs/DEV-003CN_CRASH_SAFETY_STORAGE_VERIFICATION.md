# DEV-003CN — Crash-Safety & Storage Adapter Verification

## DEV-003CM CI review

The initial persistence test suite exposed an invalid failure simulation: attempting to replace a directory with a file is not a portable way to guarantee atomic-move failure. The test depended on filesystem-specific replacement behavior. It has been replaced with an injectable package-private atomic-move strategy so failure paths can be tested deterministically.

The earlier CI run for the persistence test commit failed at `failedAtomicReplacementLeavesNoTemporaryFiles`; that run predates the deterministic failure-injection correction.

A second earlier test failure in DEV-003CL was caused by a fragile snapshot-list equality assertion. The latest test compares each immutable record field explicitly. The newest CI run for this correction must pass before claiming that regression closed.

## Implementation changes

`AtomicFileCheckpointStore` now delegates replacement to a package-private `AtomicMover` strategy. The public constructor still uses the real NIO atomic move (`ATOMIC_MOVE` + `REPLACE_EXISTING`). A package-private constructor accepts a mover for deterministic tests; this does not expose a production non-atomic fallback.

Failure behavior remains fail-closed:

- `AtomicMoveNotSupportedException` is converted to `IOException`.
- No non-atomic move fallback is attempted.
- A failed replacement cleans up the temporary file.
- The previous destination remains untouched when the mover fails before replacement.

## New crash-safety tests

- Existing checkpoint replaced by a complete new checkpoint.
- Simulated failure immediately before atomic replacement preserves the old file.
- Unsupported atomic move fails closed and preserves the old file.
- Temporary files are removed after simulated failure.

These are deterministic failure-injection tests. They do not emulate sudden power loss, kernel crash, or storage-controller write-cache loss.

## Commits

- Atomic mover injection seam: `0f0bd9bdb929be35c84b3cc85d864330d1c706b9`
- Failure-injection, fail-closed and replacement tests: `aff3ffa808372fea8dc98b2d08db38adde7e3d36`
- Restore-test field-by-field assertion: `93a578c29ab2f49afffffb6228bb5690889f4d7a`

## CI evidence

- `0f0bd9bd` workflow initially failed on the old directory-replacement assumption: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44832441268
- `aff3ffa8` core JVM tests passed; Android APK assembly was still running at the time of inspection: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44832460634
- `93a578c2` restore-test correction workflow queued at time of documentation: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44832554729

Do not mark DEV-003CN complete until the latest JVM test and Android debug APK assembly workflows both pass.

## Limits and next steps

The adapter remains a JVM/NIO implementation. Android app-private storage integration is not yet implemented. The file adapter forces file contents but does not provide a portable guarantee that parent-directory metadata is durable after sudden power loss.

Next milestone — DEV-003CO: Android Private-Storage Adapter Design & Integration

1. Define the Android storage adapter boundary without importing Android APIs into the core module.
2. Use app-private storage by default; keep SAF/export as an optional later layer.
3. Define startup recovery behavior for valid primary, missing primary, and corrupt primary checkpoint files.
4. Keep a last-known-good backup or generation strategy only if justified by the Android filesystem semantics and tests.
5. Verify atomic replacement and durability semantics on actual supported Android API levels/devices before making stronger crash-safety claims.