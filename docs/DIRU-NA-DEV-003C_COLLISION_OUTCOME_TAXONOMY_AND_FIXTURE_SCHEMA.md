# DIRU-NA-DEV-003C — Collision Outcome Taxonomy and Fixture Schema

**Status:** Trace model and synthetic unit fixtures committed; collision evaluation itself remains unimplemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source method:** `i.aBoolean(int dx, int dy, boolean probe)`, lines 12073–12443

## 1. Purpose

The source helper does not return a simple passability boolean. It has two separate local flags (`b2` and `b3`), a probe argument, and multiple independent state/world side effects. This milestone introduces a trace representation that retains those facts without pretending to execute the original helper.

## 2. Trace fields

`LegacyCollisionTrace` stores:
- source branch category;
- probe mode;
- raw helper return flag `b2`;
- raw secondary interaction flag `b3`;
- whether `aWInt == 0`;
- whether tile coordinates were committed;
- whether interpolation progress was initialized;
- whether packed moving bit `0x8` was set;
- whether an animation was requested;
- whether a world/map mutation was observed;
- whether player state mutation was observed.

The names `returnFlagB2` and `interactionFlagB3` intentionally preserve source-local semantics. They are not relabeled `canMove`, `blocked`, or `interact`.

## 3. Branch catalogue

| Branch enum | Source evidence | Caution |
|---|---|---|
| `STAGE_BOUNDS` | destination bounds before foreground lookup | `b2=true` can still reach the coordinate-commit block when `aWInt=0`; do not rename it blocked |
| `FOREGROUND_TYPE_1` | sets timer/counter and calls helper | world effects depend on probe |
| `FOREGROUND_TYPE_2_INTERACTION` | separate `b3` path and item/interaction checks | `b3` is not equivalent to `b2` |
| `FOREGROUND_TYPE_3_METADATA` | metadata threshold branches | both `b2` and local `n10` are involved |
| `FOREGROUND_TYPE_4_CHECKPOINT` | checkpoint index and save flag | mutates persistent/player state outside probe |
| `FOREGROUND_TYPE_5_FORCED_MOVEMENT` | `b2=true`; non-probe enables `xBoolean` | not a normal tile step |
| `FOREGROUND_TYPE_7_METADATA` | foreground metadata nibble | path may proceed to player layer |
| `FOREGROUND_TYPE_19_SCRIPTED_PATH` | scripted map path and world changes | not a normal tile step |
| `FOREGROUND_TYPE_28_FORCED_MOVEMENT` | non-probe enables `xBoolean` and clears active direction | distinct from type 5 |
| `PLAYER_LAYER_34_35` | `b2=probe` | return behavior differs by probe |
| `PLAYER_LAYER_31_49` | `b2=false` | explicit special values |
| `PLAYER_LAYER_BLOCKING_SET` | group of player-layer values sets `b2=true` | exact list must remain explicit in later grid fixtures |
| `PLAYER_LAYER_TYPE_10` | conditional `xByte==3` branch | metadata/counter dependent |
| `PLAYER_LAYER_TYPE_48` | separate two-cell geometry and state changes | object interaction, not generic wall |
| `PLAYER_LAYER_EMPTY` | empty cell plus edge/door checks | can clear packed moving state |
| `PLAYER_LAYER_OTHER` | default branch | may zero local delta and request an animation |
| `EARLY_RETURN_KBYTE_4` | early false return after animation request | proves return value does not encode all side effects |

This is a branch inventory, not a promise that each enum is already wired to a simulator.

## 4. Commit and interpolation invariant

At the end of the source helper, the coordinate/progress block is guarded by:

`b2 && aWInt == 0`

When entered, it sets local `jInt = 18` and applies the requested delta to local `crtPlayerX/Y`. The local snapshot is written back only when `probe == false`.

The trace constructor enforces:
1. probe mode cannot record a committed tile coordinate;
2. a tile commit requires `b2 == true`;
3. a tile commit requires `aWInt == 0`;
4. progress initialization and tile commit occur together in this source branch.

It does not infer that a committed coordinate means the destination was ordinarily passable. The stage-bounds branch is a counterexample to that simplification.

## 5. Probe is not synonymous with side-effect free

The probe flag suppresses many writes guarded by `!probe`, including final player-field writeback and the foreground type-19 scripted map path (that branch exits before its map mutations when `probe == true`). However, the player-layer type 0/8/9/47 path can still request an animation through the `kByte == 4` early-return branch. A future native implementation must classify each call site; probe is not a promise that every observable side effect is suppressed.

The trace keeps `worldMutationObserved` and `playerStateMutationObserved` independent from `probeMode` so fixtures can preserve such observations.

## 6. Synthetic fixture set

The JUnit suite currently records these synthetic observations:
- bounds branch with `b2=true` and commit when counter is zero;
- forced-movement branch in probe mode with no commit;
- interaction branch with both `b2` and `b3` true;
- early-return animation request without tile commit;
- probe-mode early return that requests an animation without committing tile coordinates or mutating the world;
- separate type-5/type-28 and type-2/type-4 branch identities;
- invalid combinations rejected by trace invariants.

These are trace-construction fixtures, not a simulation of original stage data or execution of the decompiled method.

## 7. Explicitly not implemented

- no stage grid or foreground/player layer storage;
- no collision evaluation algorithm;
- no player movement state machine;
- no object-push simulation;
- no checkpoint persistence;
- no animation engine changes;
- no Android UI/input wiring.

## 8. Next work item

**DIRU-NA-DEV-003D — Source Branch Matrix and Caller Integration Contract.** Trace the normal movement caller around the helper and turn each source branch into a table with: preconditions, helper inputs, helper flags, state writes, map writes, return handling, and follow-up animation. Then design a typed result only after that matrix has no ambiguous branch mappings.
