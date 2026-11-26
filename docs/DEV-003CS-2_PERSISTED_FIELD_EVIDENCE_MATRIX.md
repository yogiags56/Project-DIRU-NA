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
| Tile metadata grid (`eIntArrArr` / `fIntArrArr`) | Packed per-cell object-record references and metadata | DEV-003AZ traces checkpoint copy `eIntArrArr` → `fIntArrArr` and restore `fIntArrArr` → `eIntArrArr`; DEV-003BE identifies record IDs/group linkage | Include atomically with conditional object and auxiliary tables it references | High for pair and relationship; exact dimensions/cell encoding still need validation |
| Conditional object-record table (`aLongArr[15]`) | Fifteen opaque packed 64-bit object records | DEV-003AE exact save copy to `cLongArr` at 15223–15226 and restore at 15353; DEV-003BE maps type/coordinate/group bit ranges | Include as conditional feature section, opaque until semantics complete; restore atomically with tile metadata | High for capacity and copy pair; bit semantics partial |
| Conditional auxiliary table (`bLongArr[15]`) | Fifteen opaque packed 64-bit auxiliary records | DEV-003AE exact save copy to `dLongArr` at 15227–15230 and restore at 15354; DEV-003BL / DEV-003BE audit its separate role | Include separately from object table; restore atomically with tile metadata and controller state | High for capacity and copy pair; full state transitions remain incomplete |
| Per-cell animation/timer byte state | `bByteArrArr` and `cByteArrArr` are distinct runtime grids | AUDIT-002 §17.1 explicitly lists `bByteArrArr` in checkpoint contents; AUDIT-002 §3 / AUDIT-012 identify both runtime arrays | Include `bByteArrArr` as a checkpoint candidate; `cByteArrArr` remains runtime-only/unproven for checkpoint inclusion until its copy path is found | High for `bByteArrArr` category inclusion; `cByteArrArr` checkpoint status unproven |
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
| Camera/render state | Camera offsets, pixel interpolation, dirty/update markers | AUDIT-002 §17.1 describes camera/player state; AUDIT-004 §6.3 and AUDIT-009 §4 describe camera recentering from saved player tile; AUDIT-009 §4 step 6 says dirty markers are rebuilt | Recompute camera from logical player/stage state unless a direct scalar copy is found; rebuild dirty markers | High for recomputation behavior; exact camera scalar copies remain open |
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
- Tile metadata grid.
- `bByteArrArr` per-cell animation/timer state, subject to exact copy/restore and dimensions audit.
- Required progression/checkpoint identifier values proven to affect restore.

Background layer and optional mode sections remain conditional pending verification. Input, animation objects, camera, renderer caches, audio handles, and loading progress are excluded.

This is a proposed minimum, not yet an approved serialization contract. It must be checked against actual native gameplay owner design before implementation.

## 5. Open verification tasks

- Trace every copied field in candidate `saveCheckpoint()` and every corresponding assignment in `restoreCheckpoint()`; record source line ranges in a follow-up audit.
- Trace exact copy and restore statements for the background layer; AUDIT-004 §6.2 already identifies it as part of the checkpoint snapshot, while AUDIT-012 establishes its distinct runtime role.
- Enumerate dimensions and invariants for `bByteArrArr`; search for a direct checkpoint copy path for `cByteArrArr` before deciding inclusion.
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
- **Per-cell byte state:** AUDIT-012 names runtime arrays `bByteArrArr` and `cByteArrArr`. `bByteArrArr` is listed in AUDIT-002 §17.1 checkpoint contents. `cByteArrArr` is a runtime byte grid, but its checkpoint inclusion is not established by the available checkpoint-content list and must remain excluded from the schema until a direct copy/restore path is located.
- **Moving-object state:** AUDIT-004 §6.2 explicitly identifies optional mode-specific arrays and moving-object state. This now has a separate conditional row; it must not be silently dropped if that mode is supported.

Additional confirmed timing boundary from AUDIT-004 §6.1: checkpoint activation sets a deferred-save flag; the actual in-memory capture occurs only after movement interpolation settles. Native capture should preserve this safe-point concept and must not snapshot halfway through a tile transition in the first implementation.

These refinements improve category-level evidence but do not close CS-2. The exact assignment-by-assignment copy/restore trace and schema invariants remain outstanding.


## 8. Copy/restore trace refinement — 2026-11-21

See [DEV-003CS-2 Checkpoint Copy/Restore Source Trace](DEV-003CS-2_CHECKPOINT_COPY_RESTORE_SOURCE_TRACE.md).

The preliminary “integer object-state grid” label was too broad. The exact audited grid `eIntArrArr` is a packed **tile metadata grid**, paired with `fIntArrArr` during checkpoint save/restore. It contains metadata/reference relationships and must not be confused with the background/property grid `bIntArrArr`.

Two further distinct conditional feature tables are confirmed:
- `aLongArr[15]` object records ↔ `cLongArr[15]` checkpoint copy.
- `bLongArr[15]` auxiliary records ↔ `dLongArr[15]` checkpoint copy.

DEV-003AE gives exact save/restore line ranges for both 15-entry arrays. DEV-003AZ documents that tile metadata and both tables are restored together. DEV-003BL establishes the atomic object/auxiliary subsystem contract. These tables must remain separate and be restored as one coherent group with the tile metadata grid when the feature is active.

The Native isolated object/auxiliary table model is not a complete gameplay checkpoint provider. Reuse requires review against the full game-owner state model.

CS-2 remains in progress because exact stage-layer and per-cell byte-array copy/restore pairs, scalar/controller fields, and stage compatibility invariants still need tracing.


## 9. Byte-array checkpoint classification correction — 2026-11-21

AUDIT-002 §17.1 explicitly lists `bByteArrArr` among checkpoint-copied arrays. It does not list `cByteArrArr`, although §3 identifies `cByteArrArr` as a runtime animation/timing/state byte layer. The matrix now distinguishes these facts:

- `bByteArrArr`: checkpoint candidate, category inclusion supported; exact copy target and dimensions still need direct tracing.
- `cByteArrArr`: runtime state exists, but checkpoint inclusion is unproven. Keep it out of the checkpoint schema unless a direct `saveCheckpoint()` / `restoreCheckpoint()` path is found.

This prevents a runtime array from being assumed checkpointed merely because it exists in the stage model.


## 11. Scalar/controller and restore-order evidence update — 2026-11-21

See `DEV-003CS-2_CHECKPOINT_COPY_RESTORE_SOURCE_TRACE.md` §7 for the category-level scalar/controller inventory and restore-order outline.

Newly consolidated evidence:
- Player tile X/Y and checkpoint-related save byte 10 are captured/restored.
- Several player/gameplay counters, state bytes, world/stage progress and inventory-like fields are described by AUDIT-009, but their exact variable-by-variable ownership is unresolved.
- Currency/score-like counters are mentioned in AUDIT-002 §22; do not assume they share one semantic field or all belong in the stage checkpoint.
- Movement interpolation is deliberately settled before capture and reset on restore.
- Transient input/interaction/animation state is reset or reconstructed rather than blindly restored.
- Camera is recentered from logical player position; dirty/update markers are rebuilt after stage-grid restore.
- Object code 48 is reconstructed after grid restoration, with a mode/background-dependent path.

The generic “Per-cell integer object state” row is replaced with the specific tile metadata grid `eIntArrArr` / `fIntArrArr`. It must remain distinct from background layer `bIntArrArr`.

CS-2 remains open until every scalar assignment, all layer/byte-array copy pairs and dimensions, optional feature conditions, and progression/profile ownership are traced.


## 12. Durable save-record versus temporary checkpoint correction — 2026-11-21

See `DEV-003CS-2_CHECKPOINT_COPY_RESTORE_SOURCE_TRACE.md` §8.

Important correction: save-record byte 10 is a boolean-like gameplay state restored by checkpoint, **not a checkpoint index**. Save-record byte 3 maps to/from `azInt`; `azInt` is also decremented by the retry handler, so its semantic identity remains unresolved.

The durable save-record map (bytes 3–12) must not be conflated with the temporary in-memory stage checkpoint. In particular, bytes 4–7, 9, 11, and 12 are durable profile/shop/progression candidates, but checkpoint inclusion is not established by their save-record presence. Save byte 8 is a hero progression value restored by retry and must not be casually relabeled health.

Native schema rule: maintain separate durable-profile and in-memory-checkpoint ownership. Add a field to the checkpoint only when a direct capture/restore assignment or equivalent explicit evidence proves membership.

## 13. Exact source assignment ledger update — 2026-11-21

The direct source-level assignment ledger is now documented in DEV-003CS-2_CHECKPOINT_COPY_RESTORE_SOURCE_TRACE.md §9, using i.java at pinned revision 215c71d04057d66a1b08419b1a4b1043ae3e0881.

Exact pairs now recorded:
- 14 unconditional scalar pairs, including player tile X/Y, save byte 10, and opaque state fields.
- 14 additional scalar pairs guarded by lBoolean.
- caInt/amInt and cbInt/alInt are captured unconditionally but restored only under kByte == 1.
- mByteArr/oByteArr, four stage grids, conditional eIntArrArr/fIntArrArr, and two 15-entry long tables have exact copy/restore ranges.
- cByteArrArr is confirmed not copied by saveCheckpoint()/restoreCheckpoint(); bByteArrArr is copied via dByteArrArr.
- Restore-side camera computation, transient resets, dirty-cell rebuild, special-cell mutations, and object-code-48 reconstruction are explicitly separated from captured payload.

This closes the assignment-identification portion of CS-2 at static-source level. It does not close semantic naming, allocation/compatibility invariants, independent shipping-binary verification, or deterministic runtime round-trip validation.


## 14. Scalar ownership evidence update — 2026-11-24

See `DEV-003CS-2_CHECKPOINT_COPY_RESTORE_SOURCE_TRACE.md` §10.

New semantic classifications supported by source use sites:
- `aCInt`: checkpoint progression threshold; checkpoint tile handler compares encoded foreground value and advances it.
- `aaInt`: stage-object remaining count loaded from cell metadata and decremented during object removal.
- `cmInt`: selected door/lock group index tied directly to `mByteArr`; snapshot both coherently.
- `cfInt` + `ceInt`: coupled directional/phase state for a staged foreground mechanism; do not restore/reset independently.
- `aUInt` and `aVInt`: distinct pickup counters consumed by different foreground interactions; exact item names unresolved.
- `bbInt`: pickup/progression counter; exact identity unresolved.
- Optional `lBoolean` fields are initialized together in `arVoid()` and consumed by `asVoid()`; retain as one conditional sub-snapshot pending packed-record decoding.
- Prior tentative `ayInt` “spawn/original-X” label is withdrawn; it remains opaque runtime gating/position-related state.

This narrows schema ownership but does not close CS-2. Remaining work: trace all opaque scalar writers/reset points, decode optional-mode records, establish stage/profile ownership, and validate dimensions/compatibility and runtime round trips.


## 15. Optional mode lifecycle and packed records — 2026-11-24

See `DEV-003CS-2_CHECKPOINT_COPY_RESTORE_SOURCE_TRACE.md` §11.

Source evidence now establishes:
- `lBoolean` is activated by stage foreground trigger code 19; the first activation initializes the mode arrays and fields.
- Stage teardown resets the discriminator and nulls the four 15-entry long tables.
- `asVoid()` is gated by `lBoolean`.
- The 15-slot 64-bit record tables use packed fields with observed offsets 0, 4, 6, 13, 20, 26, 28, 31, 37, 43, 45, 47, 49, 54, and 57. This is an observed partial map, not a complete decoder specification.
- The `xByte` lifecycle includes idle/ready (3), operation setup (5), record processing (1→2), and completion/rebuild (4→2) paths.
- Optional-mode tables and their scalar control state must be captured/restored atomically with the stage grid and activation discriminator.

Still open: complete bitfield semantics, all transition writers, valid coordinate/slot invariants, auxiliary-table reader audit, and runtime/binary verification.


## 16. Active versus backup long-table ownership — 2026-11-24

A complete occurrence scan of the pinned source revision confirms:
- `aLongArr` and `bLongArr` are active 15-slot packed-record tables.
- `cLongArr` and `dLongArr` are only checkpoint backup buffers. They are allocated lazily during capture, copied from the active tables, and read during restore; no other runtime reader/writer was found.
- `aLongArrArr` is a separate 12×12 long grid and must not be conflated with `aLongArr`.
- Mode teardown nulls the four 15-entry tables; global/runtime initialization also allocates active tables.

Native schema should model active records and backup serialization separately; it should not treat the backup buffers as additional gameplay state.


## 17. Optional-mode lifecycle correction and writer audit — 2026-11-24

See source trace §12. Corrected an earlier interpretation of `arVoid()`:
- Trigger code 19 calls `arVoid()` while `lBoolean=false`, so the method's `if (lBoolean)` body does not execute on that path.
- Stage-load switch case 3 calls `arVoid()` only while `lBoolean=true`; this is the observed path that allocates/resets all four 15-entry long tables.
- Trigger code 19 then sets `lBoolean=true`, clears `alBoolean`, allocates `eIntArrArr` if absent, and creates the first record.
- The two entry paths must remain distinct in the Native behavioral model until runtime/original-binary validation resolves their intended relationship.

Additional field evidence:
- `duInt` counts player-layer cells with code 38 during `afVoid()`; it is the upper bound for the `dtInt` one-shot queued-work cursor.
- `dtInt` is consumed once per `alBoolean` pulse and resets with `duInt` when the queue is exhausted.
- `dxInt` is selected from neighboring stage-grid occupancy by `lVoid()`; `dvInt/dwInt` hold selected coordinates; `wByte` selects a packed-record slot; `dyInt` is a derived target coordinate.
- The `xByte` writer inventory now covers constructor, teardown, restore, mode setup, record completion, and input interaction paths.
- Packed bit accesses overlap (e.g. 7-bit offset 27 write versus 2-bit offset 26 read/write). The earlier bitfield list remains a set of observed accessors, not a canonical flat struct.

CS-2 remains open for full record graph/linked-slot invariants, stage-dimension safety, compatibility key, and runtime/binary verification.


## 18. Packed-record slot graph and validation — 2026-11-24

See source trace §13. New source-level findings:
- Active table capacity is 15. Allocator scans for free slots; one allocation path returns -1 on exhaustion, while a second path has no explicit capacity guard and may reach slot 16. Native must fail safely at capacity.
- Offset 54 (3 bits) stores a one-based linked-slot reference; zero is the no-link sentinel.
- Coordinate lookup `bByte(x,y)` returns a zero-based slot index. It must not be confused with the one-based link representation.
- Stage cell lanes contain a slot reference that must agree with the record's stored coordinates and sublayer.
- Record coordinates are 6-bit values and require validation against actual stage dimensions before grid access.
- Restore validation must check both forward record→cell/link references and reverse cell→record references before committing candidate state.
- The 3-bit lifecycle field at offset 28 controls update-loop branches; values 0–7 are observed, but semantic labels for 4–7 remain provisional.

CS-2 remains open for complete graph topology, all overlapping bitfield accessors, coordinate bounds under every stage mode, and runtime/binary validation.


## 19. Record lifecycle field refinement — 2026-11-24

See source trace §14. Offset 28 width 3 is per-record lifecycle/control, separate from global xByte, offset-0 record kind, and offset-47 operation mode.

Observed contexts:
- 0: allocator/free value.
- 1: initializing/movement transition.
- 2: active timed processing.
- 3: deferred clear; next slot scan clears entire 64-bit record.
- 5: auxiliary movement record created by xByte=5 setup; no dedicated switch case found in xByte=2 update loop.
- 6 and 7: movement/transition branch; state 7 has an additional boolean condition.
- 4: no writer or dedicated consumer found; keep unclassified.

Observed edges include 0→1, 0→7, 1→2, 2→3→0, 2→5, linked handoff 2→2 (target inherits link/context while source clears), 0→5, 6→2, and 0→6. These are guarded by stage-cell and mode conditions; they are not a simple linear enum.

Native restore must preserve raw records and validate links/cell backreferences before committing. Do not execute unknown lifecycle values as known behavior.


## 20. Accessor overload and backing-table correction — 2026-11-24

Source trace §15 separates three namespaces that must never be merged:
1. `aLongArr`: primary 64-bit records, one-based accessor API.
2. `bLongArr`: secondary 64-bit table, zero-based accessor API.
3. `eIntArrArr`: packed 32-bit stage cells, three 9-bit lanes per cell; lane-relative fields are slot ref 0:3, cell type/state 3:4, selector/flag 7:2.

Correction to prior offset summaries: 0:4, 4:2, 13:7 and 20:7 belong to secondary `bLongArr` evidence, not primary `aLongArr`. The 3:4 field is a stage-cell lane-relative field, not a primary 64-bit record accessor. The primary table's observed fields and overlaps are listed separately in source trace §15.2.

The primary record raw words must remain lossless. Do not build one flat struct across backing arrays or across the stage-cell lane coordinate system. Add golden vectors per accessor overload before implementation.


## 21. Dual-table ownership and atomic checkpoint scope — 2026-11-24

See source trace §16. Primary `aLongArr` and secondary `bLongArr` are separately allocated 15-word arrays with independent accessor index bases. They are not aliases and are not reconstructed from one another. During xByte=4, the primary table is scanned and matching secondary entries are cleared/used as a rebuild workspace. Both arrays are explicitly copied independently at checkpoint capture and restore (`aLongArr → cLongArr`, `bLongArr → dLongArr`).

The coupled optional-mode checkpoint also includes packed stage-cell grid `eIntArrArr` (backup `fIntArrArr`) and phase/scalar fields. Persisting only the primary table is incomplete. Native round-trip must preserve all 30 raw words and the coupled grid/scalars atomically, while keeping the source index conventions at the accessor boundary.


## 22. Secondary path-record field evidence — 2026-11-24

See source trace §17. The four-argument allocator initializes a free `bLongArr` word with:
- offset 0 width 4 = 1 (occupied/type marker in observed allocator path),
- offset 4 width 2 = fourth argument,
- offset 6 width 7 = first argument,
- offset 13 width 7 = second argument,
- offset 20 width 7 = third argument,
- offset 27 width 7 = first argument again.

The 7-bit fields at 6, 13, 20, 27 are adjacent within the secondary 64-bit word. `bByte(x,y)` scans all 15 secondary slots by offsets 13:7 and 20:7 and returns zero-based slot or -1. A later setup call locates the coordinate and sets offset 0:4 to 2. When `alBoolean` is active, the update path walks secondary entries in index order from `dtInt` through `duInt`, reading coordinates and calling `mVoid`.

Do not apply the primary-table offset-28 lifecycle enum to this table. The meaning of offset 4 and values at offsets 6/27 remains contextual. Preserve raw words and test allocator, lookup, ordered traversal, and state marker transitions.


## 23. Primary/secondary pointer and allocator failure evidence — 2026-11-25

See source trace §18. Primary record offset 54 width 3 is a zero-based pointer into the first eight addressable `bLongArr` slots (0..7), not a 1-based pointer to all 15 entries. Active primary processing follows this pointer and updates secondary offset 6:7 as path progress changes. Secondary offset 27:7 retains its initial allocator value in the inspected path.

The secondary allocator returns -1 when all 15 slots are occupied, but the xByte=5 setup call ignores the allocator return and relies on coordinate lookup succeeding. Primary allocator has no explicit full-table failure return in the inspected helper; with all 15 primary slots occupied, its candidate advances to 16 before indexing the array. Native code must replace both unsafe cases with typed failure and atomic no-mutation semantics.

Tests required: allocator full-table behavior for both tables, xByte=5 lookup miss, offset54 values 0..7, progress update isolation to secondary offset6, preservation of offset27, and rollback across table/grid mutations.


## 24. Stage dimensions and restore bounds — 2026-11-25

See source trace §19. The candidate stage loader reads width and height from two unsigned 16-bit header values and allocates the player, foreground, background, and per-cell byte layers as `[width][height]`. Checkpoint buffers for background, player, foreground, and byte state use the same dimensions. Capture/restore copy `width` rows and `height` elements per row; the door/lock byte buffer uses `mByteArr.length`.

Restore also contains branch-specific fixed-coordinate accesses:
- q-branch: `dIntArrArr[37][7]` and `[39][5]` require width >= 40, height >= 8.
- r-branch: `dIntArrArr[46][7]` and `[50][7]` require width >= 51, height >= 8.
- animator-slot-2 branch: `crtStagePlayerLayer[18][63]` requires width >= 19, height >= 64.

These are branch preconditions, not universal minimum dimensions for every stage. Validate the applicable branch before candidate restore and before mutating live state. The loader's flat-buffer mapping uses `x + y * width`; do not transpose without separate evidence.

`cByteArrArr` is allocated at stage load but is absent from the inspected checkpoint copy pairs. Keep it excluded. CS-2 remains open: actual stage-resource dimension inventory, all setup-path allocation guarantees, opaque scalar ownership, compatibility-key evidence, and runtime/shipping-binary validation are incomplete.


## 25. Complete candidate stage-resource dimension inventory — 2026-11-25

Source trace §20 records the full binary evidence. The pinned candidate resources contain 41 stages: Angkor `w0.bin` 14, Scotland `w1.bin` 13, Tibet `w2.bin` 14. Each record is a four-byte little-endian unsigned-16-bit width/height header followed by exactly three layers of `width * height` bytes. All three files parsed to exact EOF with no truncated payload or trailing bytes.

Dimension ranges: width 23–104; height 11–75. The only stage meeting the restore animator-slot-2 bounds (width >= 19, height >= 64) is Angkor world 0 stage 5 (30×75). q-branch bounds (width >= 40, height >= 8) match 23 stages; r-branch bounds (width >= 51, height >= 8) match 9. These are bounds-only counts, not proof of runtime branch activation.

Native must retain per-stage dimensions, checked allocation arithmetic, exact layer order/indexing, and branch-specific prevalidation before mutation. The inventory does not prove all retail variants, profile ownership, or snapshot compatibility semantics. CS-2 remains in progress.


## 26. Restore branch activation and malformed stage-loader behavior — 2026-11-25

Source trace §21 adds direct activation evidence. q/r flags start false and are reset during stage setup. During gameplay mode `kByte == 2`, a player standing on a foreground cell with low byte zero sets q when its high-byte code is 13, or r when the code is 16. Restore then mutates fixed coordinates and resets the active flag. Thus dimension eligibility counts in §25 are only bounds facts, not activation evidence.

The stage loader does not check the byte counts returned by `read(header)`, layer reads, or `skip()`; its outer loop has no explicit EOF exit. The source comments that requesting a non-existent stage can freeze. Native must use exact reads/checked cursor movement, validate stage index and dimensions before allocation, return typed failures for missing/truncated/out-of-range resources, and keep loading candidate-first so failure cannot partially replace active state.

Animator slot 2 is checked for non-null during restore and then writes player-layer cell [18][63]. Assignment is at candidate source line 4992; full creation/destruction path remains to be traced. CS-2 remains open.


## 27. Animator slot-2 lifecycle and q/r metadata boundary — 2026-11-25

Source trace §22 establishes:
- Slot 2 is assigned by the `/mm0.f` sprite chunk loader, not created by stage restore. Chunk 1 creates the animator and selects animation 0; chunk 2 falls through and reassigns slot 2 while also loading texture 27 and slot 1.
- The global graphics/resource cleanup loop frees and nulls animator slots 0–5, including slot 2. No separate slot-2 null assignment was found in the inspected source revision.
- Restore only checks non-null, resets slot 2 to animation 0, and writes player-layer [18][63]. The slot is an optional asset-lifecycle dependency, not a persisted stage identifier. Validate the coordinate only when the branch is active.
- q/r trigger codes are composite foreground int values with low byte zero and high-byte 13/16. Initial stage foreground bytes alone do not establish the high-byte producer. Keep these codes opaque until every foreground writer chain is traced.

CS-2 remains open; no Native provider or gameplay-state implementation is authorized by these findings.


## 28. q/r trigger producer and resource candidate cells — 2026-11-26

The q/r composite trigger producer is now identified. Stage initialization case 0 combines the raw foreground byte with the signed background-layer byte: `foregroundInt = (background << 8) | (foreground & 0xFF)`. For foreground byte 0, background value 13 sets q when the player reaches the cell in gameplay mode 2; background value 16 sets r. These are background metadata values, not foreground tile IDs 13/16.

Scanning the three pinned world resources in loader layer order found exactly three raw candidate cells, all in Angkor world 0 stage 13 (68×11): (0,0) background 16/player 85; (37,7) background 13/player 255; (46,7) background 16/player 255. Scotland and Tibet have no matching cells. This is static resource evidence only; runtime reachability and actual flag activation remain unverified.

Native must preserve signed-byte widening, keep low-byte foreground identity separate from high-byte metadata, and test these golden vectors. The foreground assignment inventory is not yet exhaustive because local row-array aliases and helper mutation paths still need a write-set audit. CS-2 remains open.


## 29. Foreground packed-cell mutation inventory — 2026-11-26

Source trace §24 catalogs direct foreground writes and confirmed row-alias writes. Mutation families include raw stage normalization; player/object interaction; door and lock metadata; moving/animated objects; connected 8/9 mechanism flags; bulk clear operations; top-nibble animation/event updates; and checkpoint restore. The inventory distinguishes read-only q/r trigger checks from writes and distinguishes initialization/restore from gameplay mutation.

Confirmed row aliases mean a direct global-name assignment search is insufficient: local `int[]` references to `crtStageForegrondLayer[row]` are mutated in helpers such as `iVoid`, animation update, and object interaction. The foreground cell is a packed signed 32-bit value: low-byte code, contextual bits 8–27, and top-nibble flags 28–31. Signed right shift, unsigned masking, and byte casts have distinct semantics. Example negative composite `(-193 << 8) | 7` must round-trip exactly.

This is a substantial mutation-family inventory, not a formally exhaustive call graph for every helper accepting row arrays. Continue helper/call-site analysis and preserve lossless raw cell values in Native. CS-2 remains open.


## 30. Foreground helper call graph and cross-layer ownership — 2026-11-26

Source trace §25 refines the helper inventory:
- `iVoid(x,y)` replaces low byte with 14 while preserving upper 24 bits and sets a gameplay flag.
- Recursive `aVoid(x,y,dx,dy,mode)` traverses connected low-byte 8/9 cells and toggles upper metadata bit 0x200. Door open/close helpers invoke it in both horizontal directions with different modes.
- `nVoid(tile)` replaces low byte while preserving upper value and may set/increment animation metadata.
- `jVoid(x,y)` sets top-nibble bit 28; update logic advances/clears the animation nibble.
- `aiVoid()` reconstructs player-layer code 48 and metadata; it is not a foreground writer.
- `kVoid(x,y)` updates renderer/animator state and is not itself a foreground writer in the inspected body.

The same object update may mutate foreground, player occupancy, stage metadata, and timing/dirty arrays. These are distinct state domains; do not fold renderer handles or dirty counters into the packed foreground schema by proximity. Preserve exact 32-bit signed cell values. Replace recursive traversal with bounded iterative traversal only if source visitation semantics are retained. CS-2 remains open.


## 31. Cross-layer snapshot ownership matrix — 2026-11-26

The checkpoint copy/restore code provides a decisive distinction among four stage grids:

| Grid | Snapshot? | Evidence-based role |
|---|---|---|
| `bIntArrArr` | Yes | Background-derived packed gameplay/object metadata |
| `crtStagePlayerLayer` | Yes | Player/object occupancy codes |
| `bByteArrArr` | Yes | Runtime movement/progress/timing counters that affect gameplay |
| `cByteArrArr` | No | Derived dirty/update scheduling markers |

All are allocated at stage width × height. The three resource layers are player, background, foreground; the two byte auxiliary grids are runtime-created, not source layers. Capture copies the first three authoritative grids plus foreground; restore copies them back, then invokes renderer/update invalidation. `cByteArrArr` is initialized and mutated with marker values 24/30/48/127 but excluded from capture/restore, supporting a derived-state classification.

`bIntArrArr` is a packed 32-bit state grid with overlapping/context-specific fields; `crtStagePlayerLayer` and the auxiliary bytes require signed-byte fidelity. `bByteArrArr` affects movement and state progression, so it is not merely visual. Do not give either byte grid one global semantic label. CS-2 remains open for bitfield decoding, scheduler cadence, and dimension/compatibility invariants.


## 32. Scheduler and cross-grid transaction refinement — 2026-11-26

Correction/refinement to §31: `cByteArrArr` is excluded from the checkpoint and is rebuilt by restore, but it is not merely a renderer dirty bitmap. `ahVoid()` scans a player-centered region, processes cells whose scheduler byte is positive, decrements the byte by 6, and dispatches object/foreground update logic. Values 24 and 48 represent multi-pass scheduling windows; `kVoid()` writes 48 into a 3×3 neighborhood. Values 30 and 127 also occur but their complete lifecycle remains unresolved. The original restore scans restored player/foreground cells and calls `kVoid()` to rebuild scheduler work. This proves deliberate reconstruction, not exact mid-tick phase equivalence.

`bByteArrArr` remains authoritative checkpoint data but is context-dependent progress/timing state: values such as 18 may be decremented by 6, while other object families increment, reset, threshold-check, or use it as animation displacement/frame input. Preserve raw signed bytes; do not name the whole grid “animation timer”.

`bIntArrArr` exposes overlapping context-specific masks (low 3 bits, bit 3, bits 3–5, 5–12, 10–12, 13–20, 22–27 and top-byte/sign metadata). These are not a canonical struct. Preserve all 32 bits and define per-consumer views.

Snapshot/restore must treat foreground, player occupancy, packed metadata, and progress bytes as coupled authoritative grids, mutated transactionally on the game-owner thread. Capture requires a consistent tick boundary or immutable state view. Restore validates first, commits authoritative grids, then rebuilds scheduler/renderer-derived state. Exact mid-tick replay remains unproven.


## 33. Scheduler value writer inventory — 2026-11-26

A source-wide direct-assignment scan (candidate `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`) found scheduler writes for values 0, 24, 30, 48 and 127. The main contexts are stage initialization, special background normalizer case 3, player/object code initialization, player movement, door/lock transitions, multi-cell mechanism updates, optional packed-cell edits, and explicit cancellation. See Source Trace §28 for the region-by-region writer inventory.

Important corrections:
- 127 is only proven as a normalizer initialization value; it is not yet a semantic “127 tick timer”.
- 24, 30 and 48 are distinct scheduling requests used by different object paths. The scan decrements a positive byte by 6 before dispatch, but handlers can overwrite, clear, or reschedule it. Value divided by six is not a universal duration formula.
- `kVoid` writes 48 to a 3×3 neighborhood without its own bounds checks; valid call-site coordinate constraints still need exhaustive proof.
- Scheduler state is transient and excluded from checkpoint; restore reconstructs it from authoritative grids. Exact mid-tick equivalence remains unproven.


## 34. Scheduler dispatch and `kVoid` bounds evidence — 2026-11-26

The inspected `ahVoid()` switch confirms distinct handlers for foreground low-byte codes 36, 35/37, 32, 26, 6, 33/14, 2, 3, 30/0, 7, and 8/9. These handlers can reschedule, cancel, mutate multiple authoritative grids, or dispatch other helpers after the initial scheduler decrement by 6. See Source Trace §29 for per-case behavior and test vectors. This list is explicitly partial, not the complete switch.

The two-coordinate `kVoid(x,y)` helper writes scheduler value 48 to all nine cells in a 3×3 neighborhood without bounds checks. Restore's caller is proven safe because it iterates only interior cells. Other call sites include stage normalization, movement/input, object transitions, and optional packed-cell edits; their preconditions are not all proven. Native must centralize bounds validation and test edges/corners rather than assuming every call is safe.

CS-2 remains open: complete switch coverage, all call-site coordinate proofs, object-family counter lifecycle, and tick-boundary restore equivalence remain outstanding.


## 35. Player-layer scheduler dispatch inventory — 2026-11-26

`ahVoid()` does not stop after foreground dispatch. For each eligible cell, it decrements scheduler by 6, runs the foreground low-byte switch, then usually runs a second switch on `crtStagePlayerLayer[x][y]` at the same coordinate. Labelled early exits can skip this second dispatch. This ordering is part of the runtime contract.

Player-layer codes with significant observed behavior include 54 (3×3 spread/activation), 46 (falling/moving object), 45 (animation/hazard and relocation), 14 (directional mover), 21 (directional paired chain), 16 (paired neighbor state), 37/30 (counter propagation), 48 (moving object reconstruction or `dVoid`), and 10 (conversion to foreground 32). Other codes dispatch to shared helpers `eVoid`, `lVoid`, `mVoid`, `aqVoid`, `akVoid`, `anVoid`, and `amVoid`. See Source Trace §30 for the observed code inventory and per-code details.

This confirms multi-grid update coupling and requires Native to preserve foreground-then-player-layer dispatch order, labelled early exits, and atomic mutations. Shared helper semantics and full boundary proofs remain open. CS-2 remains in progress.


## 36. Shared helper signature separation — 2026-11-27

Overloaded obfuscated method names were separated by exact signatures. The byte overload `eVoid(byte)` is a moving player-layer object transition that mutates occupancy, packed metadata, progress, and scheduler. `dVoid(byte)` performs a directional transfer/continuation and scheduler updates. `lVoid(int)` is a player-contact action dispatcher; `lVoid(int,int)` is a separate optional-mode coordinate helper. `mVoid(int)` handles player-contact progression/action or world-stage transition; `mVoid(int,int)` looks up/allocates a secondary packed record. `akVoid()` is a phase-driven foreground mechanism, while `amVoid()` is a distinct direction/phase-driven player-layer movement handler. `anVoid()` and `aqVoid()` have multi-grid propagation/interactions and remain partially traced.

Do not merge overload semantics by name. See Source Trace §31 for observed contracts, source ranges, and remaining gaps. All descriptions are static-source findings; no Native runtime behavior has been implemented or validated.
