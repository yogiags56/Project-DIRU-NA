# DIRU-NA-DEV-003Z — Packed Tile Metadata Call-Site Audit

**Status:** Source call-site audit completed; no gameplay integration performed  
**Parent baseline:** DEV-003Y, commit `d78d1311689b108df02087d4564600232213fdf7`  
**Source:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java`  
**Source revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Data layout evidence

The original stage loader allocates both arrays with the stage dimensions:

- `eIntArrArr = new int[crtStageWidth][crtStageHeight]` (line 4597).
- `cByteArrArr = new byte[crtStageWidth][crtStageHeight]` (line 3434).

The tile renderer reads `eIntArrArr[x][y]`, then iterates group indices 0, 1, and 2. For each group it extracts a 3-bit field at group-relative offset 0 and a 2-bit field at offset 7. The rendering loop uses `b8 << 3` for the vertical pixel offset, i.e. 8-pixel bands within a 24-pixel tile (source lines 7663–7720).

This supports the packed layout:

| Group-relative bits | Width | Observed role |
|---|---:|---|
| 0–2 | 3 | Nonzero/segment type discriminator |
| 3–6 | 4 | Segment parameter/state field |
| 7–8 | 2 | Small segment parameter/state field |

Each group advances by 9 bits. Three groups occupy bits 0–26. Bits 27–31 are not assigned by this helper and remain outside its proven scope.

## 2. Six-argument helper semantics

Source helper at lines 15890–15897:

`aVoid(row, column, group, value, bitOffset, width)`

- Computes `shift = group * 9 + bitOffset`.
- Reads the existing field with unsigned shift and `~(-1 << width)`.
- Replaces it using the exact Java-int expression `(word ^ (current << shift)) | (value << shift)`.
- Sets `cByteArrArr[row][column]` to 24.
- Sets `cByteArrArr[row][column + 1]` to 24.

The setter does not pre-mask `value`. The core implementation retains this behavior.

## 3. cByteArrArr lifecycle

The source proves that this is a per-cell update countdown / tick gate, not a second packed metadata word:

- It is allocated to the same stage width and height and reset to zero during stage initialization (lines 3434, 3501–3504).
- Stage setup seeds cells with values including 24, 48, and 127 depending on source tile/entity kinds (lines 3657, 3741–3760, 3874–3910).
- The main cell update checks whether the current value is positive, then subtracts 6 (lines 12842–12845).
- Several animation/update branches restore it to 24; other source branches assign 30 or 48.
- The six-argument helper refreshes both the addressed cell and its next column cell to 24.

Therefore the adjacent write is a real observable side effect and must not be removed as an optimization. Its exact reason for targeting the next column is not fully proven; do not rename it as a horizontal animation timer without further caller tracing.

## 4. Six-argument call-site inventory

All observed calls to the six-argument overload in the audited source revision:

| Source line | Group/value/field | Context evidenced by nearby code |
|---:|---|---|
| 15988 | `b6, 7, 3, 4` | Segment propagation; skip existing segment types 12 and 9, with a special terminal-cell condition |
| 16111 | `b23, 7, 3, 4` | Repeated segment update while iterating a span |
| 16131 | `b28, 8, 3, 4` | Conditional segment update when player-layer checks permit |
| 16278 | `b37, 15, 3, 4` | Segment update in a branch selected by movement/state flags |
| 16398 | `b61, 3, 3, 4` | Initializes a segment field only when the current 4-bit field is zero |
| 16423 | `b66, b67, 3, 4` | Writes a state value selected as 6 or 0 by the caller's boolean branch |
| 16439 | `b71, b73, 7, 2` | Writes a 2-bit field after advancing/wrapping a group index |
| 16577 | group 2, `aByte, 0, 3` | Writes a 3-bit field while processing a moving multi-cell object |
| 16672 | `b3, b, 0, 3` | Writes a 3-bit segment field during directional object movement |
| 16702 | `b9, n6, 3, 4` | Writes a 4-bit segment state selected by movement/neighbor conditions |
| 16737 | `b3, b6, 0, 3` | Writes a 3-bit field in an object/segment creation path |
| 16738 | `b3, b, 3, 4` | Writes a 4-bit field in the same creation path |

The group argument is supplied from byte variables whose observed control flow wraps values above 2 back to 0 while advancing the tile column. This agrees with the three 8-pixel vertical bands.

## 5. Related but distinct operation

The source's `cVoid(row, column, group)` at lines 16712–16717 clears a full 9-bit group by XORing the extracted group back into the packed word, and sets only `cByteArrArr[row][column]` to 24. It does **not** refresh the next column. Keep this separate from `LegacyPackedTileMetadata.write`; do not merge their side effects.

Other direct reads/writes of `eIntArrArr` and `cByteArrArr` exist throughout the game loop. This audit scopes only the six-argument helper and the evidence needed to preserve its contract.

## 6. Implementation boundary and next work

- `LegacyPackedTileMetadata` is a faithful isolated helper for the proven six-argument setter behavior.
- It is not yet wired into the full entity/movement update loop.
- Do not infer names for all 3-bit, 4-bit, and 2-bit fields solely from their numeric values.
- Next: audit the 3-argument group-clear helper and trace its callers; then add a separate API and tests only after its full call-site contract is documented.
- No changes to DEV-003X or the legacy slot table are required.
