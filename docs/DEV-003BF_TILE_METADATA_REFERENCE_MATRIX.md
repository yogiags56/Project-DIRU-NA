# DEV-003BF — Tile Metadata Reference Matrix

## Scope and source provenance

This audit examines the packed tile metadata grid `eIntArrArr` and every direct source expression found that extracts the low three-bit record ID from a metadata group. Source is the decompiled candidate `src/main/java/i.java` at pinned revision `215c71d04057d66a1b08419b1a4b1043ae3e0881` in `kubikaugustyn/DiamondRushSource`.

A metadata group occupies nine bits. The relevant fields observed in this audit are:
- bits 0..2: object-record ID (three bits; zero is the empty/no-record value in normal writers);
- bits 3..6: tile/group classification;
- bits 7..8: auxiliary group state/direction.

The source is decompiled Java, not original JAR bytecode. Findings below are static and must be validated against the shipping JAR and runtime before claiming exact game behavior.

## 1. Direct metadata record-ID reader matrix

| Source line | Context | ID extraction | Record fields read | Validation observed | Effect |
|---:|---|---|---|---|---|
| 7681–7684 | Visible tile rendering loop | Extracts ID from each of three groups in the tile | Reads object field at bit 45, width 2 | Checks ID nonzero before record-field read; no coordinate/group identity validation in this rendering path | Uses record field to choose vertical animation/clip offset; read-only |
| 16252 onward | Object/controller update path, controller state handling | Extracts ID from the tile group selected by the record's current group | Reads object field at bit 54, width 3; then writes fields and type/state on the referenced record | The inspected path first validates the record's own coordinates and metadata group against the tile/group; checks neighboring tile group classification before reclassification/transfer | May mutate referenced record and tile metadata; may clear/reassign records through subsequent state logic |
| 16752–16775 | Generic metadata lookup helper | Extracts ID from the requested tile/group | Reads record X (bits 31..36), Y (37..42), group (43..44), and type (bits 3..6) | Compares stored X/Y/group with requested X/Y/group. On mismatch, inspected cleanup is conditional on record type 6; it clears the tile metadata group, not the object record | Returns a derived classification/status; can unlink stale/mismatched tile metadata for Type-6 case |

The helper at 16752 is generic: it does not require Type-5. It decodes the record type and has special behavior for types 4, 5, and 6 only after the coordinate/group identity check. It must not be implemented as a Type-5-specific accessor.

## 2. Metadata group readers that are not record-ID readers

These reads are relevant to metadata semantics but do not themselves dereference an object record:

| Source line | Read | Purpose / observed behavior |
|---:|---|---|
| 1273, 9139, 10800 | Four-bit tile field at bit 0 | Player/action logic tests tile classifications 7 and 8; not the three-bit object-record ID field |
| 15978 | Four-bit group classification at bit 3 | Controller traversal reads classification for the next tile/group |
| 16225 | Two-bit field at bit 7 | Reads per-group auxiliary state |
| 16239 | Four-bit group classification at bit 3 | Checks for classification 7 |
| 16397 | Four-bit group classification at bit 3 | Checks for empty classification before writing classification 3 |
| 16715 | Clears the nine-bit metadata group | This is a write/unlink operation, not a record-ID read |
| 16785–16787 | Tests neighboring tile metadata for nonzero | Determines traversal direction; does not decode an object-record ID |

Important distinction: calls reading bit 0 width 3 from `eIntArrArr` are record-ID extraction; calls reading bit 0 width 3 from a different packed value (for example `aLongArrArr`) are unrelated and must not be conflated.

## 3. Writer/linkage behavior confirmed by the reader paths

- The object-record allocator writes a record ID into tile metadata group bits 0..2 and writes the group classification into bits 3..6.
- The metadata unlink helper clears all nine bits for the selected group and marks the tile for redraw.
- The generic lookup helper compares the record's stored coordinates and group against the tile/group being queried.
- The rendering path is intentionally lighter: it checks for a nonzero ID before reading a visual/controller field, but the inspected path does not repeat the coordinate/group identity validation.
- The controller update path contains more extensive identity and neighboring-group checks before modifying linked records.

## 4. Safe native implementation contract (evidence-backed minimum)

1. Decode the requested tile/group bounds before reading packed metadata.
2. Treat record ID zero as no linked record. Never pass zero into a one-based object-record accessor.
3. Validate IDs against the object table capacity (1..15) before indexing.
4. Decode record type, coordinates, and metadata group only after the ID range check.
5. For operations that mutate, transfer, unlink, or free a record, require stored X/Y/group to match the requesting tile/group unless the specific operation is explicitly documented as a relocation/reclassification path.
6. Keep rendering's lighter lookup as a separate API only if preserving its exact behavior is necessary; prefer a safe validated lookup in native code and test visual parity where stale metadata could alter rendering.
7. Unlinking a tile group must not implicitly free an object record. Record release and metadata unlink remain separate operations.
8. A Type-5 record may be shared by multiple tile metadata groups. Do not free it merely because one group is unlinked.
9. Treat malformed IDs, stale IDs, and out-of-range coordinates as safe lookup failures in native code; do not reproduce Java array-index crashes.

## 5. Remaining uncertainties

- The decompiled source does not prove whether every nonzero tile ID is always valid at runtime.
- The rendering path's lack of coordinate/group validation may be safe due to update ordering, but that ordering has not been runtime-traced.
- The helper's Type-6 mismatch cleanup is visible; no equivalent general stale-reference repair for Type-5 was proven here.
- The source-level inventory is based on direct references in one large decompiled file. Reflection, generated code, or original bytecode behavior is not covered.
- No JAR-vs-source bytecode comparison, runtime instrumentation, native tests, or Android build was performed.

## 6. Next milestone: DEV-003BG

Trace the three reader paths through their callers and update ordering, especially rendering versus controller updates. Establish when stale references can arise and whether a record can be safely released only after all metadata groups have been unlinked. Then define the native object-handle API and add isolated tests for empty IDs, valid links, mismatched coordinates/groups, shared references, stale IDs, and full-table allocation failure.
