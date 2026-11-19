# DIRU-NA-DEV-003AV — Type-5 Reader Inventory and Conservative Capacity Accounting

**Status:** Reader/caller inventory completed for the pinned decompiled source; capacity fixtures committed; runtime persistence remains unverified  
**Parent:** DEV-003AU  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Main object dispatcher

The primary aLongArr dispatcher at source lines 16068–16075 scans one-based IDs 1 through 15 and switches on the object type at offset 28, width 3.

The visible switch includes cases 3, 2, 1, 6, and 7. There is no case 5 in this dispatcher.

- Case 3 calls `fVoid(id)`, which assigns the entire aLongArr word to zero (16658–16660).
- Case 2 enters the active movement path (16080 onward).
- Case 1 is processed as a movement/object state.
- Cases 6 and 7 have their own handling paths.
- Type 5 falls through the switch without a matching case in this dispatcher.

This is evidence that type 5 is not advanced or released by the main movement dispatcher. It is not, by itself, proof that no other subsystem reads or clears it.

## 2. Observed type-5 creation contexts

At least two distinct creation paths are visible in the pinned source:

1. Movement completion path (16138–16145): the linked bLongArr record is marked type 3, then the aLongArr object record is written as type 5.
2. Collision/line traversal path (16571–16575): an aLongArr record is allocated/selected and marked type 5, with X/Y fields written afterward.

The type-5 records are therefore not an artifact of one isolated branch.

## 3. Reader inventory and important distinction

The source-wide direct reference scan shows that aLongArr is read through the packed getter at 15900 and written through the packed setter at 15903–15906. The principal direct record-type read is the dispatcher at 16075. The helper around 16662–16705 reads the record's type to choose collision/tile side effects, but does not implement a generic type-5 release.

No separate type-5-specific whole-word clear was identified in the audited `i.java` source revision. The explicit `fVoid(id)` whole-word clear is reached from the type-3 dispatcher case and from a small number of explicit movement branches (e.g. 16242 and 16260). No type-5 case calls it in the primary dispatcher.

This finding is scoped to the pinned decompiled Java file. It does not rule out behavior in other classes, native code, reflection, or the original shipping bytecode.

## 4. Conservative capacity model

The allocator at 16721–16730 considers a slot free only when the three-bit type at offset 28 equals zero. Thus types 1, 2, 3, 5, 6, and 7 all occupy capacity until the full 64-bit word is cleared or the array is replaced.

Capacity accounting must therefore:
- count every nonzero type as occupied;
- not treat type 3 as free until `fVoid(id)` executes;
- treat type 5 as occupied because no release path was found in the audited source;
- treat stage reinitialization as table replacement, not ordinary per-record cleanup;
- treat snapshot restore as restoring an earlier population, not freeing records.

A 15-slot table can exhaust if 15 nonzero records coexist. The source does not establish whether a legal stage can produce that state.

## 5. Added capacity regression fixtures

Added:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyObjectCapacityAccountingTest.java`

The three fixtures verify:
1. All observed nonzero types are considered occupied by the first-free scan.
2. A type-3 marker does not release capacity until the entire word is zeroed.
3. A type-5 record remains occupied until explicit clear/table reset.

These are codec-level accounting tests. They do not execute the complete source movement loop or establish legal level reachability.

## 6. Full-table allocator behavior

The allocator loop at 16721–16730 can advance to ID 16 when all 15 entries have nonzero type. It then indexes `aLongArr[b6 - 1]` at 16730 without an evident guard in that method. This remains a source-level out-of-bounds candidate. It is not yet proven reachable in normal gameplay, nor checked against the shipping JAR bytecode.

## 7. Validation limits

- Capacity test file committed to `main`.
- No local Gradle execution or successful CI run is claimed.
- No full-repository multi-class bytecode/source parity audit has been completed.
- No exact shipping JAR or live gameplay trace was available.
- No production codec behavior was changed.

## 8. Next stage — DEV-003AW

1. Expand the search beyond `i.java` to every class in the pinned source repository for reads/writes to the static aLongArr field and type-5 semantics.
2. Trace the caller and callee behavior of the type-5 creation helper at 16573, including what `aByte(...)` returns and whether its selected ID is ever released elsewhere.
3. Audit stage teardown/reload and snapshot paths against the actual game lifecycle.
4. Establish whether type-5 records are intentional persistent map objects or transient collision objects.
5. Keep any production cleanup policy blocked until full class coverage and shipping-binary/runtime evidence exist.
