# DEV-003CQ — Checkpoint Lifecycle Contract & Recovery UX

## DEV-003CP CI result

DEV-003CP's complete workflow passed on the final documentation commit. The workflow executed core JVM tests, Android unit tests, and debug APK assembly:

https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44835428861

An intermediate DEV-003CQ run on commit `83d685d6` failed before compilation because Maven Central returned HTTP 429 while Gradle resolved unrelated transitive dependencies. The failure is infrastructure/dependency-rate-limit evidence, not a test assertion or Java compiler diagnostic:

https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44836059897

Subsequent DEV-003CQ commits have new workflows queued/running; do not mark the milestone CI-verified until the latest run passes all three tasks.

## Game-state thread ownership

`AndroidCheckpointStorage.loadCandidate()` reads and decodes the checkpoint into a detached `CrossTableCheckpoint`. It does not mutate a live coordinator.

`AndroidCheckpointLoadResult.applyTo(destination)` is the explicit restore operation. The caller must invoke it on the game-state owner thread (or schedule it onto that thread). This avoids mutating game state from the checkpoint I/O executor.

`CheckpointIoDispatcher.load()` now uses `loadCandidate()` and delivers the detached candidate callback on Android's main looper. If the game loop owns a dedicated thread, the callback must post the candidate to that thread before calling `applyTo()`; main-looper delivery is not a claim that the UI thread owns game state.

`loadSafelyInto()` remains as a synchronous compatibility helper. Callers must use it only on the game-state owner thread.

## Save snapshot contract

`CheckpointIoDispatcher.save()` accepts an immutable `CrossTableCheckpoint`. The caller must capture it on the game-state owner thread before enqueueing the save. The dispatcher never reads mutable game state.

Save requests receive monotonically increasing sequence numbers and are serialized by one executor:

- A queued request is marked `SUPERSEDED` if a newer request arrived before it began writing.
- A write already in progress is allowed to finish; later requests remain ordered behind it, so the newest accepted snapshot is the final write.
- Save callbacks distinguish `SAVED`, `SUPERSEDED`, and `FAILED` and include the request sequence.
- Closing the dispatcher rejects later requests and shuts down its executor.

This prevents stale queued snapshots from overtaking newer snapshots. It does not cancel an atomic write already in progress.

## Recovery UX policy

Added a UI-neutral `CheckpointRecoveryPolicy`. It maps load results to safe application actions without deleting or overwriting data:

| Load status | Suggested action |
| --- | --- |
| `RESTORED` | Continue from checkpoint |
| `MISSING` | Offer new game or import |
| `CORRUPT` | Offer retry or preserve file for diagnosis |
| `INVALID_SNAPSHOT` | Offer retry or preserve file for diagnosis |
| `STORAGE_ERROR` | Offer storage retry |

No recovery policy action automatically starts a new game, deletes a corrupt file, or replaces the last checkpoint.

## Tests added

- Candidate decode is separate from explicit game-state apply.
- Recovery policy for missing, corrupt, invalid, and storage-error statuses.
- Existing Robolectric AtomicFile and recovery tests remain in the suite.

## Commits

- Detached load candidate result: `83d685d638f8c29990dc563517496485e720aa6a`
- Storage candidate decoding: `ea7325fde572137449104715900f51dfbe93af91`
- Ordered save result model: `21a37b9e4c5b010d182c73b455ea5b28ad30ec0d`
- Dispatcher owner-thread/ordering contract: `e10e799cb734a6128e28774eb3609764cc1c027d`
- Candidate decode/apply test: `30fccc632d3774ddc364092dbce2c6eb77f2459a`
- Non-destructive recovery policy: `c2adcf5a01798a2216e665d5a64a491f0ce611db`
- Recovery policy tests: `eb0c7834c132892ec1e5210ec54a6ceafca400ec`

## Not yet wired

The application shell does not yet have a real gameplay snapshot provider or a defined render/game-state thread. No automatic Activity lifecycle save hook is installed. The new contracts deliberately require those pieces before lifecycle callbacks are connected.

Physical Android process-kill tests and real-device AtomicFile durability tests remain outstanding.

## Next milestone — DEV-003CR: Snapshot Provider & Lifecycle Integration Boundary

1. Define the game-state snapshot provider owned by the gameplay engine.
2. Specify the game-thread executor/dispatcher used to capture and apply snapshots.
3. Add deterministic tests for queued-save supersession and in-flight ordering.
4. Define explicit pause/background save requests without relying on Activity destruction as a guaranteed save event.
5. Integrate only after a real game-state implementation is present; never persist placeholder/empty state over a user's checkpoint.