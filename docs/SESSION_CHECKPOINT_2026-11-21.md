# DIRU-NA Session Checkpoint — 2026-11-21

> Recovery checkpoint for continuing work if the ChatGPT conversation is interrupted, becomes full, or errors. Read this file first, then verify the live branch head and CI before editing.

## Repository state

- Repository: `yogiags56/Project-DIRU-NA`
- Working branch: `dev-003cr-snapshot-provider-boundary`
- Last verified HEAD when this checkpoint was prepared: `1414300b768599cf915c28b4eacbcff1e8bb3efb`
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

Finish CS-2 evidence review (updated after copy/restore trace at `a163dd3a`):
1. Trace every field copied by candidate `saveCheckpoint()` and assigned by `restoreCheckpoint()`, using precise source locations where available.
2. Enumerate dimensions and allocation paths for all stage layers and byte arrays.
3. Enumerate dimensions and invariants of integer object-state arrays and distinct byte-state arrays `bByteArrArr` / `cByteArrArr`.
4. Trace progression/inventory ownership versus native profile-save model.
5. Reconcile snapshot model with the first reproducible gameplay milestone in DEV-002.
5. Resolve optional mode arrays and first-build scope.
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

- `637acae2` — refine copy/restore trace: `bByteArrArr` has checkpoint category evidence; `cByteArrArr` checkpoint inclusion remains unproven.
- `1414300b` — update field matrix with the same byte-array inclusion distinction.

- `8d7b0680` — add CS-2 copy/restore trace to the working branch (the trace was kept off `main`; an initial misplaced copy on `main` was immediately removed in cleanup commit `20161a02`).

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
