# DIRU-NA-DEV-003BD — Type-5 Controller State 4 & Record Lifetime Investigation

**Status:** State-4 scope and Type-2-to-Type-5 transition clarified; final Type-5 lifetime remains unresolved  
**Parent:** DEV-003BC  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope and terminology

This audit separates three mechanisms that share similar numeric state values:
- `this.xByte`: controller state machine;
- type bits 28..30 in `aLongArr[id - 1]`: object-record classification;
- `bLongArr`: auxiliary 15-entry movement/continuation records.

The source is the pinned decompiled candidate `src/main/java/i.java`. No shipping-JAR bytecode comparison or live runtime trace is claimed.

## 2. Controller state 4 is not a Type-5 record update loop

In `asVoid()`, controller `xByte == 4` (approximately lines 16455–16515) scans 15 entries of `bLongArr`, not `aLongArr`.

For each auxiliary record whose low four-bit field at offset 0 equals 3, the branch:
1. reads coordinates/direction fields from the auxiliary record;
2. clears the auxiliary record word with `i.bLongArr[index] = 0L`;
3. advances the corresponding coordinate according to the encoded direction;
4. calls the six-argument `aByte(..., mode=7, ...)` helper to create/attach the next type-7 object record;
5. after the scan, sets `this.xByte = 2` and returns.

**Conclusion:** controller state 4 processes auxiliary `bLongArr` continuation records and creates/continues type-7 records. It does not directly iterate or free Type-5 records in `aLongArr`.

This also explains why a numeric controller state 4 must not be confused with object type 4 or with a Type-5 terminal state.

## 3. Type-2 to Type-5 reclassification and controller transition

In the object-table scan's type-2 branch (approximately lines 16075–16150), the code processes the current type-2 record and may:
- clear/update tile metadata and neighboring tile state;
- set the record type to 3;
- under the branch condition where `b20 == 0`, immediately set that same record's type to 5;
- if `this.dqInt == -1`, set `this.dzInt = -1` and `this.xByte = 4`.

The record ID is unchanged through this type-bit write. The transition to controller state 4 therefore does not imply the Type-5 record itself is being processed by state 4; the subsequent state-4 pass is over `bLongArr`.

This is a key distinction for native state modeling: the controller can enter state 4 as a consequence of a Type-2 record becoming Type-5, while state 4's immediate work is on the separate auxiliary-record table.

## 4. Type-5 record readers and reference semantics

The audited direct read paths include:

### 4.1 Object-table type dispatch
The `aLongArr` scan reads bits 28..30 for IDs 1..15. Its explicit switch cases are 1, 2, 3, 6, and 7. Type 5 has no explicit case in this object-update switch, so no per-frame Type-5 update branch is established here.

### 4.2 Tile metadata lookup
The helper around lines 16750–16780 reads the three-bit ID from a tile metadata group, then reads the referenced record's:
- source/lookup coordinates at offsets 31 and 37;
- group at offset 43;
- subtype from the tile metadata.

It validates coordinates/group and returns an interaction classification. It does not require that the record's type bits equal 5 in the inspected code. Therefore this is a generic tile-metadata-to-record lookup, not a Type-5-only reader.

### 4.3 Type-1/6/7 object branch
The object-table switch for types 1, 6, and 7 reads coordinates at offsets 31/37 and group at 43, then consults tile metadata. It can transfer state into another record and free the old record. This path must not be assumed to process Type-5 records because its dispatch explicitly excludes type 5.

**Conclusion:** Type-5 records are created/reclassified and can be reached indirectly through tile metadata IDs, but the inspected object-table update loop does not directly process them as a dedicated type. A definitive inventory of every reader across all methods is still incomplete.

## 5. Does tile unlink prove the Type-5 record is dead?

No. The static metadata clear helper `cVoid(x,y,group)` clears one nine-bit tile metadata group and sets the cell-state byte to 24. It does not inspect the record's other references and does not clear the corresponding `aLongArr` word.

The metadata lookup helper can invoke this cleanup when a record's coordinates/group do not match the queried tile. That proves a stale/mismatched tile group can be cleared during lookup; it does not prove the associated record has no other references or should be freed.

The update paths can also clear or rewrite tile groups while processing spans/transfers. None of these individual unlink operations establishes a complete Type-5 reclamation event.

## 6. Revised Type-5 lifetime model

The following is the strongest model supported by the pinned candidate source:

1. A Type-5 record can originate from a fresh allocation in the traversal branch.
2. A Type-5 record can also originate by in-place reclassification of a Type-2 record; the ID is preserved.
3. Tile metadata groups can store IDs that resolve to records in `aLongArr`.
4. Tile metadata unlink clears the group, not the whole object record.
5. The object-table update switch has no explicit Type-5 case.
6. Controller state 4 processes `bLongArr` auxiliary records, not the Type-5 object table.
7. Feature/stage initialization can replace the object table; stage teardown nulls live and snapshot arrays; checkpoint save/restore copies the metadata grid and object table together.

There is still no proven per-record Type-5 terminal/free transition in the inspected paths. It is plausible that some Type-5 records persist until a feature/stage reset, but this remains a hypothesis rather than an established legacy contract.

## 7. Native implementation implications

1. Model controller state and object-record type as independent enums/state fields.
2. Model `aLongArr` and `bLongArr` as separate 15-entry tables with separate codecs and lifecycles.
3. State 4 must not be implemented as a Type-5 record cleanup state; its inspected behavior operates on auxiliary records and creates type-7 records.
4. Preserve Type-2-to-Type-5 as an in-place record-type transition.
5. Keep metadata unlink separate from record reclamation.
6. Do not invent a Type-5 free event. Preserve the record until a proven reset/transition or an explicitly documented native policy is justified by stronger evidence.
7. Add tests for the controller-state/type distinction and paired checkpointing of tile metadata plus both object/auxiliary tables.

## 8. Validation status

- Controller state 4's use of `bLongArr` was inspected.
- Type-2-to-Type-5 transition and its controller-state update were inspected.
- The generic metadata lookup's record fields and cleanup behavior were inspected.
- No production code or tests changed.
- No Gradle tests or Android build were run.
- No original shipping-JAR bytecode comparison or live runtime trace was performed.
- Full cross-file/cross-method Type-5 reader inventory remains open.

## 9. Next stage — DEV-003BE

1. Complete cross-method search of every read/write of `aLongArr`, including indirect helper calls.
2. Identify every method that reads a tile metadata ID and determine whether it checks record type, coordinates, and group.
3. Trace auxiliary `bLongArr` record creation/consumption separately from Type-5 object records.
4. Determine whether the Type-2-to-Type-5 record can be linked into multiple tile metadata groups and when those links are cleared.
5. Define the evidence-backed lifecycle contract and then implement isolated native tests (no production behavior changes until tests encode proven semantics).
