# DIRU-NA-DEV-003AO — bLongArr Reachability & aLongArr Bit-57 Overlap Audit

**Status:** Static source audit completed; direct producer path narrowed; runtime slot reachability remains unverified  
**Parent:** DEV-003AN  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Important correction to DEV-003AN

DEV-003AN listed line 16495 as a direct coordinate-lookup value written into aLongArr offset 54. That needs refinement.

At line 16495, the six-argument allocator is called with type argument 7:
`this.aByte(x, y, (byte) 7, ..., bByte(x, y))`.

Inside the allocator (16719–16747), the type-7 branch writes type 7 at offset 28 and **does not write offset 54**. The final argument is passed but unused in that branch. Therefore line 16495 is not a direct offset-54 producer.

The actual direct coordinate-lookup write is line 16810, where type argument is zero. That takes the non-type-7 branch and writes the lookup result at offset 54. The caller first performs the same coordinate lookup at line 16801 and proceeds only if it is nonnegative. The second lookup at 16810 occurs after no bLongArr mutation in between, so source control flow strongly supports the same slot result, although runtime/binary validation is still outstanding.

The line-16289 call is conditional: it passes type 7 when `b40` is true (offset 54 not written), or type 5 when false (offset 54 is written from an already-read three-bit field of another aLongArr record). It is a link-copy path, not a fresh bLongArr coordinate lookup.

## 2. Offset 57 is an active field, not spare capacity

The source explicitly reads and writes aLongArr offset 57, width 1:

| Line | Operation | Meaning in source path |
|---:|---|---|
| 16099 | Read offset 57, width 1 | Branches movement processing based on `b24` |
| 16140 | Write 0 at offset 57 | Clears the flag in a movement transition |
| 16198 | Write 1 at offset 57 | Sets the flag in a movement transition |
| 16259 | Write 0 at offset 57 | Clears it while converting/updating a record |

Consequently, offset 57 is demonstrably meaningful in the active aLongArr movement record. It cannot be treated as unused padding.

## 3. Concrete spill interaction

The int-indexed setter at lines 15903–15907 clears only the old three-bit field, then ORs the unmasked input shifted by the offset:

`word = (word ^ ((long) old3BitValue << 54)) | ((long) input << 54)`

When the line-16810 caller supplies a bLongArr index from 8 through 14:
- the low three bits written at 54–56 represent index 0–6;
- bit 57 is also set because the input's fourth bit shifts into bit 57;
- the offset-54 getter still reads only bits 54–56;
- the movement processor later reads bit 57 as its independent one-bit flag.

Therefore a direct link value 8–14 can simultaneously alias the linked bLongArr slot and force the separate offset-57 flag to 1. This is more than a wrong-index issue: it can alter movement-state branching.

The allocator clears the selected aLongArr word before writing its fields, so the spill is not masked by an old word value. Subsequent explicit writes to offset 57 may later clear or set that flag, but that does not make the initial overlap safe.

## 4. bLongArr reachability: what static source proves

The bLongArr allocator at lines 15863–15884 scans all 15 slots, from 0 through 14, and selects the first slot whose type field is zero. It has no upper bound of 7 and no special reserve of slots 8–14.

The coordinate lookup at lines 15917–15930 also scans all 15 slots in ascending order. It has no type filter and no index cap at 7.

The three allocator call sites remain:
- line 12130: foreground interaction;
- line 12618: player-layer code-38 scan;
- line 16603: movement transition.

Thus the source does not enforce a global eight-slot limit. However, this alone does not prove that normal gameplay can populate a matching coordinate in slot 8–14 at the particular line-16810 call. That depends on runtime occupancy, record lifetimes, coordinate uniqueness, and timing. It remains an empirical reachability question.

## 5. Refined call-site classification

| Line | Allocator type | Final argument | Does allocator write offset 54? |
|---:|---:|---|---|
| 16289 | 7 if `b40`, otherwise 5 | Existing offset-54 value from `b36` | Only on type 5 path |
| 16495 | 7 | Coordinate lookup result | No; type-7 branch skips offset 54 |
| 16810 | 0 | Coordinate lookup result | Yes; non-type-7 branch writes offset 54 |

This corrects the broader producer inventory in DEV-003AN. The only direct fresh coordinate-index write to offset 54 among these callers is line 16810.

## 6. Remaining hazards and caveats

- The line-16810 caller guards the first coordinate lookup against -1, then repeats the lookup to supply the allocator. The current source has no mutation between those two lookups, so the repeated result should be stable in this single-threaded method flow; this is a source-level inference, not a runtime trace.
- The line-16495 call can pass -1 as an unused argument in the type-7 allocator branch. Do not treat that as proof of an invalid offset-54 write.
- The line-16289 type-5 branch copies an already truncated three-bit link. It cannot restore an original index 8–14 after aliasing has occurred.
- The 15-slot allocator and lookup permit indices 8–14, but actual normal-play reachability at line 16810 remains unproven.
- The upstream compiled classfile is older than the source revision and cannot establish shipping-JAR bytecode parity.

## 7. Next stage — DEV-003AP

1. Trace the lifecycle and occupancy of bLongArr slots around the line-16810 relocation path.
2. Establish whether slot 8–14 can be the first coordinate match when that path executes.
3. Build a focused exact-semantics regression test for the aLongArr setter/getter, including raw bit 57 outcomes for inputs 0–14 and -1.
4. Add tests for the type-7 allocator branch proving it does not write offset 54, and the type-5/non-type-7 branches proving it does.
5. Keep production codec behavior unchanged until exact target-JAR bytecode and runtime invariants are available.

No production runtime code was changed in this stage.
