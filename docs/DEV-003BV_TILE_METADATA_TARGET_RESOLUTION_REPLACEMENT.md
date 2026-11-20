# DEV-003BV — Tile-Metadata Target Resolution & Replacement Transaction

## Scope

Added a metadata-resolved replacement entry point to the native ObjectRecordTable model. Instead of requiring callers to supply a target ObjectHandle, the operation accepts the exact TileGroup metadata key, resolves its current object handle through the tile-link table, and delegates to the audited replacement transfer.

## Source basis

Pinned decompiled source:
- Repository: `kubikaugustyn/DiamondRushSource`
- File: `src/main/java/i.java`
- Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

The Type-1/Type-6 -> Type-2 replacement path in the controller reads the target object ID from tile metadata at the computed coordinate and group, copies bits 54..56 from the old record to that target record, writes Type-2 and other target fields, and then releases the old record. The model now represents target resolution through the exact tile group key.

## Implementation

New API:
`replaceFromTileMetadata(ObjectHandle sourceHandle, TileGroup targetTile)`

Behavior:
1. Reject null source or target metadata.
2. Resolve the source handle and reject stale sources.
3. Resolve the target record using the exact TileGroup key, including x, y, and group.
4. Return the lookup failure unchanged if that exact group has no valid object link.
5. Reject source == target.
6. Delegate to `replaceAndTransferLink(source, resolvedTarget, TYPE_2)`.
7. The delegated transaction transfers bits 54..56, changes target type to Type-2, and releases the source. If source release fails due to remaining tile references, the pre-operation object-table snapshot is restored.

The target handle is no longer caller-supplied. A same-coordinate tile in another group is not accepted as a fallback.

## Tests

Four focused tests were added:
1. exact tile-group resolution selects the correct target even when another group at the same coordinates points to a different record;
2. missing target group returns NO_LINK and does not fall back to another group;
3. source still referenced causes rollback of target payload/type/link and preserves both references;
4. self-target and stale-source paths are rejected.

The ObjectRecordTable test source now contains 63 test methods by inventory count (59 previous plus four). This is not itself a claim that all tests passed.

## Known boundaries

- The model's TileGroup map represents a resolved tile metadata ID but does not yet model the original packed `eIntArrArr` bit layout or every coordinate transformation.
- The source caller calculates target x/y and group before reading the tile metadata ID. Native parity for those calculations remains a separate task.
- This operation is not yet exposed through CrossTableLifecycleCoordinator; auxiliary entry lifecycle and association-map transfer during replacement need a dedicated integration pass.
- The decompiled source is not the original shipping JAR. No bytecode parity or runtime trace is claimed.
- No gameplay integration is performed.

## CI

DEV-003BV workflows triggered by the implementation and test commits must complete before this milestone is declared validated. No CI pass is claimed in this document until both core JVM tests and Android debug APK assembly succeed.

## Commits

- Metadata-resolved replacement API: `8e94e371593768a67fd19231359162340a21723a`
- Exact-group and rollback tests: `df6111cbe5ca8144eb48c240b70eafc10239619c`

## Next milestone — DEV-003BW: Coordinator-Level Cross-Table Replacement

1. Define what happens to the coordinator's sidecar association when the object handle is replaced/released.
2. Transfer association ownership to the target handle while keeping packed link and auxiliary slot consistent.
3. Ensure source tile references and target tile metadata are handled in the correct order.
4. Make object table, auxiliary entry table, and sidecar association update atomically.
5. Add coordinator tests for successful replacement, missing metadata, mismatched packed link, source still referenced, and checkpoint/restore.
6. Confirm CI and retain gameplay integration as blocked pending stronger source/JAR evidence.

No successful workflow, APK artifact, runtime parity, or gameplay integration is claimed here.
