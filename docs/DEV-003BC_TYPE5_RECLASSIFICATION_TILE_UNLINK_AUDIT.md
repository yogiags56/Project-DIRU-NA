# DIRU-NA-DEV-003BC — Type-5 Reclassification & Tile-Unlink Audit

**Status:** Static write-site and metadata-unlink audit completed; full semantic lifetime remains partially unresolved  
**Parent:** DEV-003BB  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope and evidence boundary

This stage enumerates writes to the three-bit object type field at bits 28..30, distinguishes controller state from object type, and isolates calls to the static tile-metadata clearing helper `cVoid(int x, int y, byte group)`.

The evidence is the pinned decompiled candidate source `src/main/java/i.java`. It is not a shipping-JAR bytecode comparison or runtime trace.

## 2. Object-type field write inventory

The full source search found the following explicit writes to bits 28..30:

| Approx. line | Write | Context / meaning |
|---|---:|---|
| 16142 | type 3 | Existing type-2 record transitions into type 3 in the object update state machine |
| 16145 | type 5 | Same type-2 processing branch; conditional path changes the record to type 5 when the linked metadata/state condition is met |
| 16254 | type 2 | Existing type-1/type-6/type-7 branch creates/initializes a destination record as type 2 during a transfer path |
| 16356 | type 2 | Type-1/type-6/type-7 branch changes the current record to type 2 during a transition |
| 16573 | type 5 | Type-5 traversal creation path allocates a record and marks it type 5 |
| 16643 | type 6 | Directional movement helper creates a type-6 record |
| 16733 | type 7 | Six-argument allocator mode 7 creates a type-7 record after clearing the target tile metadata group |
| 16735 | type 1 | General six-argument allocator path creates a type-1 record |

The object-type scan also reads the field in the update switch (around line 16075). The switch explicitly handles types 1, 2, 3, 6, and 7; no explicit type-5 case is present in that switch.

### Key finding: Type-5 has more than one origin

Type-5 records are not exclusively born in the traversal creation path. The update path for a type-2 record can also change the existing record's type bits to 5 (around line 16145).

This corrects the simplified model in earlier stages that treated the Type-5 allocator creation site as the only source of Type-5 records.

The two observed origins are:
1. New record allocation in the Type-5 traversal branch.
2. Reclassification of an existing type-2 record in the object update state machine.

The second origin does not allocate a new slot, so capacity accounting must distinguish new Type-5 allocations from type-2-to-type-5 transitions.

## 3. Object-type transitions and release

### 3.1 Type-2 to type-5 transition

In the type-2 update branch (around lines 16135–16150), the source first sets the current record type to 3. Under the branch condition where the relevant state value `b20 == 0`, it then changes that same record's type to 5. If the controller's `dqInt` state is -1, it also sets `dzInt = -1` and `xByte = 4`.

This is an in-place type transition. It is not a new allocation and it does not create a new record ID.

### 3.2 Type-5 update handling

The object-table scan's switch has explicit cases for types 3, 2, 1, 6, and 7. It has no explicit case 5. Therefore the inspected update loop does not directly update Type-5 records in the same way as the other listed types.

The absence of a case is not proof that the record is invalid or unreachable; it may be a deliberately persistent record state or be consumed through tile metadata/helper paths.

### 3.3 Release evidence

The four direct calls to the static whole-record helper remain:
- controller completion path freeing `wByte`;
- object-table scan freeing type-3 records;
- type-7 interaction completion path freeing the current record;
- transfer path freeing the old current record after destination initialization.

No explicit branch was found that says “if record type == 5, call fVoid(id)”. Because type-5 can be reached by reclassification from type 2, its lifetime cannot be understood only by examining the allocation branch.

No conclusion of a memory leak is made. The candidate source may intentionally keep type-5 records alive until a broader reset, or another path may alter/consume them.

## 4. Tile metadata unlink helper: exact call sites

The static helper `cVoid(int x, int y, byte group)` clears exactly one nine-bit group from `eIntArrArr[x][y]` and sets `cByteArrArr[x][y] = 24`. It does not clear `aLongArr`.

The source has the following direct calls to this exact helper within the object/metadata implementation:

| Approx. line | Caller context | Effect |
|---|---|---|
| 15971 | Periodic update of a directional object record | Clears a selected tile metadata group while processing a span |
| 16276 | Object movement/interaction path | Clears the current metadata group when the branch chooses tile cleanup |
| 16674 | Six-argument allocator mode 7 | Clears target tile group before creating a type-7 record |
| 16732 | Allocator's type-7 branch | Clears target tile group before setting the new record type to 7 |
| 16776 | Metadata lookup helper, subtype 6 | Clears the requested tile metadata group when the referenced metadata does not match the expected record coordinates/group |

These are direct static-helper call sites. Other methods named `cVoid` are overloaded instance methods for unrelated game logic and are not counted as calls to the packed tile-metadata helper.

### Important limit

The helper itself does not know or inspect the referenced record ID before clearing a group. It cannot by itself prove that the corresponding object record is now unreferenced. A native implementation must not equate a tile-group unlink with whole-record reclamation.

## 5. Reference-lifecycle graph (source-derived, incomplete)

```text
Type-2 record
   |
   | conditional state transition in update branch
   v
Type-5 record (same ID; no allocation)
   |
   | no explicit type-5 case in object-table update switch
   |
   +---- broader lifecycle / tile metadata consumers (not fully resolved)
   |
   v
stage/feature table reset or teardown (confirmed whole-table boundary)

Type-5 traversal branch
   |
   | allocate new record, mark type 5
   v
Type-5 record
   |
   +---- one or more tile metadata IDs point to same record
   |
   +---- tile metadata groups may be cleared by cVoid(...)
   |       (does not free record)
   |
   v
stage/feature table reset or teardown (confirmed whole-table boundary)
```

This graph intentionally does not invent a per-record terminal edge that has not been proven.

## 6. Capacity accounting correction

The 15 slots are shared by all object types. A Type-2-to-Type-5 reclassification does not consume an additional slot; it changes the classification of an existing slot.

Therefore:
- Count Type-5 **allocations** separately from Type-5 **reclassifications**.
- Do not estimate total Type-5 occupancy by counting only traversal-branch allocations.
- The unchecked full-table issue identified in DEV-003BA still applies to the six-argument allocation path that creates a new record.
- The type-2-to-type-5 transition does not itself trigger that allocator path.

## 7. Native implementation constraints

1. Keep record identity separate from record type; reclassification must preserve the same one-based ID.
2. Represent type-2-to-type-5 as an in-place type transition, not allocate-and-copy.
3. Keep the object-type enum/state separate from the controller `xByte` state.
4. Keep tile metadata unlink separate from record reclamation.
5. Do not invent a Type-5 terminal/free transition without bytecode/runtime evidence.
6. Track reference counts or reverse-reference sets only as diagnostics/invariants in the native model until original semantics are established; do not assume reference counting is legacy behavior.
7. Capacity tests must include both fresh Type-5 allocation and in-place type-2-to-type-5 reclassification.

## 8. Validation status

- All explicit source writes to object type bits 28..30 were enumerated.
- All direct calls to the exact static packed-metadata `cVoid(int,int,byte)` helper were isolated from overloaded methods of the same name.
- The type-2-to-type-5 in-place transition was identified.
- No production code or tests changed.
- No Gradle test or Android build was run.
- No shipping-JAR parity check or live-runtime trace was performed.

## 9. Next stage — DEV-003BD

1. Trace the type-2-to-type-5 transition's prerequisites and subsequent controller state 4 handling.
2. Determine whether controller state 4 clears, transforms, or retains the same record.
3. Trace all readers of type-5 records outside the object-table switch, especially metadata lookup and traversal helper paths.
4. Check whether all tile metadata references can be removed before stage reset.
5. Reconcile type-5 source/allocator coordinate pairs with the in-place reclassification path.
6. Define the final Type-5 lifetime contract only after those paths are proven.
