# DEV-003CB — Object-ID Reachability & Original-JAR Evidence Audit

## Scope and artifact availability

This milestone traces the three-bit object ID stored in each nine-bit tile metadata group, the 15-entry object record table, and the three-bit auxiliary link stored in object records.

Pinned source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

The Project-DIRU-NA repository tree contains no original Diamond Rush shipping JAR, game class files, or game APK. Only the Gradle wrapper JAR is present. Therefore original-JAR bytecode parity cannot be performed from the current repository state. All conclusions below are static observations from the pinned decompiled Java source.

## 1. Tile metadata object-ID write path

The generic tile metadata writer at approximately lines 15886–15898 uses a 9-bit group stride and masks each field to its declared width.

At the Type-5 traversal creation path around lines 16573–16577:
- the six-argument object allocator returns a one-based object ID;
- the code writes that returned ID into the selected tile group's field at offset 0, width 3;
- the same returned ID is used for object-record field updates.

The field width is three bits. Values 8..15 cannot be represented and are truncated by the generic writer's field mask. This creates a source-level aliasing risk: object ID 8 encodes as 0, ID 9 as 1, through ID 15 as 7.

This is not evidence that all 15 IDs are reachable through tile metadata. It is direct evidence that the allocator's declared search range is wider than the tile ID field.

## 2. Tile metadata object-ID read path

At approximately lines 16222–16253, the Type-1/6/7 replacement path:
- resolves coordinates and group from the source object record;
- reads the target tile's object ID from field offset 0, width 3;
- uses that ID to address the target object record;
- copies the source record's auxiliary-link bits 54..56 into the target record.

The three-bit tile object ID is therefore a real object-record lookup key in this path, not merely a display or collision attribute.

## 3. Object-table traversal beyond tile lookup

At approximately lines 16065–16090, controller state 2 iterates object IDs 1..15 and reads each corresponding `aLongArr` record directly. For Type-2 records it reads the packed three-bit auxiliary link at bits 54..56, then uses that value to read the auxiliary table.

This proves that object IDs 8..15 can be considered by the global object-table traversal if such records exist. It does not prove that a tile metadata reference to those IDs can be resolved correctly.

## 4. Auxiliary link read is a separate namespace

At approximately line 16090 the source extracts object-record bits 54..56 into `b21`, then reads `bLongArr[b21]` through the packed auxiliary accessor at offset 6, width 7.

Consequently:
- tile metadata bits 0..2 identify an object record (one-based IDs are written by the allocator, but only 0..7 are representable);
- object record bits 54..56 identify an auxiliary entry (zero-based indices 0..7 are directly addressable);
- the object-record allocator's 1..15 slot index is not the auxiliary-table index.

The auxiliary table itself has 15 entries, but this particular link field can only select entries 0..7. No alternate indexed access to auxiliary entries 8..14 was established by this traced reader.

## 5. Existing native safety contract and tests

The native `ObjectRecordTable` deliberately separates:
- `OBJECT_SLOT_CAPACITY = 15` for object records;
- `TILE_RECORD_ID_MAX = 7` for the representable tile metadata ID.

The model can allocate all 15 object slots, but `link()` rejects object slots 8..15 with `UNREPRESENTABLE_TILE_ID` instead of silently truncating and aliasing. Existing tests already cover:
- allocation of all 15 independent object slots;
- successful tile linking for slots 1..7;
- rejection of slot 8 as unrepresentable.

`LegacyObjectRecordLinkCodec` separately permits auxiliary link values 0..7 and rejects 8..14 in its safe encoder. Legacy truncation is exposed as a separate explicit compatibility operation and is not used by the safe native path.

No production code change was needed in this milestone: the current native contract already prevents the identified aliasing hazard.

## 6. Conclusions and unresolved questions

Established from pinned decompiled source:
1. The object allocator scans aLongArr IDs 1..15.
2. The tile metadata object-ID field is three bits wide.
3. The Type-5 creation path writes the allocator's returned ID directly into that three-bit field.
4. The replacement path reads that three-bit field and uses it as an object-record ID.
5. Global object traversal scans IDs 1..15 independently of tile metadata.
6. Object-record bits 54..56 are used as an index into bLongArr, distinct from tile metadata object IDs.

Not established:
- whether the original shipping game ever reaches object IDs 8..15 in the six-argument allocator;
- whether another invariant limits simultaneous tile-addressable objects to seven;
- whether zero tile ID is a sentinel, a valid object ID, or an artifact of truncation in this path;
- whether the decompiled source exactly matches the shipped JAR bytecode;
- whether any alternate auxiliary reader accesses bLongArr entries 8..14.

Do not change the native safe rejection policy to reproduce legacy truncation until original JAR bytecode or runtime evidence resolves these questions.

## 7. Next milestone — DEV-003CC: Object-ID Zero Semantics & Safe Legacy Compatibility

1. Trace every path that writes or clears tile metadata object-ID bits, including whether zero is explicitly written as an empty sentinel.
2. Trace every path that reads tile ID zero and determine whether it can ever reach `aLongArr[-1]` or is guarded first.
3. Compare Type-5 creation, Type-1/6/7 replacement, and Type-7 creation paths for the zero-ID contract.
4. Add focused native tests for empty tile groups, valid IDs 1..7, and rejected IDs 8..15 without aliasing.
5. If the original shipping JAR becomes available, inspect allocator and field-write bytecode before defining exact legacy-compatibility behavior.
6. Keep runtime parity and gameplay integration blocked until the above evidence is sufficient.

## Validation

DEV-003CA documentation workflow completed successfully (`44784313511`). This DEV-003CB milestone is static source audit and documentation only; no production code, APK, or runtime behavior was changed or tested.