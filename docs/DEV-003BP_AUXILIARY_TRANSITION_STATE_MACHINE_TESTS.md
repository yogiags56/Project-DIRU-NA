# DEV-003BP — Auxiliary Transition State Machine & Cross-Table Guard Tests

## Scope

Added an isolated pure-Java model for the source-supported auxiliary entry lifecycle. The model is not connected to gameplay and does not replace the opaque 15-entry checkpoint storage.

## Source-backed fields represented

- bits 0..3: lifecycle state;
- bits 4..5: direction/subtype consumed by the controller;
- bits 6..12: movement value read by the state-1 to state-3 transition;
- bits 13..19: X coordinate;
- bits 20..26: Y coordinate.

All other bits are treated as unknown payload and preserved by state changes. The consumer's source-observed whole-entry clear is modeled separately.

## Implementation

Added `AuxiliaryEntryStateMachine.java`.

### markReady

The guarded state-1 -> state-3 operation requires:
1. a valid auxiliary slot index in 0..14;
2. the slot to be representable in the legacy three-bit cross-link (0..7);
3. the decoded object-record link to match the supplied slot;
4. the auxiliary entry's current state to equal 1;
5. the source-read movement field (bits 6..12) to equal zero.

On success, only bits 0..3 are replaced with state 3. All other bits remain bit-identical.

On failure, the original raw entry is returned unchanged with a specific status.

### consume

The consume operation accepts only state 3. It returns the source-proven X, Y and direction fields and models the observed whole-64-bit-word clear by returning `0L`.

A non-state-3 entry is rejected and returned unchanged.

## Tests

Added `AuxiliaryEntryStateMachineTest.java` with seven test methods:
1. valid state-1 to state-3 transition with matching link;
2. preservation of unknown and known non-state bits;
3. rejection when movement value is nonzero;
4. rejection of wrong state and mismatched link without mutation;
5. rejection of invalid and unrepresentable slot indices;
6. state-3 field extraction and whole-word clear;
7. rejection of consume for non-state-3 entries without clearing.

The codec's five tests remain in place. The object-record/auxiliary test inventory is now 29 methods across its three test classes, based on source inspection; CI has not yet been queried for this commit series.

## Important boundaries

- The three-bit link limits safely representable slots to 0..7 although the auxiliary array has 15 slots. This model rejects 8..14 for object-linked transitions.
- The separate forensic `encodeLegacyTruncating` method remains available for compatibility experiments only.
- Unknown packed bits are not assigned semantics.
- The model does not yet represent allocation of state-1 entries, object-record type reclassification, controller state 4 scheduling, or multi-entry traversal.
- The model's precondition is a conservative interpretation of the decompiled source. It must be checked against the exact shipping JAR before claiming runtime parity.
- No Android gameplay integration, native NDK port, or shipping-JAR bytecode comparison is claimed.

## Next milestone — DEV-003BQ: Cross-Table Lifecycle Orchestrator & Atomicity Tests

1. Compose ObjectRecordTable, AuxiliaryLinkCodec and AuxiliaryEntryStateMachine behind a small isolated coordinator.
2. Ensure failed object-to-auxiliary linking does not partially allocate or mutate either table.
3. Ensure successful state transition updates the auxiliary entry and related object-record state atomically in the model.
4. Test stale handles, duplicate links, occupied slots, snapshot/restore across in-flight transitions, and rollback.
5. Preserve raw unknown auxiliary bits across checkpoint/restore.
6. Keep all coordinator code outside gameplay until original-JAR evidence and CI are available.

No CI pass, Android build, or runtime behavior is claimed by this document.
