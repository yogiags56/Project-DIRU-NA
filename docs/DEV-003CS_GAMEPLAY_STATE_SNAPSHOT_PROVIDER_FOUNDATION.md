# DEV-003CS — Gameplay State Snapshot Model & Provider Foundation (Proposal)

**Status:** Proposed next milestone; not yet approved or implemented.  
**Predecessor:** DEV-003CR — Snapshot Provider & Lifecycle Integration Boundary.  
**Branch context:** `dev-003cr-snapshot-provider-boundary`.

## 1. Purpose

Turn the checkpoint boundary established by DEV-003CR into a real, gameplay-owned snapshot provider without inventing or persisting placeholder game state.

DEV-003CR established detached checkpoint values, ordered asynchronous persistence, candidate-first restore, and a tested Android storage boundary. It deliberately did not connect Activity lifecycle events to a nonexistent gameplay state owner. DEV-003CS is proposed to establish the minimum authoritative gameplay-state model and implement a provider only for fields whose semantics and ownership are supported by the source audits.

## 2. Evidence and constraints

Use the existing read-only audit records as evidence, especially:

- `DIRU-NA-AUDIT-004_GAMEPLAY_STATE_MOVEMENT.md` — movement, collision, checkpoint capture/restore observations and open semantic questions.
- `DIRU-NA-AUDIT-010_GAME_STATE_SCREEN_TRANSITIONS.md` — separation of screen/navigation state from active gameplay state and lifecycle routing.
- `DIRU-NA-AUDIT-012_STAGE_LOADING_GAMEPLAY_INITIALIZATION.md` — incremental stage loading, three authored layers, preprocessing, and resource ownership.
- The DEV-003CQ/CR records — detached checkpoint candidates, owner-thread requirements, ordered save behavior, and non-destructive recovery.

The candidate source is research evidence only. Do not copy or redistribute proprietary/decompiled code or assets. Preserve unresolved meanings as explicitly unknown; do not guess field semantics to make a snapshot appear complete.

## 3. Scope

### In scope

1. **State ownership map**
   - Identify the authoritative gameplay simulation state separately from menu, loading, credits, and other screen states.
   - Define one explicit owner thread/executor for capture and restore application.
   - Document which state fields are mutable, derived, transient, or resource handles.

2. **Snapshot schema audit**
   - Map each proposed persisted field to source evidence, current Native model type, validation rule, and compatibility status.
   - Separate durable progress/checkpoint data from transient animation, input, render, audio, and Android lifecycle state.
   - Define required, optional, and unsupported fields. Unknown fields must not be silently fabricated.

3. **Provider contract and implementation**
   - Implement a gameplay-owned provider that captures an immutable detached checkpoint on the owner thread.
   - Apply a decoded candidate only on the same owner thread, after full validation.
   - Ensure capture failure or invalid restore leaves the live gameplay state unchanged.
   - Never save a default/empty placeholder over an existing user checkpoint.

4. **Deterministic tests**
   - Verify detached snapshot immutability after capture.
   - Verify capture and apply reject or dispatch from the wrong thread.
   - Verify invalid/incomplete candidates do not mutate live state.
   - Verify a valid snapshot round-trips through the existing codec and coordinator.
   - Verify no snapshot is produced while gameplay state is unavailable or stage initialization is incomplete.

5. **Integration boundary**
   - Add a narrow application integration point only after a real gameplay state owner exists.
   - Keep Activity callbacks as save-request signals; they must not read mutable gameplay fields directly.
   - Route save requests to the game owner for capture, then pass the immutable value to the DEV-003CR I/O dispatcher.
   - Route restore candidates back to the owner thread before applying them.

### Out of scope

- Reconstructing the entire game engine or completing all movement/collision semantics.
- Replacing the DEV-003CR persistence codec or storage adapter without evidence of a defect.
- Saving renderer objects, Android Views, input key states, audio handles, threads, or resource references.
- Wiring Activity destruction as a guaranteed save event.
- Claiming physical-device process-kill durability; that remains separate device validation.
- Adaptive aspect ratios, touch UI, or release packaging.

## 4. Proposed implementation sequence

| Step | Deliverable | Exit condition |
|---|---|---|
| CS-1 | Gameplay state ownership and lifecycle diagram | Simulation owner and screen-state boundary documented |
| CS-2 | Persisted-field evidence matrix | Every included field has evidence, type, validation, and compatibility decision |
| CS-3 | Native gameplay state model seam | Minimal real state owner exists; no placeholder-only model |
| CS-4 | Immutable snapshot provider | Capture/apply run on the owner thread and produce detached values |
| CS-5 | Provider and restore tests | Deterministic tests cover immutability, invalid candidates, thread ownership, and round-trip |
| CS-6 | Lifecycle request integration | Pause/background requests ask the owner to capture; no direct Activity state reads |
| CS-7 | CI and documentation | Core tests, Android tests, debug APK assembly pass; docs record limits |

## 5. Acceptance criteria

DEV-003CS may be marked complete only when all are true:

- A concrete gameplay state owner exists; a test-only fake does not count as the production provider.
- The owner-thread contract is explicit and enforced by implementation/tests.
- The persisted-field matrix is evidence-backed and excludes unresolved/transient fields.
- Snapshot capture returns an immutable detached value and cannot mutate the live game.
- Restore validates the complete candidate before applying it atomically on the owner thread.
- Capture/restore failure preserves the current live state and existing persisted checkpoint.
- Lifecycle save requests do not race mutable gameplay state or rely on Activity destruction.
- CI passes core JVM tests, Android unit tests, and debug APK assembly.
- Documentation states what remains unverified on physical devices.

## 6. Known blockers and decision gates

The repository README still identifies gameplay reconstruction and the first reproducible gameplay milestone as incomplete. Therefore, provider implementation cannot honestly begin by connecting the checkpoint system to an authoritative production game state until the minimum gameplay state owner has been created.

If the source audit cannot establish a field's meaning, leave it out of the persisted schema and record the gap. Do not widen the milestone with unrelated reverse-engineering work; create a separate audit task when necessary.

## 7. Relationship to DEV-003CR

DEV-003CR remains the persistence/provider boundary foundation and is not reopened by this proposal. Its source-level CI gate passed, including core JVM tests, Android storage tests, debug APK assembly, and injected AtomicFile write-failure rollback. PR #1 remains separate review/merge work. Physical-device validation is tracked independently.

## 8. Approval state

This document is a proposed definition prepared after the DEV-003CR source-level exit gate. It does not claim that DEV-003CS was previously named or approved in the roadmap. Treat the title, scope, and acceptance criteria as a proposal until the project owner confirms them.
