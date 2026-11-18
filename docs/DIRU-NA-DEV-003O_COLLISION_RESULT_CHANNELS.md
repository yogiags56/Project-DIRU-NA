# DIRU-NA-DEV-003O — Collision Result Channels and Return Trace

**Status:** Immutable result-channel model and synthetic tests committed; no collision evaluator  
**Date:** 2026-11-19  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source method:** `i.aBoolean(int,int,boolean)`, lines 12073–12444

## 1. CI correction for DEV-003N

Run #31 failed two type-7 precedence tests because the fixtures encoded the foreground type in the low byte incorrectly. The legacy switch reads the low byte as the type and checks bits 4–7 of the high byte for type-7 metadata. Correct raw examples are `0x0007` (metadata nibble 0), `0x1007` (nibble 1), `0x2007` (nibble 2), and `0xF007` (nibble 15). The precedence implementation was not changed; only test fixtures were corrected.

## 2. Return-path observations

The pinned helper has multiple independent local channels:
- `b2`: the boolean ultimately returned on the normal path;
- `b3`: an auxiliary branch flag, set in foreground type 2's non-probe branch;
- `n3`: a foreground-to-player-layer control-flow gate;
- `axInt`: a local copy of the movement/interaction counter, decremented in selected object branches;
- `kInt2`: direction/state bits, including bit 0x8 used by later state handling.

A special two-step branch for player-layer 0/8/9/47 returns `false` immediately when the `kByte==4` geometric threshold is met. This bypasses the normal final state writeback.

At normal completion, the method returns `b2`. The method may commit local state only when `probe==false`; this is distinct from the returned boolean. The source also conditionally updates `aDInt`, local movement coordinates, direction state, and animation/helper state before writeback.

## 3. Result model

`LegacyCollisionEvaluationResult` is an immutable carrier with separate fields for:
- normal versus early completion;
- returned boolean;
- local collision flag;
- auxiliary branch flag;
- whether the player-layer switch was evaluated;
- whether state commit is suppressed.

It does not infer these values, execute branches, or mutate game state. The constructor enforces the observed early-return contract: the traced early-return path returns false.

## 4. Tests

Tests ensure return value, local collision, auxiliary branch, player-layer visitation, and commit suppression remain distinct. Invalid early-return/true combinations and null completion are rejected.

## 5. Boundary

This milestone does not implement a full evaluator, reproduce all state writes, or claim gameplay movement. It provides a faithful result vocabulary for the next evaluator implementation.

## 6. Next

**DEV-003P — Foreground Type 7 and Type 3 Pure Evaluation.** Implement the narrow foreground-only decision table for the two branches that control `n3`, preserving their distinct metadata expressions and the fact that both skip player-layer evaluation. Keep all side effects out of the pure evaluator.
