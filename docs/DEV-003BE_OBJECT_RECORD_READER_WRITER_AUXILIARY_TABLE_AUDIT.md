# DEV-003BE — Complete Object-Record Reader/Writer & Auxiliary Table Audit

## Scope and provenance

Static audit of the decompiled candidate source `src/main/java/i.java` from `kubikaugustyn/DiamondRushSource`, pinned to revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This is source-level evidence only. It is not a comparison against the original shipping JAR bytecode and is not a runtime trace.

## Executive result

The cross-file audit is not applicable to this candidate's monolithic `i.java`, but the full-file inventory of direct `aLongArr` and `bLongArr` references is now available. The two 15-slot tables have separate packed layouts and separate roles:

- `aLongArr[15]` stores object records. Its per-record type occupies bits 28..30; coordinates occupy bits 31..36 and 37..42; metadata group occupies bits 43..44. Other fields are used by controller logic.
- `bLongArr[15]` is an auxiliary work table. Its low four bits are a state/kind field, bits 4..5 encode a direction, and coordinates are stored in the seven-bit fields beginning at bits 13 and 20. It is not the Type-5 object-record table.

No production source or tests were changed in this milestone.

## 1. Complete direct-reference inventory

### `aLongArr`

Full-file direct-reference locations:

| Line | Operation |
|---:|---|
| 489 | Static declaration |
| 4908 | Set to null during teardown |
| 15226 | Save checkpoint: copy to `cLongArr` |
| 15353 | Restore checkpoint: copy from `cLongArr` |
| 15842 | Allocate 15-entry table in `arVoid()` |
| 15900 | Packed-field reader `aInt(recordId, bit, width)`; indexes `recordId - 1` |
| 15906 | Packed-field writer `aVoid(recordId, value, bit, width)`; indexes `recordId - 1` |
| 16659 | Clear a record slot during object-record allocation |
| 16730 | Clear a record slot during object-record allocation |
| 17752 | Allocate 15-entry table during class/static initialization |

The packed-field accessors are the indirect access path for nearly all object-record reads/writes. Their ID parameter is one-based. The allocator scans slots 1..15 for a record whose type bits 28..30 are zero, clears the selected slot, and initializes its fields. The Type-5 creation path uses the six-argument allocator and stores the resulting record ID in tile metadata.

The whole-record release helper `fVoid(byte)` at approximately line 16674 clears `aLongArr[id - 1]`. Earlier call-site analysis found four callers; this helper does not itself unlink tile metadata.

### `bLongArr`

Full-file direct-reference locations:

| Line | Operation |
|---:|---|
| 490 | Static declaration |
| 4910 | Set to null during teardown |
| 15230 | Save checkpoint: copy to `dLongArr` |
| 15354 | Restore checkpoint: copy from `dLongArr` |
| 15844 | Allocate 15-entry table in `arVoid()` |
| 15910 | Packed-field reader for an auxiliary-table slot |
| 15914 | Packed-field writer for an auxiliary-table slot |
| 16469 | Clear a consumed auxiliary entry in controller state 4 |
| 17753 | Allocate 15-entry table during class/static initialization |

The auxiliary-table coordinate lookup helper `bByte(x,y)` scans all 15 slots and compares only the coordinate fields at bits 13 and 20. It does not test the low-four-bit state/kind field before returning a matching slot. Therefore a coordinate match alone is not proof that the entry is currently active; callers must inspect the relevant state where required.

## 2. Auxiliary table lifecycle and state-4 consumer

Controller state 4 scans all 15 `bLongArr` entries. For entries whose low four bits equal 3, it:

1. Reads the packed X/Y coordinates and direction.
2. Clears the auxiliary slot immediately.
3. Advances or adjusts the coordinate according to the direction value.
4. Calls the six-argument object allocator in mode 7 for the supported direction branches, or follows the alternate/default path.
5. Returns the controller to state 2 after the scan.

This confirms that state 4 consumes auxiliary work records, not a list of Type-5 object records. The distinction between controller state (`xByte`) and object type bits (28..30) remains essential.

## 3. Object-record and tile-metadata access paths

The object-record packed-field helpers are generic and do not impose a Type-5-only restriction. The allocator writes object type, coordinates, metadata group, and other controller fields. The Type-5 traversal path writes the allocated record ID into tile metadata and updates record fields through the generic accessors.

The metadata reader around lines 16750–16775 extracts the tile's record ID, then reads that record's X coordinate, Y coordinate, metadata group, and type. It validates the stored coordinates and group against the tile being queried. When they do not match, its inspected cleanup branch clears metadata for a Type-6 record. This helper is not a blanket Type-5-only reader: it is a generic metadata/record consistency path and branches on the decoded record type.

The tile metadata ID itself is held in the packed `eIntArrArr` tile data, not in `bLongArr`. The Type-5 traversal's record ID reference and the auxiliary table's coordinate references are separate linkage mechanisms.

## 4. Checkpoint, initialization, and teardown contract

- `arVoid()` initializes fresh 15-entry object and auxiliary arrays when the feature is active, and resets controller fields.
- Static/class initialization also creates both 15-entry arrays.
- Checkpoint save copies object records and auxiliary records independently into `cLongArr` and `dLongArr`, alongside the tile metadata grid.
- Checkpoint restore copies each table back independently, alongside the tile metadata grid and controller state.
- Stage teardown nulls both active arrays and their checkpoint copies.

This establishes that both tables participate in checkpoint snapshots. It does not establish persistence beyond the current stage lifecycle.

## 5. Cross-method conclusions

1. `aLongArr` is the object-record store; `bLongArr` is an auxiliary coordinate/direction work table.
2. Controller state 4 is an auxiliary-table consumer, not a Type-5 terminal cleanup state.
3. Coordinate lookup in `bLongArr` is not equivalent to checking that an entry is active, because the lookup helper compares coordinates without checking the low-four-bit state.
4. Generic object-record field access does not guarantee that the referenced record is Type-5. Any native reader must explicitly enforce the type and linkage rules required by its caller.
5. Checkpoint correctness requires restoring tile metadata, object records, auxiliary records, and their controller state as a coherent snapshot.
6. The allocator's previously documented Type-5 full-table edge case remains important: the separate safe allocator must not be confused with the six-argument object allocator.

## 6. Remaining unknowns and limits

- The decompiled candidate has not been compared instruction-for-instruction with the original shipping JAR.
- No runtime trace has confirmed when auxiliary records are created relative to all possible consumers.
- The full semantic contract of every tile metadata reader still needs caller-by-caller validation, including bounds assumptions and stale-ID handling.
- This audit does not prove the maximum simultaneous number of live object records or auxiliary records in shipped stages.
- No native implementation or tests were changed or executed, and no Android/Gradle build was run.

## Next milestone: DEV-003BF

Build a caller-by-caller metadata reference matrix: enumerate every path that extracts a tile record ID, document type/coordinate/group validation and cleanup behavior, then derive explicit safe native lookup and unlink contracts. Only after that matrix is complete should isolated native tests be introduced.
