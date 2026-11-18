# DIRU-NA-DEV-003V — Packed Slot Allocator

**Status:** Pure allocator and synthetic tests committed; no live stage integration  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchors:** `i.java:15863–15884`, `15899–15915`

## CI baseline

Run #39 passed: core JVM tests, Android debug APK assembly, and artifact upload.

## Source behavior

The allocator scans exactly 15 entries in `bLongArr`. An entry is available when its low four bits are zero. The first such entry is selected; if none exists, the helper returns -1 and performs no packed writes.

On allocation it writes these fields, in source order:

| Field | Bit offset | Width | Value |
|---|---:|---:|---|
| Slot active marker | 0 | 4 | 1 |
| Type byte | 4 | 2 | fourth argument |
| Anchor tile | 6 | 7 | first argument |
| Anchor X | 13 | 7 | second argument |
| Anchor Y | 20 | 7 | third argument |
| Anchor tile mirror | 27 | 7 | first argument |

The helper's byte arguments are signed Java bytes. The original write expression shifts the signed byte as an int and then widens to long; this implementation intentionally preserves that behavior rather than silently normalizing all values to unsigned.

## Implementation

`LegacyPackedSlotAllocator` clones the supplied 15-word array, selects the first free slot, and returns a result containing the selected index and updated copy. Input storage is never mutated. A full table returns -1 with an unchanged copy.

## Tests

Synthetic tests cover first-free selection, skipping occupied slots, full-table failure, all packed fields, preservation of unrelated bits, and defensive copying.

## Boundary

This model covers only the four-argument allocator. It does not implement the distinct six-argument `aByte` helper called later by `mVoid`, does not touch `eIntArrArr` or `cByteArrArr`, and does not mutate live game state.

## Next

**DEV-003W — Packed Slot Read/Write Round-Trip and Free-State Semantics.** Verify each field extractor and writer against the legacy bit formulas, including signed-byte edge cases, before wiring allocator results into scripted object initialization.
