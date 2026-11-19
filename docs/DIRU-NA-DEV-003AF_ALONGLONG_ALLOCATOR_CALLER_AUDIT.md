# DIRU-NA-DEV-003AF — aLongArr Allocator Caller & Mode Audit

**Status:** Caller inventory committed; mutual-exclusion invariant remains unproven  
**Parent:** DEV-003AE  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Four-argument motion allocator

Signature: `aByte(byte, byte, byte, byte)`, source lines 15863–15884.

Direct call found:
- Line 16603: `this.aByte((byte)n25, (byte)n24, (byte)n26, (byte)n23)`.

The call occurs in a movement/transition state path after the code updates a direction field and movement coordinates. Immediately afterward, the caller obtains a separate slot via `bByte(n24, n26)`, then changes that selected record's 4-bit type field to 2. This is a strong indication that the four-argument allocator creates a motion/segment record consumed by the following transition sequence. It does not prove that this record population is mutually exclusive with the object layout.

The allocator itself:
- scans slots 0–14 using the 4-bit type field at offset 0;
- selects the first type-zero entry;
- clears the selected 64-bit word;
- writes type 1 at offset 0, subtype at 4, first argument at offsets 6 and 27, second argument at 13, third argument at 20;
- returns the zero-based slot, or -1 if full.

## 2. Six-argument object allocator

Signature: `aByte(int, int, byte, byte, byte, byte)`, source lines 16719–16747.

Three direct call sites were found:

| Line | Caller context | Evidence |
|---:|---|---|
| 16289 | Movement/segment transition | Allocates a record at the preceding tile when the player-layer cell is passable; type is selected as 7 or 5 and direction is -1 |
| 16495 | Directional movement/update switch | Allocates type 7 record using computed coordinates, direction deltas, and the tile's group |
| 16810 | Object/segment relocation | After a tile/player-layer update, allocates a record at the next column with type/group/direction arguments set to zero or derived from the current tile |

The six-argument allocator scans record IDs 1–15 using the 3-bit field at offset 28, clears the selected word, writes type 1 or 7, then writes X, Y, group, and direction. Type 1 also writes the mode field at offset 54 and updates tile metadata; type 7 clears the associated tile group before writing the record.

## 3. Layout coexistence: evidence is insufficient

Both allocators write the same static `aLongArr` array. Their type fields overlap in the underlying 64-bit word but are interpreted at different offsets. The source paths are distinct and their callers are in movement/state-machine code, but this audit has not established a complete mode transition proving that one layout is inactive whenever the other is active.

Therefore:
- keep both layout views over one shared backing array;
- do not allocate separate arrays for each layout;
- do not clear or migrate all records when switching layout without proof;
- do not build a combined record manager yet.

## 4. bLongArr remains separate

The source has distinct getter/setter overloads for `bLongArr`, with its own 15-entry array. In the transition path around line 16469, a `bLongArr` entry is cleared after a type-3 check. It has separate snapshot storage `dLongArr`. The next audit must reconstruct its own field layout and lifecycle rather than reusing either `aLongArr` layout.

## 5. Next stage — DEV-003AG

Reconstruct the independent `bLongArr` record codec:
- enumerate all getter/setter calls and bit offsets;
- identify all types written and tested;
- map allocation, release, and movement transition behavior;
- confirm `dLongArr` snapshot/restore behavior;
- add tests for exact source bit operations and slot indexing.

After that, return to the state-machine question of whether the two aLongArr layouts can coexist. No runtime integration is claimed in this stage.
