# DIRU-NA-DEV-003AC — Packed Tile / Object Record Boundary Audit

**Status:** Source-level boundary audit committed; no runtime integration  
**Parent:** DEV-003AB  
**Source:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java`  
**Revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Critical distinction: three independent storage systems

The source contains several similarly named packed-data helpers. Their backing arrays and index domains prove they are separate systems:

| Storage | Backing array | Index domain | Purpose established by source |
|---|---|---:|---|
| Tile metadata | `eIntArrArr` | stage row/column | Three 9-bit groups per tile; six-argument setter and three-argument group clear |
| 15-entry long records | `aLongArr` | 1–15, helper converts to index 0–14 | Temporary object/segment records allocated by scanning the first free 3-bit type field |
| Other long records | `bLongArr` | byte index | Separate packed-long getter/setter overload |
| Entity event queue | `aByteArr`, `cByteArr`, `bByteArr` | ring index 0–7 | Stores coordinate/type events; unrelated to the tile metadata clear helper |

Do not merge these storage systems into one generic packed-slot model.

## 2. The 15-entry `aLongArr` allocator is not the DEV-003X lookup table

At lines 16719–16747, the source scans slot IDs 1 through 15 and selects the first whose 3-bit type field at bit offset 28 is zero. It clears that selected 64-bit record, then writes fields:

| Bit offset | Width | Meaning supported by source |
|---:|---:|---|
| 28 | 3 | Record type/state; zero is free, 1 and 7 are explicitly written |
| 31 | 6 | First coordinate |
| 37 | 6 | Second coordinate |
| 43 | 2 | Tile group/band |
| 45 | 2 | Direction/state |
| 54 | 3 | Additional mode/state, written on non-type-7 path |

The type-7 branch clears the source tile group through the static `eIntArrArr` helper, then marks the long record as type 7. The other branch writes type 1 and additional record fields, including source tile group and segment metadata.

This is a distinct allocator over `aLongArr`; it must not be confused with DEV-003X's `bLongArr` 15-slot X/Y lookup, nor with `eIntArrArr` tile groups.

## 3. Lookup/validation helper behavior

The static helper at lines 16750–16781 extracts the record's 3-bit type, coordinates, group, and 4-bit tile subfield. If the record's encoded coordinates/group match the requested tile and the tile subfield is 4 or 5, it maps those states to return values 2 or 4. Otherwise, if the tile subfield is 6, it clears the requested tile group. It then returns the tile subfield shifted left by one.

This establishes a stale-record cleanup path: a tile group can be cleared when the tile's state says 6 but its referenced record does not match the expected coordinates/group.

## 4. Movement helper boundary

The instance helper at lines 16662–16705 first checks the target player-layer cell for blocking values. On a passable target it updates the long record's coordinate field, writes a tile metadata 3-bit field, and then either:
- clears the destination tile group when the boolean mode is true; or
- writes a 4-bit segment state selected from movement direction and a special coordinate comparison.

This is evidence that tile group clear participates in record movement/relocation. It is not enough to claim all gameplay semantics or to wire a new entity system.

## 5. Ring queue is unrelated

The instance `cVoid(int,int,int)` at lines 11009–11016 writes two coordinates and a type into `aByteArr`, `cByteArr`, and `bByteArr`, then advances a ring index modulo 8. This overload has no direct relationship to the static packed tile group clear at line 16712.

## 6. Engineering decision

No new production helper is added in this stage. The correct next task is to reconstruct the `aLongArr` record codec as its own isolated component, with an explicit 64-bit layout and tests, while leaving the `eIntArrArr` tile helper and DEV-003X `bLongArr` work untouched.

Before implementing, confirm:
- all reads/writes of `aLongArr` use the same bit offsets and width assumptions;
- whether the 15 slots are reset on stage load/restart;
- every type value used by callers, not only 0, 1, and 7;
- whether coordinate fields are unsigned six-bit values in all call paths;
- behavior when no free slot exists (source scan can reach beyond the nominal slot range unless guarded by caller invariants).

No gameplay integration or CI success is claimed.
