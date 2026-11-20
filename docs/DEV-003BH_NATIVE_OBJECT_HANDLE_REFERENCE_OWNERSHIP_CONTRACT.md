# DEV-003BH — Native Object Handle & Reference Ownership Contract

## Scope and source provenance

This document defines the minimum native API contract from the static findings in the decompiled candidate `kubikaugustyn/DiamondRushSource/src/main/java/i.java`, pinned to revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This is a design contract, not a production implementation. The source has not been compared instruction-for-instruction with the original shipping JAR, and no runtime trace or native test has been performed.

## 1. Critical representation finding: record table capacity versus tile-link width

The source has a 15-slot object table:
- the six-argument object allocator scans record IDs 1..15;
- object accessors use one-based IDs and index `aLongArr[id - 1]`;
- the allocator writes the returned ID into tile metadata using a three-bit field at group offset 0.

A three-bit field can encode only values 0..7. Therefore, the source-level evidence presents a representation mismatch: the allocator can return IDs 8..15, but the tile metadata field cannot faithfully encode those values. The decompiled writer expression passes the ID to a three-bit packed-field writer, which retains only the low three bits.

**Do not assume that all 15 object slots are safely addressable through tile metadata.** IDs 8..15 may be used by non-tile-linked paths, may expose a source defect, or may rely on an unobserved convention. This requires original-JAR bytecode and runtime validation. Native code must not silently truncate an object handle into the three-bit tile field.

The API therefore distinguishes:
- `ObjectSlotId`: one-based object-table slot, valid range 1..15;
- `TileRecordId`: three-bit metadata value, valid range 0..7, where 0 means no link;
- `ObjectHandle`: native-safe opaque handle, not interchangeable with either packed representation.

Until the discrepancy is resolved, linking a slot above 7 into a tile metadata group must return an explicit `UNREPRESENTABLE_TILE_ID` result. Do not alias it to slot 1..7.

## 2. Native data model

Recommended conceptual model:

```cpp
using ObjectSlotId = uint8_t;   // 1..15
using TileRecordId = uint8_t;   // 0..7; 0 = empty
using ObjectHandle = uint32_t; // native generation-tagged handle

enum class RecordType : uint8_t {
    Empty = 0,
    Type1 = 1,
    Type2 = 2,
    Type3 = 3,
    Type4 = 4,
    Type5 = 5,
    Type6 = 6,
    Type7 = 7
};

struct ObjectRecord {
    bool occupied;
    uint32_t generation;
    RecordType type;
    uint8_t x;
    uint8_t y;
    uint8_t metadataGroup;
    uint8_t legacyFields[/* project-defined packed fields */];
};
```

The generation counter prevents a stale native handle from accidentally resolving to a newly reused slot. This is a native safety improvement; it is not a claim that the original game used generation counters.

The exact field widths and coordinate bounds must follow the validated stage dimensions and packed source representation. Do not use the illustrative structure above as a byte-for-byte serialization layout.

## 3. Required API surface

### Allocation and lookup

```cpp
Result<ObjectHandle> allocateObject(RecordType type, ObjectInit init);
Result<ObjectRecordView> lookupObject(ObjectHandle handle);
Result<ObjectRecordView> lookupObjectBySlot(ObjectSlotId slot);
Result<ObjectRecordView> lookupTileLinkedObject(TileCoord tile, uint8_t group);
```

Rules:
- Allocation is atomic: either a complete initialized record is published or no slot/state changes.
- Object-table slots are checked before indexing.
- A slot is occupied only when its record type/state is non-empty according to the validated packed contract.
- Handle lookup validates slot and generation.
- Tile-linked lookup additionally validates the tile coordinate, group index, nonzero tile ID, representability, occupancy, and stored record X/Y/group identity.
- Never use a raw packed tile ID as an unchecked object-array index.

### Link and unlink

```cpp
Result<void> linkTileRecord(TileCoord tile, uint8_t group, ObjectHandle handle);
Result<void> unlinkTileRecord(TileCoord tile, uint8_t group);
```

Rules:
- Link validates that the handle is live and that its slot is representable by the current three-bit legacy tile field.
- Link updates the tile metadata and reverse-reference bookkeeping as one logical transaction.
- Unlink clears only the selected nine-bit metadata group and removes exactly that reverse reference.
- Unlink does not implicitly release the object record.
- Re-linking the same tile/group to the same handle is idempotent or returns a clearly defined already-linked result; it must not increment a reference count twice.

### Reclassification and relocation

```cpp
Result<void> reclassifyObject(ObjectHandle handle, RecordType newType);
Result<void> relocateObject(ObjectHandle handle, TileCoord newCoord, uint8_t newGroup);
```

Rules:
- Reclassification preserves handle identity and does not allocate a second slot unless the source behavior explicitly requires a replacement.
- Relocation updates the record's canonical coordinates/group and all affected tile links transactionally.
- A failed relocation leaves the old record and all old links intact.
- The API must not infer that changing a record type automatically releases or clears its tile references.

### Release

```cpp
Result<void> releaseObject(ObjectHandle handle);
```

Rules:
- Release fails with `OBJECT_STILL_REFERENCED` while any tile group or other native owner still references the record.
- Release clears the record only after all references are removed and the handle generation is invalidated.
- The legacy `fVoid(id)` behavior (clear the object slot) is not a sufficient native release contract because it does not itself unlink tile metadata.
- If a compatibility mode must reproduce legacy behavior, isolate it behind an explicitly named unsafe/legacy function and never use it for normal native ownership.

## 4. Reference ownership model

Use a native reverse-reference set per object handle, keyed by `(tileX, tileY, group)`. A reference count may be cached, but the set is the source of truth during development and debug validation.

Required invariants:
1. Every nonzero tile metadata link maps to exactly one live object handle.
2. Every reverse-reference entry maps back to a tile group containing that handle's representable legacy ID.
3. A record's stored coordinates/group must match the canonical link identity for operations that require identity validation.
4. An object cannot be released while its reverse-reference set is non-empty.
5. Unlinking one group cannot affect other groups referencing the same record.
6. Checkpoint save/restore includes object records, tile metadata, reverse-reference state (or deterministically rebuilds it), and generation counters.
7. Failed operations do not leave half-updated tile metadata or ownership state.

Because the source permits a record to be referenced by multiple tile groups, a single owner coordinate stored in the record is not enough to reconstruct all references. The native reverse-reference set must account for every tile group that links to the handle.

## 5. Legacy compatibility boundaries

- Preserve the original 15-slot object table as a compatibility constraint until bytecode/runtime evidence says otherwise.
- Do not conflate the 15-slot object table with the three-bit tile metadata ID.
- Preserve separate object and auxiliary tables (`aLongArr` and `bLongArr`).
- Keep tile-group unlink separate from object release.
- Keep controller-state values separate from object type values.
- Keep the legacy Type-5 allocator's full-table out-of-bounds behavior out of the native implementation; return a safe allocation failure.
- Do not assume all legacy helper call sites perform the same validation.

## 6. Required tests before integrating this API

1. Empty tile ID 0 returns `NO_LINK` and never indexes slot zero.
2. Valid tile ID 1..7 resolves only to the matching live record.
3. Tile ID outside 0..7 is rejected before array access.
4. Object slot 1..15 can be looked up directly, independently of tile-link representability.
5. Attempting to link object slot 8..15 returns `UNREPRESENTABLE_TILE_ID` until the source representation discrepancy is resolved.
6. Stale generation handle cannot access a reused slot.
7. Coordinate mismatch and group mismatch fail validated lookup.
8. Unlinking one of multiple tile references leaves the remaining references and record intact.
9. Release fails while any tile reference remains.
10. Release succeeds after all references are unlinked and invalidates the old handle.
11. Reclassification preserves handle and reverse references.
12. Failed relocation leaves all previous links intact.
13. Full 15-slot allocation failure is atomic and does not write outside the table.
14. Checkpoint round-trip restores tile metadata, object records, reverse references, and generations consistently.

## 7. Open questions and next milestone

The 15-slot versus three-bit tile-ID mismatch is now a blocking reverse-engineering question. Before implementing the legacy-compatible link path, inspect all writers/readers, the original JAR bytecode, and runtime behavior to determine whether IDs 8..15 can actually be produced on reachable paths and whether the metadata encoding has an additional indirection not visible in the current decompiled source.

### Next: DEV-003BI — Packed-ID Encoding Discrepancy Investigation & Native Contract Tests

First resolve the slot-width mismatch and document reachable ID ranges. Then implement only a standalone test harness for the safe API contract; do not wire it into gameplay until the representation is settled.
