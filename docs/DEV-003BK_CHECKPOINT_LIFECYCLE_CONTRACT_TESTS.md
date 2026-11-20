# DEV-003BK — Checkpoint & Lifecycle Contract Tests

## Scope

Extend the isolated object-record safety model with immutable checkpoint snapshots and atomic restore. This remains a platform-independent Java 17 harness in the `core` module; it is not connected to gameplay and does not claim original-JAR parity.

## Implementation

Updated:
- `core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/ObjectRecordTable.java`
- `core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/ObjectRecordTableTest.java`

### Snapshot contents

The snapshot captures:
- schema version;
- every occupied record's one-based slot ID, generation, type and canonical anchor;
- the full 15-entry generation array, including empty slots;
- tile-group-to-slot links.

Reverse-reference sets are intentionally not serialized. They are derived state and are rebuilt from the authoritative tile-link map during restore. This avoids persisting two independently mutable representations of the same ownership relation.

### Restore behavior

Restore constructs and validates a temporary candidate table before changing the live table. It rejects:
- null or unsupported schema;
- missing or incorrectly sized generation array;
- non-positive generations;
- invalid or duplicate slot IDs;
- invalid record types, coordinates, groups, or generation mismatches;
- tile links outside the representable range 1..7;
- links to unoccupied slots.

Only after full validation does restore replace the live slots, generation array and tile-link map. Invalid restore returns `INVALID_SNAPSHOT` and leaves the existing live state unchanged.

## Added tests

Five additional JUnit tests cover:
1. checkpoint round-trip for multiple records and shared tile references;
2. reverse-reference rebuilding after a prior unlink;
3. atomic rejection of a record-generation mismatch;
4. preservation of generation history across release, checkpoint and slot reuse;
5. rejection of corrupted tile links to missing/unrepresentable slots without damaging current state.

The original nine contract tests remain in place.

## Validation status

GitHub Actions was triggered by the implementation and test commits. The latest test workflow status must be checked before treating the test suite as passing. This document does not claim a local Gradle run or original-device/JAR validation.

## Known boundaries

- No serialization to a persistent file format has been integrated; this milestone tests the in-memory immutable snapshot model.
- Snapshot schema migration is not implemented; only schema version 1 is accepted.
- The model does not yet snapshot the auxiliary `bLongArr` table or all game checkpoint state.
- Relocation and full legacy packed-field serialization remain outside this milestone.
- The 15-slot/three-bit mismatch remains unresolved against the original shipping JAR.
- No gameplay integration or Android runtime parity is claimed.

## Next: DEV-003BL — Auxiliary Table & Whole-Checkpoint Lifecycle Contract

Audit how `bLongArr` participates in save/restore and stage transitions, then extend the isolated checkpoint model to include auxiliary records only after their fields and ownership semantics are proven. Add invariants for atomic whole-checkpoint restore and cross-table consistency. Keep original-JAR comparison as a separate required validation track.
