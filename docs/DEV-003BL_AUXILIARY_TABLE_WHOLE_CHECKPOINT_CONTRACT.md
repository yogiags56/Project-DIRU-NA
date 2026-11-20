# DEV-003BL — Auxiliary Table & Whole-Checkpoint Lifecycle Contract

## Scope and source provenance

Static audit of `kubikaugustyn/DiamondRushSource/src/main/java/i.java`, pinned to `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This milestone audits the auxiliary `bLongArr` table and extends the isolated Java checkpoint model to preserve its opaque payload. It does not integrate the model into gameplay and does not claim bytecode-level or runtime parity with the original shipping JAR.

## 1. Source-level findings

### Table shape and storage

- `bLongArr` is declared as a static `long[]`.
- Stage/object subsystem initialization allocates it as `new long[15]` (around line 15844).
- The broader class initialization also allocates a 15-entry `bLongArr` (around line 17753).
- The subsystem teardown path nulls it (around line 4910).
- The packed reader and writer access an entry by zero-based index and extract/replace caller-specified bit ranges (around lines 15909–15914).

The table is therefore a separate 15-entry packed 64-bit structure. Its entries must not be confused with the 15-entry `aLongArr` object-record table, even though both tables have the same capacity.

### Observed use

- Controller state 4 scans all 15 entries. For entries whose low four-bit field equals 3, it reads X, Y and direction fields, clears the auxiliary entry, advances/derives coordinates and creates or attaches a Type-7 object record (around lines 16455–16515).
- A coordinate lookup helper scans the auxiliary table by stored X/Y fields (around line 15917 onward).
- Other call sites use that lookup to change auxiliary entry state and then continue controller processing (around lines 16605 and 16801–16810).
- The complete semantic inventory of every bit field and all state transitions is not yet established. This document intentionally treats each entry as opaque 64-bit data.

### Checkpoint coupling

The source's checkpoint save path copies:
- tile metadata grid `eIntArrArr` into its checkpoint grid;
- `aLongArr` into `cLongArr`;
- `bLongArr` into `dLongArr`.

The restore path copies the checkpoint grid back and restores:
- `cLongArr` into `aLongArr`;
- `dLongArr` into `bLongArr`.

Both arrays are copied at their full 15-entry lengths in the same checkpoint branch (around lines 15220–15231 and 15348–15355). This is direct evidence that auxiliary state is part of the same checkpoint snapshot as object records and tile metadata.

## 2. Native model changes

Updated:
- `core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/ObjectRecordTable.java`
- `core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/ObjectRecordTableTest.java`

The immutable snapshot now includes an opaque 15-entry `long[]` auxiliary payload representing the legacy `bLongArr` table.

New API:
- `auxiliaryEntryRaw(index)`
- `setAuxiliaryEntryRaw(index, value)`

The API bounds-checks indices 0..14 and does not interpret or modify packed fields. Snapshot construction and access use defensive copies.

Restore validates the auxiliary array is present and exactly 15 entries long. It restores the object records, generation history, tile links, rebuilt reverse references and auxiliary payload from a temporary validated candidate. If validation fails, the active table and auxiliary payload remain unchanged.

The legacy four-argument Snapshot constructor remains available for existing tests/callers and supplies a zero-filled auxiliary table. New checkpoint producers should use the full constructor or `snapshot()` so actual auxiliary values are retained.

## 3. Added test coverage

Three additional JUnit tests:
1. round-trip all 15 auxiliary entries bit-for-bit, including zero, all-ones, sign-bit and mixed patterns;
2. verify defensive copies and reject negative/out-of-range auxiliary indices;
3. reject a snapshot with the wrong auxiliary array length without changing live auxiliary state.

This brings the isolated test inventory to 17 test methods, pending CI completion.

## 4. Whole-checkpoint invariant

The safe model now treats the following as one atomic snapshot boundary:
- object record slots and types;
- object handle generation history;
- tile-group links;
- derived reverse-reference ownership (rebuilt on restore);
- 15 opaque auxiliary entries.

The original source also snapshots the tile metadata grid and multiple controller/player fields. Those are not yet represented in this isolated object-record harness. Therefore this is a whole object/auxiliary subsystem checkpoint contract, not yet a complete game-state checkpoint implementation.

## 5. Validation status and limitations

- Source audit is static and based on the pinned decompiled candidate source.
- No original shipping-JAR bytecode comparison or runtime trace was performed.
- No native NDK/C++ implementation or gameplay integration was made.
- No CI pass is claimed until the latest workflow run reports success.
- Auxiliary field semantics remain opaque; do not infer business meaning from bit offsets without tracing all readers/writers.
- Snapshot schema migration remains unimplemented; schema version 1 is the only accepted version.

## 6. Next milestone: DEV-003BM — Auxiliary Field Semantics & Cross-Table Transition Matrix

Trace every auxiliary-table reader/writer and document each bit range, transition, and caller. In particular:
- identify all creation paths for nonzero auxiliary entries;
- enumerate all low-four-bit state values and transitions;
- map the X/Y/direction fields and their coordinate bounds;
- establish which transitions create, modify or clear `aLongArr` object records;
- determine whether auxiliary entries can reference object IDs or only coordinates/state;
- compare checkpoint save/restore with stage initialization and teardown.

Only after this matrix is evidence-complete should the native model gain typed auxiliary records instead of opaque raw entries.
