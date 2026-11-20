# DIRU-NA-DEV-003AZ — Type-5 Creation Call-Site & Stage Lifecycle Audit

**Status:** Static source audit completed for the pinned candidate revision; per-level creation bound remains unproven  
**Parent:** DEV-003AY  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope and evidence boundary

This stage enumerates the observable Type-5 creation/update entry path and traces the relevant packed tile grid and 15-entry object table through stage initialization, checkpoint save/restore, and teardown.

Evidence is from the decompiled candidate source `src/main/java/i.java` at the pinned revision. It is not a shipping-JAR bytecode comparison, runtime trace, or proof that every decompiler reconstruction is exact.

## 2. Type-5 creation and update call path

### 2.1 Creation occurs in the object update state machine

The Type-5 creation sequence is inside `asVoid()`, in the `switch (this.xByte)` branch for state 5 (approximately source lines 16480–16590):

1. The routine reads the tile's packed direction/state.
2. On the first eligible tile in that invocation, it calls the six-argument `aByte(..., mode=8, ...)` allocator.
3. The returned one-based record ID is reclassified to object type 5 (bits 28..30).
4. The invocation stores the source X/Y pair at offsets 14 and 20.
5. Later eligible tiles in that invocation store the same record ID in their tile metadata.
6. The traversal computes and stores a span/length field on the record.

The local one-shot gate is initialized once for the traversal and disabled after the first record allocation. This establishes **at most one newly allocated Type-5 record per execution of this traversal branch**, not one per level.

### 2.2 Entry into the update state

The full source has one direct call site for `asVoid()` in the main gameplay update loop (approximately line 10616). The branch is therefore serviced by the ordinary gameplay update loop when its state is active.

The state is selected by `lVoid(x,y)` (method around line 16790), which sets `xByte = 5` when the `lBoolean` feature is active and a neighboring packed tile direction is present.

Observed `lVoid(x,y)` call sites include:
- approximately line 10547: tile transition during a gameplay update;
- approximately line 13890: a tile interaction in the player/environment event dispatcher;
- approximately line 14019: another tile interaction/event branch.

Other calls to the overloaded `lVoid` names are unrelated overloads (for example integer-only animation/event helpers) and are not counted as Type-5 traversal triggers.

**Conclusion:** Type-5 traversal is gameplay-triggered and then advanced by the main update loop. The three listed coordinate call sites are confirmed examples, not proof that the list is exhaustive across all possible dispatch paths or runtime state transitions.

## 3. Object table and tile metadata initialization

### 3.1 Fifteen-entry object table

The constructor initializes `aLongArr = new long[15]` and `bLongArr = new long[15]` (approximately line 17752).

The feature initialization helper `arVoid()` (approximately line 15840) replaces all four related arrays when `lBoolean` is active:
- `aLongArr` — 15 object records;
- `bLongArr` — 15 auxiliary packed records;
- `cLongArr` — checkpoint snapshot for object records;
- `dLongArr` — checkpoint snapshot for auxiliary records.

It also resets the Type-5/object state fields, including `xByte`, `dqInt`, `dvInt`, `dwInt`, `dxInt`, and related fields.

### 3.2 Stage initialization path

The staged initialization state machine contains a feature-enabled branch (approximately lines 4596–4600) that calls `arVoid()`, creates a fresh `eIntArrArr[stageWidth][stageHeight]` tile metadata grid, then calls `afVoid()`.

This is direct evidence of a stage-initialization reset for the tile metadata grid and the 15-slot object table when the feature is enabled. The call to `arVoid()` is conditional on `lBoolean`; the audit does not generalize this reset to stages where the feature is inactive.

### 3.3 First activation during gameplay

The player/environment interaction path (approximately lines 12120–12135) checks a special foreground tile. If the feature has not yet been activated, it calls `arVoid()`; it then sets `lBoolean = true` and creates `eIntArrArr` if it is null.

This is a separate first-activation path, not a per-traversal reset. It means the first interaction can initialize the object table and tile metadata grid during gameplay.

## 4. Checkpoint save and restore

### 4.1 Save

`saveCheckpoint()` (approximately lines 15190–15270) copies the live tile metadata grid `eIntArrArr` into `fIntArrArr` when `lBoolean` is active.

It also allocates `cLongArr` and `dLongArr` if needed, then copies:
- `aLongArr` → `cLongArr`;
- `bLongArr` → `dLongArr`.

This proves the checkpoint snapshot includes the Type-5/object table and its tile metadata references together. It is not just a player-position checkpoint.

### 4.2 Restore

`restoreCheckpoint()` (approximately lines 15330–15370) copies:
- `fIntArrArr` → `eIntArrArr`;
- `cLongArr` → `aLongArr`;
- `dLongArr` → `bLongArr`.

It also restores the associated Type-5 state-machine fields. Thus the inspected restore path reinstates the metadata grid and object records from the same checkpoint snapshot, avoiding the specific mismatch scenario of restoring tile IDs without restoring their record table.

This is confirmed for the inspected checkpoint implementation; it does not prove every save/load mechanism in the full game uses this same path.

### 4.3 Retry and checkpoint triggers

The source calls `restoreCheckpoint()` from multiple gameplay paths, including explicit checkpoint interaction and the retry/death flow (examples around lines 1270, 1505, 1519, and 11022). The restore implementation above is therefore directly relevant to retry behavior.

## 5. Stage teardown

The resource teardown method `pVoid()` (approximately lines 4800–4920) nulls:
- `eIntArrArr`;
- `fIntArrArr`;
- `aLongArr`;
- `bLongArr`;
- `cLongArr`;
- `dLongArr`;
- and the stage player/foreground and supporting tile arrays.

This is a whole-table/resource teardown, not per-record reclamation. No individual Type-5 reference-count release is established by this method.

The stage map navigation code also resets the separate world-map array `aLongArrArr`; that is not the same object table as `aLongArr` and must not be conflated with it.

## 6. Capacity and upper-bound result

Confirmed constraints:
- The object table has exactly 15 slots.
- The allocator scans for a free record and returns `-1` when all 15 are occupied.
- One traversal invocation creates at most one Type-5 record.
- A Type-5 record may be referenced by multiple tile metadata entries from that traversal.
- Checkpoint save/restore snapshots the tile metadata grid and the object table together.
- Stage initialization and first activation can reset/initialize the table; teardown nulls it.

**A numeric maximum Type-5 count per level cannot yet be derived.** The number of times the traversal state can be entered in one level is not bounded by the inspected local loop or the one-shot gate. Therefore the only defensible hard table capacity is 15 total object records across the relevant object types, not 15 Type-5 records and not a smaller guessed per-level number.

Potential exhaustion is a real condition the allocator handles by returning `-1`. The inspected creation caller must be checked for how it handles that sentinel before any native implementation assumes allocation always succeeds. No claim is made here that all callers safely handle it.

## 7. Native implementation implications

1. Model the tile metadata grid and the 15-entry object table as one checkpointed state unit.
2. Save and restore both in the same transaction/snapshot boundary.
3. Distinguish stage initialization/feature activation reset from ordinary tile metadata cleanup.
4. Do not reclaim Type-5 records on single-tile metadata removal without a proven source lifecycle rule.
5. Preserve the 15-slot limit and explicitly model allocator exhaustion.
6. Audit the allocator-failure path at the Type-5 caller before implementing the native branch.
7. Keep `aLongArrArr` (world map progression) separate from `aLongArr` (packed object records).

## 8. Validation status

- Source-level call path and lifecycle inspected at the pinned candidate revision.
- No production code changed.
- No new test added: a faithful tile metadata/state-machine fixture is not yet implemented.
- No local Gradle test or Android APK build was run during this documentation-only audit.
- No shipping-JAR parity or live-gameplay result is claimed.

## 9. Next stage — DEV-003BA

1. Trace the exact allocator-failure sentinel path from the Type-5 creation call.
2. Establish whether `aByte(..., mode=8)` can return `-1` and whether subsequent packed writes/metadata links are guarded.
3. Inspect the full Type-5 state transitions and completion/abort paths, including when `fVoid(id)` is called.
4. Reconcile source offsets and record ID conventions with the native codec tests.
5. Only then design a focused lifecycle/reference-integrity fixture and add tests without changing production semantics prematurely.
