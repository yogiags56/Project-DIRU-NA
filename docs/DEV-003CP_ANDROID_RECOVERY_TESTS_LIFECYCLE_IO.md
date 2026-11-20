# DEV-003CP — Android Storage Recovery Tests & Lifecycle I/O

## DEV-003CO verification

All three DEV-003CO workflows completed successfully:

- Android store: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44833296593
- Android storage facade: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44833301410
- DEV-003CO documentation: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44833335562

Each workflow ran core JVM tests and assembled the Android debug APK.

## Explicit recovery result

Added `AndroidCheckpointLoadResult` with distinct statuses:

- `RESTORED`: checkpoint decoded, validated, and restored.
- `MISSING`: neither primary checkpoint nor AtomicFile backup exists.
- `INVALID_SNAPSHOT`: destination is invalid or decoded checkpoint fails semantic validation.
- `CORRUPT`: bytes are malformed and binary decode throws.
- `STORAGE_ERROR`: Android file read failed.

`AndroidCheckpointStorage.loadSafelyInto()` never turns missing or corrupt storage into an empty world and never clears the destination on those failures. It checks for a recoverable primary/backup file, reads through `AtomicFile.openRead()`, then invokes the candidate-first binary decode/restore path.

`AndroidAtomicCheckpointStore.hasRecoverableFile()` checks both the base file and the legacy `.bak` sibling used by the platform AtomicFile recovery protocol.

The original strict `loadInto()` API remains available for callers that prefer IOException propagation.

## Android recovery tests

Added Robolectric tests for:

- AtomicFile write/read round trip.
- Missing checkpoint classification.
- Valid checkpoint restore.
- Corrupt binary checkpoint classification without mutating destination state.
- Recovery of a valid `.bak` when a partial/invalid primary file is present.
- Invalid destination classification without modifying the file.
- Oversized write rejection.

Added JUnit and Robolectric test dependencies to the app module.

## Background I/O dispatcher

Added `CheckpointIoDispatcher`:

- Uses a single-thread executor to serialize checkpoint disk I/O off the UI thread.
- Loads into the supplied coordinator in the background.
- Saves an immutable `CrossTableCheckpoint` snapshot in the background.
- Posts completion callbacks to the main looper.
- Exposes `close()` to shut down its executor.

Callers must capture an immutable checkpoint snapshot from the game-state owner before submitting a save. The current launcher shell does not yet own a real game-state snapshot provider, so no automatic Activity onStop save hook is wired at this milestone; adding a hook before that provider exists would only save an empty/test model and could overwrite real progress.

## CI workflow update

Updated `.github/workflows/android.yml` to run `:app:testDebugUnitTest` between core JVM tests and debug APK assembly. This ensures the Robolectric storage recovery tests are actually executed by CI rather than merely compiled into the app module.

CI runs for the new app tests and dispatcher are in progress at documentation time:

- Gradle app test configuration: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44835298884
- Android storage tests: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44835305137
- Checkpoint I/O dispatcher: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44835338304
- CI workflow update: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44835402711

Do not mark DEV-003CP complete until the latest workflow passes `:core:test`, `:app:testDebugUnitTest`, and `:app:assembleDebug`.

## Commits

- Recoverable AtomicFile presence: `8cc756e35818c93f147188edc0d97540231a47cd`
- Explicit load result: `8322b255569ed644c4f2491991fc9172e37a1943`
- Safe load classification: `5fe8e998a74869f7aa58b2c38ae5f77d7b0a8088`
- Robolectric dependency: `7e9dbd9b61c322be27ddd896e415855ee9f290ea`
- Android recovery tests: `07aa015d262dd18644c090e93b4726061db9a8fb`
- Background I/O dispatcher: `c4015e4f355c7bc16c86e224497315b0ead5eb7c`
- CI runs app unit tests: `4aa8e7520a135d8d328a1cc78ecd561d0024714b`

## Scope limits

Robolectric verifies Android API behavior in a simulated environment. Actual process-kill timing, storage-controller behavior, and AtomicFile recovery on physical devices remain unverified. The dispatcher is implemented but intentionally not wired to MainActivity lifecycle until a real game-state snapshot provider exists.

## Next milestone — DEV-003CQ: Checkpoint Lifecycle Contract & Recovery UX

1. Define a game-state snapshot provider contract and ensure capture occurs on the owning game thread.
2. Define safe save triggers (pause/background/explicit checkpoint) and prevent stale asynchronous writes from overtaking newer snapshots.
3. Define user-facing recovery decisions for missing, corrupt, and invalid checkpoints; preserve damaged files for diagnosis/export.
4. Add tests for serialized save ordering and dispatcher shutdown behavior.
5. Only then connect lifecycle callbacks to the actual game state.