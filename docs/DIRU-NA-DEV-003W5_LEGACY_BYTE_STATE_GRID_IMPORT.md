# DIRU-NA DEV-003W5 — Legacy byte-state grid import

## Source-backed correction

The earlier collision seed represented a generic auxiliary grid and a foreground cache, but did not explicitly carry the two byte grids identified by the stage loader audit. This milestone adds those grids without assigning unsupported semantic names to every byte value.

Pinned read-only source: `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

Evidence already recorded in project audits:
- `bByteArrArr` and `cByteArrArr` are distinct per-cell byte arrays allocated at stage dimensions.
- `cByteArrArr` is reset during stage setup, receives values including 24, 30, 48 and 127 in observed paths, and the main cell update decrements positive values by 6.
- The six-argument packed metadata setter refreshes `cByteArrArr[row][column]` and `[row][column+1]` to 24.
- The separate packed 32-bit tile metadata is `eIntArrArr`; its six-argument field writer uses three 9-bit groups. It must not be confused with either byte grid.
- The three serialized stage layers remain player/object, background (`bIntArrArr`), and foreground. The background grid is later mutated and carries integer properties/state in runtime.

## Implementation

- Added `LegacyCollisionByteStateGrids`, an immutable detached snapshot of both byte arrays.
- Added `B_BYTE_STATE` and `C_BYTE_STATE` typed per-cell state-intent channels.
- Extended `LegacyCollisionStageStateSeed` with defensive-copy byte-array import and strict cell-count validation. The legacy constructor remains as a compatibility/test convenience that explicitly zero-seeds both byte grids.
- Extended the copy-on-write root to publish both byte arrays atomically with the other grids/state.
- Store reads preserve Java signed-byte values. Commit writes use Java byte narrowing, matching byte-array assignment semantics.
- The commit boundary revalidates expected byte values before preparing and publishing the new root.
- Added tests for defensive import, signed-byte reads, atomic writes to both byte grids, and old-snapshot stability.

## Important unresolved distinctions

- The old `metadata` seed array is the store's packed-metadata port; source audits identify the actual packed word grid as `eIntArrArr`. The source-derived factory still needs to map it directly.
- The old `auxiliary` and `foregroundCache` arrays are retained for existing branch-model compatibility, but are not substitutes for `bByteArrArr` or `cByteArrArr`. Their exact production source ownership must be verified before they are populated.
- `bIntArrArr` is already represented by StageGrid.backgroundLayer. Do not duplicate it into an unrelated auxiliary array.
- The new byte-state grid type is raw storage only; it does not implement timer ticking or animation semantics.

## Next

DEV-003W6: trace the stage setup allocations and all writers/readers for `bByteArrArr`, `cByteArrArr`, and `eIntArrArr`; introduce a named, source-derived seed factory from decoder output; add real-format round-trip fixtures. Do not enable GameSession integration until these arrays have one authoritative owner.
