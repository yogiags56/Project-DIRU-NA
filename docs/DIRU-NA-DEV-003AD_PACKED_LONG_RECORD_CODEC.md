# DIRU-NA-DEV-003AD — Packed Long Record Codec Reconstruction

**Status:** Isolated codec and regression tests committed; runtime integration deferred  
**Parent:** DEV-003AC  
**Source:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java`  
**Revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Key finding: two overlapping layouts share aLongArr

The 15-element `aLongArr` is accessed through two distinct allocation paths. They use overlapping bit positions and must be treated as separate record layouts, not combined into one universal schema.

### Layout A — zero-based motion/segment record

Allocator: four-argument `aByte(byte, byte, byte, byte)`, source lines 15863–15884, called by movement/event handling.

| Offset | Width | Observed write |
|---:|---:|---|
| 0 | 4 | Type = 1 |
| 4 | 2 | Subtype argument |
| 6 | 7 | First byte argument |
| 13 | 7 | Second byte argument |
| 20 | 7 | Third byte argument |
| 27 | 7 | First byte argument (same input as offset 6) |

Slots are scanned from index 0 through 14. The first record whose 4-bit type field at offset 0 is zero is selected. If none is free, this allocator returns -1.

### Layout B — one-based object/segment record

Allocator: six-argument instance `aByte(int, int, byte, byte, byte, byte)`, source lines 16719–16747.

| Offset | Width | Observed write |
|---:|---:|---|
| 28 | 3 | Type = 1 or 7 |
| 31 | 6 | X coordinate |
| 37 | 6 | Y coordinate |
| 43 | 2 | Group/band |
| 45 | 2 | Direction/state |
| 54 | 3 | Mode/state on type-1 path |

Record IDs are 1 through 15; helper access converts the ID to `id - 1`. The allocator scans the 3-bit type field at offset 28. The type-7 path clears the associated tile group and writes type 7; the type-1 path writes the other fields shown above.

The source type-7 branch and its caller constraints are not exposed as a separate convenience allocator in this core API. The committed API implements the type-1 path and raw field operations only.

## 2. Shared low-level bit semantics

The source getter reads:

`(int) (aLongArr[id - 1] >>> offset & ~(-1L << width))`

The source setter reads the current field and performs:

`(old ^ (current << offset)) | ((long) value << offset)`

The new value is not masked before shifting. This detail is preserved by the codec's raw write method.

## 3. Snapshot/restore lifecycle

The source creates `aLongArr` with 15 elements during initialization/reset and clears it to null during teardown. When saving a retry/checkpoint state, it copies all 15 entries into `cLongArr`; restore copies them back. Thus the complete 15-word array is part of the retry snapshot, not merely transient local scratch memory.

The separate `bLongArr` is copied to/from `dLongArr`; it remains a distinct store and is not modeled by this codec.

## 4. Implementation

Added `LegacyPackedLongRecordCodec` with explicit nested `MotionLayout` and `ObjectLayout` views over the same 15-word storage. This makes the overlapping interpretations explicit and prevents accidental field aliasing from being hidden.

- Motion layout uses zero-based slots and a 4-bit type field at offset 0.
- Object layout uses one-based IDs and a 3-bit type field at offset 28.
- Both use exact 64-bit unsigned reads and source-style unmasked writes.
- Allocation clears the selected word before writing the record.
- Object direction values below zero normalize to 2, matching the source allocator.

## 5. Validation and limitations

Regression tests cover both layouts, field reads/writes, allocation index conventions, no-free-slot behavior for the zero-based allocator, negative direction normalization, unmasked write behavior, and invalid ranges.

No gameplay integration is claimed. The two layouts overlap and must not be simultaneously populated as independent records without proving the source's mode/lifecycle invariants. The source's one-based allocator appears to assume a free slot exists; the wrapper returns -1 safely when full rather than reproducing a possible out-of-bounds failure.

Next: DEV-003AE — audit every caller and lifecycle transition for both aLongArr layouts, including retry snapshot timing and whether the two allocation paths can be active in the same stage.
