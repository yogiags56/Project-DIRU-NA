# DEV-003BU — Explicit Replacement & Type-7 Lifecycle Semantics

## Scope

This milestone implements two source-specific lifecycle operations in ObjectRecordTable:
1. explicit replacement of an existing target record with Type-2 while transferring the source record's packed auxiliary link;
2. a dedicated Type-7 allocation path that creates a fresh linkless payload.

The model remains platform-independent and is not wired into gameplay.

## Source basis

Pinned source:
- `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`

Relevant source paths identified in DEV-003BT:
- around lines 16227–16254: Type-1/Type-6 replacement path copies bits 54..56 from the old record to the existing target record, changes target type bits to Type-2, then calls whole-record release on the old record;
- around lines 16725–16737: Type-7 is a separate allocator branch, after clearing the selected raw word, and does not write bits 54..56;
- around line 16659: whole-record release clears the complete `aLongArr` word.

## Implementation

### `replaceAndTransferLink(sourceHandle, targetHandle, targetType)`

The API is deliberately constrained to the observed transition:
- source record must be Type-1 or Type-6;
- target type must be Type-2;
- source and target must be distinct live handles;
- Type-7 source is rejected because the source branch releases it without transferring a link. The resolved target may itself currently be Type-7: the legacy code writes the transferred link and overwrites its type field with Type-2 without first checking the target's prior type.
- bits 54..56 are copied from source to target;
- target type bits 28..30 are set to Type-2;
- source is released only after the target has been updated;
- the operation snapshots state first and restores it if source release fails, for example because the source still has tile references.

This models the explicit source transfer operation rather than using generic in-place reclassification. It does not choose the target object ID from tile metadata; the caller supplies the already-resolved target handle.

### Dedicated Type-7 allocation

- Generic `allocate(RecordType.TYPE_7, ...)` now returns `INVALID_TRANSITION`.
- `allocateType7(...)` is the explicit creation entry point.
- Type-7 records cannot be used as source or target in generic reclassification/replacement.
- Snapshot validation rejects Type-7 records whose raw three-bit auxiliary-link field is nonzero.
- Type-7 semantic reads continue to return the explicit linkless status.

The model does not infer that a zero raw field is a real auxiliary association. Type-7 remains linkless.

## Tests

Five new focused test methods were added:
1. successful Type-6 -> existing Type-2 target link transfer and source release;
2. atomic rollback if the source still has references;
3. rejection of Type-7 and self replacement;
4. dedicated Type-7 allocation and reclassification rejection;
5. replacement/checkpoint/restore continuity.

Two additional invariant tests:
6. generic allocator rejects Type-7 while dedicated allocator succeeds;
7. restore rejects a Type-7 record snapshot with nonzero link bits without changing live state.

The focused object-record suite is now 59 test methods by source count (51 previously plus eight). This count does not substitute for an executed test result.

## Known boundaries

- The target record is supplied as an existing handle. The native model does not yet resolve target IDs from the tile metadata grid exactly as the game does.
- The replacement API currently models the audited Type-1/Type-6 -> Type-2 transfer branch only.
- The model does not claim the original allocator's full 15-slot selection or overflow behavior.
- The pinned decompiled source is not the original shipping JAR; binary parity and runtime traces remain open.
- Gameplay integration remains blocked.

## CI status

The workflows triggered by this milestone must be checked after completion. No pass is claimed in this document until the GitHub Actions run confirms both core JVM tests and Android debug APK assembly.

## Commits

- Explicit replacement and initial Type-7 lifecycle: `8b57592db3def6d54cc8f2c142508a3394a33566`
- Replacement/Type-7 tests: `72b3a7f35943fcbb411cdb64010a11097fb12967`
- Dedicated Type-7 allocator enforcement: `03a16451b4dbf94a16e7d62bbdad68820c8f579b`
- Restrict replacement to audited source transition: `e777feb57e95d22347613a6050d9e871999ccacc`
- Type-7 snapshot invariant tests: `45501324ed62058bbc7f84e28b9b4f5f9af46193`

## Next milestone — DEV-003BV: Tile-Metadata Target Resolution & Replacement Transaction

1. Audit how the source obtains the target object ID from tile metadata immediately before replacement.
2. Add a guarded table operation that resolves the target handle from the exact tile group and performs link transfer + source release atomically.
3. Preserve all target references and ensure source references are cleared/absent before commit.
4. Test missing target metadata, stale target, same source/target, occupied tile groups, and rollback.
5. Check CI for every DEV-003BU commit.
6. Keep runtime/gameplay integration blocked pending original-JAR provenance and parity evidence.

## CI correction and latest validation\n\nAn early DEV-003BU workflow exposed one outdated Type-7 test still calling the generic allocator after the dedicated Type-7 entry point was enforced. The failing test produced a null-handle NPE; this was a test fixture mismatch, not a production-code runtime failure. The fixture was corrected to call \`allocateType7()\`.\n\nA subsequent source review also refined replacement semantics: the source branch rejects Type-7 as the record being released, but it does not check the resolved target's prior type before writing Type-2. The native replacement API therefore permits a Type-7 target to be overwritten as Type-2. This correction and its regression test are included in commits \`74e475603b643f0b23fe460852feade70a92bd38\` and \`5d731e1f2a7ac020f8469810981eba8e80723892\`.\n\nThe corrected workflow for the current HEAD must complete before CI is claimed.\n\nNo shipping-JAR parity or gameplay integration is claimed here.
