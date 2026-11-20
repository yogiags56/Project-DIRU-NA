# DEV-003BJ — Isolated Object-Record Contract Harness

## Scope

This milestone adds a platform-independent Java safety model and JUnit 4 tests under the existing `core` module. It does not integrate the object-record model into gameplay or change Android UI/rendering code.

## Files added

- `core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/ObjectRecordTable.java`
- `core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/ObjectRecordTableTest.java`

## Implemented contract

- 15 one-based object slots are represented independently from the three-bit tile link range 1..7.
- IDs 0 and missing tile links return `NO_LINK`.
- Slots 8..15 can be allocated as object records but cannot be published as tile links; linking returns `UNREPRESENTABLE_TILE_ID` without changing tile or reverse-reference state.
- Native handles carry a generation value to reject stale handles after slot reuse.
- Tile links and reverse-reference sets are updated under synchronized methods.
- Multiple tile groups can reference one object record.
- Unlink removes only the selected tile-group reference.
- Release is rejected while any reference remains.
- Reclassification preserves the handle and reference set.
- A canonical anchor check is exposed separately from shared tile-link lookup. This is deliberate: a record's stored anchor coordinate cannot be required to equal every tile coordinate when the source behavior permits one record to span multiple tiles.

## Test coverage added

1. Allocate all 15 slots and reject the 16th allocation.
2. Link slot 7 and reject slot 8 without partial mutation.
3. Empty/missing tile link behavior.
4. Shared object references across multiple tile groups.
5. Generation-tagged stale-handle rejection after slot reuse.
6. Reclassification without losing links.
7. Separate canonical-anchor validation from shared-link resolution.
8. Duplicate link idempotency and occupied tile-group rejection.
9. Release only after every tile link is removed.

## Important limitations

- The harness models the proposed safe native contract; it does not claim original-game parity.
- It is written in Java 17 within the current platform-independent `core` module. It is not yet a C/C++ NDK implementation.
- It does not yet model the complete legacy packed bit layout, auxiliary `bLongArr` table, checkpoint serialization, object relocation, or all object types' individual lifecycle semantics.
- The source-level 15-slot/three-bit discrepancy remains unresolved against the original shipping JAR.
- Gameplay integration is intentionally deferred.
- GitHub Actions CI was triggered by the code and test commits. At the time of this document, the newest workflow run is still in progress; no passing result is claimed until GitHub reports completion.

## Next milestone: DEV-003BK — Checkpoint & Lifecycle Contract Tests

Extend the isolated model with snapshot/restore semantics and invariants for records, tile links, reverse references, and handle generations. Add tests for checkpoint round trips, restore after unlink/release, and corrupted or inconsistent snapshots. Keep gameplay integration blocked until the legacy encoding question and original-JAR validation are resolved.
