# DEV-003CO — Android Private-Storage Adapter

## CI prerequisite

DEV-003CN's latest CI workflows completed successfully:

- Atomic replacement/failure-injection tests: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44832460634
- Restore-test correction: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44832554729
- DEV-003CN documentation: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44832610249

Each passed the repository workflow, including core JVM tests and Android debug APK assembly.

## Android adapter

Added app-layer package `MeongDev.DIRUNA.GloftDIRU.storage`:

- `AndroidAtomicCheckpointStore` implements the core `CheckpointStore` interface using `android.util.AtomicFile`.
- `AndroidCheckpointStorage` constructs the persistence service from the application context's private `filesDir`.

The core module remains a plain Java library and has no Android imports.

## Storage contract

- Default location: app-private internal files directory.
- Default filename: `diru-na.checkpoint`.
- No storage permission, shared/external storage, SAF, or network dependency.
- Uses `AtomicFile.startWrite()`, `finishWrite()`, and `failWrite()` for Android's backup/replace protocol.
- Uses `AtomicFile.openRead()` so the platform adapter performs its documented backup recovery behavior when opening.
- Reads are bounded to the codec's 16 MiB maximum and reject empty files.
- Writes reject null, empty, and oversized payloads.
- A simple filename is required; path separators and dot traversal names are rejected.
- `delete()` delegates to `AtomicFile.delete()` and removes its managed checkpoint/backup state.

`AndroidCheckpointStorage` exposes `save(CrossTableCheckpoint)` and `loadInto(coordinator)`. The existing core persistence service and binary codec remain responsible for semantic validation and candidate-first restore.

## Manifest and backup policy

The existing manifest sets `android:allowBackup="false"`. This adapter stores only in app-private internal storage. Android Auto Backup/device-transfer behavior is therefore not part of this checkpoint contract.

## Validation

CI for the two adapter commits must pass both `:core:test` and `:app:assembleDebug`. The workflow verifies compilation and APK packaging; it does not execute Android `AtomicFile` behavior on a physical device or emulator.

Physical-device verification remains required for process termination during writes, backup recovery, and filesystem behavior on supported Android versions.

## Commits

- `AndroidAtomicCheckpointStore`: `364ecbb5b0bac08e1f6719432b617d43148c0793`
- `AndroidCheckpointStorage`: `dbdfee997cd2abcdd24aead3fa9bb32d6b65f934`

## Next milestone — DEV-003CP: Android Storage Recovery Tests & Lifecycle Wiring

1. Add Android-specific tests for successful write/read, failed write recovery, missing file, and corrupt checkpoint rejection.
2. Verify `AtomicFile` recovery after interrupted writes on an emulator/device.
3. Define when application lifecycle should request save; avoid doing blocking file I/O on the UI thread.
4. Add a controlled recovery result that distinguishes missing checkpoint, valid checkpoint, and invalid/corrupt checkpoint without silently resetting user data.
5. Keep gameplay state capture separate until the real game-state adapter exists.