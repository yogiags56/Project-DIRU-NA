# DIRU-NA-DEV-003BA — Type-5 Allocator Failure & State Transition Audit

**Status:** Critical unchecked-capacity failure identified in the pinned decompiled candidate source  
**Parent:** DEV-003AZ  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope and evidence boundary

This is a static audit of `src/main/java/i.java` from the pinned decompiled source. It distinguishes the simple four-argument allocator from the Type-5 object-record allocator overload. No production code is changed. Findings are not yet confirmed against the original shipping JAR bytecode or runtime.

## 2. Critical finding: Type-5 allocator does not guard a full table

The Type-5 creation branch in `asVoid()` (around source lines 16540–16580) calls:

```java
this.aByte(n31, n30, (byte) 8, (byte) (-2), (byte) 2, (byte) 0)
```

This resolves to the six-argument overload declared around source line 16720:

```java
private byte aByte(int n, int n2, byte b, byte b2, byte b3, byte b4)
```

The overload scans one-based IDs 1 through 15, looking for a record whose type field (bits 28..30) is zero. Its loop condition is effectively:

```java
b6 = next ID;
if (b6 > 15 || recordType(b6) == 0) break;
```

After the loop, it immediately executes:

```java
i.aLongArr[b6 - 1] = 0L;
```

There is no `b6 <= 15` check and no `return -1` path in this overload. If all 15 records are occupied, the scan exits with `b6 == 16`, then attempts `aLongArr[15]` on a 15-element array. In Java this is an array-index-out-of-bounds failure.

**Conclusion from inspected source:** the Type-5 creation allocator has an unchecked full-table path. It does not return `-1` when full. The caller also does not receive a sentinel to check before subsequent packed writes.

This is a source-level control-flow finding. Runtime occurrence depends on reaching the allocator with all 15 slots occupied and should be verified against the original JAR/runtime before claiming a reproduced crash.

## 3. Important distinction: a different allocator safely returns -1

A separate four-argument overload around source lines 15860–15885:

```java
private byte aByte(byte b, byte b2, byte b3, byte b4)
```

scans for a free record and explicitly returns `-1` if no free slot is found.

That is **not** the overload called by the Type-5 creation path above. The safe `-1` behavior must not be generalized to every allocator overload.

There are also other `aByte` overloads for movement/record operations. Their return values such as `-2` are movement/control outcomes, not allocator failure sentinels.

## 4. Type-5 creation transition

The inspected `asVoid()` state-5 branch:
1. Selects the current tile and derives the traversal direction.
2. Calls the six-argument record allocator on the first eligible tile.
3. Writes Type-5 into the allocated record's type bits (28..30).
4. Writes source coordinates at offsets 14 and 20.
5. Writes the record ID into traversed tile metadata entries.
6. Stores the traversal span/length and updates traversal state.
7. Transitions the state machine onward (the observed path sets `xByte = 1` and then advances through the update logic).

The one-shot gate limits one new Type-5 allocation per execution of that traversal branch. It does not protect the allocator from exhaustion caused by other live object records or earlier traversals.

## 5. Record release observations

The whole-record release helper `fVoid(byte id)` simply assigns:

```java
i.aLongArr[id - 1] = 0L;
```

The inspected object-update state machine contains several explicit release paths:
- When an object in the object-update switch reaches type 3, the code calls `fVoid(id)`.
- In the type 1/6/7 interaction path, completion/transition branches call `fVoid(id)`, including a branch that changes the controller state to 3 and another that transfers state to a neighboring record before freeing the old record.
- A Type-5 creation itself is not followed by an immediate free; it establishes tile metadata references and later participates in the object's update/interaction machinery.

The release helper does not clear tile metadata references by itself. Any native implementation must model the ordering and the corresponding metadata changes rather than assuming a record free automatically cleans all references.

The decompiled state machine is complex and overloaded. This audit does not claim that every terminal Type-5 path has been exhaustively enumerated or that a specific user-visible interaction always frees the Type-5 record. That requires a focused control-flow pass and runtime trace.

## 6. Capacity implications

- `aLongArr` contains 15 records.
- Type-5 allocation uses a distinct overload from the overload that safely returns `-1`.
- The Type-5 allocator's full-table path appears to index element 15 in a 15-element array.
- The allocator does not implement a safe refusal/rollback path in the inspected code.
- One traversal invocation allocates at most one Type-5 record, but total concurrent Type-5 and other object records share the same 15 slots.

Therefore, the native implementation must **not** reproduce the unchecked array access. It should preserve the 15-slot format while defining an explicit, safe allocation failure result and an atomic creation failure path that leaves no tile metadata reference to an unallocated record. Whether the legacy game crashes, avoids the condition through gameplay constraints, or has decompiler/source divergence remains to be verified.

## 7. Native implementation requirements (not yet implemented)

1. Keep the 15-entry table and one-based external record IDs.
2. Search for a free slot before writing any record fields.
3. If no slot exists, return an explicit allocation failure; do not index slot 15.
4. Make Type-5 creation atomic: on allocation failure, do not write tile metadata IDs or mutate neighboring tiles as though creation succeeded.
5. Keep tile metadata cleanup and whole-record release as distinct operations.
6. When freeing a record, validate ID range 1..15 before converting to zero-based array index.
7. Add tests for empty, partially occupied, exactly full, invalid ID, and shared tile references.
8. Do not alter legacy behavior in unrelated allocator overloads without separate evidence.

## 8. Validation status

- Type-5 allocator overload and call site inspected at pinned source revision.
- The full-table out-of-bounds path is statically established by the loop and subsequent array index.
- No runtime crash was reproduced.
- No shipping-JAR parity test was performed.
- No production source or tests were changed.
- No Gradle test or Android build was run for this documentation-only stage.

## 9. Next stage — DEV-003BB

1. Complete the Type-5 terminal-state/control-flow matrix: creation, active traversal, completion, abort, transfer, and cleanup.
2. Enumerate every `fVoid(id)` call with caller state and record type preconditions.
3. Establish whether Type-5 records can remain live after their final tile reference is removed.
4. Compare the decompiled candidate allocator with original JAR bytecode if available.
5. Design the allocator exhaustion and atomic-creation tests before implementing a native fix.
