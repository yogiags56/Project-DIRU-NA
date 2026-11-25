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
