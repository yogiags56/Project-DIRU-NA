# DIRU-NA-DEV-003W — Packed Field Codec and Legacy Shift Semantics

**Status:** Packed codec, allocator correction, and synthetic tests committed  
**Source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchors:** `i.java:15886–15887`, `15899–15915`

## CI #40 finding

The allocator test exposed a genuine Java arithmetic detail. The legacy setter extracts the existing field from a long, but performs `current << offset` and `value << offset` as 32-bit int shifts before widening to long. At offset 27, high bits can overflow. For example, writing 42 at offset 27 yields a readable 5-bit residue of 10, not 42. A 64-bit-clean implementation would be incompatible.

## Implementation

Added `LegacyPackedFieldCodec`:
- `read` reproduces unsigned long shift followed by width mask.
- `write` reproduces int shift overflow/sign extension, then long XOR/OR.
- Bounds validate the 64-bit word and field width.

Updated `LegacyPackedSlotAllocator` to use the codec for slot occupancy and all six writes. This fixes the previous 64-bit clear behavior and preserves legacy semantics.

## Tests

Tests cover normal field round trips, offset-27 overflow, negative byte values, field bounds, allocator selection, full-table behavior, defensive copy, and preservation under the exact legacy operation.

## Next

**DEV-003X — Packed Slot Lookup and Free-State Semantics.** Verify the 15-slot coordinate lookup, active marker interpretation, and slot invalidation against the source helpers before object initialization is integrated.
