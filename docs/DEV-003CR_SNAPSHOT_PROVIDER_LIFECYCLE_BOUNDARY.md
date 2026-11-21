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

Contract tests added in `CheckpointSnapshotProviderContractTest` use a deliberately
small test provider to verify detached capture, capture-failure short-circuiting,
and candidate-first rejection. They are executable examples of the interface
contract, not a substitute for a production gameplay adapter.

Still required before live integration:

- A concrete provider backed by real gameplay state.
- A documented game-thread executor/dispatcher and owner-thread assertions.
- Deterministic tests for Android save supersession and in-flight ordering.
- Android process-kill and physical-device AtomicFile durability tests.

## Validation status

This change is a source/API and documentation change. It is not considered
runtime-verified until the repository CI runs core JVM tests, Android unit
tests, and debug APK assembly on the resulting commit.


## Deterministic save request sequencing

The core module now provides `CheckpointSaveSequence`, a small synchronized
sequence gate used by the Android dispatcher. Each accepted save receives a
monotonic sequence. A queued worker checks whether a newer request has arrived
immediately before beginning its write; stale queued work is reported as
superseded. The gate does not interrupt a write that has already begun.

The dispatcher now depends on the `CheckpointStorage` interface rather than
the concrete Android storage adapter. This makes the I/O coordinator easier
to test with controlled storage implementations while keeping Android
AtomicFile details behind `AndroidCheckpointStorage`.

The sequence gate has pure JVM tests for monotonic allocation, supersession,
and invalid sequence rejection. Dispatcher-level tests for actual executor
ordering, callback delivery, and close/rejection behavior remain outstanding;
the sequence tests alone do not prove those Android concurrency behaviors.


## Dispatcher concurrency and callback tests

Added Robolectric tests for the serialized Android dispatcher using a controlled
fake storage and latches. The scenarios cover an older queued save being
superseded behind a blocked load, an already-started write completing before
a newer save, save I/O failure reporting, rejection of save after close, and
load runtime failure reporting as `STORAGE_ERROR`. The load failure test also
asserts callback delivery on the Android main looper.

The dispatcher now converts unexpected runtime exceptions and null load
results into explicit `STORAGE_ERROR` outcomes instead of allowing the
executor task to terminate without notifying the caller. Save callbacks retain
the existing `SAVED`, `SUPERSEDED`, and `FAILED` outcomes.

These tests verify executor sequencing and callback routing under Robolectric;
they do not replace process-kill, filesystem durability, or physical-device
AtomicFile testing. A production game-thread dispatcher is still required
before applying loaded candidates to live gameplay state.


## CI failure diagnosis and correction

The first CI execution of the detached-snapshot contract test reached the test
suite (345 tests executed) and found one assertion failure. The captured
checkpoint restored correctly, but the assertion compared the coordinator
restore result against the object-table `Status` enum. These are distinct
enum types even though both expose an `OK` value. The assertion now explicitly
expects `CrossTableLifecycleCoordinator.Status.OK`. This was a test assertion
type mismatch, not a checkpoint restore implementation failure. A fresh CI
run on the corrected branch head is still required to verify the fix and
continue into Android unit tests and APK assembly.
