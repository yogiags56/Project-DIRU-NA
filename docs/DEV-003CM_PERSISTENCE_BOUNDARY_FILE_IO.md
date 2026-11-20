# DEV-003CM — Persistence Boundary & File I/O Contract

## Scope

Added a platform-independent whole-checkpoint persistence boundary and a JVM/NIO file adapter. Android-specific storage APIs are intentionally not introduced in this milestone.

## Components

- `CheckpointStore`: minimal byte-array read/write interface.
- `AtomicFileCheckpointStore`: writes a sibling temporary file, drains the buffer, calls `FileChannel.force(true)`, then replaces the destination with `ATOMIC_MOVE` plus `REPLACE_EXISTING`.
- `CheckpointPersistenceService`: connects `CrossTableCheckpointBinaryCodec` to a `CheckpointStore`; validates on save and decodes/validates before restoring on load.

## File replacement contract

1. Reject null, empty, or over-16-MiB payloads before creating a temporary file.
2. Create parent directories and a temporary sibling file.
3. Write all bytes and force file contents before replacement.
4. Replace the destination only through an atomic move.
5. If atomic move is unsupported, fail rather than silently downgrade to a non-atomic replacement.
6. Delete the temporary file on any unsuccessful write/replacement path.

The adapter forces the temporary file contents but does not claim portable directory-entry fsync or power-loss durability across every filesystem. Android production storage integration must separately choose and verify platform-appropriate durability semantics.

## Regression tests

`CheckpointPersistenceServiceTest` covers:

- whole-file write/read round-trip;
- service save/load round-trip into a coordinator;
- oversized write rejection preserving the last known-good file;
- truncated-file load rejection without mutating a populated destination;
- missing-file behavior without destination mutation;
- failed replacement cleanup of temporary files.

## CI failure found and corrected

DEV-003CL had one failing assertion in `decodeIntoRestoresOnlyAfterSuccessfulDecodeAndValidation`: the test assumed an object handle from the pre-restore destination could never equal a handle in the restored snapshot. Handles are slot-plus-generation values, so identical slot/generation values can legitimately identify the restored record. The test now compares the restored records, tile links, and associations against the source snapshot rather than treating a matching handle value as stale. This was a test expectation error, not evidence of a failed atomic restore implementation.

Correction commit: `dabad67082e6992de435d1dfaa5fdae739c06272`.

## Commits

- `CheckpointStore`: `58f5928a2de292f722ccab41cb6f3629deb3cfbf`
- Atomic file adapter: `21ed7d8e12bdf2aa319967d7f6e4519949cc61c6`
- Persistence service: `d40a35e25c8bc5d474ae178702a35a16c017a8b3`
- File persistence tests: `1858444311af10a0281b80fdcc2d4bef520fae49`

## CI status

At documentation time, the latest workflows for the restore-test correction and persistence commits were still running:

- Restore test correction: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44831894240
- Store interface: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44831932558
- Atomic file adapter: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44831936807
- Persistence service: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44831945055
- Persistence tests: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44831966515

Do not mark this milestone complete until core JVM tests and Android debug APK assembly pass on the latest commits.

## Scope limits

This is a JVM/NIO reference adapter and does not yet integrate Android app-private storage, SAF, backup/restore, encryption, or original Diamond Rush save files. It does not claim portable power-loss durability of directory metadata on all filesystems.

## Next milestone — DEV-003CN: Crash-Safety & Storage Adapter Verification

1. Verify successful replacement of an existing valid checkpoint.
2. Verify the previous file remains readable after a simulated failure before replacement.
3. Test filesystem behavior when atomic move is unsupported, and preserve the fail-closed contract.
4. Decide whether a generation/backup slot is needed for filesystems with weaker guarantees.
5. Design an Android adapter separately, with app-private storage as the default and no silent non-atomic fallback.