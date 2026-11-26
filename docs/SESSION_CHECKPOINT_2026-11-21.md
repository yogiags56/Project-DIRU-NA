# DIRU-NA Session Checkpoint — 2026-11-21

> Recovery checkpoint for continuing work if the ChatGPT conversation is interrupted, becomes full, or errors. Read this file first, then verify the live branch head and CI before editing.

## Repository state

- Repository: `yogiags56/Project-DIRU-NA`
- Working branch: `dev-003cr-snapshot-provider-boundary`
- Last verified content HEAD before this checkpoint refresh: `d019cc911de06f0bd31ea5025b55bdef5bd8895a`
- PR #1: https://github.com/yogiags56/Project-DIRU-NA/pull/1
- PR #1 was still open, draft, and unmerged at last verification. Do not merge without explicit authorization.
- Main branch is not the working branch. All current DEV-003CS proposal and progress documents are on the working branch.

## Project identity and constraints

- Project: DIRU-NA (Diamond Rush Native Android), independent native Android reconstruction.
- Package/namespace: `MeongDev.DIRUNA.GloftDIRU`.
- Core gameplay logic belongs in plain Java `:core`; Android lifecycle/storage belongs in `:app`.
- Do not use FreeJ2ME as the game runtime.
- Initial logical viewport: 240×320 portrait; adaptive ratios later, preserving proportions.
- Evidence-led reverse engineering only. Candidate decompiled source is static evidence, not runtime truth. Do not redistribute proprietary code/assets.
- Do not invent opaque field semantics. Keep unresolved fields out of a production schema.
- Original candidate `saveCheckpoint()/restoreCheckpoint()` is an in-memory respawn snapshot, not evidence of durable RMS serialization.

## Completed work

### DEV-003CR — Snapshot Provider & Lifecycle Integration Boundary
Source-level exit gate met:
- Detached snapshot/persistence contract and lifecycle boundary.
- Ordered async persistence dispatcher and callback/concurrency tests.
- Android private storage adapter using AtomicFile.
- Candidate-first restore and non-destructive handling of corrupt/truncated/empty/unsupported/oversized checkpoints.
- Size-boundary preservation tests.
- Injected mid-write IOException test verifies AtomicFile rollback preserves previous valid checkpoint.
- Core JVM tests, Android storage tests, and debug APK assembly passed for verified CR runs.
- Physical-device durability/process-kill validation remains separate and unverified.
- Live production gameplay provider remains unavailable until a real Native gameplay state owner exists.

Important docs:
- `docs/DEV-003CR_SNAPSHOT_PROVIDER_LIFECYCLE_BOUNDARY.md`

Latest known successful CR workflows:
- rollback test: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44848229552
- CR documentation: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44848243427
- later CS-1 docs and roadmap runs at HEAD `ed3b431e`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44849471922
- Recheck latest workflow after current commits; do not assume current HEAD has passed until confirmed.

### DEV-003CS — Gameplay State Snapshot Model & Provider Foundation
This milestone was not previously defined in recovered history. It was created as an explicit proposal and remains subject to project-owner approval; do not describe it as an already approved historical milestone.

Proposal:
- `docs/DEV-003CS_GAMEPLAY_STATE_SNAPSHOT_PROVIDER_FOUNDATION.md`

CS-1 completed as an architecture/evidence document:
- `docs/DEV-003CS-1_GAMEPLAY_STATE_OWNERSHIP_LIFECYCLE.md`
- Separates application screen/navigation state, gameplay simulation, input, incremental stage loading, rendering, Android lifecycle, and persistence I/O.
- Proposes single-writer Native game-owner thread.
- Defines safe capture and candidate-first atomic restore flow.
- No production gameplay owner/provider has been implemented.

CS-2 currently in progress:
- `docs/DEV-003CS-2_PERSISTED_FIELD_EVIDENCE_MATRIX.md`
- Initial field-category inventory, evidence references, include/exclude/conditional classification, proposed minimum schema, validation rules, and unresolved questions drafted.
- It is NOT a final schema and NOT ready for production provider implementation.
- Roadmap updated to mark CS-2 in progress.

## Current work

Continue CS-2 semantic ownership, optional-mode lifecycle, and invariant audit (updated after primary/secondary pointer handoff and allocator capacity hazard audit at `d019cc91`):
1. Exact assignment ledger is complete at static-source level in source trace §9. Semantic ownership update in §10 classifies `aCInt`, `aaInt`, `cmInt`, `cfInt/ceInt`, pickup counters, and optional mode state. Remaining: opaque fields, allocation/compatibility invariants, and independent/runtime validation.
2. Enumerate dimensions and allocation paths for all stage layers and byte arrays; exact row copy extent is width rows × height elements.
3. Confirmed exact mapping: `bIntArrArr`→`cIntArrArr`; `bByteArrArr`→`dByteArrArr`; player layer→`eByteArrArr`; foreground→`dIntArrArr`; `cByteArrArr` has no copy path in these methods. Optional mode uses two active 15-entry packed-record tables and two capture/restore-only backup buffers; do not confuse `aLongArr` with separate `aLongArrArr` 12×12 grid.
4. Preserve strict separation between durable profile save and in-memory checkpoint; continue opaque scalar ownership and `kByte`/`lBoolean` mode lifecycle trace.
5. Optional mode activation order corrected: trigger-19 calls arVoid while inactive (no-op); stage-load case 3 calls it while active and allocates/resets tables. Full scalar writer inventory and dtInt/duInt queued-work semantics recorded. Packed-record slot allocation/link semantics, reverse cell references, and per-record lifecycle states/transitions 0–7 now traced at source level. Primary and secondary tables are independent 15-word arrays and explicitly checkpointed. Secondary bLongArr allocator initializes marker 0:4, argument 4:2, fields 6:7, 13:7, 20:7, 27:7. Coordinate lookup uses 13:7/20:7; ordered update traversal uses dtInt..duInt. Primary offset54:3 is a zero-based pointer to bLongArr slots 0..7; primary processing updates linked secondary offset6:7 progress while offset27:7 retains initial value in observed path. Secondary allocator returns -1 when full but one xByte=5 caller ignores it; primary allocator can index past end when full. Native code must use typed failure and atomic no-mutation semantics. Continue validating xByte=5 failure paths, state-5 path, exact xByte=4 helper chain, coordinate invariants, and runtime/golden vectors.
6. Define canonical world/stage IDs and compatibility rules.
7. Reconcile snapshot schema with first reproducible gameplay milestone in DEV-002.
8. Update matrix and roadmap; keep CS-2 in progress until all critical questions are answered.

## Planned after CS-2

- CS-3: Native gameplay state model seam. Depends on actual first reproducible gameplay owner from DEV-002. Do not create a fake placeholder production owner.
- CS-4: Immutable snapshot provider, only after real owner and schema exist.
- CS-5: Deterministic capture/restore/thread/round-trip tests.
- CS-6: Android lifecycle requests routed to game owner; no Activity direct reads of mutable state.
- CS-7: CI, documentation, acceptance review.
- Separately: physical-device validation for process kill/storage durability.
- Separately: PR #1 review/merge after authorization.

## Latest commits from this session

- `313c6da4` — document primary-to-secondary pointer, progress updates, xByte=5 handoff, and both allocator capacity hazards.
- `d019cc91` — add pointer-width and capacity-failure evidence to the matrix.

- `771fd9e3` — document secondary-table allocator schema, coordinate lookup, and sequential traversal evidence.
- `bb2aa92f` — add secondary path-record field evidence to the matrix.

- `7630dd0f` — trace independent allocation, xByte=4 rebuild workspace use, and paired checkpoint copies of primary/secondary tables.
- `4af28bcd` — add dual-table ownership and coupled checkpoint scope to the evidence matrix.

- `e5ed6e05` — separate 64-bit record accessors from 9-bit stage-cell lane accessors; initial catalog.
- `1c1fc91e` — correct catalog by backing table and overload after source call-site audit.
- `38ca4f9a` — mirror corrected accessor namespaces in the evidence matrix.

- `87136cb4` — document record lifecycle states 0–7, guarded transitions, handoff semantics, and separation from xByte/kind/operation fields.
- `8ff64c00` — update evidence matrix with per-record lifecycle transition evidence.

- `b4b6950a` — map 15-slot allocation, one-based link references, zero-based coordinate lookup, cell backreferences, and capacity hazards.
- `a84194e1` — update evidence matrix with packed-record graph and restore validation requirements.

- `d95a9b93` — correct optional-mode activation ordering and add full scalar writer/transition inventory, queued-work semantics, and overlapping bitfield-access warning.
- `e13ed13d` — update persisted-field matrix with corrected lifecycle and writer evidence.

- `580d7246` — verify active versus checkpoint-backup ownership of the optional mode long tables.
- `75f71dd9` — record active/backup table distinction in the persisted-field matrix.

- `683bfcc5` — document optional mode activation/teardown, 15-slot packed 64-bit records, bit offsets, and xByte lifecycle.
- `4afcd02f` — update persisted-field matrix with optional mode lifecycle and packed-record evidence.

- `30290c65` — trace semantic ownership of checkpoint scalar fields using source writers/readers.
- `7023cb1f` — update persisted-field evidence matrix with newly supported semantic classifications.

- `d6927033` — add exact source-level scalar, mode-conditional, stage-grid, and byte-array assignment ledger with original source line ranges.
- `95f9e30d` — update persisted-field matrix with exact assignment evidence and corrections.

- `f41da7b9` — add category-level scalar/controller and post-restore reconstruction audit to CS-2 source trace.
- `ad4a04ce` — align persisted-field matrix with tile-metadata distinction and scalar/controller evidence.

- `637acae2` — refine copy/restore trace: `bByteArrArr` has checkpoint category evidence; `cByteArrArr` checkpoint inclusion remains unproven.
- `1414300b` — update field matrix with the same byte-array inclusion distinction.

- `8d7b0680` — add CS-2 copy/restore trace to the working branch (the trace was kept off `main`; an initial misplaced copy on `main` was immediately removed in cleanup commit `20161a02`).
- `0f63c3e1` — checkpoint recovery document updated with trace location.
- `7a1553fc` — checkpoint recovery document updated with latest byte-array evidence; this was the content HEAD before the current checkpoint refresh.

- `c35fbfba` — add CS-2 exact copy/restore trace for metadata and conditional object/auxiliary tables.
- `a163dd3a` — correct CS-2 field matrix: `eIntArrArr` is tile metadata; add `aLongArr[15]` and `bLongArr[15]` conditional snapshot tables.

- `72ad625b` — refine CS-2 matrix: background layer included as candidate, separate byte-state arrays, moving-object state row.
- `15b30bf3` — record evidence refinement and deferred checkpoint safe-point finding.

- `2a31f462` — CS-1 ownership/lifecycle document.
- `f12c2212` — CS-1 completion recorded in DEV-003CS proposal.
- `ed3b431e` — roadmap CS-1 progress; CI passed (run 44849471922).
- `31344ade` — initial CS-2 persisted field matrix.
- `c8b76c5a` — roadmap marks CS-2 in progress.

## Immediate resume procedure

1. Fetch branch `dev-003cr-snapshot-provider-boundary` and confirm current HEAD.
2. Fetch latest Actions runs for that branch and confirm CI status for latest HEAD. If still running, inspect again later; if failed, inspect job logs and fix only the actual defect.
3. Read this checkpoint, the CS-2 matrix, AUDIT-004, AUDIT-009, AUDIT-010, AUDIT-012, and the CS-1 ownership document.
4. Continue CS-2 source trace; do not skip ahead to provider implementation.
5. After any substantial work, update this checkpoint and roadmap with exact commit SHA, tests, and remaining tasks.

## Safety rules for future sessions

- Always create/update a repository checkpoint before ending a substantial work session.
- Never claim a test, CI run, commit, PR state, or merge unless verified from GitHub.
- Do not merge PR #1 or change branches without explicit authorization.
- Do not overwrite the last known good checkpoint or silently reset state on failed restore.
- Do not implement against guessed decompiler field meanings.


## Resume update — 2026-11-25 (stage-dimension audit)

### Verified repository state
- Working branch: `dev-003cr-snapshot-provider-boundary`.
- Verified branch HEAD before this documentation update: `4cf7723ecdc45b94631585ea1cf3fc8317ab9d46`.
- PR #1 remains open, draft, and unmerged per the last verified checkpoint; do not merge without explicit authorization.
- The workflow associated with HEAD `4cf7723ecdc45b94631585ea1cf3fc8317ab9d46` was observed completed with conclusion `success` (Android Build and Core Tests). This is not a result for the new documentation commits.

### New CS-2 work completed
- Inspected the pinned candidate source's stage header parsing and layer allocation.
- Confirmed dimensions are read as two unsigned 16-bit values and live grids are allocated `[width][height]`.
- Confirmed checkpoint buffers `cIntArrArr`, `dByteArrArr`, `eByteArrArr`, and `dIntArrArr` use the same dimensions; capture and restore copy width rows and height elements.
- Confirmed `oByteArr` mirrors `mByteArr.length`.
- Confirmed source flattening uses `x + y * width`; do not transpose based on assumptions.
- Identified branch-specific restore coordinates: q-branch width >= 40/height >= 8; r-branch width >= 51/height >= 8; animator-slot-2 branch width >= 19/height >= 64. These are conditional branch preconditions, not universal stage minima.
- Reconfirmed `cByteArrArr` is allocated but not included in the inspected checkpoint copy/restore methods.
- Added evidence and remaining tasks to the CS-2 source trace and persisted-field matrix.

### Current CS-2 focus
1. Inventory dimensions across available stage resources and reconcile with fixed-coordinate restore branches.
2. Trace every stage setup branch for allocation/nullability guarantees before capture and restore.
3. Continue opaque scalar writer/reader/reset ownership and stage-vs-profile progression classification.
4. Define compatibility-key fields only after direct source evidence.
5. Keep CS-2 open until deterministic Native validation and independent runtime/binary evidence are available.

### Validation boundary
- This update is documentation/source-audit work only; no Native implementation or new automated tests were added.
- Recheck GitHub Actions for the new documentation commit(s) before treating CI as green.
- Preserve the previous last-known-good commit; do not merge PR #1 or change branches.


## Resume update — 2026-11-25 (41-stage binary inventory)

### Work completed
- Retrieved and decoded all three pinned candidate stage resources as Base64.
- Parsed 14 Angkor stages from `w0.bin`, 13 Scotland stages from `w1.bin`, and 14 Tibet stages from `w2.bin`.
- Verified exact file exhaustion: 47,686 / 46,956 / 56,887 bytes respectively; no truncated stage payload and no trailing bytes.
- Recorded all 41 dimensions and binary Git blob SHAs in source trace §20.
- Confirmed candidate width range 23–104 and height range 11–75.
- Reconciled fixed-coordinate predicates: q-branch bounds match 23 stages; r-branch 9; animator-slot-2 bounds match only Angkor world 0 stage 5 (30×75). These are bounds-only, not proof of runtime branch activation.
- Updated source trace §20 and Evidence Matrix §25.

### Current CS-2 focus
1. Trace every stage-load path and allocation/nullability guarantees, including malformed/truncated resource handling.
2. Trace q/r flag and animator-slot-2 activation sites; never infer activation from dimensions.
3. Continue opaque scalar ownership and world/stage/profile separation.
4. Define snapshot compatibility identity only from direct evidence.
5. Keep Native provider implementation blocked until a real gameplay state owner exists.

### Validation boundary
- Binary parsing was executed against the pinned candidate resources. This is static candidate-resource validation, not runtime gameplay or retail-binary validation.
- Documentation-only update; no Native code or automated tests added. CI must be checked for the final documentation commit.
- Do not merge PR #1 or change branches without explicit authorization.


## Resume update — 2026-11-25 (restore activation and loader failure audit)

### New source findings
- q/r activation is runtime-driven: while `kByte == 2`, player foreground low byte 0 plus high-byte code 13 sets q; code 16 sets r (candidate source line 9561–9572).
- Restore consumes q/r and clears fixed cells; dimension eligibility does not prove activation.
- Animator slot 2 is checked for non-null and reset; restore writes player-layer [18][63]. Assignment site is line 4992 and requires deeper lifecycle trace.
- Stage loader ignores short-read/skip counts and has no explicit EOF exit in its outer loop. Source comment identifies freeze for non-existent stage. Native must use exact reads, bounds checks, typed errors, and candidate-first commit.
- Added source trace §21 and Evidence Matrix §26.

### Next
1. Inspect animator slot-2 assignment context and lifecycle.
2. Trace foreground high-byte codes 13 and 16 to their stage-resource origins and mechanics.
3. Define typed Native resource parser failures and transactional stage replacement.
4. Continue opaque scalar ownership and world/stage/profile separation.
5. Reconcile compatibility identity after direct evidence.

No Native runtime/provider implementation or new automated tests were added. Keep CS-2 in progress and PR #1 unmerged.


## Resume update — 2026-11-25 (animator asset lifecycle)

### Work completed
- Traced the sole direct assignment to `aClassbArr[2]`: the `/mm0.f` chunk loader. Chunk 1 creates slot 2 and sets animation 0; chunk 2 falls through and assigns slot 2 again while loading texture 27 and slot 1.
- Traced broad resource teardown: slots 0–5 are freed and nulled, including slot 2. No separate slot-2 null assignment was found in the inspected source revision.
- Confirmed restore does not serialize animator state; it resets slot 2 to animation 0 and writes player-layer [18][63] when slot 2 is non-null.
- Clarified q/r values are composite foreground ints (low byte 0, high-byte code 13/16). Raw stage foreground bytes do not alone prove their producer; keep semantic labels unresolved.
- Added Source Trace §22 and Evidence Matrix §27.

### Next
1. Enumerate all writes to `crtStageForegrondLayer` and trace composite high-byte 13/16 producers.
2. Inspect slot-2 chunk fallthrough and replacement semantics; do not port accidental fallthrough without an explicit asset-loading model.
3. Continue opaque scalar ownership and world/stage/profile separation.
4. Define compatibility identity only after direct evidence.
5. Keep CS-2 open; no placeholder gameplay provider.

This is static source analysis only. No Native implementation or automated tests were added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (q/r producer resolved)

### Work completed
- Confirmed stage resource layer order: player, background, foreground.
- Identified exact q/r producer in stage normalization case 0: `(background << 8) | (foreground & 0xFF)`.
- q is generated by raw foreground byte 0 + background byte 13; r by raw foreground byte 0 + background byte 16, then checked during gameplay mode 2 at the player's current cell.
- Scanned all three pinned world binaries using exact loader layer order. Found 3 candidate cells, all Angkor world 0 stage 13 (68×11): (0,0) bg16/player85; (37,7) bg13/player255; (46,7) bg16/player255. No matching cells in Scotland or Tibet.
- Runtime reachability/activation remains unverified; player byte 85 remains opaque.
- Added Source Trace §23 and Evidence Matrix §28. Full foreground write-set is still incomplete because aliases/helper mutations need tracing.

### Next
1. Finish foreground mutation write-set including row-array aliases and helper methods.
2. Audit signed byte widening and raw-to-normalized stage conversion semantics.
3. Continue world/stage/profile ownership and opaque scalar audit.
4. Define snapshot compatibility identity only from direct evidence.
5. Keep CS-2 open; no provider implementation until real Native gameplay owner.

Static candidate-resource analysis only; no runtime behavior or retail variants claimed. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (foreground write-set audit)

### Work completed
- Enumerated direct global foreground references and identified confirmed local row-array aliases.
- Classified stage raw loading/normalization, gameplay interaction, door/lock updates, moving/animated object updates, bulk clears, top-nibble event/animation updates, and checkpoint restore as separate mutation families.
- Confirmed helper `iVoid` mutates through local row aliases by masking low byte and OR-ing 14.
- Confirmed animation/object update code mutates rows through aliases for tile codes 32/34/35/36/37 and high-byte/top-nibble animation fields.
- Reaffirmed packed signed 32-bit cell semantics: low byte, contextual bits 8–27, top nibble 28–31; signed shift, unsigned mask, and byte cast differ.
- Added Source Trace §24 and Evidence Matrix §29.

### Next
1. Build helper/call-site graph for every method receiving/capturing foreground row arrays.
2. Trace helper mutations and separate foreground writes from player-layer/metadata writes.
3. Establish top-nibble and signed high-byte semantics with golden vectors.
4. Reconcile foreground cell ownership with snapshot compatibility and world/stage/profile boundaries.
5. Keep CS-2 open; do not implement placeholder gameplay provider.

Static source audit only. No Native runtime code or tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (foreground helper call graph)

### Work completed
- Confirmed `iVoid(x,y)` low-byte replacement (14) preserving upper 24 bits; collected call-site references.
- Decoded recursive `aVoid(x,y,dx,dy,mode)`: traverses connected foreground low-byte 8/9 cells and toggles upper metadata bit 0x200. Door open/close helpers invoke it in both horizontal directions with different modes.
- Traced `nVoid(tile)`: low-byte replacement plus conditional animation metadata changes.
- Traced `jVoid(x,y)`: sets top-nibble animation/event bit 28; update path increments/clears nibble.
- Distinguished `aiVoid()` restore reconstruction of player-layer code 48 from foreground writes.
- Distinguished `kVoid(x,y)` renderer/animator update side effect from foreground mutation.
- Reaffirmed cross-layer separation: foreground packed ints, player occupancy bytes, stage metadata ints, and timing/dirty auxiliary arrays.
- Added Source Trace §25 and Evidence Matrix §30.

### Next
1. Decode overloads of obfuscated `aVoid` with exact method descriptors and map helper call graph.
2. Trace all writer/reader paths for `bIntArrArr`, player layer, and auxiliary dirty/timing arrays.
3. Establish top-nibble and 0x200 semantics with golden vectors and bounds/cycle tests.
4. Continue stage/world/profile ownership and snapshot compatibility identity.
5. Keep CS-2 open; no provider until real Native gameplay owner exists.

Static source audit only. No Native runtime code or tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (cross-layer snapshot ownership)

### Work completed
- Audited allocation and initialization of player, background-derived metadata, foreground, and two runtime auxiliary grids.
- Confirmed `bIntArrArr` is packed 32-bit gameplay metadata and is copied into/out of `cIntArrArr` by checkpoint capture/restore.
- Confirmed `crtStagePlayerLayer` occupancy/object codes are copied into/out of `eByteArrArr`.
- Confirmed `bByteArrArr` contains movement/progress/timing values that affect gameplay and is copied into/out of `dByteArrArr`.
- Confirmed `cByteArrArr` is initialized and mutated as dirty/update scheduling markers but is not copied by checkpoint; restore triggers visual/update invalidation. Treat as derived state pending full scheduler trace.
- Added Source Trace §26 and Evidence Matrix §31.

### Next
1. Decode `bIntArrArr` packed field contexts without assuming one universal struct.
2. Trace `bByteArrArr` contexts and exact signed-byte/wrap behavior.
3. Trace `cByteArrArr` marker consumers, scheduling cadence, and rebuild contract.
4. Define stage identity/dimension compatibility and transactional snapshot validation.
5. Continue world/profile ownership and opaque scalar audit; keep CS-2 open.

Static source analysis only. No Native gameplay/provider code or tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (scheduler semantics corrected)

### Work completed
- Refined `cByteArrArr`: it is not merely a dirty renderer grid; it is a transient object-update scheduler. `ahVoid()` scans a player-centered, clipped region, processes positive scheduler values, decrements by 6, and dispatches foreground/object update logic.
- Confirmed `kVoid(x,y)` writes 48 into a 3×3 neighborhood. Restore excludes `cByteArrArr`, then scans restored player/foreground state and calls `kVoid` to rebuild scheduler work.
- Established that restore intentionally rebuilds transient work but exact mid-tick phase equivalence is not proven.
- Refined `bByteArrArr`: raw signed-byte progress/timing values are object-family dependent; value 18 is often decremented by 6, while other paths increment, reset, threshold-check, or use it for animation displacement/frame selection.
- Cataloged overlapping contextual masks in `bIntArrArr`; no canonical packed struct should be imposed.
- Added Source Trace §27 and Evidence Matrix §32.

### Next
1. Trace lifecycle/cadence for scheduler values 24/30/48/127 by object family.
2. Verify `kVoid` 3×3 bounds and update scan clipping.
3. Build object-family `bByteArrArr` golden vectors.
4. Build descriptor-aware `bIntArrArr` accessor map.
5. Define a game-owner tick-boundary capture contract and test restore/rebuild across multiple ticks.
6. Keep CS-2 open; no provider implementation until the real Native gameplay owner exists.

Static source audit only. No Native runtime code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (scheduler writer inventory)

### Work completed
- Performed a source-wide direct-assignment scan for `cByteArrArr`; documented initialization, normalizer, player/object initialization, movement, door/lock, mechanism, optional packed-cell, and explicit cancellation writers.
- Confirmed value 127 is written by normalizer case 3 only in the inspected direct-assignment inventory; do not label it a 127-tick timer.
- Confirmed values 24, 30 and 48 are distinct scheduling requests. The scan subtracts 6 before dispatch, but handlers may overwrite, clear, or reschedule the value; no universal duration formula is valid.
- Confirmed player movement can couple scheduler=30, progress byte=18, occupancy code=32, and packed metadata direction bits.
- Confirmed `kVoid` writes 48 to a 3×3 neighborhood without local bounds checks; caller coordinate constraints remain to be exhaustively proven.
- Added Source Trace §28 and Evidence Matrix §33.

### Next
1. Trace scheduler values 24/30/48/127 through every foreground-code handler in `ahVoid()`, including overwrite and reschedule behavior.
2. Prove safe coordinate preconditions for every `kVoid` caller and 3×3 neighborhood.
3. Decode object-family `bByteArrArr` lifecycle together with scheduler transitions.
4. Build descriptor-aware `bIntArrArr` consumer map.
5. Define game-owner tick-boundary capture and multi-tick restore/rebuild tests.
6. Keep CS-2 open; no Native provider implementation until the real gameplay owner exists.

Static source audit only. No Native runtime code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (scheduler handler and bounds audit)

### Work completed
- Inspected `ahVoid()` dispatch for foreground codes 36, 35/37, 32, 26, 6, 33/14, 2, 3, 30/0, 7, and 8/9.
- Recorded handler-specific reschedule/cancel behavior and multi-grid side effects. Confirmed scheduler decrement-by-6 is only the pre-dispatch action; handlers can overwrite, clear, or reschedule current/neighbor cells.
- Confirmed foreground code 3 explicitly cancels scheduler at phase >= 6; code 7 advances substate on every third global tick; code 36 clears after its frame progression reaches 16; code 32 uses sprite frame count.
- Audited two-coordinate `kVoid` call sites. Restore's interior scan proves its own 3×3 writes safe; all other call-site coordinate preconditions remain partially unproven.
- Added Source Trace §29 and Evidence Matrix §34.

### Next
1. Continue complete `ahVoid()` switch coverage, including remaining foreground codes and player-layer dispatch.
2. Prove bounds preconditions for every `kVoid(x,y)` call site, especially offset/neighbor callers and stage normalizer.
3. Decode object-family `bByteArrArr` lifecycle alongside scheduler transitions.
4. Build descriptor-aware `bIntArrArr` accessor map and cross-grid golden vectors.
5. Define game-owner tick-boundary capture and multi-tick restore/rebuild validation.
6. Keep CS-2 open; no provider implementation until the real gameplay owner exists.

Static source analysis only. No Native gameplay code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-26 (player-layer scheduler dispatch)

### Work completed
- Traced the second dispatch stage inside `ahVoid()`: after foreground low-byte dispatch, the same eligible cell is usually dispatched by `crtStagePlayerLayer[x][y]`; labelled early exits can skip this stage.
- Inventoried player-layer codes and helper routes, including 54, 50, 49/43/19, 48, 46, 45, 44, 40, 36, 28, 16, 14, 10, 21, 32/11, 37/30, 24/27/26, 42/2/53/51/52/5/4/6/7/41, 47, 1/0/9/8, and 23/22.
- Identified high-coupling handlers: code 54 3×3 spread, code 46 falling/moving object, code 45 animation/hazard relocation, code 14 directional mover, code 21 paired chain, code 16 paired state, and codes 37/30 counter propagation.
- Confirmed these handlers mutate several authoritative grids and scheduler values in one visit; snapshot capture must not occur mid-handler.
- Added Source Trace §30 and Evidence Matrix §35.

### Next
1. Trace shared helper families `eVoid`, `dVoid`, `lVoid`, `mVoid`, `aqVoid`, `akVoid`, `anVoid`, `amVoid` by exact method descriptors.
2. Finish complete foreground switch inventory and map labelled early-exit semantics.
3. Prove neighbor/target coordinate bounds using stage border and normalization invariants.
4. Build per-object-family `bByteArrArr` golden vectors and packed metadata accessors.
5. Define end-of-tick snapshot capture contract and transactional multi-grid update model.
6. Keep CS-2 open; no provider implementation until a real Native gameplay owner exists.

Static source analysis only. No Native gameplay code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-27 (shared helper signature audit and CI review)

### Work completed
- Corrected helper name ambiguity by inventorying overloads and separating exact signatures: eVoid(), eVoid(int), eVoid(byte); dVoid(int), dVoid(int,int), dVoid(byte), dVoid(boolean); lVoid(), lVoid(int), lVoid(int,int); mVoid(), mVoid(int), mVoid(int,int).
- Traced observed behavior for eVoid(byte), dVoid(byte), lVoid(int), mVoid(int), akVoid(), and partial bodies of amVoid(), anVoid(), aqVoid().
- Confirmed eVoid(byte) and several other handlers perform coupled multi-grid movement/phase transitions; they must be atomic within the game-owner tick.
- Inspected failed CI run #348 logs: two AndroidCheckpointStorageTest cases failed through MavenArtifactFetcher.java:129 with ExecutionException caused by IOException from HttpURLConnection; 22 tests completed, 2 failed. Logs do not expose the underlying network cause, so classify as dependency/network fetch failure candidate, not proven product-code regression.
- Inspected run #349: job “Core JVM tests and Android debug build” completed successfully for commit 475bc81dea3bdb1d412b5f0ad050085846321cf3. This run's PR metadata points to head adf0c50f..., so the run verifies the PR head snapshot at execution time; current branch HEAD still needs its own latest CI.
- Added Source Trace §31 and Evidence Matrix §36.

### Next
1. Finish complete bodies of amVoid(), anVoid(), aqVoid(), and all helper overloads with two coordinates.
2. Trace all call sites and side effects for each exact signature.
3. Convert confirmed transitions into descriptive Native action contracts and golden vectors.
4. Complete scheduler/foreground/player-layer dispatch and boundary preconditions.
5. Keep CS-2 open; no provider implementation until real Native gameplay owner exists.

Static source analysis only. No Native gameplay code/tests added. PR #1 remains unmerged absent explicit approval.


## CI follow-up — 2026-11-27

- Run #351 (commit `971b044e878555ece8bbf7face0f36aa9fa9e329`) failed before test execution because Maven Central returned HTTP 429 while Gradle attempted to download `org.robolectric:nativeruntime-dist-compat:1.0.16`. This is a confirmed external dependency rate-limit failure, not a test assertion or source compilation failure.
- Run #350 completed successfully for commit `adf0c50f383b5d28ad3227a012419b0a4f8ad8d4`; Run #349 also succeeded for the preceding documented snapshot. The new helper documentation commit and checkpoint update still require a clean CI run.
- Earlier Run #348 did reach `:app:testDebugUnitTest`: 22 tests completed, 2 AndroidCheckpointStorageTest cases failed with an IOException surfaced via MavenArtifactFetcher.java:129. Run #351 provides a separate, explicit Maven Central 429 cause for its own dependency-resolution failure; do not conflate the two runs.
- Current branch HEAD at this checkpoint update is `7382aabcfca1f5c68408553c44cb9125c907ce70`. No code changes or merge actions were performed.


## Resume update — 2026-11-27 (complete selected helper bodies)

### Work completed
- Inspected full `amVoid()` body: metadata substate 8–11, direction/polarity mapping, three-candidate neighbor selection, occupancy/metadata/progress transfer, kVoid scheduling, collision damage, progress decrement by 5.
- Inspected full `anVoid()` body: directional x-neighbor selection, packed counter decrement, target scheduler 30 vs current scheduler 24, target progress 18, occupancy eligibility, code-32 cleanup scan, final neighborhood scheduling.
- Inspected full `aqVoid()` body through closing brace: optional-mode target side, objective/pickup counters, `aaInt` decrement, occupancy transfer, foreground mutation, progress thresholds, high metadata flag reconciliation through bVoid/cVoid, cyclic 3-bit substate.
- Clarified `aqVoid()` is not simply a pickup handler; it is a composite interaction/transfer controller with objective and derived-state side effects.
- Added Source Trace §32 and Evidence Matrix §37.

### Next
1. Continue full caller inventories for `amVoid()`, `anVoid()`, and `aqVoid()`; capture the caller context and preconditions for every invocation.
2. Trace `dVoid(int,int)`, `lVoid(int,int)`, and `mVoid(int,int)` by exact descriptor, plus all other overloaded helper signatures.
3. Build transition tables and golden vectors for objective count, derived high flags, optional-mode cell, and neighbor movement.
4. Complete full foreground dispatch and neighbor bounds proof.
5. Keep CS-2 open; no production snapshot provider until a real Native gameplay owner and validated schema exist.

Static-source analysis only. No Native runtime code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-27 (caller and overload cross-check)

### Work completed
- Mapped scheduler caller codes: `anVoid()` for player-layer 32; `amVoid()` for 11; `aqVoid()` for 47/1/0/9/8; `lVoid(int)` for 42/2/53/51/52/5/4/6/7/41; `mVoid(int)` for 24/27/26; `eVoid(byte)` for 43/19.
- Traced `dVoid(int,int)` callers: stage transition for foreground 15/16, plus two cleanup scans; documented its player-layer switch and intentional fall-through.
- Traced `lVoid(int,int)` callers: foreground/player transition code 10, player-layer code 37, and stage transition code 34/35; it selects optional-mode adjacent cell in x-1, x+1, y-1 priority order and sets xByte=5.
- Traced `mVoid(int,int)` callers: foreground code 19 activation and optional-mode queue pulse in UVoid; the latter decodes coordinates from secondary packed-record offsets 13/20.
- Confirmed caller continuation after helper return may add more writes, so transaction boundary covers full caller branch and nested calls.
- Added Source Trace §33 and Evidence Matrix §38.

### Next
1. Trace transitive callees from the caller branches (e.g. `ajVoid`, `aByte`, `jVoid`, `bVoid/cVoid`) where they affect authoritative snapshot state.
2. Build explicit transaction diagrams for objective completion, stage-wide transitions, optional-mode activation/queue, and moving-object transfer.
3. Prove coordinate/bounds preconditions for fixed-row cleanup scans and direction-derived targets.
4. Produce a first schema candidate crosswalk: every included field to capture source, restore behavior, owner, and validation invariant; keep it provisional.
5. Keep CS-2 open until transitive state ownership and independent/runtime validation are resolved.

Static-source analysis only. No Native gameplay runtime code/tests added. PR #1 remains unmerged absent explicit approval.
