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


## Robolectric dispatcher test synchronization correction

The first Android test run confirmed the core JVM suite passes but exposed two
test-harness timing defects: a worker latch timed out under the CI runner, and
callback assertions could run before the paused main looper was drained after
the worker posted its result. The tests now use a longer bounded worker wait
and explicitly pump the main looper until the expected callback count is
observed. This is a test synchronization adjustment; production dispatcher
semantics are unchanged. A fresh CI run is required to verify the change.


## CI verification and process-recreation recovery coverage

The corrected dispatcher synchronization tests passed in CI on commit
`2bb805782e733c22456fd5a6b56cb1b210e989c8`. The workflow completed
successfully, including the core JVM test suite and Android debug build.

Additional Robolectric storage coverage now verifies recovery through a newly
constructed storage adapter (modeling process recreation without reusing the
previous adapter instance) and restoration when only the AtomicFile backup is
present. These are host-side tests of Android adapter behavior; they do not
simulate an actual OS process kill or prove physical-device power-loss
durability. Those remain device validation items.


## Corrupt and truncated recovery safety

Added storage adapter tests that seed a destination coordinator with a live
object/auxiliary association before loading a corrupt checkpoint. The test
asserts the live object and association remain unchanged and that the corrupt
file is retained. A separate truncated-payload test verifies the load outcome
is `CORRUPT` and the stored bytes remain available for diagnosis.

These tests exercise the candidate-first recovery boundary: decoding failure
must not mutate the live coordinator or delete the source checkpoint. CI for
the new test commit is required before treating this coverage as verified.


## Latest CI compile failure and correction

The new non-destructive recovery test initially failed at Android test
compilation because its fixture referenced `ObjectRecordTable` without
importing the core type. The compiler reported unresolved
`ObjectRecordTable` references in `AndroidCheckpointStorageTest`. Added the
missing import; no production source behavior was changed. The corrected
commit requires a fresh Android test and debug assembly run.


## Successful CI and malformed-file boundary tests

The corrected import commit `2409ca3f9f312b047ac362664229ed61661843ac`
completed GitHub Actions successfully: the combined core JVM test and Android
debug build job concluded `success`.

Added two more Android storage boundary tests:
- A zero-byte checkpoint file is reported as `STORAGE_ERROR` by the AtomicFile
  read path and is retained rather than treated as a missing/new save.
- A checkpoint with an unsupported binary format version is reported as
  `CORRUPT` and its original bytes remain available.

The tests are committed separately; they require their own fresh CI run.


## Encoded-size boundary and previous-save preservation

The latest CI runs for commits `67a9d68` and `95f96e7` both completed
successfully. Added storage tests for the 16 MiB encoded-size boundary:
- Reject an oversized new write while preserving and re-reading the previous
  valid checkpoint.
- Reject an oversized on-disk checkpoint as a storage error while retaining
  the file for diagnosis.

The write-side test checks the adapter's preflight rejection path. It does not
simulate a low-level I/O failure after `AtomicFile.startWrite()`; that requires
a fault-injection seam or device-level filesystem test and remains outstanding.
The new tests require a fresh CI run.


## Atomic write failure rollback test

Added a package-private write-operation seam to the Android AtomicFile adapter.
The production constructor still uses the normal single-call stream write; the
seam is available only to package-local tests. A Robolectric test first writes
a known-good checkpoint, injects an IOException after a partial write to the
AtomicFile output stream, and then opens a fresh adapter to verify that the
previous checkpoint remains readable and byte-identical. This exercises the
adapter's `failWrite()` recovery path rather than only its input-size preflight.

The test is included in the current branch CI run. Host-side AtomicFile behavior
is not a substitute for physical-device power-loss testing. Device validation
is therefore recorded as post-integration validation, not a blocker for the
source-level DEV-003CR boundary. Live gameplay integration remains explicitly
out of scope until a concrete game-state provider and owner-thread dispatcher
exist.


## DEV-003CR source-level exit gate — verified

The fault-injection test commit `144b5c3a7331023f6d3883248fb094eed2dce720`
passed GitHub Actions workflow
`44848229552`. The final documentation head
`19afbac174823755518b0f7150a7866e3a8e2ce9` also passed workflow
`44848243427`. Both workflows completed the core JVM tests, Android storage
unit tests, and Android debug APK assembly successfully.

The source-level DEV-003CR exit gate is therefore met for the implemented
snapshot-provider contract, persistence adapter, dispatcher tests, recovery
behavior, and injected mid-write rollback scenario. This does not claim a
production gameplay provider exists: no real gameplay engine or authoritative
game-state thread is present yet. Physical-device process-kill and storage
durability checks remain device-validation work. PR #1 remains open and
unmerged pending review/authorization.
