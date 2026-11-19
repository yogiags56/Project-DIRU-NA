# DIRU-NA-DEV-003AA — Packed Tile Group Clear

**Status:** Isolated helper and regression tests committed; no gameplay integration  
**Parent:** DEV-003Z call-site audit  
**Source:** `i.java` at `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`, lines 16712–16717

## Exact source behavior

The three-argument source helper:

1. Reads `eIntArrArr[row][column]`.
2. Computes `shift = group * 9`.
3. Extracts the complete 9-bit group with unsigned right shift and mask `0x1FF`.
4. XOR-clears exactly that group: `word ^ ((word >>> shift & 0x1FF) << shift)`.
5. Sets only `cByteArrArr[row][column]` to 24.

Unlike the six-argument setter in DEV-003Y, this operation does not refresh `column + 1`.

## Implementation

Added `LegacyPackedTileGroupClear` as a separate core helper. It preserves the source's 32-bit int shift/XOR expression and single-cell byte-state side effect. The group domain is validated as 0–2, consistent with the three 9-bit bands evidenced by the renderer and call sites.

## Regression coverage

- Clear group 0/1/2 field independently while preserving unrelated bits.
- Refresh current byte-state cell to 24.
- Do not change the adjacent byte-state cell.
- Reject invalid group and invalid cell before mutation.

## Boundary

No semantic names are assigned to all subfields within the 9-bit group. No movement/collision integration is claimed. The helper remains separate from DEV-003Y's six-argument field setter.
