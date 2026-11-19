# DIRU-NA-DEV-003AR — Relocation Reference Model & Offset-54 End-to-End Fixture

**Status:** Isolated relocation model and regression fixtures committed; live gameplay reachability remains unverified  
**Parent:** DEV-003AQ  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Source path modeled

The model follows the relevant source sequence in `mVoid(x,y)` (lines 16798–16811):

1. Coordinate lookup scans bLongArr from slot 0 upward and returns the first coordinate match.
2. If found, the matched bLongArr record's type field is set to 2.
3. The player-layer cell is set to -1.
4. The six-argument aLongArr allocator is called with:
   - X = x
   - Y = y + 1
   - type = 0
   - direction = 0
   - final argument = a second coordinate lookup for (x,y)
5. Since type is not 7, the allocator writes that final argument to offset 54, width 3.

The isolated model reproduces the bLongArr coordinate lookup/type-2 mark and the aLongArr type-1 allocation with its offset-54 value. It intentionally does not model player-layer arrays, tile metadata arrays, render callbacks, or the complete movement engine.

## 2. New implementation

Added:
`core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyRelocationReferenceModel.java`

The model:
- clones both 15-word input tables;
- performs first-match coordinate lookup;
- returns unchanged tables and no allocation if lookup misses;
- marks the found bLongArr record as type 2;
- allocates the first free aLongArr object record using the observed x/y/group/direction/mode fields;
- exposes the matched bLongArr slot and allocated one-based aLongArr ID.

The model is intentionally named and documented as an isolated reference model, not a complete game simulation.

## 3. New regression fixtures

Added:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyRelocationReferenceModelTest.java`

Scenarios:
- **Slot 8:** coordinate lookup returns index 8; bLongArr type becomes 2; the aLongArr offset-54 three-bit readback aliases to 0; bit 57 becomes 1.
- **Slot 7:** offset-54 readback remains 7 and bit 57 remains clear.
- **No coordinate match:** neither table changes and no aLongArr record is allocated.
- **Zero coordinate:** an all-zero bLongArr table matches (0,0) at slot 0 because the source lookup does not filter by record type; the model therefore marks that empty slot as type 2 and creates the aLongArr record.

The slot-8 case is a direct end-to-end reproduction of the identified encoding hazard under the modeled source sequence. It still does not prove that a normal game level produces the necessary slot population and coordinate at runtime.

## 4. Important implications

### 4.1 The second lookup

The source first looks up (x,y) to decide whether relocation proceeds, then repeats the same lookup when passing the final allocator argument. No bLongArr mutation occurs between those two lookup expressions in this method body. In the isolated single-threaded model, the same first-match slot is therefore supplied to the allocator.

### 4.2 Type-2 does not release the slot

The relocation path marks the bLongArr record type as 2. The observed type-3 consumer clears records whose type is 3. This model does not treat type 2 as released, and does not invent a type-2 cleanup.

### 4.3 Zero-coordinate alias

The coordinate lookup compares only coordinates. A zero-initialized slot therefore matches (0,0), even though its type field is zero. This is a source-level consequence. Whether a valid gameplay path calls `mVoid(0,0)` is not established.

## 5. Validation status

- Model implementation and test source were committed to `main`.
- No local Gradle execution is claimed.
- No successful CI status check is available for these commits.
- No live level trace or exact shipping JAR bytecode comparison was performed.
- No production codec semantics were changed.

## 6. Next stage — DEV-003AS

1. Verify the current model against the complete source semantics of the six-argument allocator, including the type-7 special branch and all external tile-array side effects.
2. Add a minimal tile/player-layer state fixture for the `mVoid` preconditions rather than treating the method as an unconditional relocation.
3. Identify candidate level scripts/maps that can generate nine simultaneous bLongArr entries and a later coordinate lookup to the high slot.
4. Inspect type-2 and type-3 transitions across the full movement loop to establish whether a type-2 record can persist long enough to be referenced again.
5. Keep production changes blocked pending target-JAR parity and live-level reachability evidence.
