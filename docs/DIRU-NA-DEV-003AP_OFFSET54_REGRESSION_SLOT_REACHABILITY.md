# DIRU-NA-DEV-003AP — Offset-54 Bit Semantics Regression & Slot Reachability Status

**Status:** Regression tests added; execution/CI result not yet available; gameplay reachability remains unresolved  
**Parent:** DEV-003AO  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope

This stage adds focused regression coverage for the already implemented source-compatible `LegacyPackedLongRecordCodec` field writer/reader. It does not change production behavior, widen the field, clamp indices, or introduce a remapping.

## 2. Source-level slot reachability findings

The bLongArr allocator (source lines 15863–15884) scans indices 0 through 14 and allocates the first slot whose type field is zero. It does not stop at index 7.

The coordinate lookup (15917–15930) scans the same complete 15-slot array in ascending order, compares coordinate fields only, and returns the first match. It does not filter by record type or limit its search to eight slots.

Three allocator call sites remain visible:
- 12130: foreground type-19 handling;
- 12618: player-layer value 38 scan;
- 16603: movement transition.

The source therefore permits allocation and lookup at indices 8–14. However, static inspection does not establish that the line-16810 relocation path can encounter such an index during a normal playable level. That requires a reproducible level/state fixture or runtime trace showing the active slot population and the first coordinate match.

## 3. Offset-54 exact-semantics test added

New test file:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyALongArrReferenceEncodingTest.java`

Coverage:
- Inputs 0–14 written at offset 54, width 3.
- Readback of the three-bit field equals `value & 7`.
- Bit 57 is asserted clear for values 0–7 and set for values 8–14.
- Bits 58–63 remain clear for nonnegative inputs 0–14.
- Writing -1 is separately asserted to set the low three bits, bit 57, and bits 58–63 under the source-compatible long-shift expression.

The test intentionally exercises the current codec's existing write semantics; it is not a proposed correction. A temporary unsupported assertion that claimed to exercise the source allocator's type-7 branch was removed because the current `ObjectLayout` test adapter models only type-1 allocation. No claim is made that the type-7 allocator branch has been covered by this test.

## 4. Important semantic distinction

The aLongArr setter's input is a byte in the source method signature, promoted to long before shifting:
`(long) b << offset`.

This differs from the bLongArr setter, which shifts its byte input as an int before widening:
`(long) (b2 << offset)`.

Do not consolidate these overloads or reuse one helper without preserving their distinct Java promotion and overflow behavior. The new tests target the aLongArr long-shift path only.

## 5. Validation status

- Test source was added and reviewed through GitHub.
- No local Gradle execution was performed in this stage.
- No successful GitHub Actions run or CI status is claimed.
- The test suite's actual pass/fail status remains pending execution in a configured build environment.
- No original target JAR bytecode has been supplied or authenticated for this exact source revision.

## 6. Remaining blockers

1. Construct a deterministic legal level fixture that causes bLongArr slot allocation beyond index 7 and makes that slot the first coordinate match at the line-16810 path, or prove a runtime invariant that prevents it.
2. Capture the exact aLongArr word before and after the line-16810 call, including bits 54–63.
3. Inspect the original target JAR bytecode for the packed getter/setter and six-argument allocator. The upstream compiled classfile is older than the audited source and is not sufficient evidence.
4. Add a faithful type-7 allocator adapter/test only after its full source side effects and dependencies are modeled; do not label a type-1 test as type-7 coverage.

## 7. Next stage — DEV-003AQ

Continue with a source-driven lifecycle model of the three bLongArr allocation paths and the relocation caller. Prioritize a reproducible slot-population trace. Keep production codec behavior unchanged until reachability and original-binary semantics are established.
