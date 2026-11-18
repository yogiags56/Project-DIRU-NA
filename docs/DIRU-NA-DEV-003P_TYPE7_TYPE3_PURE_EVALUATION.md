# DIRU-NA-DEV-003P — Foreground Type 7 and Type 3 Pure Evaluation

**Status:** Narrow pure evaluator and synthetic tests committed; no full movement evaluator  
**Date:** 2026-11-19  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchors:** `i.java:12149–12155`, `i.java:12229–12240`

## 1. CI correction

Run #32 showed that two DEV-003N test fixtures still encoded foreground type 7 incorrectly. The legacy switch uses the low byte as type; the metadata nibble is bits 4–7 of the high byte. Correct examples:
- `0x0007`: type 7, metadata nibble 0, skip player-layer;
- `0x1007`: type 7, metadata nibble 1, skip player-layer;
- `0x2007`: type 7, metadata nibble 2, evaluate player-layer;
- `0xF007`: type 7, metadata nibble 15, evaluate player-layer.

The test had incorrectly used `0x0017` (type 23) and expected nibble 1 to evaluate. The implementation in `LegacyForegroundPrecedence` follows the source expression; only fixtures/expectations needed correction.

## 2. Type 7

The source checks:
`(raw >> 8 & 0xF0) >> 4 < 2`.

If true, local `n10=1`, then `n3=n10`, so the player-layer switch is skipped. Otherwise, execution reaches player-layer evaluation. This branch does not itself change `b2`.

## 3. Type 3

The source checks signed arithmetic shift:
`raw >> 8 < 3`.

- If true: `b2=true`, then skip the player-layer switch.
- Otherwise: `b2=false`, `n10=1`, then skip the player-layer switch.

Both outcomes skip player-layer evaluation; the collision flag differs. This is not the same metadata expression as type 7 and must not be masked to an unsigned byte.

## 4. Implementation

`LegacyForegroundBranchEvaluator` evaluates only types 7 and 3. Other types return `NOT_HANDLED`, preventing this narrow evaluator from claiming behavior it does not implement. The decision separately records whether the branch is handled, whether player-layer evaluation is skipped/reached, and the effect on the local collision flag.

## 5. Tests

Tests cover type-7 nibble boundaries, unchanged local collision flag for type 7, signed type-3 high-byte threshold including negative values, and explicit non-handling of unrelated types.

## 6. Boundaries and next step

No stage reads, side effects, checkpoint progression, scripted type-19 mutation, player-layer evaluation, or final method return is implemented here.

Next: **DEV-003Q — Pure Foreground Type 5/6 and Type 1 Precedence.** Model only the exact local-flag and control-flow effects for those branches, keeping their probe-dependent side effects separate.
