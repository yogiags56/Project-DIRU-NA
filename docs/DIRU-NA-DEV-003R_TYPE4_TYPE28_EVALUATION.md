# DIRU-NA-DEV-003R — Foreground Type 4 and Type 28 Evaluation

**Status:** Pure outcome model and synthetic tests committed; no state mutation or complete movement evaluator  
**Date:** 2026-11-19  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchors:** `i.java:12157–12164`, `i.java:12166–12171`

## 1. CI baseline

DEV-003Q run #34 completed successfully: core JVM tests, Android debug APK assembly, and artifact upload all passed.

## 2. Foreground type 4 — checkpoint

The source condition is `!probe && ((raw & 0xFFFFFF00) >> 8) >= aCInt`. The packed high 24 bits are interpreted as a signed Java int after masking and arithmetic shift.

Only when both conditions pass, the legacy method requests:
- `shouldSaveCheckpoint = true`;
- `aCInt = checkpointValue + 1`;
- `akInt = i.aSInt + 13`.

The branch exits the foreground switch in either case, so player-layer evaluation is skipped. Probe mode short-circuits before evaluating the checkpoint comparison or requesting writes.

The evaluator accepts the current checkpoint progress and global stage clock as inputs. It returns proposed next values; it does not write any fields.

## 3. Foreground type 28

When probe is false, the source sets local `xBoolean=true` and object field `atBoolean=false`. Probe mode suppresses both assignments. The branch skips player-layer evaluation in both modes.

The pure result exposes the local flag effect; the object-field assignment remains an explicit integration responsibility and is not executed here.

## 4. Tests

Synthetic tests cover checkpoint equality threshold, below-threshold no-op, probe suppression, signed high-24-bit interpretation, timer offset +13, type-28 probe behavior, player-layer flow, and unrelated-type non-handling.

## 5. Boundaries

No checkpoint save, object mutation, timer write, player movement, animation, or stage modification occurs in this milestone. The result contains intent only.

## 6. Next

**DEV-003S — Foreground Type 2 Inventory/Metadata Evaluation.** Model the exact high-byte switch and inventory thresholds, preserving the unusual default branch that exits the foreground/player-layer path without claiming a general passability result.
