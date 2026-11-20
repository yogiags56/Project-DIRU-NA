# DIRU-NA-DEV-003BB — Type-5 Terminal-State & Record Release Matrix

**Status:** Static call-site inventory completed; direct Type-5 reclamation remains unproven  
**Parent:** DEV-003BA  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope and terminology

This audit distinguishes:
- object record type stored in bits 28..30 of `aLongArr[id - 1]`;
- controller state stored in `this.xByte`;
- tile metadata group/subtype stored in `eIntArrArr`.

These are separate state domains. A `case 5` in the controller state machine is not evidence that the current object record has type 5.

Evidence is static decompiled source from `src/main/java/i.java` at the pinned candidate revision. It is not original JAR bytecode parity or a runtime trace.

## 2. Complete inventory of whole-record release helper calls

The source file contains 14 textual `fVoid(` occurrences, but only four are calls to the static whole-record helper `fVoid(byte id)`. The other occurrences are unrelated rendering/player-damage overloads or the helper declaration itself.

| Source line | Caller context | Observed precondition / action | Interpretation |
|---|---|---|---|
| ~15951 | `asVoid()`, controller update of `wByte`, object record direction/state field = 1 | If current Y equals `dyInt`, resets `dqInt`/`dzInt`, frees `wByte`, sets controller state 4 | Completion/termination of the currently controlled record |
| ~16077 | `asVoid()`, object-table scan, record type bits = 3 | Frees scanned record | Cleanup of a type-3 record; not evidence of Type-5 release |
| ~16242 | `asVoid()`, object-table scan, record type in 1/6/7 branch, with `b40 == true` (record type 7) | On matching next-tile metadata subtype 7, frees current record and sets controller state 3 / interaction flag | Type-7 completion/transition |
| ~16260 | Same 1/6/7 branch, non-type-7 path after transfer fields have been written to another record | Frees the old current record after the destination record is initialized | Transfer/replacement of an old type-1 or type-6/7-branch record |

The helper itself is only:

```java
private static void fVoid(final byte b) {
    i.aLongArr[b - 1] = 0L;
}
```

It clears the selected record word. It does not scan or clear tile metadata references and has no visible ID bounds check.

## 3. Type-5 record terminal-state matrix

The Type-5 creation site in controller state 5 allocates a record and writes object type 5 into bits 28..30, then stores source coordinates and links traversed tile metadata to the returned one-based ID.

The inspected object-table update switch handles record types 1, 2, 3, 6, and 7. There is no explicit `case 5` in that switch. This is important: the Type-5 record appears to function as a persistent traversal/metadata anchor rather than following the same per-frame movement update path as types 1/2/3/6/7.

| Lifecycle event | Source evidence | Finding |
|---|---|---|
| Creation | Controller state 5; six-argument allocator; type bits set to 5; tile metadata IDs written | Confirmed |
| Active traversal setup | One record ID is shared across eligible tiles; span/length field is written | Confirmed |
| Per-frame object update | Object-table switch has no explicit type-5 branch | No direct Type-5 per-frame update branch found |
| Normal completion | Type-1 controller path frees `wByte`; type-7 branch may free current record; neither is explicitly type 5 | No direct Type-5 free proven |
| Abort/cancel | No unambiguous direct Type-5 `fVoid(id)` call found in the inspected state machine | Unknown |
| Transfer | Old record is freed after a destination record is initialized in the type-1/6/7 branch; no evidence proves old record is type 5 | Generic transfer exists; Type-5 participation unproven |
| Tile metadata removal | `cVoid(x,y,group)` clears one 9-bit tile metadata group and cell state; does not call whole-record helper | Confirmed separate operation |
| Stage reset/teardown | `arVoid()` replaces the 15-entry object arrays; `pVoid()` nulls live/snapshot arrays | Whole-table lifecycle boundary confirmed |
| Checkpoint restore | Copies saved tile metadata and object table back together | Paired restore confirmed |

## 4. Important interpretation: do not infer a missing Type-5 free

The absence of a direct `fVoid(type5Id)` call in the inspected state-machine paths is a meaningful audit result, but it does not yet prove a memory leak or a permanently orphaned record.

Possible explanations still requiring evidence:
1. Type-5 records are intentionally retained as level-scoped metadata anchors until a full stage reset.
2. A tile metadata cleanup path or another method not yet traced causes a separate lifecycle transition.
3. The record type is changed before a later generic release path.
4. Decompiled control flow or source coverage obscures a path present in the original bytecode.

No one of these explanations is selected as fact in this stage.

## 5. Reference and cleanup ordering

The audited code establishes three distinct operations:
- clearing one tile metadata group with `cVoid`;
- freeing one whole record with `fVoid(id)`;
- resetting/replacing the complete object and metadata arrays at stage/feature initialization or teardown.

A native implementation must keep these operations separate. It must not automatically free a Type-5 record when a single tile link is cleared, and it must not assume that a record release also clears every tile link.

The static helper `fVoid(id)` has no visible range validation. The native equivalent must validate the external one-based ID before indexing the zero-based 15-entry table.

## 6. What this stage establishes

**Confirmed from the pinned candidate source:**
- Four call sites invoke the static whole-record clear helper.
- One call site frees type 3 records during the object-table scan.
- One controller path frees its active `wByte` record on a coordinate completion condition.
- A type-7 interaction path can free its current record.
- A transfer path initializes a destination record before freeing the old record.
- No direct call site explicitly identifies a type-5 record ID as the argument to `fVoid`.
- Type-5 records are created and referenced from tile metadata, but have no explicit type-5 case in the inspected object-table update switch.

**Not established:**
- Whether Type-5 records are intentionally stage-lifetime objects.
- Whether all tile references to a Type-5 record are eventually cleared before stage reset.
- Whether Type-5 records are converted to another type elsewhere.
- Whether the original shipping JAR has the same lifecycle.
- Whether an orphaned Type-5 record is reachable in ordinary gameplay or contributes to allocator exhaustion.

## 7. Native design constraints

1. Preserve the 15-slot table and one-based ID convention.
2. Keep tile unlink, record free, and whole-stage reset as distinct API operations.
3. Validate IDs before record reads/writes/frees.
4. Track tile references in a diagnostic/test model, but do not introduce reference-count reclamation as a claimed legacy behavior.
5. Do not free Type-5 records on inferred completion until a source/runtime lifecycle is demonstrated.
6. Model stage reset and checkpoint restore as explicit whole-state transitions.
7. Keep allocator exhaustion handling atomic and safe, independent of the legacy unchecked access.

## 8. Validation status

- All textual `fVoid(` occurrences in the pinned source were classified by overload.
- Four calls to the static whole-record helper were identified.
- No production source or test code changed.
- No Gradle tests or Android build were run.
- No shipping-JAR bytecode comparison or live-runtime trace was performed.

## 9. Next stage — DEV-003BC

1. Search every write to object type bits 28..30 and determine whether type 5 is ever reclassified.
2. Trace every call to `cVoid(x,y,group)` and map which tile metadata references can be cleared during gameplay.
3. Determine whether the full Type-5 anchor lifetime is bounded by stage reset or another lifecycle event.
4. Build a source-derived transition graph for type-5 creation, reference insertion, unlink, and reset.
5. Prepare the native lifecycle fixture only after those transitions are explicit.
