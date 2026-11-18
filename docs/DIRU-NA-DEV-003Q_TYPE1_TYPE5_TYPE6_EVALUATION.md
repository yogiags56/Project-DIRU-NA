# DIRU-NA-DEV-003Q — Foreground Type 1, 5 and 6 Evaluation

**Status:** Narrow pure evaluator and synthetic tests committed; full movement evaluator not implemented  
**Date:** 2026-11-19  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchors:** `i.java:12141–12147`, `i.java:12173–12179`, `i.java:12226–12228`

## 1. CI #33 fixture corrections

Two test assumptions were incorrect:
- `0xFF03` is positive 65283 as a Java int; signed high byte -1 with low byte 3 must be represented as `0xFFFFFF03`.
- `0xF007` has type 7 in the low byte and metadata nibble 15 in bits 4–7 of the high byte, so the legacy type-7 branch falls through to player-layer evaluation. The earlier expectation of SKIP was wrong.

No type-3/type-7 production expression is changed by these fixture corrections.

## 2. Type 1

The source sets timer `bjInt=120`, increments `amInt`, and invokes `bVoid(n6,n7,(byte)1)` only when probe is false. Both probe modes leave the foreground switch through the branch's labeled break, skipping player-layer evaluation.

## 3. Type 5

The source always sets local `b2=true`. Only when probe is false does it set local `xBoolean=true` and object field `atBoolean=true`. It also skips player-layer evaluation in both modes.

## 4. Type 6

The branch performs no visible assignment in this case and exits the foreground switch through the labeled break. Player-layer evaluation is skipped; the existing local collision value remains unchanged.

## 5. Implementation

`LegacyForegroundType156Evaluator` returns a pure decision describing flow, local collision effect, and requested side effects. It does not execute timers, counters, stage writes, or object state changes.

## 6. Tests

Tests cover probe on/off for type 1 and type 5, type 6 unchanged behavior, player-layer skip behavior, and non-handled types.

## 7. Next

**DEV-003R — Foreground Type 4 and Type 28 Probe-Gated Effects.** Capture checkpoint threshold/save-state intent and type-28 local/object flag changes as data, preserving the exact signed high-byte operation and probe gate.
