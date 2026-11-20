# DEV-003BQ — Cross-Table Lifecycle Coordinator & Atomicity Tests

## Scope

Added an isolated coordinator joining the existing object-record table, safe three-bit auxiliary-link codec, and auxiliary-entry state machine. This is a test/model layer only; it is not wired into game runtime or gameplay.

## Implementation

New class:
`core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/CrossTableLifecycleCoordinator.java`

### attach

- Validates object type, tile, auxiliary slot representability (0..7), state-1 input, and auxiliary-slot vacancy before allocation.
- Allocates an object record, links the requested tile group, stores the raw auxiliary entry, and records a logical association.
- Captures an object-table snapshot before mutation. If tile linking fails after object allocation, restores that snapshot so the object allocation is rolled back.
- Rejects occupied or already-associated auxiliary slots.

### markReady

- Resolves the logical object-to-auxiliary association.
- Rejects missing/stale object handles.
- Delegates state and movement preconditions to `AuxiliaryEntryStateMachine`.
- Writes the updated auxiliary word only after the transition succeeds.

### consume

- Requires an associated live object and a state-3 auxiliary entry.
- Uses the state machine to extract source-proven fields and clear the whole auxiliary word.
- Removes the logical association only after successful consumption.

### snapshot and restore

- Captures the object-table checkpoint and logical association map together.
- Restore first validates an isolated candidate object table and every association.
- Rejects invalid handles, duplicate auxiliary-slot associations, unrepresentable slots, empty or unclassified auxiliary states.
- Mutates the live object table and association map only after all validation succeeds.

## Important model boundary

The coordinator uses a sidecar logical association map keyed by the native generation handle. It does **not** claim to write or emulate the legacy packed three-bit field inside the actual object-record payload. The current `ObjectRecordTable` abstraction does not expose the raw legacy `aLongArr` bits. This layer is therefore a safe lifecycle regression model, not byte-exact legacy object-record integration.

Similarly, attach accepts a prebuilt raw state-1 auxiliary word. It does not yet reproduce the original four-argument allocator's exact field construction or all 15-slot allocation policy. Slots 8..14 remain rejected for safe object-linked use because the three-bit link cannot represent them.

## Tests

New class:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/CrossTableLifecycleCoordinatorTest.java`

Nine test methods cover:
1. attach -> state 1 to state 3 -> consume lifecycle;
2. rejection of unrepresentable slot before allocation;
3. rollback after occupied tile-group link failure;
4. duplicate auxiliary slot rejection;
5. movement-precondition failure without mutation;
6. checkpoint/restore of an in-flight association and continued lifecycle;
7. invalid association snapshot rejection without live-state replacement;
8. stale object-handle rejection;
9. rejection of association restore when the raw auxiliary state is neither 1 nor 3.

The focused object-record suite now contains 38 test methods by source count: 17 object-table, 5 link-codec, 7 entry-state-machine, and 9 coordinator tests.

## CI status and limitation

GitHub Actions run `44745725813` (coordinator implementation commit `29b8b38`) failed in the `Run core JVM tests` step before Gradle could resolve a large set of buildscript dependencies. The log reports `ModuleVersionNotFoundException` for dependencies including Guava, protobuf, ASM, Kotlin stdlib, and others from the configured Google Maven, Maven Central, and Gradle Plugin repositories. The Android debug APK step was skipped.

A subsequent workflow for the test commit was still in progress when this document was written. No passing test result or APK build is claimed. This failure is currently a dependency-resolution/build-environment failure, not evidence that the tests themselves passed or failed.

## Commits

- Coordinator implementation: `29b8b38f01553d23b36962395e80a38c739b099a`
- Restore-state validation correction: `31982ccd0c6deda9618475505d303294eaf45a26`
- Coordinator tests: `daa3c93acd5a89cda620a407b4a2a86601960361`
- Invalid-state restore regression: `2903d139ee193045cc9fff6d637e5bbc2d3f51ca`

## Next milestone — DEV-003BR: Raw Object-Record Link Integration Audit

1. Revisit the source bit layout for the `aLongArr` auxiliary link field and the Type-7 exception.
2. Decide whether ObjectRecordTable needs an optional legacy raw-payload field or a dedicated legacy codec wrapper.
3. Model the exact original field write/read masks without conflating one-based object IDs and zero-based auxiliary indices.
4. Add tests for link write, read, reclassification preservation, snapshot/restore, and invalid slot outcomes.
5. Investigate and fix the Gradle dependency-resolution problem before claiming CI validation.
6. Keep gameplay integration blocked until raw-field semantics and shipping-JAR provenance are verified.

No runtime gameplay integration, shipping-JAR parity, passing CI, or APK output is claimed.
