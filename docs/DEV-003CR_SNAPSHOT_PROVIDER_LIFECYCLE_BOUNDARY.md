# DEV-003CR — Snapshot Provider & Lifecycle Integration Boundary

## Baseline

This milestone continues directly from DEV-003CQ. It retains the detached
checkpoint candidate, serialized background I/O, ordered save requests, and
non-destructive recovery policy already present on the baseline branch.

## Snapshot provider boundary

Added the platform-independent
`CheckpointSnapshotProvider` interface in the core object-record package.

The contract makes the ownership boundary explicit:

- Capture and restore are called only on the gameplay-state owner thread.
- Capture returns a complete, detached `CrossTableCheckpoint`.
- A provider must fail capture when gameplay state is not ready; it must not
  manufacture an empty or placeholder snapshot.
- Restore must be atomic from the gameplay engine's perspective. A failed
  restore must leave live state unchanged.
- The provider performs no disk I/O and has no Android dependency.

The existing Android `CheckpointIoDispatcher` remains responsible only for
serialized background disk I/O. The future integration sequence is:

1. Schedule capture on the game-state owner thread.
2. Obtain a complete immutable checkpoint from the provider.
3. Enqueue that checkpoint with `CheckpointIoDispatcher.save()`.
4. On load, decode a detached candidate on the I/O executor.
5. Schedule `CheckpointSnapshotProvider.restoreCheckpoint()` on the
   game-state owner thread.

The main-looper callback currently used by the Android dispatcher is not
assumed to be the gameplay thread. The future integration must explicitly
bridge to the actual game-state executor.

## Lifecycle request rules

- An explicit pause/background transition may request a save, but capture must
  first run on the game-state owner thread.
- Activity callbacks are best-effort request points, not guaranteed durable
  save events. Do not block the UI thread waiting for disk I/O.
- Process death can occur before an asynchronous request completes. The
  application must not claim that an Activity destruction callback guarantees
  a final save.
- Repeated requests may use the existing ordered save queue. A newer queued
  snapshot may supersede an older queued snapshot; an in-flight atomic write
  is allowed to finish.
- A capture failure must not enqueue a save and must not alter the previous
  checkpoint.
- Missing, corrupt, invalid, and storage-error recovery remains explicit and
  non-destructive.

## Deliberately not integrated yet

The application shell still has no real gameplay engine, concrete snapshot
provider, or authoritative game-state thread. Therefore this milestone adds
the integration contract only; it does not install Activity autosave hooks or
pretend that the current lifecycle coordinator represents a complete game.

Still required before live integration:

- A concrete provider backed by real gameplay state.
- A documented game-thread executor/dispatcher and owner-thread assertions.
- Deterministic tests for save supersession and in-flight ordering.
- Tests proving capture failure never enqueues or overwrites a checkpoint.
- Android process-kill and physical-device AtomicFile durability tests.

## Validation status

This change is a source/API and documentation change. It is not considered
runtime-verified until the repository CI runs core JVM tests, Android unit
tests, and debug APK assembly on the resulting commit.
