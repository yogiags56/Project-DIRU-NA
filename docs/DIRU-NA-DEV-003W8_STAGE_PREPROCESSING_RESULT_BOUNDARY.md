# DIRU-NA DEV-003W8 — Stage preprocessing result boundary

## Source evidence

Reviewed the pinned candidate source `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`, `src/main/java/i.java`, stage initialization step 9.

Directly observed:
- Step 9 iterates the stage in x-major/y-minor order.
- At the start of each cell, `bByteArrArr[x][y]` and `cByteArrArr[x][y]` are reset to zero.
- Foreground preprocessing may repack a tile as `(background << 8) | foregroundCode`, remove/normalize selected codes, and write neighboring foreground cells.
- Background runtime values are mutated for selected player/object codes; examples include object codes 48, 47, 46, 45, 44, 41, 36, 34, 28, 11, 43, 30, 37, 10, 16, and 7.
- The two byte grids are not interchangeable: cByte receives several explicit initial values (including 24, 48 and 127); bByte is independently reset and receives selected writes.
- The pass builds derived counters, feature flags, coordinate tables, and object/entity arrays after or during its scan.

This is static inspection of the candidate source, not runtime validation. Decompiled local names and numeric codes are retained as evidence, not treated as semantic names.

## Implementation

Added `LegacyStagePreprocessingResult`:
- Immutable detached boundary for transformed StageGrid, background runtime grid, bByte grid, and cByte grid.
- Exact cell-count validation and overflow-safe dimensions inherited from StageGrid.
- Defensive copies on construction and on every bulk accessor.
- Bounds-checked per-cell reads.
- No preprocessing algorithm, defaults, fabricated counters, or GameSession wiring.

Added `LegacyStagePreprocessingResultTest`:
- Defensive copy checks for all runtime arrays and transformed stage.
- Exact array-length validation.
- Out-of-bounds access rejection.

## Explicit non-goals / unresolved

- This type is not a step-9 executor and does not claim the source preprocessing pass is complete.
- It currently omits derived door tables, item/collectible counters, feature flags, entity arrays, and player/camera initialization. These require separately traced, typed ownership before inclusion.
- Foreground neighbor access in source contains edge-sensitive reads. The Native pass must define safe, source-compatible boundary handling only after each branch is mapped.
- The source pass has x-major/y-minor iteration; preserve this order in the eventual executor because mutations can affect later reads.
- No production adapter from ParsedStage to this result exists.
- No collision seed or GameSession integration is enabled.

## Next

DEV-003W9: produce a source-evidence matrix for each step-9 mutation branch, including scan order, read/write coordinates, source values, derived arrays/counters, and neighbor preconditions. Then implement small isolated transformation functions with table-driven tests. Do not claim full preprocessing until every branch and side effect in the audited method is covered.


## Follow-up source trace — branch ordering and side effects

A second pass over the step-9 body confirms additional details that must be retained by the future executor:

- The primary pass is x-major then y-minor. It is stateful: writes to neighboring cells can affect later iterations.
- Player code 79 sets player start coordinates and camera-related vertical offsets, then clears the player-layer marker.
- Player codes 34/35 alter player/foreground cells and feature flags; codes 38/39 activate optional-mode state, with code 38 normalizing foreground to 27.
- Codes 14 and 28 set cByte state to 24 and may normalize background runtime data. Code 11 sets background to 16 or 0 according to its authored value and sets cByte to 48.
- Codes 43/19, 22/23 and 49 set feature bits and selected cByte values. Codes 30/37/10 clear background values; code 16 may move a marker upward and copy background state to the adjacent cell.
- The switch includes Java fall-through across codes 6/7/24/26/27/8/4/5/2/0/1. A Native port must encode this as explicit ordered effects or preserve exact fall-through; independent per-code handlers would silently change behavior.
- A later foreground pass repacks foreground code 7 using local group lookup data, checks adjacent code-17 doors, may set cByte to 24 and may clear the foreground cell above.
- Foreground code 17 creates stage-derived group counters and dialog selectors. The mutable group counter is checkpointed; the dialog selector is rebuilt from stage data.

These findings remain static-source evidence. They do not establish retail-runtime equivalence or complete coverage of every branch.
