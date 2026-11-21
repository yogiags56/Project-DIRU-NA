# DEV-003CS-2 — Evidence-backed Persisted Field Matrix

**Status:** Initial evidence matrix drafted; semantic and schema review still required.  
**Milestone:** Proposed DEV-003CS — Gameplay State Snapshot Model & Provider Foundation.  
**Evidence:** AUDIT-004, AUDIT-009, AUDIT-010, AUDIT-012; candidate source pinned at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

## 1. Purpose and boundary

This matrix identifies candidate state categories for a future native checkpoint model. It does not define final serialized field names, binary layout, or schema version. The audited source is static decompilation evidence, not runtime-verified behavior. Opaque decompiler identifiers must not be promoted to semantic field names without tracing their readers/writers.

The original candidate's `saveCheckpoint()` / `restoreCheckpoint()` represent an in-memory respawn snapshot. AUDIT-009 explicitly notes that these methods do not serialize the complete snapshot to RMS. The native DEV-003CR storage system is separate. This matrix therefore identifies gameplay state that may be needed for native checkpoint recovery; it does not assert that the original game has a durable app-restart checkpoint format.

## 2. Evidence-backed candidate matrix

| Logical category | Candidate data | Evidence | Proposed treatment | Confidence / open work |
|---|---|---|---|---|
| Player grid position | Integer tile X and Y | AUDIT-004 identifies `crtPlayerX`, `crtPlayerY`; movement/camera calculations use tile coordinates | Include in durable snapshot | High that coordinates exist; validate bounds against stage dimensions |
| Player facing / logical direction | Direction encoded in gameplay state; direction-indexed movement vectors | AUDIT-004 maps pending direction values 1–4 and low bits of `kInt` | Include only normalized logical facing if required after safe-point capture | Medium; distinguish facing from pending input |
| Active stage identity | World index and stage index/identity | AUDIT-012 stage stream selection; AUDIT-010 world/stage preparation; AUDIT-009 stage snapshot context | Include stable identity and content/schema compatibility key | High category; exact canonical IDs and compatibility policy remain to be defined |
| Stage dimensions | Width and height | AUDIT-012 stage header stores unsigned 16-bit width/height and allocates width×height arrays | Include and validate before allocating/accepting arrays | High |
| Player/object layer | Per-cell authored/transformed player/object codes | AUDIT-012 parses layer 1 into `crtStagePlayerLayer[x][y]`; AUDIT-009 says checkpoint captures/restores it | Include full validated grid if required to restore dynamic world | High category; define whether snapshot stores authored or transformed runtime layer |
| Background layer | Per-cell background values | AUDIT-004 §6.2 explicitly lists background layer in checkpoint snapshot; AUDIT-012 §2 identifies layer 2 as `bIntArrArr[x][y]` and shows it participates in foreground metadata and object preprocessing | Include in the candidate checkpoint model; preserve as a distinct channel from foreground/player-object data | High that it is a distinct runtime layer and part of audited snapshot category; exact byte-for-byte copy/restore path still needs source-line trace |
| Foreground layer | Per-cell foreground codes and packed metadata | AUDIT-012 parses layer 3 into `crtStageForegrondLayer[x][y]`; AUDIT-004 identifies checkpoint code 4 and context-dependent upper metadata; AUDIT-009 says foreground layer is restored | Include full validated runtime grid | High |
| Per-cell integer object state | Integer grid for object/entity runtime state | AUDIT-009 explicitly lists object-state integer grid in checkpoint snapshot/restore | Include after documenting dimensions, object semantics, and entity reconstruction | High category; values and multi-cell invariants incomplete |
| Per-cell byte timers/state | Byte grids used by runtime tile/entity updates | AUDIT-009 explicitly lists per-cell byte timers; AUDIT-012 identifies runtime arrays `bByteArrArr` and `cByteArrArr` initialized by object-layer preprocessing | Candidate include, preserving each array separately; do not merge until exact roles and dimensions are traced | High that per-cell byte state exists; exact array-to-snapshot mapping and semantics remain open |
| Player/gameplay counters | Multiple player and gameplay counters | AUDIT-009 says broad snapshot contains several counters/state fields; AUDIT-004 documents movement and damage state families | Candidate include only counters that affect future simulation/progression | Medium; enumerate readers/writers and invariants before naming |
| Progression/inventory-like state | Stage progress, inventory-like fields, checkpoint-related save byte 10 | AUDIT-009 lists progression/inventory-like fields and save byte 10 | Include durable progression values needed to resume; keep profile save separate where appropriate | Medium; identify ownership and avoid duplicate/conflicting persistence |
| Moving-object state | Optional mode-specific arrays and moving-object state | AUDIT-004 §6.2 explicitly lists optional mode-specific arrays and moving-object state; AUDIT-009 describes optional arrays/long values under `lBoolean` | Conditional snapshot section only when the mode and moving entities are supported | Medium; identify exact entities, arrays, lengths, and reconstruction invariants |
| Optional mode-specific arrays | Arrays and long-value arrays when mode flag is enabled | AUDIT-009 explicitly describes optional arrays/long values guarded by `lBoolean` | Conditional schema section only if mode is supported and enabled | Medium; enumerate dimensions, mode identity, and invariants |
| Optional mode-specific bytes/counters | Extra bytes/counters for mode | AUDIT-009 describes additional mode-specific bytes/counters | Conditional include after semantic audit | Low/medium; exact field list not yet proven |
| Retry accounting | `azInt`, `bdInt` behavior | AUDIT-009 documents decrement/increment and transition to global state 12 | Do not name as lives/continues yet; decide product semantics separately | Medium; user-facing meaning unresolved |
| Checkpoint identity/index | Checkpoint-related value stored from save byte 10; checkpoint tile code 4 triggers behavior | AUDIT-004 and AUDIT-009 | Include stable checkpoint identity/index only after confirming relationship to stage/progression | Medium; avoid confusing tile marker with durable checkpoint record |
| Screen/global state | Global state byte; state 2 active gameplay, 4 title/menu, 21 loading, 22 credits | AUDIT-010 | Exclude from gameplay checkpoint; restore via application/game-flow policy | High exclusion |
| Input state | Held-key bitmask and pending key input | AUDIT-004 key callbacks and shared bitmask | Exclude; clear/reinitialize input on restore | High exclusion |
| Movement interpolation | `jInt` interpolation timer and transient movement/action flags | AUDIT-004 sets/decrements movement timer; AUDIT-009 says restore resets movement state | Exclude by default; capture only if future deterministic resume explicitly requires mid-move continuation | Medium; safest capture point is stable movement boundary |
| Animation/animator slots | Animator indices and animation state | AUDIT-004 and AUDIT-009 describe animation reset/reselection on restore | Exclude transient animator objects/handles; reconstruct a safe animation from logical state | High exclusion for objects; exact logical animation state unresolved |
| Camera/render state | Camera offsets, pixel interpolation, dirty/update markers | AUDIT-004 camera derived from tile coords; AUDIT-009 says dirty markers rebuilt after restore | Exclude derived camera/cache state; recompute after commit | High exclusion |
| Audio/resource handles | Playback and loaded resource objects | AUDIT-012 resource lifecycle; AUDIT-009 describes sound side effects | Exclude; reacquire/rebind through platform/resource systems | High exclusion |
| Partial stage-loader state | Incremental loader step counter and partially initialized layers/resources | AUDIT-010 state 21; AUDIT-012 incremental stage initialization | Exclude; never checkpoint partially initialized stage | High exclusion |

## 3. Required schema design rules

1. **Stable identity first:** every snapshot must identify stage/world and schema/content compatibility before accepting grids.
2. **Bounded dimensions:** validate width, height, multiplication overflow, per-layer element count, and aggregate payload size before allocation.
3. **Distinct channels:** preserve player/object, background, foreground, integer object state, and per-cell byte state separately. Never flatten layers into a single tile enum.
4. **Detached values:** snapshot arrays must be deep-copied or represented by immutable value containers; no aliases to live mutable grids.
5. **Atomic restore:** validate all fields and cross-field invariants before mutating the live game owner.
6. **Safe-point capture:** initial implementation captures only when movement interpolation/deferred checkpoint updates have settled and no transition/loading transaction is active.
7. **Explicit optional sections:** mode-specific state must carry a mode identifier and validated lengths; unsupported mode data must reject restore rather than be silently ignored.
8. **No invented semantics:** opaque counters/flags remain excluded or marked unresolved until their readers/writers are traced.
9. **Separate profile save from checkpoint:** durable preferences/progression and in-stage world rollback may have different lifetimes and should not be conflated.
10. **Version independently:** native checkpoint schema version must be explicit and must not reuse J2ME internal save-byte meanings without a verified mapping.

## 4. Minimum first native checkpoint candidate

Subject to CS-2 review, the smallest useful first schema candidate is:

- Native schema version.
- Stable world/stage identity and dimensions.
- Player tile X/Y and normalized facing.
- Foreground runtime layer.
- Player/object runtime layer.
- Integer object-state grid.
- Per-cell byte timers/state that are proven necessary.
- Required progression/checkpoint identifier values proven to affect restore.

Background layer and optional mode sections remain conditional pending verification. Input, animation objects, camera, renderer caches, audio handles, and loading progress are excluded.

This is a proposed minimum, not yet an approved serialization contract. It must be checked against actual native gameplay owner design before implementation.

## 5. Open verification tasks

- Trace every copied field in candidate `saveCheckpoint()` and every corresponding assignment in `restoreCheckpoint()`; record source line ranges in a follow-up audit.
- Trace exact copy and restore statements for the background layer; AUDIT-004 §6.2 already identifies it as part of the checkpoint snapshot, while AUDIT-012 establishes its distinct runtime role.
- Enumerate all integer object-state and byte timer dimensions, map `bByteArrArr` / `cByteArrArr` to snapshot fields, and document multi-cell entity invariants.
- Trace progression/inventory fields to the existing native profile-save model before deciding ownership.
- Resolve mode-specific array lengths and whether mode state is in scope for first playable build.
- Define canonical world/stage IDs and compatibility policy for changed stage content.
- Reconcile the native checkpoint schema with the actual first reproducible gameplay milestone in DEV-002.

## 6. CS-2 status

**Completed in this draft:** evidence-backed category inventory, proposed include/exclude/conditional classifications, minimum schema candidate, validation rules, and unresolved questions.

**Not complete:** final field names/types, exact serialized schema, full call-site trace for every copied field, Native owner implementation, runtime validation, or physical-device testing.

CS-2 remains **in progress** until the open verification tasks are resolved and the field matrix is reviewed. No production snapshot provider should be implemented from this draft alone.


## 7. Evidence refinement — 2026-11-21

A second pass against AUDIT-004 §§6.1–6.3 and AUDIT-012 §§2–3 corrected two classifications:

- **Background layer:** AUDIT-004 §6.2 explicitly includes the background layer in the checkpoint snapshot. AUDIT-012 identifies it as the second distinct stage channel, `bIntArrArr[x][y]`, and documents its participation in foreground metadata/object preprocessing. It is now a candidate include, not an optional omit pending proof of mutation. Exact copy/restore statements still need a line-level trace.
- **Per-cell byte state:** AUDIT-012 names runtime arrays `bByteArrArr` and `cByteArrArr`. Both must remain distinct candidate channels until their snapshot mapping, dimensions, and semantics are established.
- **Moving-object state:** AUDIT-004 §6.2 explicitly identifies optional mode-specific arrays and moving-object state. This now has a separate conditional row; it must not be silently dropped if that mode is supported.

Additional confirmed timing boundary from AUDIT-004 §6.1: checkpoint activation sets a deferred-save flag; the actual in-memory capture occurs only after movement interpolation settles. Native capture should preserve this safe-point concept and must not snapshot halfway through a tile transition in the first implementation.

These refinements improve category-level evidence but do not close CS-2. The exact assignment-by-assignment copy/restore trace and schema invariants remain outstanding.
