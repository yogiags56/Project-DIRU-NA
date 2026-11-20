# DEV-003BW — Coordinator-Level Cross-Table Replacement

## Scope

Added a coordinator-level replacement operation that resolves the target from exact tile metadata and transfers the source object's auxiliary association to that target. The object-record packed link and coordinator sidecar association are updated as one synchronized coordinator operation.

## API

`CrossTableLifecycleCoordinator.replaceFromTileMetadata(sourceHandle, targetTile)`

Preconditions and guards:
- source handle must be live and present in the coordinator association map;
- source packed bits 54..56 must match its sidecar slot;
- source auxiliary entry must be in allocated (1) or ready (3) state;
- target must resolve from the exact TileGroup key;
- source and target must be different handles;
- target must not already own an independent coordinator association;
- a non-Type-7 target with a different packed auxiliary slot is rejected if that other slot contains a non-empty auxiliary entry.

Successful operation:
1. delegates exact tile metadata lookup and object replacement to ObjectRecordTable;
2. transfers the packed three-bit link to the target and writes Type-2;
3. releases the source record (the source must have no remaining tile references);
4. removes the old source-handle association and assigns the same auxiliary slot to the target handle.

The coordinator is synchronized for this operation. ObjectRecordTable's replacement method snapshots and rolls back its own state if source release fails. The sidecar map is mutated only after the object-table replacement succeeds; there are no fallible steps after that point.

## Tests

Five coordinator-level tests added:
1. successful replacement from a source Type-6 record to a metadata-resolved Type-7 target, followed by mark-ready and consume using the transferred association;
2. rejection when target already has an independent association;
3. rollback when source retains another tile reference;
4. missing target and source packed-link/sidecar mismatch rejection;
5. checkpoint/restore after replacement, then continuation through mark-ready and consume.

Test count is based on source inventory only; CI is the authority for pass/fail.

## Known boundaries and safety policy

- Source must already be unlinked from all tile metadata references before replacement. This is required by the model's whole-record release rule and the source's release ordering.
- The exact target group is represented by TileGroup. Original packed `eIntArrArr` bit decoding and coordinate transforms remain to be audited.
- The coordinator's auxiliary entries remain stored in ObjectRecordTable as opaque `bLongArr`-like words; the new operation transfers ownership/association but does not rewrite the auxiliary entry's packed payload.
- Target with a distinct active auxiliary entry is rejected rather than silently destroying that entry.
- The target may be Type-7 because the pinned source replacement path overwrites the resolved target type with Type-2; Type-7 is not allowed as the source record for link transfer.
- No gameplay integration, original shipping-JAR parity, or runtime trace is claimed.

## Commits

- Coordinator cross-table replacement implementation: `47c7cc8b8a6f90bbd39547abf0db5b96b30cf61d`
- Coordinator regression tests: `51f8aa1aa11a8cec69111172f44d5f049be54609`

## Next milestone — DEV-003BX: Auxiliary Entry Payload Transfer & Packed Metadata Audit

1. Revisit source call ordering around target tile ID read, metadata unlink/write, auxiliary entry state, and old record release.
2. Establish whether the auxiliary `bLongArr` word is preserved, mutated, or consumed during each replacement branch.
3. Audit sidecar ownership against source semantics; decide whether coordinator association transfer should always occur or only for selected source states/types.
4. Add tests for target's existing raw link pointing to empty, same, or active auxiliary slots, plus source state 1 vs state 3.
5. Validate coordinator restore against dangling and duplicate auxiliary associations.
6. Run CI for this milestone before proceeding.

No successful CI result, APK artifact, runtime parity, or gameplay integration is claimed in this document.
