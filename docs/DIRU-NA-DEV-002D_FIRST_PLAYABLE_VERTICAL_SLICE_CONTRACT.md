# DIRU-NA-DEV-002D — First Playable Vertical Slice Contract

**Status:** Implementation contract / readiness gate; no gameplay owner or playable slice is claimed  
**Date:** 2026-11-27  
**Repository:** yogiags56/Project-DIRU-NA  
**Source baseline:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. Purpose

Turn the existing source audit and pure-core primitives into the smallest deterministic Native gameplay slice. This document is a bounded implementation contract, not evidence that the slice exists. The first slice must prove that input can drive a player through a stage-owned world state, with explicit collision outcomes and deterministic update steps, before renderer/audio/persistence integration is expanded.

The project baseline remains a 240×320 portrait logical viewport. The core simulation must not depend on Android classes, display refresh rate, Canvas, or storage APIs.

## 2. Existing implementation inventory

The repository already contains:
- HeldKeyState, LegacyDirectionResolver, and LegacyInputDispatcher with unit tests;
- LegacyDirectionVector with source-derived tile deltas;
- immutable collision input/neighborhood snapshots, checked coordinate resolver, safe capture, branch input validation, and typed observation/result carriers;
- isolated pure evaluators for selected foreground branches and packed-record/tile metadata helpers;
- Android application shell and core JVM module;
- DEV-003CR persistence boundary and Android private-storage adapter.

These components are isolated contracts and pure helpers. They are not yet composed into a single mutable gameplay owner. The README correctly continues to mark the first reproducible gameplay milestone incomplete.

## 3. Vertical-slice boundary

### Included
1. A single-writer, platform-independent GameSession (or equivalently named owner) with explicit lifecycle: create stage, accept input snapshot, advance one deterministic simulation tick, expose immutable render snapshot.
2. A stage model with explicit width/height and separate player/object, background, and foreground layers. Preserve source signed-byte widening for background values where required.
3. A player model keeping pending direction, active direction, logical tile coordinate, interpolation progress, and packed state flags distinct.
4. Source-compatible input dispatch and direction-vector mapping.
5. A deliberately narrow collision evaluator for only the fully proven subset. Any unimplemented source branch must return an explicit unsupported/incomplete outcome; it must never silently become passable.
6. Deterministic tick tests for input priority, move start, collision rejection, coordinate commit, interpolation countdown, and stage-edge safety.
7. A synthetic, legally authored test stage fixture. Do not copy proprietary level or sprite payloads into this repository.

### Excluded
- Full 41-stage content integration until rights/provenance are reviewed.
- Full enemy AI, puzzle inventory, door-group scripts, object-push and falling systems unless required by the chosen slice and independently proven.
- Renderer parity, audio playback, touch UI polish, adaptive aspect ratios.
- Production checkpoint provider integration before a real owner exists.
- Any claim of original-device parity without runtime comparison.

## 4. Required owner contract

The owner is the only writer of mutable gameplay state. Android input callbacks enqueue normalized key transitions or provide an immutable input frame; they do not mutate player or stage fields. The owner serializes all simulation transitions.

Minimum state:
- stage identity and compatibility key;
- immutable stage dimensions and validated layer storage;
- player logical X/Y;
- pending and active direction;
- interpolation progress and source-observed movement flags;
- held-key bitset / current input frame;
- simulation tick index;
- explicit state for any supported collision interaction.

Rendering consumes an immutable projection captured after a tick. Rendering must not advance simulation or mutate the stage.

## 5. Movement contract and source fidelity

The first movement slice must preserve these audited facts:
- Normal input priority is UP, DOWN, LEFT, RIGHT; simultaneous input selects the first matching branch.
- Direction codes map to logical tile deltas: 1=(0,-1), 2=(+1,0), 3=(0,+1), 4=(-1,0).
- Pending direction is distinct from active direction.
- Ordinary movement eligibility is state-gated; a requested direction alone does not imply movement begins.
- The common ordinary interpolation path starts at 18 and decrements by 6 per applicable update: 18→12→6→0.
- Direction 5, direction 6/object-push, special movement timer, and scripted displacement are separate paths, not generic ordinary movement.
- The legacy collision helper's boolean return is not named canMove; preserve return flag, interaction flag, probe mode, local state, and persisted commit as separate channels.
- Collision must be checked before any Native logical tile commit. Probe calculations must not commit mutable state.

No fixed milliseconds-per-move value is allowed until source tick cadence is established.

## 6. Narrow collision acceptance policy

A first slice may support only a documented branch subset. For each supported branch:
- capture all required named samples;
- validate every coordinate before indexing;
- retain raw packed values;
- evaluate into a typed immutable decision;
- apply the decision and related stage/player mutations atomically on the owner thread;
- test both accepted and rejected cases.

If a branch is not supported, the owner returns UNSUPPORTED_BRANCH or an equivalent typed non-commit result. No default-to-empty, default-to-passable, or guessed metadata interpretation is allowed.

## 7. Deterministic acceptance tests

The first playable milestone is not accepted until all of these pass:
1. Stage fixture dimensions and all layer lengths validate; malformed/overflowing dimensions reject before allocation.
2. Player spawn is in bounds and does not overlap an invalid fixture cell.
3. Input priority exactly matches the audited order.
4. A supported empty/traversable move commits exactly one logical tile delta.
5. A blocked or unsupported target leaves player coordinates and all stage layers unchanged.
6. Ordinary interpolation follows 18→12→6→0 with no negative progress.
7. A second move cannot start while ordinary interpolation is active unless a separately audited path allows it.
8. Probe mode never commits player or stage state.
9. Every out-of-bounds target/neighbor returns a typed failure without array access.
10. Identical initial state + identical input/tick sequence produces byte-for-byte/equality-identical simulation snapshots.
11. Android debug APK assembles and launches the gameplay shell; launch alone is not gameplay acceptance.
12. A recorded manual test demonstrates at least one complete stage movement interaction on device/emulator using the synthetic fixture.

## 8. Dependency and sequencing gates

- DEV-003CS-2 remains an evidence workstream. It must close its critical field-ownership and compatibility questions before a production snapshot schema is approved; it need not block a synthetic movement slice that does not persist state.
- Existing DEV-003A–003U documents provide source movement/collision contracts and sample requirements. Existing DEV-003P–003T evaluators cover selected foreground branches only; they do not form a complete collision evaluator.
- Before writing the owner, complete the minimal collision branch subset and prove all required context/samples. Then implement owner, deterministic tests, and synthetic stage fixture.
- Integrate DEV-003CS snapshot provider only after the owner exists and owns all mutable gameplay state.

## 9. Exit evidence

Required artifacts:
- core source for owner, stage model, player motion state, supported collision evaluator and immutable render snapshot;
- unit tests and deterministic fixtures;
- updated README/roadmap with exact supported branch list;
- CI run on the exact HEAD showing core tests and Android debug APK assembly;
- short manual verification record identifying device/emulator, build SHA, fixture, input sequence, and observed result.

Until all evidence exists, README must continue to say “First reproducible gameplay milestone” incomplete.


## Implementation progress update — 2026-11-27

First isolated core primitives have now been added:
- `core/stage/StageGrid.java`: immutable row-major three-layer stage grid, defensive copies, positive/bounded dimensions, exact layer lengths, checked coordinate access. Values remain ints so a future decoder can preserve signed-byte widening.
- `core/movement/OrdinaryMovementInterpolation.java`: ordinary-only 18→12→6→0 source-derived update clock. It does not perform collision, mutate stage data, or commit coordinates. Special/scripted movement remains separate.
- Unit tests cover defensive copying, row-major reads, bounds, overflow/invalid layer sizes, interpolation sequence, active-move guard, invalid direction, and reset.

Not implemented yet: GameSession owner, player logical state, collision gate, input-to-tick integration, immutable render projection, Android shell hookup, and end-to-end synthetic fixture. This is a foundation increment, not a playable milestone.
