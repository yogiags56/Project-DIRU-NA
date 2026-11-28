# DIRU-NA DEV-003W9.4 — Special-object lifecycle and call-site audit

## Source identity and confidence

Static source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This document records call-site and mutation behavior visible in the decompiled source. It is not a runtime equivalence claim.

## 1. Lifecycle phases

### Stage construction
- Pass B allocates `eByteArr` as `awInt * 3` when the special-object counter is positive.
- Initial records are populated in the Pass B player-layer scan. The record index is encoded in the high byte of `bIntArrArr[x][y]`.
- For the initial player code 48 path, candidate X is derived from the current cell and the direction flag in the cell below. The initial record writes candidate X to both endpoint bytes; row is Y when the classifier recognizes the candidate player cell, otherwise -1.
- Do not generalize equal endpoints to all record states.

### Runtime update
- The main gameplay update invokes `VVoid()` when `awInt > 0`, after `ahVoid()` and before `asVoid()`.
- `VVoid()` scans records in three-byte increments. It processes only records with row byte > 0.
- It adjusts the left endpoint by scanning left and the right endpoint by scanning right through `aByte(...)`, then visits every inclusive X coordinate between the new endpoints and calls `aBoolean(x,y)`.
- `aByte` checks player-layer occupancy/classification while extending the span. Its boundary logic is asymmetric and depends on stage width and X position; preserve the exact condition rather than replacing it with a generic flood fill.
- `aBoolean(x,y)` classifies the player-layer cell. If recognized, it marks a 3×3 cByte neighborhood, changes the player-layer cell to 9, packs the entity type into the high bits of the background/object grid, and invokes `cVoid(x,y,1)`. It also checks player overlap and may damage the hero. This is a gameplay mutation, not merely a collision query.

### Rendering
- `EVoid()` is invoked from the render path when `awInt > 0`, after the normal tile/object draw work.
- It reads each three-byte record as horizontal endpoints plus row, clips the span against the visible viewport, and draws the corresponding animated special-object visual.
- It reads record state; it does not define the record lifecycle or restore state.

### Interaction
- A separate player interaction path scans `eByteArr` in increments of three. It compares the row with the target row and checks whether the target X is immediately adjacent to either endpoint. It can set an interaction/blocked condition and exits that scan on a match.
- This is a distinct consumer from rendering and from VVoid's update loop.

## 2. Mutation and removal paths

### `aBoolean(x,y)`
- Calls the pure classifier `dInt(x,y)`.
- On a recognized code: sets cByte 48 around the cell via `kVoid`; changes player-layer value to 9; rewrites the background/object grid with a packed entity code (including an extra flag for original player code 43); then calls `cVoid(x,y,1)`.
- Separately checks whether the player occupies the same position and may call the hero-damage path.
- Therefore this helper changes stage state and must not be modelled as a pure predicate.

### `dVoid(x,y)` — coordinate overload
- This overload is a removal/cleanup path, not the same operation as `dVoid(byte)`.
- Depending on the player-layer code it clears foreground and related cells. The switch has fall-through from code 0 through cases 19/43/45/46 and then 48; preserve the exact fall-through.
- The code-48 path clears the foreground at the current cell, conditionally clears the player cell below when it is also 48, calls `jVoid` for affected cells, and writes -1 to the row byte of the eByteArr record indexed by the high byte of the current background/object grid.
- This marks a record inactive; it does not compact the array or change the record index.

### `aiVoid()`
- Reads the direction/orientation bits from the background/object grid cell below the current object.
- Derives a candidate X (current X, X+1, or X-1 depending on low direction bits and flag 0x10).
- Resolves the record index from the high byte of the current cell's background/object value.
- If the candidate player-layer cell is empty, writes row=Y, right endpoint=candidateX, left endpoint=candidateX. Otherwise it writes row=-1.
- This is a record reconstruction/update helper; it is not the initial Pass B classifier.

### `dVoid(byte)` — byte overload
- Handles the movement/update state for the code-48 object.
- When the byte-state condition allows movement, it updates direction bits and adjacent player/background cells, writes a new code-48 marker, initializes the related byte state, marks cByte neighborhoods, calls `kVoid`, and finally calls `aiVoid()`.
- In the alternate branch it can damage the player or clear movement bits. It updates cByte at the current and adjacent row cells.
- Restore calls this overload for code-48 cells whose background/object flag 0x8 is clear; it calls `aiVoid()` when flag 0x8 is set.

## 3. Checkpoint reconstruction order

Observed restore sequence relevant to special objects:

1. Restore checkpointed scalar/player coordinates and mutable grid arrays.
2. Restore optional-mode arrays and long/scalar state when applicable.
3. Rebuild cByte neighborhood state by scanning interior cells and calling `kVoid` for cells with player or foreground values in the legacy range.
4. Restore remaining mode-specific state.
5. If `eByteArr` exists, scan rows outermost then columns (Y-major, X-minor). For each player-layer code 48, set current object coordinates and call `dVoid((byte)48)` when background bit 0x8 is clear, otherwise call `aiVoid()`.

The record array is thus reconstructed/updated from restored grids and object direction state. It is not copied as checkpoint data. The restore scan order is observable and must be preserved.

## 4. Array write-site inventory

Observed direct eByteArr write families:
- Pass B initial population: three bytes per record; record index comes from the allocation/population counter and is stored in the high byte of the object grid.
- `VVoid()`: updates endpoint bytes 0 and 1.
- `dVoid(x,y)`: sets byte 2 to -1 to deactivate a removed record.
- `aiVoid()`: rewrites all three record fields based on candidate position and occupancy.
- Rendering and interaction paths are read-only consumers.

No evidence in this source indicates array compaction during removal. Record slots remain stable for their object-grid index.

## 5. Corrections to earlier notes

- The first two eByteArr endpoint bytes being equal is only an initial record state and a possible aiVoid reconstruction state; VVoid mutates them into a span.
- `aBoolean` is a mutating helper, not a pure boolean classifier.
- `dVoid(int,int)` and `dVoid(byte)` are distinct overloads with different responsibilities. The former clears/removes; the latter performs special-object movement/update behavior.
- Restore reconstruction invokes the byte overload or `aiVoid()` according to background flag 0x8, after restoring grids and rebuilding cByte neighborhoods.

## 6. Native implementation gates

Before implementing a native SpecialObjectSystem:
1. Define an explicit three-byte compatibility record and stable slot index.
2. Preserve signed row sentinel -1 and all byte narrowing behavior.
3. Implement exact legacy scan order and endpoint expansion conditions.
4. Separate render reads, update mutations, interaction checks, removal, and restore reconstruction.
5. Preserve overload semantics as distinct named operations.
6. Add fixtures for left/right expansion, occupied endpoints, player overlap, removal, inactive records, both restore branches, and multiple objects.
7. Trace `jVoid`, `cVoid`, and `aByte` dependencies before porting the mutating paths.
8. Do not activate collision/gameplay integration until deterministic fixtures and full helper traces pass.

## 7. Milestone boundary

W9.4 is source audit and documentation only. No runtime adapter, object system, checkpoint implementation, collision activation, or GameSession integration is added.
