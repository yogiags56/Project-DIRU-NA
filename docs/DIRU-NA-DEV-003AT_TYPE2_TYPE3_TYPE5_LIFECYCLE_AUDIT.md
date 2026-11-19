# DIRU-NA-DEV-003AT — aLongArr / bLongArr Type 2, 3, 5 Lifecycle Audit

**Status:** Source transition paths audited and focused regression fixtures committed; full runtime lifecycle and binary parity remain unverified  
**Parent:** DEV-003AS  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Confirmed source state transitions

### bLongArr

- Four-argument allocator (15863–15884) selects the first record whose type nibble at offset 0 is zero, then writes type 1.
- Movement processing can change a referenced bLongArr entry to type 3 (16144).
- The type-3 consumer scans all 15 entries, reads coordinates/subtype, then clears the entire word (16465–16469).
- Relocation helper `mVoid(x,y)` marks a coordinate match as type 2 (16801–16810).
- The type-2 marker is not a release state. The only explicit whole-word clear observed in this audited consumer is for type 3.

### aLongArr object layout

- The six-argument allocator (16719–16747) finds the first one-based ID whose type field at offset 28, width 3, is zero.
- Non-type-7 allocation writes type 1 and offset-54 mode/reference; type-7 allocation writes type 7 and performs tile/render side effects instead.
- In movement processing, object type 2 is actively processed (16080 onward). On a completion path, the object becomes type 3, its referenced bLongArr record becomes type 3, and a subsequent type-3 object pass clears the aLongArr word via `fVoid(id)` (16138–16145, 16075–16078, 16658–16660).
- On non-completion branches, type 2 can remain type 2 while movement counters/coordinates are updated. It is therefore not accurate to describe type 2 as immediately transient.
- One completion branch writes object type 5 after marking the linked bLongArr slot type 3 (16143–16145). The main movement switch shown at 16075 has cases 3, 2, 1, 6, and 7, but no case 5. This branch therefore leaves a type-5 record outside those movement cases; the source excerpt does not establish whether another subsystem later clears it.
- The six-argument allocator's first-free predicate only treats type 0 as free. A type-5 record remains occupied in that allocator scan until some other code clears it.

## 2. Regression test added

File:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyTypeTwoThreeFiveTransitionTest.java`

The focused fixtures cover:
1. A representative completion sequence in which an object type-2 record is finalized as type 5 and its referenced bLongArr slot is marked type 3.
2. A non-completion state where object type 2 remains type 2.
3. bLongArr type 2 remains allocated until changed to type 3 and explicitly whole-word-cleared.
4. Type 5 is not considered a free object record by the first-free scan; the next free one-based record ID is selected instead.

The fourth test was corrected after review: the initial test name incorrectly suggested type 5 was reusable. It now explicitly verifies that type 5 remains occupied. This correction is committed separately.

These tests are field-level fixtures against existing codecs. They are not an execution of the full game loop and do not prove every branch/caller transition.

## 3. Capacity and leak implications

- bLongArr type 2 can persist until a later movement transition changes it to type 3; it is not freed merely by being marked 2.
- aLongArr type 2 can persist during movement processing.
- aLongArr type 5 is not free under the allocator predicate and is not handled by the primary movement switch excerpt. Whether it is eventually cleared elsewhere remains an open source-wide question.
- Consequently, capacity analysis must count type 2 and type 5 as occupied unless a specific clear path is demonstrated. Do not count type 3 as immediately free until its consumer actually executes the whole-word clear.

## 4. Remaining unresolved behavior

1. Search every write to aLongArr type offset 28 for explicit zeroing or whole-word clear beyond the known type-3 consumer.
2. Trace all creation sites for types 2 and 5 and all transitions into/out of those states.
3. Confirm whether type-5 objects are intentionally persistent, cleared by a stage reset, or released by another method.
4. Determine whether aLongArr capacity can be exhausted in a legal level, including the type-5 records.
5. Continue the offset-54 slot 8–14 reachability investigation with a real stage fixture or runtime trace.

## 5. Validation

- Regression test file added and corrected on `main`.
- No local Gradle execution is claimed.
- No CI success is claimed.
- No exact shipping JAR bytecode or runtime trace was available.
- No production code or codec behavior was changed.

## 6. Next stage — DEV-003AU

Perform a source-wide write/clear inventory for aLongArr:
- all writes to type offset 28;
- all whole-word assignments to `aLongArr[index]`;
- every caller of the type-3 cleanup;
- all stage/reset initialization paths.
Then build a conservative capacity accounting model where types 1, 2, 5, 6, and 7 count as occupied, and only proven clears return a record to the free pool.
