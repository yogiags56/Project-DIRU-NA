# DIRU-NA-DEV-003AX — Type-5 Tile-Reference and Coordinate-Use Audit

**Status:** Type-5 map-reference role identified in the pinned source; lifetime/capacity bound still open  
**Parent:** DEV-003AW  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Key correction: type 5 is referenced by tile metadata

The type-5 line-traversal creation path at 16571–16577 does more than allocate a record and change its type:

1. Calls the ordinary aLongArr allocator with mode 8.
2. Changes the allocated record's type field at offset 28 to 5.
3. Writes the source tile coordinates into offsets 14 and 20.
4. Writes the returned one-based aLongArr record ID into the tile metadata group at offset 0, width 3, for each traversed tile (16577).

This means type-5 records are not simply orphaned allocator entries. The level tile metadata contains references to the allocated record ID.

The caller's loop uses a local `n32` gate initialized to 1 and changes it to 0 after the first eligible tile (16571–16575). Therefore this particular traversal invocation creates at most one type-5 record, then links subsequent eligible tiles to that same record. This is a per-invocation bound, not a proven per-level bound.

## 2. Coordinate fields have two roles

The ordinary allocator initially writes:
- X at offset 31, width 6;
- Y at offset 37, width 6.

The type-5 caller additionally writes:
- source X at offset 14, width 6;
- source Y at offset 20, width 6.

The record therefore preserves both the allocator's coordinate pair and an explicit caller-side coordinate pair. Do not collapse these into one pair in a compatibility model without checking all consumers.

The helper at 16750–16780 obtains a three-bit object ID from tile metadata, reads the referenced aLongArr record's coordinate fields at offsets 31 and 37 and its group field at offset 43, then compares those values with the requested tile/group. Depending on the tile's four-bit subtype at offset 3, it returns a derived tile code; for subtype 6 it clears that tile's nine-bit metadata group through `cVoid`.

This is a concrete reader path connecting tile metadata to the referenced aLongArr record. It establishes that the record is used as part of tile/collision lookup, not just allocated and forgotten.

## 3. Type-5 consumption path

The interaction dispatcher at 16512–16527 has a case 5 branch. It reads the current tile metadata and calls the helper above to resolve the tile code. It then proceeds through the case-5 interaction logic using the resolved code and associated map coordinates.

This branch is distinct from the primary aLongArr movement dispatcher at 16075. The absence of `case 5` in the movement dispatcher does not mean type-5 records are globally unread: they are reachable through the tile metadata lookup path.

The source evidence therefore corrects the overly broad interpretation from earlier stages:
- type 5 is not processed by the primary object-movement switch;
- type 5 is referenced by tile metadata and used by a separate interaction/collision path;
- no type-5-specific whole-word release was identified in the audited source excerpt.

## 4. Capacity implications

The new evidence supports a map-marker interpretation for type 5. It may be intentionally retained while tile metadata points to it. Clearing it merely because the movement dispatcher has no type-5 case would break those tile references.

The creation gate limits one traversal invocation to at most one new type-5 record, but the number of invocations and the cleanup/rebuild policy across the level remain unresolved. A 15-slot exhaustion scenario cannot be accepted or dismissed yet.

The offset-54 reference issue remains separate: type-5 creation uses the ordinary allocator with offset-54 value 0, so this path does not itself create a high bLongArr reference. It can, however, consume aLongArr capacity and affect future allocation availability.

## 5. Tests and implementation status

- No production code changed.
- Existing capacity tests remain valid for allocator occupancy semantics.
- No new test was added because the current core model does not yet model the full tile metadata grid and its cross-reference lifecycle.
- No local Gradle run, successful CI result, or shipping-JAR bytecode parity is claimed.

## 6. Next stage — DEV-003AY

1. Trace the full case-5 interaction branch to identify every tile metadata rewrite, clear, and record reuse.
2. Determine whether clearing a tile's metadata group also clears the referenced type-5 aLongArr record, or whether those operations are independent.
3. Inspect all callers of the traversal routine and establish how often a new type-5 marker can be created during one level.
4. Build a reference-integrity invariant: every nonzero tile object ID must resolve to a live aLongArr record with matching coordinate/group fields.
5. Use an original JAR/classfile comparison if obtainable; the pinned decompiled source is not sufficient to settle binary parity.
