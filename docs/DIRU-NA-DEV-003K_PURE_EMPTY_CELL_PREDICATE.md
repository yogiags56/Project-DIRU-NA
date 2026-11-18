# DIRU-NA-DEV-003K — First Pure-Core Collision Predicate

**Status:** Pure predicate and synthetic truth-table tests committed; no movement evaluator  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. Source anchor

The source helper `eBoolean(int,int)` at `src/main/java/i.java:14309–14313` reads:
- `crtStagePlayerLayer[x][y]`;
- `crtStageForegrondLayer[x][y] & 0xFF`.

It returns true exactly when the player-layer byte is -1 and the unsigned low-byte foreground type is not 14, 33, 5, or 28.

## 2. Native pure predicate

`LegacyEmptyCellPredicate.isLegacyEmpty(playerLayerRaw, foregroundRaw)` is a direct pure transcription of that boolean expression. It has no stage access, bounds behavior, mutation, animation, or side effects.

High bits in foreground are deliberately ignored for the exclusion list, because the source masks with `0xFF`.

## 3. Tests

Synthetic tests cover:
- ordinary foreground types with player layer -1;
- all four excluded foreground values;
- excluded values with nonzero high bits;
- non-empty player-layer values;
- unsigned low-byte interpretation.

## 4. Critical semantic boundary

This predicate does **not** establish that a requested move is allowed. It is only the source's `eBoolean` sub-condition used by selected legacy branches. The full collision helper also checks other neighboring cells, stage edges, counters, flags, probe mode, and may mutate objects or animation state.

## 5. CI note

Run #25 (the DEV-003I test fixture correction) passed. Run #26 failed one test in the new validation gate because its purported out-of-bounds origin `playerX=-1, dx=1` resolves to valid target X=0. The fixture has been corrected to `playerX=-2`; the coordinate resolver itself was not changed. Run #27 validates that correction.

## 6. Next step

Add predicates only when their exact source expressions and raw-value semantics are independently verified. Next candidate is a foreground low-byte classifier, followed by a distinct player-layer classifier. Keep all of these separate from a combined movement decision until side effects and return flags are fully represented.
