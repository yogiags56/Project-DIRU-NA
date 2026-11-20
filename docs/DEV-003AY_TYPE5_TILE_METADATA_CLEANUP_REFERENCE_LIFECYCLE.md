# DIRU-NA-DEV-003AY — Type-5 Tile Metadata Cleanup and Reference-Lifecycle Audit

**Status:** Source-level cleanup semantics clarified; per-level lifetime and caller bounds remain unresolved  
**Parent:** DEV-003AX  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope

Continue the type-5 investigation without changing production code. Trace the tile metadata reader/clear path, compare it with the aLongArr whole-record clear helper, and establish what can and cannot be concluded about type-5 record lifetime.

This is a static audit of the pinned decompiled source. It is not a shipping-JAR bytecode parity result or a live-gameplay trace.

## 2. Confirmed metadata lookup

The helper previously identified in DEV-003AX (approximately source lines 16750–16780) reads the tile's three-bit object/record ID from the packed tile metadata group. It then reads the referenced aLongArr record's fields:

- X: offset 31, width 6
- Y: offset 37, width 6
- group: offset 43, width 2

It compares the record coordinates/group with the requested tile and uses the tile's four-bit subtype to derive the interaction result.

This confirms that the tile metadata ID is a live cross-reference into the 15-record aLongArr table. A native model must not treat the tile's three-bit ID as an independent decorative field.

## 3. Tile metadata clearing is not record clearing

The subtype-6 branch in the lookup helper calls `cVoid(x, y, group)`.

The inspected `cVoid` implementation:
1. Reads the packed tile metadata word from `eIntArrArr[x][y]`.
2. Clears the selected nine-bit metadata group.
3. Sets `cByteArrArr[x][y]` to 24.

It does **not** call `fVoid(recordId)`, and does not directly zero `aLongArr[recordId - 1]`.

The separate `fVoid(byte id)` helper performs the whole-record clear:
`i.aLongArr[id - 1] = 0L`.

Therefore, the audited metadata-clear operation and the audited record-clear operation are distinct operations. The source excerpt does not establish that clearing a tile reference automatically releases the referenced record.

**Compatibility consequence:** do not implement tile metadata removal as an implicit aLongArr free, and do not free a type-5 record merely because one tile metadata group was cleared. A record may be referenced by multiple traversed tiles.

## 4. Type-5 line-traversal creation and linking

The creation path previously identified around source lines 16571–16577:
- requests an ordinary allocator record using mode 8;
- reclassifies the returned record as type 5;
- writes source coordinates at offsets 14 and 20;
- stores the returned one-based record ID in tile metadata for eligible traversed tiles.

The local `n32` gate is initialized to 1 and becomes 0 after the first eligible tile. Thus one invocation creates at most one type-5 record and links later eligible tiles in that invocation to the same record.

This is only a per-invocation bound. The inspected excerpt does not prove how many times the traversal routine is invoked in a level, whether records are reused, or whether a level transition replaces the complete table.

## 5. Lifecycle conclusions

**Supported by the pinned source:**
- Tile metadata contains a three-bit record ID.
- The lookup path dereferences that ID and validates coordinate/group fields.
- The subtype-6 cleanup clears a nine-bit tile metadata group and updates the per-cell byte state.
- The cleanup path does not itself perform the whole-word aLongArr clear.
- A type-5 marker can be shared by multiple traversed tile metadata entries created during one traversal invocation.

**Still unknown:**
- Whether another caller clears the corresponding type-5 record after all tile references disappear.
- Whether traversal invocations are bounded or deduplicated at a higher level.
- Whether stage teardown replaces the aLongArr table before stale references can be used.
- Whether snapshot/restore can reinstate tile references without the matching record population.
- Whether the original shipping JAR has the same control flow as this decompiled source.

## 6. Native reference-integrity requirements

Before implementing the type-5 lifecycle, the core model should be able to express and test these invariants:

1. Every nonzero tile record ID must be in the valid one-based range 1..15.
2. Every nonzero tile record ID must resolve to a non-free record.
3. A resolved record's coordinate/group fields must be checked using the same semantics as the legacy helper; do not assume the two coordinate pairs at offsets 14/20 and 31/37 are interchangeable.
4. Clearing one tile metadata group must not silently clear a record that may still be referenced by another tile.
5. Record reclamation must require evidence that no live tile metadata references remain, or a separately proven legacy lifecycle event such as whole-table replacement.
6. A failed allocation must be represented explicitly in the native implementation; never index record ID 16 in a 15-entry array.

These are safe native design requirements, not claims that the original game used reference counting or guarded allocation.

## 7. Production and validation status

- No production source or codec behavior changed.
- No new test was added: the current core model does not yet contain a tile metadata grid or a cross-reference lifecycle model.
- Existing capacity fixtures remain codec-level tests only.
- No local Gradle run, successful CI result, shipping-JAR parity check, or live-runtime validation is claimed.

## 8. Next stage — DEV-003AZ

1. Enumerate every call site of the line-traversal/type-5 creation routine and classify its gameplay trigger.
2. Trace every assignment/reinitialization of the tile metadata grid and aLongArr table across stage load, retry, checkpoint restore, and stage exit.
3. Determine whether any lifecycle path makes type-5 records unreachable before table replacement.
4. Build a source-derived per-level upper-bound model for type-5 creation; keep unknown call counts explicit.
5. Add a dedicated tile-reference model and tests only after the source semantics and reset boundaries are sufficiently established.
6. Keep production reclamation/capacity changes blocked until original JAR/classfile or runtime evidence is available.
