# DEV-003BT — Legacy Record Lifecycle & Type-Reclassification Matrix

## Scope and source pin

This milestone audits every direct object-record type-field write found in the pinned decompiled source. It is a source-only audit; no production code or tests were changed.

Source:
- Repository: `kubikaugustyn/DiamondRushSource`
- File: `src/main/java/i.java`
- Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

The packed type field is bits 28..30 of `aLongArr[objectId - 1]`. The auxiliary link field is bits 54..56. The generic packed-field setter is the four-argument `aVoid(int objectId, byte value, byte bitOffset, byte width)` helper around lines 15902–15908.

## Direct type-field write inventory

All direct type-field writes located by scanning the pinned source are listed below.

| Source line | Value written | Context / interpretation |
|---:|---:|---|
| 16142 | 3 | Type-2 controller branch changes the current record to Type-3 during a transition. |
| 16145 | 5 | In the same branch, a subsequent condition changes that same record to Type-5. |
| 16254 | 2 | A replacement record is reclassified to Type-2 after copying the old record's auxiliary link field. |
| 16356 | 2 | Another movement/traversal path reclassifies the current record to Type-2. |
| 16573 | 5 | A newly allocated mode-8 record is reclassified from allocator-initialized Type-1 to Type-5. |
| 16643 | 6 | Controller completion path sets the record to Type-6. |
| 16733 | 7 | Six-argument allocator's Type-7 special branch. |
| 16735 | 1 | Six-argument allocator's non-Type-7 initialization branch. |

No direct write of value 4 was found in this source revision. This is a search result for the pinned source, not proof that Type-4 is impossible in every build or binary.

A separate foreground-layer animation update around line 13233 writes bits 28..31 in `crtStageForegroundLayer`; it is not an `aLongArr` object-record type write and must not be mixed into this matrix.

## Link-field lifecycle observations

### Allocator initialization

At approximately lines 16719–16747, the six-argument allocator:
- scans one-based object IDs 1..15;
- clears the selected `aLongArr` word;
- for non-Type-7 creation, writes Type-1, writes the passed auxiliary index to bits 54..56, and writes coordinate/group metadata;
- for Type-7 creation, writes Type-7 through a separate branch and does not write bits 54..56.

Thus the allocator's initial non-Type-7 type is Type-1, even when a caller subsequently changes it to Type-5 or Type-6.

### Type-2 -> Type-3 -> Type-5 path

Around lines 16131–16145, the Type-2 controller branch reads the three-bit link from bits 54..56, uses it to address the auxiliary table, then writes Type-3 and conditionally Type-5 to the same object ID. Neither type write touches bits 54..56. This is direct evidence that the packed link is retained through this particular Type-2 -> Type-3 -> Type-5 path.

The branch also performs an auxiliary-table state update. That is a separate table operation and must not be mistaken for clearing or rewriting the object's packed link.

### Type-1/Type-6/Type-7 -> Type-2 replacement path

Around lines 16227–16254, the controller handles Type-1, Type-6, and Type-7 records. If the encountered tile condition allows replacement:
- Type-7 takes a special terminal path: the old record is released with `fVoid`, and no replacement Type-2 record is created in that branch.
- For non-Type-7 records, the code allocates/reuses the tile's referenced object ID, copies bits 54..56 from the old record into the replacement record, writes Type-2, and releases the old record.

This is an explicit link transfer, not a generic reclassification. The source copies the link field before releasing the old record.

### Type-5 and Type-6 creation/completion

- Around line 16573, a mode-8 traversal allocation is changed from Type-1 to Type-5. The type setter touches only bits 28..30, so the allocator-written link remains unchanged.
- Around line 16643, a controller branch changes the current record to Type-6. The type setter does not rewrite bits 54..56.
- Type-7 is explicitly created by the special allocator branch and has no newly written link field.

### Release

The whole-record release helper around line 16659 clears `aLongArr[objectId - 1]` to zero. It therefore clears type, link, coordinates, and other packed fields together. This differs from tile metadata unlinking, which does not itself free the record.

## Evidence-backed transition matrix

| Transition / operation | Link-field behavior established by source |
|---|---|
| New non-Type-7 allocation | Writes supplied 3-bit auxiliary index |
| New Type-7 allocation | Special branch; does not write link |
| Type-2 -> Type-3 -> Type-5 | Retains existing link bits |
| Type-1 or Type-6 -> replacement Type-2 | Copies link bits to replacement, then releases old record |
| Type-7 terminal branch | Releases whole record; no replacement link transfer in that branch |
| Type-1 -> Type-5 after mode-8 allocation | Retains allocator-written link |
| Transition to Type-6 | Type write itself leaves link untouched |
| Whole-record release | Clears all packed fields, including link |

The source shows field-local writes: a type change does not inherently clear the link. Link retention is therefore the default effect of the generic setter, while allocation, explicit link copy, and whole-record release have their own semantics.

## Native model implications

1. The raw payload's type field and link field must remain independently addressable.
2. Reclassification that only changes bits 28..30 must preserve bits 54..56.
3. Replacement must copy the link explicitly before old-record release.
4. Type-7 creation must initialize a fresh record with no meaningful link; a stale raw three-bit value must not be treated as an auxiliary association.
5. Whole-record release must clear the complete raw payload and advance the native generation.
6. Do not infer a Type-4 creation path from the enum alone.
7. The existing native `reclassify` API is a simplified field-local operation. It does not yet model the source's full replacement transaction or distinguish Type-7 creation from arbitrary reclassification to Type-7.

## CI status for prior milestone DEV-003BS

All five workflows associated with DEV-003BS completed successfully:
- `44748233962` — raw payload storage;
- `44748279736` — ObjectRecordTable tests;
- `44748328656` — coordinator packed-link integration;
- `44748366859` — coordinator regression tests;
- `44748412495` — documentation commit.

The final workflow's job completed both `Run core JVM tests` and `Assemble Android debug APK`, followed by `Upload debug APK`, all with success. This validates the repository state through the DEV-003BS documentation commit. It does not establish original-JAR behavioral parity.

## Next milestone — DEV-003BU: Explicit Replacement & Type-7 Lifecycle Semantics

1. Add an explicit native operation for replacing an object record while transferring its packed link.
2. Ensure old-record release and replacement are atomic under failure.
3. Model Type-7 creation as a dedicated allocation path with no semantic auxiliary link.
4. Prevent generic Type-7 reclassification from accidentally presenting stale link bits as a valid association.
5. Test Type-2 -> Type-3 -> Type-5 link retention, Type-1/6 -> Type-2 transfer, Type-7 terminal release, and slot reuse/generation behavior.
6. Keep gameplay integration blocked pending shipping-JAR parity and broader runtime evidence.

No source-to-shipping-JAR parity, runtime trace, or gameplay integration is claimed.
