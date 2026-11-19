# DIRU-NA-DEV-003Y — Six-Argument Packed Tile Metadata Helper

**Status:** Core helper and regression tests implemented; no live gameplay integration claimed  
**Baseline:** DEV-003X, commit `244220f9db74bc00383c7d687207bd291a22ae2b`  
**Audit source:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source anchor:** `i.java`, helper immediately following the 15-slot allocator

## Scope separation

This helper operates on the source's `eIntArrArr` (packed 32-bit tile metadata) and `cByteArrArr` (paired per-cell byte state). It is distinct from the `bLongArr` 15-slot object table modeled in DEV-003X. No `long` codec or slot-table behavior is reused.

## Reconstructed operation

Source signature, with decompiler names retained for traceability:

```java
aVoid(int n, int n2, byte b, byte b2, byte b3, byte b4)
```

The successful-path behavior is:

1. Read `eIntArrArr[n][n2]`.
2. Compute the field shift as `b * 9 + b3`.
3. Extract the current field using unsigned right shift and the mask `~(-1 << b4)`.
4. Replace it with the original int expression `(word ^ (current << shift)) | (b2 << shift)`.
5. Set `cByteArrArr[n][n2]` to 24.
6. Set `cByteArrArr[n][n2 + 1]` to 24.

Both shifts in the setter are 32-bit Java `int` operations. The new value is not pre-masked; preserving that detail is important for source parity.

The reconstructed core API is `LegacyPackedTileMetadata.write(row, column, group, value, bitOffset, width)`. The wrapper validates the addressed cells and packed-field bounds before mutation. For valid source call patterns, the packed-word expression and two byte writes are preserved.

## Evidence and observed call pattern

The source helper is called with group values in the range 0–2. One observed update uses group `b6`, value 7, bit offset 3, and width 4. Other call sites write values 3 or 7 into 4-bit fields. This supports a 9-bit group stride and a 32-bit packed word; it does not establish semantic names for every field.

The paired byte assignments are recorded as raw side effects only. Their higher-level meaning (timer, animation duration, or another per-cell state) is not assigned without tracing every reader and writer.

## Tests

- Group stride and field extraction for groups 0, 1, and 2.
- Replacement of the selected field while preserving unrelated bits.
- Both adjacent byte-state writes to 24, without changing the following cell.
- Unmasked int setter behavior.
- Invalid field and missing adjacent-cell rejection before mutation.

## Boundaries and next work

- No integration into movement, collision, or entity update paths is claimed.
- The helper's callers and the complete read/write lifecycle of `cByteArrArr` remain to be traced.
- Exact array allocation dimensions and all edge-cell padding assumptions remain open.
- No proprietary game assets are copied.
