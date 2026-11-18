# DIRU-NA-DEV-003I — Safe Neighborhood Capture

**Status:** Branch-scoped raw grid capture and synthetic tests committed; evaluator not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source revision:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. CI correction

Run #22 failed at test compilation because `Coordinate(int,int)` was private while same-package tests needed to construct expected values. The constructor is now public. Run #23 compiled and ran 94 tests, but exposed one incorrect expected coordinate in the type-48 test: with player X=3 and dx=1, the two-step X coordinate is 5, not 6. The fixture expectation is corrected; the resolver formula is unchanged.

## 2. Capture contract

`LegacyCollisionNeighborhoodCapture` accepts an explicit branch, a read-only grid interface, player origin, and movement delta. It:
- verifies declared grid dimensions match the configured stage bounds;
- computes each required coordinate with the checked resolver;
- reads raw foreground, player-layer, packed metadata, and optional auxiliary values;
- records only samples requested by the selected branch;
- returns an explicit status rather than synthesizing missing cells.

The capture layer does not execute collision decisions and does not mutate the grid.

## 3. Branch capture

- Generic/foreground branches capture target.
- Player-layer 0/8/9/47 captures target, two-step target, and target-above-player-row.
- Empty player-layer captures target, target-above-player-row, and target-relative above/below.
- Type 48 reads target metadata first, derives the legacy upper/lower offsets, then captures two-step, both destination cells, both source-clear cells, and support.
- Foreground type 19 captures the target, scans player-layer upward at targetX+3 until value 39, then captures the foreground run at targetX beginning one row below the anchor Y until low byte differs from 19. The terminator is retained in the ordered run.

## 4. Failure statuses

- `OUT_OF_BOUNDS`: any fixed coordinate or type-19 foreground-run successor leaves stage bounds.
- `TYPE19_ANCHOR_NOT_FOUND`: upward scan reaches the top edge before player-layer value 39.
- `TYPE19_FOREGROUND_RUN_NOT_TERMINATED`: the downward scan reaches the stage boundary before observing a non-19 terminator.
- Incomplete results contain no snapshot, preventing consumers from accidentally using partial samples.

## 5. Tests

Synthetic tests cover branch-limited capture, type-48 metadata-driven geometry, out-of-bounds two-step rejection, type-19 anchor and terminator capture, type-19 missing-anchor behavior, distinct unterminated-run status, auxiliary-grid presence/value separation, and dimension mismatch.

## 6. Boundaries and next work

No gameplay evaluator, source mutation replay, player movement, animation, Android UI, or APK behavior was implemented here. Next: **DEV-003J — Capture Result and Validator Integration**, including precise failure status for the type-19 downward run and end-to-end completeness validation. Then the first pure-core branch evaluator can consume only validated captures.
