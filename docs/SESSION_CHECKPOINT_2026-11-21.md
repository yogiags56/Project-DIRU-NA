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


## Resume update — 2026-11-27 (transitive helper effects and state classification)

### Work completed
- Identified code-47 caller continuation: `aqVoid()` is followed by `ajVoid()`; transaction boundary includes both.
- Identified `jVoid(int,int)` as neighborhood scheduler/invalidation helper, provisionally derived/transient.
- Identified `bVoid(int,int,int)` as recursive neighborhood operation with operation-byte selector; distinct from other bVoid overloads.
- Identified `cVoid(int,int,int)` as coordinate operation dispatcher with observed selectors 1, 3, 5, -1; distinct from packed-bit static helper and other cVoid overloads.
- Added provisional field classes: authoritative candidates, derived/transient, optional-mode conditional, unresolved opaque.
- Added Source Trace §34 and Evidence Matrix §39.

### Next
1. Inspect complete bodies and branches of `ajVoid()`, `bVoid(int,int,int)`, and `cVoid(int,int,int)`.
2. Enumerate all callers and nested side effects of each exact overload.
3. Reconcile scheduler-grid capture with restore-time dirty/scheduler reconstruction.
4. Prove neighbor bounds and recursion ordering for all recursive neighborhood operations.
5. Build caller-complete transition vectors for stage completion, movement, and optional-mode queue.
6. Keep CS-2 open; no final schema or runtime provider implementation yet.

Static source audit only. No gameplay code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-27 (corrected transitive helper semantics)

### Important correction
A deeper body inspection corrected three provisional descriptions from the preceding audit:
- `cVoid(int,int,int)` is an 8-slot render-animation ring buffer using `aByteArr`, `cByteArr`, `bByteArr`, and modulo-8 read/write indices `adInt/aeInt`. It is transient presentation state, not a coordinate operation dispatcher.
- `bVoid(int,int,byte)` recursively clears matching foreground cells: left/right/up recursion followed by a downward loop. The method has no explicit bounds checks; prove border preconditions and add safe Native bounds validation.
- `jVoid(int,int)` sets foreground packed metadata bit `0x10000000` then invokes `alVoid()`; the latter decrements `mByteArr[cmInt]` and scans/updates matching door cells when a lock-group counter reaches zero. This is authoritative foreground/door progression, not scheduler invalidation.

The prior provisional labels in Source Trace §34 and Evidence Matrix §39 are explicitly superseded by Source Trace §35 and Evidence Matrix §40. No gameplay implementation was added.

### Next
1. Trace every `alVoid()` caller and all lock-group counter writers/initializers.
2. Trace full `bVoid(int,int,byte)` caller preconditions and stage border guarantees.
3. Reconcile foreground high-bit/door-group state with direct checkpoint copy and restore.
4. Confirm animation ring-buffer reset/rebuild policy and keep it outside authoritative snapshot payload unless presentation resume is required.
5. Keep CS-2 open; no final snapshot schema/provider until all state ownership and invariants are validated.


## Resume update — 2026-11-27 (door-group state capture and restore traced)

### Work completed
- Traced stage loader allocation of `mByteArr/lByteArr`: length derived from highest set bit of stage group mask; foreground code 17 cells initialize mByteArr from hashtable2 (default 0) and lByteArr from hashtable3 (default 57).
- Traced foreground code 26 interaction: `cmInt` selected from foreground high byte, validated against mByteArr length, then matching code-17 cells scanned.
- Traced `alVoid()`: selected group counter decremented; on zero, matching code-17 foreground cells and selected neighboring cells are transformed/scheduled. Callers include player-layer codes 7 and 36, and `jVoid(x,y)`.
- Confirmed direct temporary checkpoint capture: `cnInt=cmInt`; `mByteArr` copied to `oByteArr`. Restore copies `cnInt` back to `cmInt` and `oByteArr` back to `mByteArr`. Foreground grid is also directly copied.
- Identified `lByteArr` as stage-loaded text selector data not copied by the temporary checkpoint; immutability/reconstruction still needs proof.
- Added Source Trace §36 and Evidence Matrix §41.

### Next
1. Trace stage binary fields and hashtable2/3 writers to identify how group counts and text selectors are encoded.
2. Determine whether lByteArr is immutable stage metadata or mutable progression state.
3. Build golden vectors for group counter initialization, decrement-to-zero, multiple groups, and snapshot round-trip.
4. Prove behavior for invalid group IDs and counter underflow.
5. Continue flood-clear border/caller precondition audit; keep CS-2 open and schema provisional.

Static source audit only. No Native gameplay runtime code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-27 (door-group data lineage refinement)

### Work completed
- Corrected the previous wording: `hashtable2`/`hashtable3` are local temporary tables constructed during stage initialization, not standalone binary/persistent tables.
- Established `mByteArr` initial counts by scanning foreground code 17 cells and inspecting player-layer code directly above (qualifying codes 19/36/43/45/46/49); group key is the background-layer value at that coordinate.
- Qualifying code-17 cells are cleared; other code-17 cells retain group ID in foreground high bits.
- Established `lByteArr` selectors: code 36 selects 58 if not already set; other qualifying codes select 56, overridden by 51/52/53 for kByte modes 4/5/3; missing group defaults to 57.
- Confirmed no post-stage-load writes to `lByteArr` in pinned class; it is read only for door-dialog text and nulled on stage teardown. Treat as reconstructible stage/mode metadata, not mutable checkpoint payload, provided restore uses identical stage/mode identity.
- Confirmed `cmInt` initialized to -1, selected by foreground code 26 from high byte, bounds-checked against mByteArr length; `alVoid()` decrements without an explicit clamp.
- Added Source Trace §37 and Evidence Matrix §42. Exact binary record populating background-layer group IDs remains unlocated; current evidence begins at decoded stage grids.

### Next
1. Trace stage decoder and binary record that populate background layer; identify group-ID encoding and valid domain.
2. Prove the group mask cannot exceed signed 32-bit assumptions (or document source behavior/limitation).
3. Build golden vectors for qualifying/nonqualifying code-17 cells, mode-dependent text selectors, multiple groups, and counter decrement-to-zero/underflow.
4. Continue flood-clear border precondition proof.
5. Keep CS-2 open; no final schema/provider until source ownership and runtime validation are complete.

Static source audit only. No Native gameplay code/tests added. PR #1 remains unmerged absent explicit approval.


## Resume update — 2026-11-27 (stage binary layer format traced)

### Work completed
- Identified resources `/w0.bin`, `/w1.bin`, `/w2.bin`.
- Per-stage record: 4-byte header; width and height are unsigned little-endian 16-bit.
- Payload: player, background, foreground layers; each width×height row-major bytes; mapping index is `x + y*width`.
- Background bytes widen as signed Java bytes into int `bIntArrArr`; this is checkpoint-copied and is the source of door-group IDs.
- Found source parser robustness gaps: unchecked read(byte[]) byte counts and unchecked skip(width*height*3). Native parser must use readFully, checked seek, positive/bounded dimensions, and overflow-safe size arithmetic.
- Critical unresolved: group mask uses `1 << background`; Java int shift aliasing for group IDs >=32. Need enumerate actual bundled resources before fixing allowed domain.
- Added Source Trace §38 and Evidence Matrix §43.

### Next
1. Retrieve and enumerate actual world resource binaries, record boundaries, dimensions, all layer values and group IDs.
2. Establish group-ID range and max stage dimensions.
3. Build strict parser/golden fixtures and malformed-input tests.
4. Prove recursive flood-clear borders using actual stage geometry.
5. Keep CS-2 open; no final schema/provider until resource and runtime validation pass.

Static source analysis only; no production gameplay implementation added. PR #1 remains unmerged without explicit approval.


## Resume update — 2026-11-27 (progress estimate, binary inventory reconciliation, CI diagnosis)

### Correction / reconciliation
The full world binary inventory is already complete under `DIRU-NA-AUDIT-029_WORLD_STAGE_STREAM_BINARY_INVENTORY.md`: all three resources, 41 stage records, dimensions, exact byte consumption, and no trailing bytes are documented. Do not repeat full binary decoding as an open task. The remaining narrow task is a histogram of background-layer values at foreground code-17 cells to validate group IDs against the 32-bit mask. The current GitHub fetch connector only accepts UTF-8 text and rejected raw .bin data; no group histogram was computed in this session.

### Progress estimate (approximate engineering judgment)
- DEV-003CS-2: 60–65% complete as static evidence/documentation. Major copy/restore paths, layer ownership, optional records, caller boundaries, and door-group capture are mapped; full transitive ownership, opaque fields, invariant closure, and runtime vectors remain.
- Static source/resource reverse-engineering: 65–70% documented; runtime parity not established.
- Native gameplay reconstruction toward first playable: 5–10%; DEV-002 first reproducible gameplay milestone remains unimplemented.
- Overall path to first playable Native Android release: 25–30%. This is a planning estimate, not a measured completion metric. Foundation and persistence boundary are substantially advanced, but actual gameplay, renderer/input integration, parser tests, and runtime validation remain major work.

### CI failure diagnosis
Workflow run #367 (commit `916ffa07f1556acb77ce267b98e41a92ac215b42`) failed at `app:testDebugUnitTest` because Maven Central returned HTTP 429 while downloading `org.robolectric:nativeruntime-dist-compat:1.0.16`. This is an external dependency rate-limit failure, not evidence of a source/test assertion failure. Re-run or fresh CI after rate-limit recovery; do not label the commit green. New documentation commits are later than this failed run and still need their own workflow result.

### Next
1. Obtain binary-capable access to the already inventoried world files and compute the targeted code-17/background group-ID histogram; preserve signed-byte widening semantics.
2. Continue opaque scalar and transitive helper ownership audit, and prove flood-clear edge preconditions using actual stage dimensions.
3. Build deterministic golden vectors and malformed parser tests.
4. Move the critical path toward DEV-002 first gameplay owner; do not spend indefinitely on CS-2 before defining a bounded exit gate.
5. Keep PR #1 unmerged absent explicit authorization.

No gameplay runtime implementation was added in this update.


## Resume update — 2026-11-27 (first DEV-002E core primitives)

### Implemented
- `core/stage/StageGrid.java`: immutable three-layer row-major grid, positive dimensions, overflow-safe cell count, exact array lengths, defensive copies, checked coordinate reads. Int values retained to preserve signed-byte widening semantics from a future decoder.
- `core/movement/OrdinaryMovementInterpolation.java`: isolated ordinary movement clock based on audited 18→12→6→0 progression; starts only after caller acceptance, prevents overlapping ordinary moves, and does not commit coordinates or perform collision.
- Added tests: StageGridTest and OrdinaryMovementInterpolationTest.

### Not yet implemented
- GameSession single-writer owner
- Player logical position and pending/active direction integration
- Collision acceptance/typed unsupported branch gate
- Immutable render snapshot
- Input-to-tick integration and synthetic end-to-end gameplay fixture
- Android shell hookup and manual playable verification

### Immediate next
1. Inspect and run core JVM tests for new classes.
2. Add PlayerMotionState and immutable GameplayRenderSnapshot.
3. Add GameSession tick owner that uses dispatcher and movement clock but fails closed until a collision evaluator subset is proven.
4. Add tests for no-mutation on blocked/unsupported/out-of-bounds attempts.
5. Run CI on exact HEAD and update checkpoint with workflow evidence.
6. Keep PR #1 unmerged absent explicit authorization.

This is an implementation increment only; it is not a first playable build.


## Resume update — 2026-11-27 (GameSession foundation)

### New core implementation
- `PlayerMotionState.java`: immutable player logical coordinate, pending/active direction, and interpolation projection with validation.
- `GameplayRenderSnapshot.java`: immutable stage/player/tick render projection.
- `GameSession.java`: platform-independent single-writer owner; captures construction thread as owner; consumes immutable key-mask input per tick through LegacyInputDispatcher; blocks out-of-bounds before policy call; missing/null/unsupported collision policy fails closed; logical tile coordinate commit happens only after explicit ACCEPTED decision; ordinary interpolation starts at the same accepted transition; active interpolation advances before dispatching new input.
- `GameSessionTest.java`: no-policy fail-closed; blocked no-mutation; accepted logical commit/interpolation; input ignored while interpolation active; bounds rejection before policy; cross-thread mutation rejection.

### Critical limitation
The MoveAcceptancePolicy is only a seam. It is not the original collision evaluator. The ACCEPTED test uses a test double and proves owner semantics, not game passability. No production collision policy is wired. Special movement, scripted displacement, object push, contextual interactions, animation, camera, stage mutation, renderer, and Android shell integration remain out of scope. No first playable milestone is claimed.

### Next
1. Run CI on exact branch HEAD and inspect failures/logs.
2. Review GameSession against DEV-003A–003E for pending vs active direction, helper commit timing, and ordinary tick semantics.
3. Implement only a bounded source-derived collision subset with explicit typed outcomes and no-mutation tests.
4. Add synthetic fixture integration tests; do not treat the injected test policy as gameplay behavior.
5. Connect immutable render projection to Android shell after core acceptance.
6. Keep PR #1 unmerged absent explicit authorization.


## Resume update — 2026-11-27 (collision grid capture bridge)

### CI confirmation for previous GameSession foundation
- Run #379, SHA `0b106a8b53404f255749e5a65af84a0b742bc776`: completed successfully.
- Run #378, SHA `65741e07475a0d066ad86a449facc0303d9780a9`: completed successfully.
- Run #377, SHA `7019298f99334af27b471d2b8e4cc0393228555d`: completed successfully.
- Run #376, SHA `2d9b6afdb37d49f9aa6eec84c79571e5bbe8d62b`: failed; this was the intermediate render-snapshot-only commit, and must not be counted as passing.
These runs validate their exact commits, not the newer collision adapter commit.

### New implementation
- `LegacyCollisionGridView.java`: read-only adapter from immutable StageGrid to LegacyCollisionNeighborhoodCapture.Grid.
- Packed metadata is mandatory and separately supplied; no assumption that background values equal collision metadata.
- Auxiliary layer is optional, but absence remains explicit; no fake zero-filled layer.
- Defensive copies and checked reads.
- `LegacyCollisionGridViewTest.java`: six tests for explicit layer mapping, auxiliary presence/absence, defensive copies, mismatch rejection and bounds.

### Current limitation
This only connects stage data to the existing bounds-checked sample capture API. It is not a collision evaluator and does not prove passability. The legacy helper's raw b2/b3 flags, probe behavior, branch side effects, and final coordinate writeback remain separate. Do not wire this adapter as a permissive GameSession policy.

### Next
1. Add a branch capture facade that combines grid capture and branch-context validation.
2. Build a typed outcome composer that preserves raw helper flags, local effects, probe mode, early return, and persisted writeback separately.
3. Add golden synthetic fixtures for known foreground/player-layer branches.
4. Implement transactional application only for proven ordinary movement subset.
5. Connect production policy to GameSession only after all above are validated.
6. Run CI against exact latest HEAD; keep PR #1 unmerged absent explicit authorization.


## Resume update — 2026-11-27 (branch capture facade)

### Implemented on the existing DEV-003CR branch
- Added `LegacyCollisionBranchCaptureFacade` as the single branch-scoped entry point.
- The facade performs bounds-checked raw neighborhood capture, then branch-specific context/sample validation.
- It exposes a neighborhood snapshot only when both phases pass; capture or context failures remain explicit.
- Added `LegacyCollisionBranchCaptureFacadeTest` for valid capture, out-of-bounds rejection, missing type-2 inventory context, and grid-dimension mismatch.

### Validation status
- Previous exact HEAD `722cdb6fd7592a1fe52b63676fd70fc8d64e6c30` passed GitHub Actions run #383, including core JVM tests, Android storage unit tests, and debug APK assembly.
- The facade implementation and tests are newer commits. They must not be called CI-passing until a workflow run for the latest exact HEAD completes successfully.

### Boundary preserved
- The facade is capture/validation infrastructure only; it does not evaluate collision, interpret raw tile values, mutate map/player state, or authorize movement.
- Next: inspect the branch capture tests under CI, then implement a typed outcome composer that retains raw b2/b3, probe mode, early-return behavior, local effects, and persisted writeback as independent channels.
- Keep production collision policy disconnected from GameSession until source-derived branch fixtures and transactional no-mutation tests pass.
- Keep PR #1 open and unmerged.


## Resume update — 2026-11-27 (DEV-003H typed outcome composer)

### Implemented
- Added `LegacyCollisionOutcomeComposer`.
- Converts an observed `LegacyCollisionTrace` into the existing `LegacyCollisionEvaluationResult` vocabulary.
- Preserves raw b2 return and b3 auxiliary flag as independent channels.
- Requires caller to supply local-collision classification and player-layer visitation explicitly; neither is guessed from the trace.
- Derives completion from the trace's normal/early-return observation and commit suppression from whether player snapshot writeback occurred.
- Added `LegacyCollisionOutcomeComposerTest` covering independent b2/b3 channels, probe writeback suppression, early return, and null input rejection.

### Boundary / known limitation
- This composes observed facts; it does not evaluate grid samples or infer the source branch's local collision flag.
- It does not apply state changes, mutate StageGrid, or authorize movement.
- The distinction between player snapshot writeback and every possible side effect remains important: a helper can have non-player side effects while not writing the player snapshot. The result field retains the existing name `stateCommitSuppressed`; consumers must not interpret it as proof that absolutely no side effect occurred.
- Latest exact commit requires CI verification; do not carry forward prior CI status.

### Next
1. Run and inspect CI on latest exact branch HEAD.
2. Audit result semantics for probe and early-return branches against pinned source.
3. Build the pure evaluator for the narrow, source-proven foreground type-3/type-7 subset with raw packed-value fixtures.
4. Add caller-direction tests separately; keep production movement policy disconnected until source-derived fixtures pass.
5. Keep PR #1 open and unmerged.


## Resume update — 2026-11-27 (DEV-003I type-3 control-flow correction)

### Source audit finding
A full read of pinned `i.aBoolean(int,int,boolean)` around `Label_0738` exposed an incorrect prior interpretation of foreground type 3. The type-3 case exits the foreground switch block, after which `n3=n10` is assigned:
- Signed high-byte `raw >> 8 < 3`: sets `b2=true`, leaves `n10=0`, therefore player-layer evaluation continues.
- Signed high-byte `raw >> 8 >= 3`: sets `b2=false`, sets `n10=1`, therefore player-layer evaluation is skipped.
Type 7 retains its separate packed-nibble test and existing flow.

### Corrective commits on the branch
- `LegacyForegroundBranchEvaluator`: type-3 flow now depends on signed high-byte threshold.
- `LegacyForegroundPrecedence`: same source-derived correction.
- `LegacyForegroundBranchEvaluatorTest`: asserts low/negative metadata continue to player-layer; threshold and above skip.
- `LegacyForegroundPrecedenceTest`: updates type-3 flow assertions.
- `DIRU-NA-DEV-003P_TYPE7_TYPE3_PURE_EVALUATION.md`: documents the corrected control-flow explanation.

### Validation / boundary
- The source audit was against pinned `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`.
- These are source/test/documentation corrections only. No full collision evaluator or production GameSession policy is claimed.
- CI for the newest exact HEAD must be checked before calling the correction build-verified.

### Next
1. Add a cross-model parity test ensuring `LegacyForegroundBranchEvaluator` and `LegacyForegroundPrecedence` agree for all type-3 and type-7 raw values across signed high-byte/nibble boundaries.
2. Run CI on exact latest HEAD and inspect test results.
3. Continue with source-derived foreground branch coverage; do not connect incomplete evaluator to production movement.
4. Keep PR #1 open and unmerged.


### Follow-up parity guard
- Added `LegacyForegroundModelParityTest` to compare the narrow evaluator and precedence model across signed type-3 high-byte boundaries (-128 through 127 around threshold 3) and type-7 packed-nibble boundaries.
- Tightened the signed fixtures to use representable high-byte values rather than values that overflow when shifted into the packed cell.
- Latest source/test HEAD for this continuation: `8bed5f4146d1116243f3e3a3e9da95ad81351fb8`.
- CI status for this exact HEAD remains unverified.


## Resume update — 2026-11-27 (DEV-003J labeled-exit control-flow audit)

### Source finding
Pinned source `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`, method `i.aBoolean(int,int,boolean)`, shows that `break Label_0738` exits the complete foreground block, bypassing `n3=n10`. As `n3` remains initialized to zero, the player-layer switch still executes for foreground types 1, 2, 4, 5, 6 and 28, and for type 3 when signed high byte < 3. Type 7 nibble < 2 and type 3 high byte >= 3 reach `n3=n10` with n10=1 and skip player-layer evaluation.

### Corrected implementation and tests
- Updated `LegacyForegroundType156Evaluator` flow for types 1/5/6 to continue to player-layer evaluation.
- Updated `LegacyForegroundType2Evaluator` flow, including probe, to continue to player-layer evaluation.
- Updated `LegacyForegroundType428Evaluator` flow for types 4/28 to continue to player-layer evaluation.
- Updated the three corresponding test classes to assert the source-derived flow.
- Clarified the type-428 evaluator documentation and movement/collision integration matrix.

### Important semantic consequence
Foreground type 5 initially sets b2=true, but player-layer evaluation still follows and may change b2. Therefore the foreground decision is not a final collision return. Likewise, type 2's b3 and counter effects are independent of the player-layer outcome.

### Validation
- Changes are committed directly to the existing DEV-003CR branch.
- Latest CI status must be checked against the exact branch HEAD; no previous run is carried forward.
- Production movement remains disconnected from these partial pure evaluators.

### Next
1. Audit all existing foreground evaluator flow outputs against labeled block exits and the trailing n3 assignment.
2. Add one cross-evaluator control-flow parity test covering types 1,2,3,4,5,6,7,28 with probe and non-probe cases.
3. Run CI on latest HEAD and inspect test failures.
4. Continue with player-layer evaluator only after foreground flow contract is consistent.


### Cross-evaluator contract test
- Added `LegacyForegroundFlowContractTest` to guard the labeled-exit fallthrough contract across types 1, 2, 3, 4, 5, 6, 7 and 28.
- Explicit `n10=1` paths (type 7 nibble >= 2; type 3 signed high byte >= 3) are asserted to skip player-layer evaluation.
- Latest source/test HEAD before this checkpoint update: `fe8be78736f931f0cbf6e72d4fc795f1d6acabf0`.


## Resume update — 2026-11-27 (DEV-003R narrow player-layer evaluator)

### Implemented
- Added `LegacyPlayerLayerEvaluator` as a pure evaluator for the simple source branches.
- Added tests for values 34/35, 31/49, explicit blocking set, type 10 mode/probe behavior, type 28 direction-clear intent, and NOT_HANDLED for geometry-dependent cases.
- Added `DIRU-NA-DEV-003R_PLAYER_LAYER_NARROW_EVALUATOR.md` documenting scope and non-goals.

### Source-derived behavior
- 34/35 return the probe flag.
- 31/49 return false.
- The explicit blocking set returns true.
- Type 10 only forces true when xByte==3; its metadata write to 1 is a non-probe intent when current metadata <= 0.
- Type 28 clears active direction but preserves the incoming b2 value.

### Deliberate limits
- -1 depends on stage edges and nearby player-layer/door conditions.
- 0/8/9/47 depend on two-cell geometry, counter, direction, and kByte==4 early return.
- 48 depends on multi-cell push geometry and several world writes.
- Default/unknown values remain NOT_HANDLED pending richer local-delta and animation intents.
- No world mutation or GameSession integration is performed.

### Next
DEV-003S: pure geometry evaluator for 0/8/9/47 with explicit sampled cells and exact kByte==4 early-return behavior; then dedicated 48 push geometry and -1 boundary/door evaluator. CI must be checked against the latest exact branch HEAD.

## Resume update — 2026-11-27 (DEV-003S two-cell movement decision)

### Implemented
- Added `LegacyTwoCellMovementEvaluator`, a pure decision model for player-layer 0/8/9/47 based on explicit sampled facts.
- Preserved early return before counter decrement and exposed legacy animation intent 8/9.
- Modeled counter/probe gate, non-zero direction, destination passability, foreground type-7 high-nibble exception, and adjacent player-layer blocker exception for foreground low byte 0x23.
- Accepted push emits a world-push intent only outside probe; rejected push preserves incoming return flag and emits local direction / player metadata clear intents.
- Added `LegacyTwoCellMovementEvaluatorTest` for early return, animation choice, accepted probe/non-probe push, foreground type-7 exception, adjacent blocker exception, rejected push, and counter gate.
- Added `DIRU-NA-DEV-003S_TWO_CELL_MOVEMENT_DECISION.md` with source-derived contract and explicit caller responsibilities.

### Correction to earlier narrow evaluator
- Type 28 effect is now named `CLEAR_DIRECTION_AND_MOVEMENT_BLOCK`, matching source assignments `lByte=0` and `kInt2 &= 0xFFFFFFF7`; return flag remains unchanged.
- Type 28 test now checks the complete local effect name.

### Limits
- No grid sampling, coordinate derivation, push mutation, byte-array writes, or GameSession integration is implemented.
- The early-return coordinate condition and `eBoolean` passability must be supplied from a caller with exact source-coordinate mapping.
- CI is not yet verified for the latest exact branch HEAD.

### Next
DEV-003T: add an immutable, named-coordinate sampling snapshot; verify exact coordinate transforms for target/adjacent samples; build fixture-driven parity tests before connecting any world mutation.

## Resume update — 2026-11-27 (DEV-003T named-coordinate snapshot)

### Implemented
- Added immutable `LegacyTwoCellMovementSnapshot` carrying player origin, movement delta, sampled player/foreground values, movement counter/state, probe flag, destination passability, and the early-return inputs.
- Encoded distinct source coordinate formulas:
  - target foreground: `(playerX + 2*deltaX, playerY + 2*deltaY)`;
  - adjacent blocker sample: `(playerX + deltaX, playerY + 1)`;
  - horizontal push destination: `(playerX + 2*deltaX, playerY + deltaY)`;
  - current player cell for push clear: `(playerX + deltaX, playerY + deltaY)`.
- Added raw foreground low-byte and signed-high-byte decoding accessors.
- Added `LegacyTwoCellMovementEvaluator.evaluate(snapshot)` so the decision is derived from one named immutable input.
- Added `LegacyTwoCellMovementSnapshotTest` for coordinate formulas, early-return gate, raw sample decoding through the evaluator, and push coordinate outputs.

### Limits / safety boundary
- Snapshot population remains caller-owned. It does not read stage arrays, check bounds, or execute `eBoolean`.
- No stage/player-layer writes, push animations, or GameSession integration have been added.
- The push coordinate getters describe the source horizontal write target; they do not imply a committed push.
- CI and replay against recorded game states remain outstanding.

### Next
DEV-003U: model player-layer 48's separate two-cell push using an immutable snapshot and explicit sampled cells. Preserve probe suppression, exact source coordinate formulas, duplicate foreground-cache assignment only if confirmed as meaningful, and all world writes as intents. Then cover player-layer -1 boundary/door conditions separately.


### DEV-003T parity refinement
- Added resolved movement-state derivation matching the source's `kInt2 = (kInt & 0xFFFFFFF8) | directionCode`.
- Direction codes follow source priority: +X=2, -X=4, -Y=1, otherwise=3.
- Snapshot-based evaluation now uses the resolved direction bits for the early-return animation selector, not raw state bits.
- Added tests for all four direction codes and the corresponding right/left animation selection. Corrected the rightward expectation to code 2.


## Resume update — 2026-11-27 (DEV-003U player-layer 48)

### Implemented
- Added `LegacyPlayerLayer48Snapshot`, an immutable carrier for source and destination player-layer samples and their named coordinates.
- Reconstructed the metadata-bit-0x8 offset selection: destination pair offsets are (-1,0) when clear and (0,+1) when set.
- Encoded target coordinates `(playerX+2*deltaX, playerY+2*deltaY)`, source coordinates `(playerX+deltaX, playerY+deltaY)`, and guard sample `(sourceX, sourceY+offsetB+1)`.
- Added `LegacyPlayerLayer48Evaluator` for the counter/probe/direction/two-empty-destinations/nonnegative-guard acceptance gate.
- Accepted non-probe pushes emit two destination writes, two source clears, metadata copy/update, animation-byte-18, and foreground-cache write intents. Probe acceptance suppresses every world write.
- Rejected pushes preserve the incoming return flag and emit local direction-clear / movement-block intents when horizontal delta is non-zero.
- Added `LegacyPlayerLayer48EvaluatorTest` for offset geometry, both directions, counter and probe gates, destination/guard blockers, and write suppression.
- Added `DIRU-NA-DEV-003U_PLAYER_LAYER_48_PUSH.md`.

### Source-specific note
- The original source repeats the same foreground-cache assignment three times. The evaluator represents its final observable effect as one write intent; it does not preserve redundant write count.

### Limits
- Snapshot values are caller-sampled; no stage-array access, bounds checking, mutation executor, animation invocation, or GameSession integration is present.
- Tests are synthetic. Run the existing Gradle core suite and Android build through the PR workflow before treating this milestone as verified.

### Next
DEV-003V: model player-layer -1 edge/door/nearby-door predicates with a named immutable snapshot. Then design a transactional intent applier shared by 0/8/9/47 and 48, with probe-mode no-mutation guarantees and replay fixtures.


## Resume update — 2026-11-27 (DEV-003V player-layer -1)

### Implemented
- Added immutable `LegacyPlayerLayerMinusOneSnapshot` with explicit target, near-below, above, and below coordinates and caller-supplied samples.
- Added `LegacyPlayerLayerMinusOneEvaluator` for outer-stage-edge blocking and the three source door/neighbor exceptions.
- Preserved distinct door rules: counter A >= 24 requires player-layer 28 with metadata bit 0x8 clear; counter B >= 24 accepts player-layer 28 without that metadata test.
- Preserved horizontal near-below rule: deltaX non-zero, sampled player-layer 0, metadata low bits exactly 3.
- Probe mode suppresses every interior exception. Movement-block bit 0x8 is cleared in all -1 paths.
- Added synthetic unit tests for all edges, exact near-below conditions, both door-counter rules, and probe suppression.
- Added `DIRU-NA-DEV-003V_PLAYER_LAYER_MINUS_ONE.md`.

### Limits
- Snapshot sampling is caller-owned; no bounds-safe array sampler or world mutation executor is connected.
- The original method's incoming return flag is false at this switch branch; evaluator keeps it explicit for reuse.
- CI and recorded gameplay replay remain necessary.

### Next
DEV-003W: reconcile all evaluator outputs with the original method's final state-commit section; define one transactional intent format and ensure probe calls never mutate player, stage, metadata, animation, or cache state. Then add replay fixtures and integrate with GameSession.


### CI finding and correction — 2026-11-27
- PR workflow run #432 reached `:core:test` and reported 398 tests with one failure: `LegacyForegroundFlowContractTest.explicitN10OneBranchesSkipPlayerLayer`.
- The failing fixture used foreground raw `0x2007` but expected the type-7 skip path. Source condition is `((raw >> 8 & 0xF0) >> 4) < 2`; packed nibble 2 at `0x2007` correctly falls through to player-layer evaluation. The test fixture was corrected to `0x1007`, whose packed nibble 1 takes the skip path.
- The workflow run was on earlier PR merge commit `94892cb` and failed before Android storage tests or APK assembly. The corrected branch HEAD is `a9ede7ef62934ba88d2f0236b58d93709ea628a4`; a fresh successful run has not yet been confirmed.


### DEV-003V test-fixture correction
- Corrected left/top edge fixtures to make the target cell (playerX+deltaX, playerY+deltaY) land on coordinate 0. This matches the source boundary check; the earlier fixtures started at 0 but stepped inward and therefore did not represent an edge target.


## Resume update — 2026-11-27 (DEV-003W mutation-plan foundation)

### Implemented
- Added `LegacyCollisionMutationIntent`: typed layer, coordinate, expected old value, and requested new value.
- Added `LegacyCollisionMutationPlan`: immutable ordered write-set with probe-mode marker.
- Builder rejects all probe writes, repeated same-layer destinations, null intents, and reuse after build; distinct layers at the same coordinate remain separate destinations.
- Added `LegacyCollisionMutationPlanTest` for immutability, ordering, probe rejection, duplicate rejection, layer separation, and builder lifecycle.
- Added `DIRU-NA-DEV-003W_TRANSACTIONAL_COLLISION_INTENTS.md`.

### CI finding and correction
- Workflow #442 failed at `:core:compileTestJava`, before any test execution. The compiler identified one extra argument in the bottom-edge fixture of `LegacyPlayerLayerMinusOneEvaluatorTest`; corrected the fixture to match the helper signature.
- No successful CI run has yet been confirmed after this correction.

### Important boundary
- Mutation plans are proposals only. No stage writes, player-state writes, animation/cache updates, expected-value validation, or atomic applier exists yet. Do not connect this plan to GameSession until all write channels and source parity are represented.

### Next
DEV-003W2: add typed local-player and presentation/cache intents; then implement a preflight validator against a mutable owner-thread stage model, followed by an all-or-nothing applier and tests. Re-run the full CI pipeline before integrating with GameSession.


## Resume update — 2026-11-27 (DEV-003W2 state intents and preflight)

### CI baseline
- Verified workflow run #455 for prior checkpoint `01e2270017fb511176cc5832b21a66d27828f9c3`: completed successfully. Core JVM tests, Android storage unit tests, and Android debug APK assembly all passed. This is a baseline for that exact commit, not for the new W2 commits.

### Implemented
- Added `LegacyCollisionStateIntent` with validated PLAYER, ANIMATION, MOVEMENT_FLAGS, and coordinate-scoped FOREGROUND_CACHE fields.
- Extended `LegacyCollisionMutationPlan` to carry immutable grid and state intent lists; probe mode rejects both; duplicate destinations are rejected.
- Added `LegacyCollisionMutationTarget` owner-thread adapter contract with grid/state reads and a single atomic commit method.
- Added `LegacyCollisionMutationApplier` to preflight owner-thread, bounds, and expected values before invoking exactly one atomic commit; probe is a no-op.
- Added tests for state-channel validation, probe state-write rejection, duplicate state writes, out-of-bounds cells, stale grid/state values, owner-thread rejection, commit rejection, and one commit invocation.
- Added `DIRU-NA-DEV-003W2_STATE_INTENTS_AND_PREFLIGHT.md`.

### Atomicity caveat
- Production atomicity is NOT implemented. It is a mandatory contract on `LegacyCollisionMutationTarget.commitAtomically`; current tests use a recording fake only. Existing `StageGrid` is immutable and `GameSession` has no production collision transaction adapter.

### Next
DEV-003W3: implement a production owner-backed mutable stage/state store with copy-on-write transaction commit (validate all intents, clone affected layers/state, apply to clones, swap one immutable state root), then test rollback/no partial visibility and owner-thread enforcement. Only after that map evaluator decisions into plans and integrate with GameSession.


## Resume update — 2026-11-27 (DEV-003W3 copy-on-write store)

### Implemented
- Added `LegacyCollisionStageStateStore`, a single-owner implementation of `LegacyCollisionMutationTarget`.
- Store holds player/background/foreground, packed-metadata, auxiliary, foreground-cache arrays and scalar state behind one volatile immutable root.
- Successful commits clone private arrays/map, apply all intents to clones, then publish one new root reference.
- Expected values and bounds are revalidated inside the commit boundary, closing the applier-preflight/commit gap.
- Foreground cache reads and writes now use the same coordinate-indexed root array.
- Added detached immutable `stageSnapshot()` and root version counter.
- Added `LegacyCollisionStageStateStoreTest` for atomic multi-channel publication, old-snapshot stability, stale-state no-partial-write, out-of-bounds cache, probe no-op, and owner-thread enforcement.
- Added `DIRU-NA-DEV-003W3_COPY_ON_WRITE_STORE.md`.

### Limits / integration guard
- Store is not wired into GameSession.
- Existing StageGrid seeds only player/background/foreground; packed metadata and auxiliary arrays currently initialize to zero. A real decoded-stage import path is mandatory before collision integration.
- Scalar state map also initializes to zero; GameSession player/animation/movement state must be imported explicitly.
- No evaluator-to-plan mapper or recorded gameplay replay is connected.

### Next
DEV-003W4: define complete immutable initial-stage/session import including metadata, auxiliary arrays, player fields, animation and movement flags; expose detached full-root snapshots; add import/round-trip tests. Only then implement evaluator-to-plan mapping and integrate with GameSession.


## Resume update — 2026-11-27 (DEV-003W3/W4 copy-on-write store and complete seed)

### DEV-003W3 implementation
- Added `LegacyCollisionStageStateStore`, a single-owner copy-on-write implementation of `LegacyCollisionMutationTarget`.
- All grid arrays, metadata, auxiliary, foreground cache and scalar state are held behind one volatile root. Successful commits prepare cloned arrays/map and publish one root reference.
- Commit revalidates expected values and bounds; failed validation leaves the previous root/version unchanged.
- Fixed foreground-cache reads to use the published cache array (rather than the scalar state map).
- Added store tests for atomic multi-channel commit, old snapshot stability, stale state rejection, bounds, probe no-op and wrong-thread rejection.

### DEV-003W4 complete import
- Added immutable `LegacyCollisionStageStateSeed` for decoded StageGrid, packed metadata, auxiliary cells, foreground cache, player tile/directions/interpolation, animation ID/frame and movement-block flag.
- Added `LegacyCollisionStageStateStore(LegacyCollisionStageStateSeed)` production import constructor. The StageGrid-only constructor remains a zero-seeded test/prototype convenience.
- Added tests for complete seed import, defensive copies, and invalid array lengths.
- Added `DIRU-NA-DEV-003W3_COPY_ON_WRITE_STORE.md` and `DIRU-NA-DEV-003W4_COMPLETE_STATE_IMPORT.md`.

### Integration guard
- Seed API exists, but the real legacy decoder has not yet been audited/wired to populate metadata and auxiliary arrays. Do not infer packed formats or activate collision writes from zero-filled layers.
- GameSession still owns its original StageGrid/player fields separately. A single-owner integration refactor is required; avoid competing mutable owners.
- No evaluator-to-plan mapper, real-asset replay, or gameplay collision activation is claimed.

### Next
DEV-003W5: audit actual decoded-stage formats and the loader path, build a source-derived seed factory, and add real stage asset round-trip fixtures. Only after that map verified evaluator decisions to plans and refactor GameSession to use the store as its single state owner.


## Resume update — 2026-11-27 (DEV-003W5 raw legacy byte-state grids)

### Source audit correction
- Stage loader audit distinguishes the three serialized layers (player/object, background `bIntArrArr`, foreground) from runtime arrays.
- Packed tile metadata `eIntArrArr` is a separate 32-bit grid with three 9-bit groups.
- `bByteArrArr` and `cByteArrArr` are separate per-cell byte grids. Existing audits show `cByteArrArr` receives values including 24/30/48/127 and is decremented by 6 when positive; the six-argument metadata writer refreshes current and next-column cells to 24.
- Do not conflate the byte grids, packed metadata, and background runtime grid.

### Implemented
- Added `LegacyCollisionByteStateGrids` immutable snapshot type.
- Added typed `B_BYTE_STATE` and `C_BYTE_STATE` state channels with coordinate-scoped `BYTE_VALUE` fields.
- Extended `LegacyCollisionStageStateSeed` to import both byte grids defensively and validate exact cell counts.
- Extended copy-on-write store root, preflight reads, expected-value revalidation, cloning, commit writes, and snapshot access for both byte arrays.
- Signed byte values are preserved on reads; writes narrow to Java byte as source array assignment does.
- Added tests for defensive import, signed values, atomic multi-grid writes and immutable prior snapshots.
- Added `DIRU-NA-DEV-003W5_LEGACY_BYTE_STATE_GRID_IMPORT.md`.

### Boundaries
- The old seed `metadata` port corresponds to the packed-metadata slot; the source-derived factory must map actual `eIntArrArr` values into it.
- The old `auxiliary` and `foregroundCache` ports remain compatibility abstractions, not substitutes for bByte/cByte arrays.
- `bIntArrArr` is already StageGrid.backgroundLayer.
- No real decoder-to-seed adapter or GameSession integration yet.

### Next
DEV-003W6: trace all source allocation/read/write sites for bByteArrArr, cByteArrArr and eIntArrArr; introduce a named decoder-to-seed factory; add synthetic binary and real-format round-trip fixtures. Keep runtime activation disabled until a single authoritative owner exists.


## Resume update — 2026-11-27 (DEV-003W6 bounded stage stream parser)

### Repository / evidence baseline
- Working branch remains `dev-003cr-snapshot-provider-boundary`; PR #1 remains the existing review boundary (do not merge without explicit authorization).
- Read the current W3/W4/W5 implementation and source audits AUDIT-012 and AUDIT-029.
- AUDIT-029 confirms the stage stream layout: opaque prefix byte, unsigned stage-count byte, then repeated LE u16 width/height and three width×height byte layers (player/object, background, foreground).
- The pinned static binary inventory covers 41 records: 14 Angkor, 13 Scotland/Bavaria-resource, 14 Tibet/Siberia-resource. The opaque prefix meaning and two map/title count anomalies remain unresolved.

### Implemented
- Added `core/.../stage/WorldStageStreamParser.java`.
- Parser uses a bounded cursor, validates selected index, dimensions and configured limits, checks payload availability before allocation, skips unselected records exactly, and rejects trailing bytes.
- Preserves signed-byte widening into StageGrid integer arrays.
- Added `WorldStageStreamParserTest.java` for multi-record selection/skipping, layer order/dimensions, signed bytes, invalid index, truncation, trailing bytes, limits, zero count and zero dimensions.
- Added `DIRU-NA-DEV-003W6_WORLD_STAGE_STREAM_PARSER.md`.

### Scope guard
- Parser returns the three serialized authored layers only. It is not a full runtime-preprocessed stage or collision seed.
- Runtime arrays `bByteArrArr`, `cByteArrArr`, `eIntArrArr`, transformed foreground, session scalars and object tables are created or changed by later initialization passes.
- No GameSession integration or gameplay collision activation.
- No real source assets copied into this repository; static audit evidence only.

### Next
DEV-003W7: build a provenance-safe per-world/per-stage fixture manifest and add an explicit stage preprocessing result model based on the complete audited step-9 pass. Continue distinguishing serialized authored layers from runtime-derived state. Do not construct a production collision seed until every required runtime field has a verified source and owner.


## Resume update — 2026-11-27 (DEV-003W7 stage fixture manifest)

### Verified baseline
- Working branch: `dev-003cr-snapshot-provider-boundary`.
- PR #1 remains open, draft and unmerged; do not merge without explicit authorization.
- Stage parser W6 and AUDIT-029 were fetched and reviewed before this update.
- AUDIT-029 records 41 stage dimensions from the pinned candidate resource audit: Angkor 14, Scotland resource/Bavaria UI 13, Tibet resource/Siberia UI 14; total 50,453 cells per layer.
- The 14/13/14 inventory is not a title or map-node mapping. Angkor and Tibet/Siberia have unresolved catalog count mismatches.

### Implemented
- Added `core/.../stage/WorldStageFixtureManifest.java` with explicit world identities, zero-based indices, dimensions, checked cell counts and defensive entry enumeration.
- Added `WorldStageFixtureManifestTest.java` verifying counts, selected dimensions, total 50,453 cells per layer, and invalid input handling.
- Added `DIRU-NA-DEV-003W7_WORLD_STAGE_FIXTURE_MANIFEST.md`.

### Integrity boundary
- No per-stage SHA-256 values were invented. The current audit provides whole-resource sizes and parsing results, but per-stage hashes were not recomputed from verified source bytes in this implementation session.
- No source payloads were copied into the app or repository.
- No stage title/map node relationships were inferred.
- This is static manifest metadata and JVM tests, not retail-runtime validation.

### Next
DEV-003W8: define a separate immutable stage-preprocessing result model only after tracing exact allocation, write order and ownership for each runtime-derived field. Keep unknown fields unresolved and keep GameSession integration/collision activation blocked.


## Resume update — 2026-11-27 (DEV-003W8 preprocessing result boundary)

### Source inspection
- Re-fetched AUDIT-012, AUDIT-014, W7 manifest, StageGrid, W6 parser, and collision seed from the working branch.
- Fetched pinned candidate `kubikaugustyn/DiamondRushSource/src/main/java/i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881` and directly inspected stage-load step 8 and step-9 preprocessing.
- Step 9 scans x-major/y-minor; resets bByte/cByte per cell; mutates foreground and background runtime layers; sets byte-state values; and builds derived counters, flags, coordinate/entity tables.
- Foreground codes can read/write neighboring cells. Preserve source scan order and explicitly validate neighbor preconditions in Native implementation.

### Implemented
- Added `core/.../stage/LegacyStagePreprocessingResult.java`: immutable defensive-copy boundary for transformed StageGrid, background runtime values, bByte and cByte grids.
- Added `LegacyStagePreprocessingResultTest.java`: copy isolation, exact lengths, bounds checks.
- Added `DIRU-NA-DEV-003W8_STAGE_PREPROCESSING_RESULT_BOUNDARY.md`.

### Scope guard
- This is a result container, not the full step-9 preprocessing algorithm.
- Derived door tables, counters, feature flags, entity arrays, player/camera setup are not yet represented.
- No ParsedStage-to-result adapter, collision seed creation, or GameSession integration.
- Static candidate source only; no runtime validation.

### Next
DEV-003W9: build a per-branch step-9 source-evidence matrix (scan order, reads/writes, neighboring-cell preconditions, derived arrays/counters/flags) and implement isolated transformations with table-driven tests. Keep unverified behavior unresolved.


## Resume update — 2026-11-27 (DEV-003W9 step-9 evidence matrix)

### Source inspection
- Directly inspected the pinned `i.java` step-9 source at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.
- Confirmed x-major/y-minor traversal and per-cell reset of bByte/cByte.
- Mapped directly observed foreground branches including 31/19/17/14/33/2/8/9/7/30/1/26/0/4/5/28/3/6/20..25/34.
- Mapped directly observed player/object branches including 48/47/46/45/44/42/41/40/12/36/18/34/35/39/38/14/28/79/11/49/43/19/22/23/30/37/10/16/6/7/26/24/27/8/4/5/2/0/1.
- Recorded derived arrays and counters created after the first scan. Second scan remains incomplete.
- Recorded neighbor and index hazards; no full semantic tile catalog claimed.

### Implemented
- Added `DIRU-NA-DEV-003W9_STEP9_MUTATION_EVIDENCE_MATRIX.md` as a source-evidence matrix. This is documentation/audit, not executable preprocessing.

### Scope guard
- W8 result type exists but is only a container.
- No full preprocessing executor, production seed adapter, or GameSession integration.
- The branch matrix is partial; exact helper semantics, second scan, and safe edge behavior remain unresolved.
- No runtime validation against retail build.

### Next
DEV-003W10: implement a narrow phased preprocessing pass beginning with per-cell byte resets and only fully bounded, directly evidenced branches. Unsupported/unmapped codes must fail closed. Add table-driven fixtures and do not publish production collision state from partial preprocessing.
