# DIRU-NA-DEV-003X — Packed Slot Lookup and Free-State Semantics

**Status:** Lookup/invalidation model and codec correction committed; no live object integration  
**Source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Anchors:** `i.java:15909–15929`, `16465–16469`

## CI #41

Run #41 passed: core JVM tests, Android debug APK assembly, and artifact upload.

## Critical codec correction

The exact setter is `word ^ ((long) readField << offset) | (long) (value << offset)`. The clearing shift is 64-bit; the new-value shift is 32-bit int before widening. The previous codec incorrectly performed the clearing shift as int too. The codec now preserves this asymmetry.

## Coordinate lookup

The source scans slots 0 through 14 and compares packed X at offset 13 width 7 and packed Y at offset 20 width 7. It does not check the active marker before comparing coordinates; therefore an earlier inactive/zeroed slot at coordinate (0,0) can match a (0,0) lookup. First matching index wins; no match returns -1.

## Invalidation

At the inspected source call site, a slot is cleared to zero only after its low four-bit marker is exactly 3. This milestone models that guarded invalidation as a copy-returning operation; it does not claim every slot lifecycle path uses marker 3.

## Tests

Tests cover first-match coordinate lookup, no match, lookup independent of marker, marker-3 invalidation, preservation of other markers, defensive copies, and mixed int/long shift semantics.

## Next

**DEV-003Y — Six-Argument Packed Tile Metadata Helper.** Audit and model the distinct `eIntArrArr` bitfield setter plus paired `cByteArrArr` duration writes. Keep it separate from the `bLongArr` slot table.
