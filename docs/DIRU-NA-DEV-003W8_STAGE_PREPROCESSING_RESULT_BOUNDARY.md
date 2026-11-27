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
