# DIRU-NA-DEV-003N — Foreground / Player-Layer Precedence

**Status:** Control-flow policy and synthetic tests committed; full collision evaluator not implemented  
**Date:** 2026-11-19  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchor:** `i.java:12113–12243`

## 1. CI

DEV-003M run #30 completed successfully, including core JVM tests, Android debug APK assembly, and artifact upload.

## 2. Key control-flow finding

The foreground switch does not generally suppress player-layer evaluation. The legacy helper initializes `n10=0`, sets it only in foreground type 7's low metadata branch or foreground type 3, then assigns `n3=n10`. The player-layer switch runs only when `n3==0`.

Therefore the exact precedence rule in this source region is:

- Foreground type 7 with `((raw >> 8 & 0xF0) >> 4) < 2`: skip player-layer switch.
- Foreground type 7 with that value >=2: continue to player-layer switch.
- Foreground type 3: always skip player-layer switch. If `raw >> 8 < 3`, set local collision flag true; otherwise set it false.
- Other foreground types in this switch—including 1,2,4,5,6,19,28 and default—continue to player-layer switch after their own branch path. Probe mode may suppress side effects but does not by itself change this precedence for those cases.

## 3. Matrix

| Foreground low byte | Metadata condition | Probe | Player-layer switch | Foreground local flag |
|---|---|---|---|---|
| 7 | packed nibble 0 or 1 | either | skipped | unchanged here |
| 7 | packed nibble >=2 | either | evaluated | unchanged here |
| 3 | high-byte signed value <3 | either | skipped | true |
| 3 | high-byte signed value >=3 | either | skipped | false |
| 1,2,4,5,6,19,28 | branch-specific | either | evaluated | branch-specific effects may occur |
| other | none | either | evaluated | unchanged here |

The type-7 expression uses bits 4–7 of the high byte, not the entire high byte. Type 3 uses signed `raw >> 8 < 3`; these are not interchangeable metadata tests.

## 4. Native policy

`LegacyForegroundPrecedence.decide(raw, probeMode)` returns only:
- whether player-layer evaluation is reached;
- whether this foreground branch explicitly sets the local collision flag true or false.

It intentionally does not execute foreground side effects, player-layer evaluation, or final return logic. The probe parameter is retained for API continuity, but source inspection shows it does not change this narrow precedence decision for the represented cases.

## 5. Tests

Synthetic tests cover:
- type-7 metadata threshold on both sides;
- type-3 skip behavior and true/false local collision outcomes;
- ordinary and side-effect foreground cases continuing to player-layer evaluation in both probe modes;
- type-7 nibble extraction rather than whole-byte comparison.

## 6. Boundary / next work

This is not a complete truth table for `aBoolean`. It does not model early out-of-bounds, auxiliary-grid gate, type-19 scan effects, type-2 inventory state, checkpoint updates, player-layer special branches, recursive clears, object pushes, or final return flags.

Next: **DEV-003O — Collision Local-Flag and Return-Path Trace.** Trace the full helper's local `b2`, `b3`, `n3`, `axInt`, and return paths to distinguish local collision status, auxiliary branch selection, mutation, and method return.
