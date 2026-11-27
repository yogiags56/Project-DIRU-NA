# DEV-003CS-2B — Restore Helper Transitive Side-Effect Audit

**Status:** Static-source helper bodies inspected; semantic classification and runtime vectors remain open.  
**Evidence:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.  
**Native branch:** `dev-003cr-snapshot-provider-boundary`.

> This document follows helper calls reached from `restoreCheckpoint()`. It does not claim behavioral equivalence with the shipping JAR or authorize a production restore implementation.

## 1. Restore call graph and mode-specific branch

The observed restore body invokes or conditionally invokes:

- `yVoid()` only when `kByte == 5`;
- `kVoid(x,y)` during the interior-grid scan for cells whose player-layer value is 0..79 or whose foreground low byte is 0..79;
- `dVoid((byte)48)` or `aiVoid()` for each restored player-layer code-48 cell when `i.eByteArr != null`, selected by background/property bit `0x8`;
- `hVoid(...)` indirectly through `dVoid((byte)48)` when the code-48 cell has per-cell timer 6 and foreground low byte 6.

The restore is therefore not a simple copy operation. These calls mutate or reconstruct runtime state after the saved grids are copied.

## 2. `kVoid(int x, int y)` — 3×3 byte-layer writes

The exact helper writes value 48 to all nine cells in `i.cByteArrArr` in the square centered on `(x,y)`:

- x−1, x, x+1;
- y−1, y, y+1.

It does not write `bByteArrArr`, the player layer, or the foreground layer. The current static audit does not establish the full semantic meaning of value 48 in `cByteArrArr`; retain it as an exact derived byte-layer mutation rather than naming it a generic dirty flag without further consumer tracing.

**Native implication:** the restore post-processing phase must reproduce the exact 3×3 writes, including scan order and boundary preconditions. The original helper itself has no bounds checks; caller grid-loop bounds and valid stage dimensions are part of its safety assumptions. A Native implementation must validate the neighborhood before applying it and must not silently write outside the stage.

## 3. `dVoid(byte 48)` — moving-object continuation

The helper reads the selected coordinates from `bEInt/bFInt`.

Before the main branch, if the cell's per-cell byte timer is 6 and the foreground low byte is 6, it calls `hVoid(foreground >> 8)`. That door-related side effect must be traced independently; it is not part of the generic moving-object reconstruction.

If `bByteArrArr[x][y] <= 0`:

1. Read `bIntArrArr[x][y]` and inspect low three bits.
2. If low bits equal 2, set bit `0x10`; if low bits equal 4, clear bit `0x10`.
3. Let `nextY = y + 1`.
4. If player-layer at `(x,nextY)` is empty and player occupancy permits:
   - clear player-layer at `(x,y-1)`;
   - place code 48 at `(x,nextY)`;
   - write `(adjustedCurrentBackground & ~7) | 3` at `(x,nextY)`;
   - write `(background[x][y-1] | 0x8)` at `(x,y)`;
   - set `bByteArrArr[x][nextY] = 18`;
   - write value 48 into `cByteArrArr[x-1][y-2]`, `[x][y-2]`, and `[x+1][y-2]`;
   - invoke `kVoid(x,y)`, which writes the 3×3 region centered at `(x,y)` to 48;
   - invoke `aiVoid()` to rebuild the span record.
5. Otherwise, if low background bits equal 3 and the player occupies `(x,nextY)`, call `hurtHero(2,48,0)`; then clear low three bits at the current background cell.

If the timer is positive, it is decremented by 6 instead of moving the object.

Finally, it writes 24 to `cByteArrArr[x][y]` and `cByteArrArr[x][y-1]`.

This helper mutates multiple authoritative and derived arrays and may trigger damage/door handling. It cannot be implemented as a simple “rebuild span” callback.

## 4. `aiVoid()` — derived moving-object span record

The helper uses the packed high byte of `bIntArrArr[x][y]` multiplied by 3 as an offset into `i.eByteArr`.

It derives a neighboring x-coordinate from the low-three-bit state and bit `0x10` of the background cell below. It then writes a three-byte span tuple:

- first byte: selected x-coordinate, or `-1` if the adjacent player-layer cell is occupied;
- second byte: selected x-coordinate;
- third byte: current y-coordinate.

The precise tuple semantics and every consumer of `eByteArr` remain to be closed. The data is reconstructed from restored layers and packed metadata; it is not copied as part of the direct checkpoint assignment ledger.

**Native implication:** preserve the exact three-byte tuple and slot calculation only after the complete allocation/consumer audit. Do not persist stale span bytes as an independent checkpoint field.

## 5. `yVoid()` — mode-5 restore side effects

When `kByte == 5`, restore calls `yVoid()`. The helper sets:

- `aoInt = 13`;
- `aqInt = 4`;
- `atInt = 408`;
- `kByte = 5`;
- ORs bits `0x80` and `0x8` into `cLong`;
- `aeBoolean = false`;
- `OInt = 16`, `PInt = 16`, `QInt = 19`, `RInt = 18`.

These are explicit mode-5 controller/transition initializations. Their user-facing meaning remains opaque; preserve values and branch scope, not guessed labels.

## 6. Revised restore phase model

Based on the direct copy ledger and these helper bodies, the native restore candidate should be structured as:

1. Validate stage identity, dimensions, mode identity, and optional-feature presence.
2. Build detached copies of confirmed saved scalar/grid/table state.
3. Apply only mode-specific restore fields supported by the saved mode.
4. Restore the four always-copied arrays and the conditional metadata/object/auxiliary group.
5. Reproduce the exact interior scan and `kVoid` 3×3 writes on a detached derived byte layer.
6. Reconstruct code-48 objects in the original scan order, including `dVoid` timer/door/occupancy branches and `aiVoid` span tuples.
7. Rebuild remaining caches/resources only after their exact dependencies and helper write sets are audited.
8. Validate the complete candidate again, then atomically publish it to the live gameplay owner.

Any helper that can invoke damage, door transitions, or mutate multiple layers must be represented as a deterministic candidate transformation. It must not mutate live state during validation.

## 7. Open evidence gaps

- Trace all consumers and writers of `cByteArrArr` and identify the exact meaning of values 24 and 48.
- Trace `hVoid(int)` and its transitive `aVoid` / `jVoid` writes before modeling the door branch.
- Complete `eByteArr` allocation, slot-count, consumer, and teardown audit.
- Trace all writers/readers of `bEInt/bFInt` and code-48 movement state.
- Confirm whether code-48 reconstruction can call damage under reachable restore conditions and how the original runtime orders it.
- Add source-derived fixtures for 3×3 writes, code-48 timer branches, span tuple construction, mode-5 restore, and invalid-edge cases.
- Obtain runtime/retail-JAR vectors before claiming behavioral compatibility.

**Exit:** Four helper bodies are recorded with exact observed writes and branch conditions. CS-2 remains open; no production gameplay checkpoint provider is implemented.

## 8. `hVoid(int doorIndex)` — checkpoint-time door countdown side effect

This helper is reached from `dVoid((byte)48)` when the restored moving-object cell has `bByteArrArr[x][y] == 6` and the foreground low byte at that cell equals 6.

Observed behavior:

1. A negative door index returns immediately. Otherwise the helper reads the door's x/y coordinates from `crtStageDoorXs[doorIndex]` and `crtStageDoorYs[doorIndex]`.
2. It reads the foreground low byte and signed high portion at the door coordinate. If high metadata has any of bits `0xF0` set, it returns.
3. It checks that the foreground low byte is 7 and that the high metadata's upper byte identifies the same door index. A mismatch only prints a diagnostic; it does not stop the operation.
4. It decrements the low-nibble door countdown.
5. If the countdown reaches zero:
   - set high-metadata bit `0x10` at the door cell;
   - call `aVoid(x, y-1, +1, 0, 1)` and `aVoid(x, y-1, -1, 0, 1)`;
   - OR bit `0x200` into the high metadata of the foreground cell directly above the door;
   - write 24 to `cByteArrArr[x][y]`.
6. Otherwise, write the decremented low nibble back while preserving the other high-metadata bits.
7. Write the updated high metadata and original low-byte foreground code back to the door cell, then play sound 8.

### 8.1 Transitive horizontal head propagation

The called five-argument `aVoid(x,y,dx,dy,flag)` walks in one direction while the next coordinate remains strictly inside stage bounds. It continues only through foreground low-byte codes 8 or 9. For each matching cell it sets high-metadata bit `0x200` when flag is 1 (or clears that bit for flag 0), then recursively continues in the same direction. Any other foreground code stops the walk.

For this restore call, the two directions are horizontal across the row immediately above the door. The resulting mutation marks a contiguous run of foreground 8/9 cells on both sides, bounded by the first nonmatching cell or stage edge.

**Native implication:** door countdown handling is a gameplay mutation, not merely cache reconstruction. It must be evaluated against a detached candidate and preserve exact door index mapping, countdown nibble, foreground high bits, horizontal run boundaries, and mode/timer preconditions. Do not run it as a side effect against live state before restore validation.

### 8.2 Remaining door gaps

- Trace stage-load creation/order of `crtStageDoorXs/Ys` and verify index stability for the same stage/mode.
- Trace all writers/readers of foreground code 6, 7, 8, 9 and high-metadata bits `0x10`, `0xF0`, and `0x200`.
- Determine whether the restore-triggered timer==6 path is reachable in shipped gameplay and whether it can overlap other door-controller transitions.
- Add golden fixtures for countdown values 1 and greater than 1, high-metadata early return, horizontal propagation stop boundaries, and invalid door indices.

## 9. `eByteArr` allocation, initial population, and observed consumers

The pinned stage-preprocessing path provides additional evidence for the derived three-byte records:

- Allocation: when `awInt > 0`, `eByteArr = new byte[awInt * 3]`.
- Initial population: the stage scan visits player-layer code 48 cells whose background/property bit `0x8` is set. It derives a neighboring x-coordinate from the current background cell's bit `0x10`, checks that candidate with `dInt(candidateX,y)`, and writes a three-byte tuple at `slot * 3`. The high byte of the source background cell stores the slot index. If the candidate is invalid, the third tuple byte is set to `-1`.
- Restore reconstruction: `aiVoid()` uses that packed slot index to rewrite the tuple after restoring/moving a code-48 object.
- Renderer: `EVoid()` iterates the array in steps of three and uses the tuple's y and two x endpoints to calculate visible moving-object geometry and animation.
- Runtime updater: `VVoid()` iterates the same records, recalculates both x endpoints through `aByte(...)`, then visits the inclusive horizontal span.
- Collision/interaction: another source branch scans tuples in steps of three to detect whether a code-48 span already covers a candidate cell before initiating a new interaction.

This confirms that `eByteArr` is a derived moving-object span/index structure consumed by rendering, runtime span updates, and collision/interaction checks. It is not an independent save payload. The exact meaning of every endpoint sentinel and every packed high-byte slot lifecycle still needs a full writer/consumer audit.

**Native implication:** reconstruct `eByteArr` only after the authoritative player/background layers and packed slot metadata are restored. Keep its allocation count and three-byte record layout deterministic; validate slot index bounds, candidate coordinates, and endpoint sentinels before publication. Rendering must consume the derived records read-only.

## 10. Span capacity and recognized-object predicate

The stage-preprocessing pass resets `awInt = 0`. For each player-layer code-48 cell it:
- sets the cell's background/property integer to 16 when its low three bits equal 4, otherwise sets it to 0;
- increments `awInt`;
- creates a second code-48 cell one row above, with background/property value 8;
- writes the 3×3 `cByteArrArr` value-48 neighborhood around that upper cell.

After this pass, `eByteArr` is allocated to exactly `awInt * 3` bytes. A later stage scan fills one three-byte record for each code-48 cell whose background/property bit 8 is set. The packed high byte of that cell's background/property value stores the record slot. This explains why the table is a fixed three-byte-per-preprocessed-object array rather than a growable list.

The static predicate `dInt(x,y)` used while building the initial endpoint returns:
- 34 for player-layer code 1 (violet gem);
- 35 for code 45 (monkey);
- 37 for codes 19 or 43 (snake variants);
- 36 for code 46 (wasp);
- 39 for code 49 (turtle);
- -1 for empty/other values.

Thus initial endpoint validity depends on a specific neighboring occupant set, not merely on array bounds. Keep this mapping source-derived and do not infer visual semantics beyond the source comments/call sites.

**Remaining uncertainty:** the full slot identity and endpoint update lifecycle across all code-48 movement branches is not yet proven. In particular, do not assume slot order remains stable if objects are removed, split, or re-created; trace all writes to the packed high-byte slot index and all writes to `eByteArr` before choosing a Native stable-ID design.

## 11. Native immutable span-view boundary (DEV-003CS-3B)

A platform-independent `MovingObjectSpanTable` value type now preserves the observed three-byte record layout as immutable signed-byte triples:

- `firstX`, `secondX`, and `rowY` are retained exactly as bytes;
- input length must be divisible by three;
- slot access is bounds checked;
- constructor input and exported raw bytes are defensively copied.

Synthetic tests cover slot ordering, negative row sentinel preservation, defensive copying, malformed partial triples, null input, and slot bounds.

This is intentionally a **read-only representation boundary**, not a reconstruction algorithm. It does not infer endpoint meaning, allocate legacy slots, mutate stage layers, update spans, or serialize the table as checkpoint state. Full integration remains blocked on complete slot-lifecycle evidence and the authoritative mutable stage owner.

## 12. Native detached initial-span reconstruction increment (DEV-003CS-3C)

A first detached `MovingObjectSpanReconstructor` has been added against immutable `StageGrid`. It implements the static stage-preprocessing projection only:

- row-major traversal;
- select player-layer code 48 with background metadata bit `0x8`;
- choose the candidate x coordinate from bit `0x10` in the cell below;
- preserve the candidate x in both endpoint bytes;
- use the source y coordinate when the candidate occupant is one of the recognized source codes (1, 45, 19, 43, 46, 49), otherwise preserve the source's `-1` row sentinel;
- write the zero-based slot into the high byte of the detached packed background metadata while preserving the lower 24 bits.

The result contains an immutable `MovingObjectSpanTable` and a defensive copy of the packed metadata. The supplied `StageGrid` is never mutated. Invalid source geometry (missing below-cell or candidate outside the stage) is rejected rather than reading outside the grid.

Synthetic tests cover right/left candidate selection, row-major slot assignment, recognized and unrecognized occupants, non-flagged code-48 cells, detached metadata, and invalid geometry. The direction fixtures were corrected after review so the below-cell bit and expected candidate agree with the source rule.

**Boundary:** this is only the initial preprocessing projection. It does not yet implement the later span updater, moving-object runtime/collision mutation, restore-time helper sequence, slot removal/reuse lifecycle, or atomic checkpoint publication. Do not claim full moving-object gameplay support.

## 13. Runtime span endpoint updater and gameplay side-effect boundary (DEV-003CS-3D)

The legacy `VVoid()` iterates `eByteArr` in three-byte records and only updates records whose row byte is greater than zero. For each active record it recalculates the first endpoint with `aByte(firstX, -1, 0, row)` and the second with `aByte(secondX, +1, width-1, row)`, then visits every cell in the inclusive updated interval.

The directional scan continues through:
- negative/empty player-layer values;
- code 31;
- occupants recognized by `dInt`: 1, 45, 19, 43, 46, and 49.

It stops at the first other occupant or the source-defined edge. The new `MovingObjectSpanUpdater` reproduces the endpoint projection as a detached, pure operation over immutable `StageGrid`. It preserves inactive row sentinels, rejects invalid active coordinates, and rejects dimensions above 128 because legacy coordinates are signed bytes.

**Important:** legacy `VVoid()` does more than endpoint recalculation. For every cell in the inclusive span it calls `aBoolean(x,y)`. If `dInt` recognizes the occupant, that helper calls `kVoid`, changes the player-layer code to 9, rewrites packed background metadata (including the special snake-43 flag), and calls `cVoid(x,y,1)`. It can also hurt the hero when collision/animation preconditions hold. These gameplay side effects are deliberately not performed by the pure updater and remain a separate transaction milestone.

Synthetic tests cover endpoint movement, traversable/blocked cells, inactive sentinels, invalid row/endpoints, and signed-byte dimension limits. Full-source golden vectors and the per-cell gameplay mutation transaction remain open.

## 14. Detached interaction mutation plan (DEV-003CS-3E)

The recognized-occupant portion of legacy `aBoolean(x,y)` is now represented as an immutable plan, not immediately applied to live stage state. For each recognized occupant inside an active span's inclusive interval, the plan records:

1. player-layer replacement with code 9;
2. packed background metadata update using `(old & 0xF03FFFFF) | (dInt << 22) | snake43Flag`, where the additional `0x10000000` flag is used only for code 43;
3. the nine `cByteArrArr` marker writes of value 48 in the centered 3×3 region, matching `kVoid(x,y)`;
4. the `cVoid(x,y,1)` interaction-queue event.

The planner validates the active span interval and requires the full 3×3 marker region to be inside the stage. It does not mutate the source `StageGrid`.

**Explicit exclusions:** the legacy hero-overlap damage branch in `aBoolean` depends on player position, cooldown, animation selection, and sprite animation state. The ring-buffer mechanics behind `cVoid` also depend on its live queue index. Neither is executed by this detached plan. A later transaction layer must apply cell changes, marker writes, and queue events atomically and separately model collision damage with the correct gameplay context.

## 15. Atomic detached moving-span mutation transaction (DEV-003CS-3F)

A transaction layer now applies the detached `MovingObjectSpanMutationPlan` to private copies and returns one immutable result containing the updated `StageGrid`, byte-valued marker layer, and interaction queue snapshot.

Source-backed details:
- The legacy interaction queue uses eight slots; its next index advances with `(index + 1) & 0x7`.
- Each queue write stores x, y, and type as bytes and resets the corresponding status byte to zero.
- The marker grid is byte-valued, matching the legacy `cByteArrArr` representation.
- Cell mutations carry expected player/background values. All cell preconditions and all marker/event bounds are checked before any result is published.
- The planner now simulates sequential player-layer mutation across overlapping spans, so a recognized object converted to code 9 by an earlier span is not enqueued a second time by a later overlapping span.
- Stage dimensions above 128 are rejected at both planning and application boundaries because span/queue coordinates are signed bytes.

Synthetic tests cover sequential overlap, queue wraparound, cell/marker/queue publication, stale-precondition rejection, and preservation of caller-owned input state.

**Boundary:** this is an immutable transaction result, not yet a live GameSession integration. Hero collision/damage still needs explicit player position, cooldown, animator state, and a source-derived decision model. Runtime queue consumption and stage-owner publication also remain open.
