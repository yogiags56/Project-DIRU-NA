# DIRU-NA-DEV-003G — Branch Input Completeness Audit

**Status:** Static read audit, missing-input validator, and unit tests committed; collision evaluator not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source method:** `i.aBoolean(int,int,boolean)`, lines 12073–12444

## 1. Audit method

Every array access in the helper body was reviewed, including read-only dependencies, mutation destinations, and the called `eBoolean(int,int)` predicate (lines 14309–14313). A distinction is maintained between:
- values required to decide a branch;
- values required to reproduce a side effect;
- values written by the helper but not read as branch inputs.

The audit does not infer unknown semantics from obfuscated names.

## 2. Direct array read inventory

| Source location | Read | Requirement |
|---|---|---|
| 12106 | optional `eIntArrArr[target]` and `iByteArr[10]` | auxiliary-grid presence/value plus global auxiliary gate |
| 12113–12114 | foreground target raw | low-byte type and high-bit metadata |
| 12120 | player-layer scan at `(targetX+3, scanY)` | ordered type-19 upward scan until value 39 |
| 12130 | `bIntArrArr[anchor]` | packed metadata at type-19 anchor |
| 12134 | foreground values down target column | ordered type-19 clear-run until first non-19 |
| 12150, 12159, 12192, 12230 | foreground target high bits | type-specific metadata |
| 12194, 12204 | `iByteArr[9]` | inventory/collection counter for foreground type 2 |
| 12243 | player-layer target | player-layer branch selector |
| 12281 | packed metadata at target | player-layer type 10 initialization guard |
| 12303–12304 | foreground two-step target | low byte and high metadata for push path |
| 12305–12306 | player-layer and foreground at `(targetX, playerY+1)` | support/obstruction checks |
| 12306 and eBoolean | player-layer and foreground at two-step target | `eBoolean`: player layer -1 and foreground not 14/33/5/28 |
| 12316 | packed metadata at target | object-push metadata transfer |
| 12341 | packed metadata at target | clear low direction bits on failed push |
| 12350 | packed metadata at target | type-48 geometry selector bit 0x8 |
| 12358 | two player-layer destination cells and support cell | type-48 acceptance test |
| 12364–12365 | packed metadata at source cells | type-48 metadata transfer |
| 12389 | player-layer and packed metadata above/below target | empty-cell door/obstacle gate |

The helper writes, but does not read within this method, `crtStagePlayerLayer`, `bIntArrArr`, `bByteArrArr`, `cByteArrArr`, and foreground cells in the type-19 clear-run. These are output mutation ports, not additional input requirements for the initial branch decision.

## 3. Scalar and external dependencies

The branch decision and output are not determined by map cells alone.

| Source field / helper | Branches | Required input category |
|---|---|---|
| `probe` parameter | all | probe mode |
| `aWInt` | final local coordinate/progress commit | movement counter |
| `iByteArr[10]` | auxiliary-grid gate | global auxiliary gate |
| `iByteArr[9]` | foreground type 2 | inventory counter |
| `aCInt`, `i.aSInt` | checkpoint type 4 | checkpoint index and stage counter |
| `xByte` | player-layer type 10 | type-10 mode |
| `aXInt` | player-layer 0/8/9/47 and 48 | push/movement progress |
| `kByte`, `dInt()`, `eInt()` | player-layer 0/8/9/47 early return | gate plus derived stage/sprite vertical geometry |
| `arInt`, `bInt`, animator slot 5 current frame and frame metadata | `dInt/eInt` | derived two-step/vertical threshold inputs |
| `aNInt`, `aPInt` | empty player-layer | two independent door/obstacle timers |
| `lBoolean` | foreground type 19 | one-shot scripted-path gate |

`dInt()` is `10 + arInt * ((arInt > 0 ? 1 : 0) + 2)`. `eInt()` depends on stage height, animator slot 5 Y, current animation/frame, frame metadata field 3, and `bInt`. These cannot be replaced by a guessed fixed coordinate.

## 4. Neighborhood model correction

The type-19 path has two distinct ordered sequences:
1. upward player-layer scan at fixed X = targetX + 3 until player-layer value 39;
2. downward foreground run at fixed X = targetX, starting at targetY + 1, until low byte is not 19.

The neighborhood snapshot now stores both sequences independently, plus the anchor. This avoids conflating the anchor search with the foreground clearing run.

The type-48 path requires seven named samples: target, two-step target, two destination cells, two source-clear cells, and support. The empty-layer path requires target, target-above-player-row, target-relative above, and target-relative below.

## 5. Validator contract

`LegacyCollisionBranchInputValidator` verifies that a synthetic branch fixture supplies the required named samples and source context fields. Missing values are returned explicitly. It never fills defaults and never decides whether a move succeeds.

This is a conservative first validator:
- it validates declared branch-specific minimums;
- it does not execute helper logic;
- it does not yet validate every sample coordinate against a source expression;
- it does not validate stage bounds for the data-dependent type-19 scan;
- it does not prove all external helper side effects.

## 6. Tests

Tests cover:
- out-of-bounds branch with no cell sample requirement;
- foreground type 2 missing inventory counter;
- incomplete type-48 geometry;
- empty-layer missing timer inputs;
- type-19 missing scan/clear-run/anchor;
- complete explicit type-19 sample lists;
- null branch rejection.

These tests validate the data contract only, not gameplay collision behavior.

## 7. Remaining gaps before evaluator implementation

1. Add coordinate formula validation for every named role.
2. Add an explicit stage-bounds proof for all scan and two-step coordinates.
3. Represent auxiliary-grid presence separately from its global gate and value.
4. Capture output mutation targets for object push and type-48 object movement.
5. Model the external `aByte`, `mVoid`, `kVoid`, `bVoid`, and `gVoid` effects as trace ports.
6. Decide whether a branch fixture represents pre-state, post-state, or both.

## 8. Next work item

**DEV-003H — Coordinate Binding and Bounds Proof.** Define a checked coordinate resolver for each named sample role, reject out-of-stage reads before array access, and model the type-19 dynamic scan as a bounded sequence that terminates only when its sentinel is present. Keep the legacy source's unbounded scan as an observed behavior, but make the native reconstruction safe against malformed stage data.
