# DIRU-NA-DEV-003U — Type 19 Side-Effect Order

**Status:** Source audit and immutable intent model committed; no side effects applied
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881
**Source anchors:** `i.java:12120–12138`, `15840–15860`, `15863–15897`, `16798–16811`

## CI #37 correction

Run #37 failed one synthetic test because `0x1213` still has low byte `0x13` (foreground type 19). The intended non-19 terminator is now `0x1200`; the scan implementation itself is unchanged.

## Exact source order

1. If `lBoolean` is false, call `arVoid()`.
2. Set `lBoolean=true`.
3. Set `alBoolean=false`.
4. Allocate `eIntArrArr` only if it is null.
5. Call the four-byte `aByte(...)` allocator with anchor tile value and coordinates.
6. Call `mVoid(anchorX, anchorY)`.
7. Increment anchor Y; clear consecutive foreground low-byte type-19 cells until first non-19 cell.

Important inversion: `arVoid()` itself resets arrays and counters only if `lBoolean` is true. At this call site it is invoked only when `lBoolean` is false, so the helper body does not perform those resets in this path. Then the caller sets `lBoolean=true`.

The four-byte `aByte` searches up to 15 slots in `bLongArr`, initializes a free slot's packed fields, and returns the slot index or -1. `mVoid` checks `bByte(anchorX,anchorY)`; if a slot exists, it updates packed metadata, clears the player-layer anchor cell, sets `xByte=1`, and invokes a separate six-argument `aByte` helper.

`mVoid` and the later foreground clearing are mutations. The current intent model only records order; it does not execute these helpers or mutate stage data.

## Next

**DEV-003V — Type 19 Packed Slot Allocation Model.** Decode the slot search and packed-field writes into a bounded pure allocator model, keeping the distinct six-argument helper outside scope.