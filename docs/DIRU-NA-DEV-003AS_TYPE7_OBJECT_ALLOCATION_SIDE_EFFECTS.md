# DIRU-NA-DEV-003AS — Type-7 Object Allocation Side-Effect Model

**Status:** Isolated type-7 branch model and regression tests committed; full allocator/runtime parity not yet established  
**Parent:** DEV-003AR  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Source behavior confirmed

The six-argument allocator at source lines 16719–16747:
- searches one-based aLongArr record IDs 1–15 using the type field at offset 28, width 3;
- clears the selected 64-bit record before writing;
- when `b == 7`, calls `cVoid(n,n2,b3)`, then writes type 7 at offset 28;
- does **not** write offset 54 in the type-7 branch;
- writes X at offset 31 width 6, Y at 37 width 6, group at 43 width 2, and direction at 45 width 2;
- normalizes negative direction to 2.

The `cVoid` helper (16712–16716) clears exactly one 9-bit group in `eIntArrArr[n][n2]` using group index `b3`, then sets `cByteArrArr[n][n2]` to 24.

This confirms that type 7 is not merely an allocation that skips a reference field: it also has tile-metadata and render-state side effects.

## 2. Added isolated model

File:
`core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyTypeSevenObjectAllocationModel.java`

It models:
- first-free one-based object record selection;
- whole-word clearing of the selected aLongArr record;
- the type-7 record fields;
- clearing the selected 9-bit tile metadata group;
- setting the corresponding render-state byte to 24;
- no write to offset 54.

The model copies the input arrays and returns snapshots, so test fixtures can compare pre/post state.

**Boundary caveat:** the model returns record ID -1 when all 15 records are occupied. The decompiled source loop appears to advance to ID 16 and then index `aLongArr[15]` without a visible capacity guard before clearing it. That possible out-of-bounds behavior is not emulated here and requires separate verification. Do not describe the model as exact for a full table.

## 3. Added regression tests

File:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyTypeSevenObjectAllocationModelTest.java`

Coverage:
1. Type-7 allocation clears only the selected nine-bit tile group, leaves adjacent groups unchanged, sets render state to 24, sets type 7, and leaves offset 54 and bit 57 clear.
2. Allocation chooses the first free one-based record ID and maps negative direction to 2.

These tests cover the isolated model's intended normal-capacity behavior. They do not execute the upstream game or prove parity against the shipping JAR.

## 4. Relationship to the offset-54 hazard

The type-7 branch does not itself create the offset-54 spill. The previously identified non-type-7 branch writes the supplied mode/reference value at offset 54 width 3 without masking the input. Therefore:
- type 7: offset 54 remains clear after the allocator's initial whole-word clear;
- non-type-7: supplied values 8–14 can spill into bit 57 under Java long-shift semantics;
- the type-7 branch's tile-group clear is a separate observable side effect and must be preserved in any future port.

## 5. Validation status

- Model and test files were committed to branch `main`.
- No local Gradle test execution is claimed.
- No successful CI status check is available in this stage.
- No target JAR bytecode comparison or live gameplay trace was performed.
- No production codec behavior was modified.

## 6. Next stage — DEV-003AT

1. Audit type-2 and type-3 transitions across all movement and cleanup branches, including whether type-2 records are overwritten or persist.
2. Inspect all aLongArr record types 1, 5, and 7 as a state machine; preserve the two distinct packed layouts and their call-site-specific meanings.
3. Investigate the apparent full-table behavior of the six-argument allocator.
4. Add source-derived tests for the exact `cVoid` nine-bit clearing expression, including signed/overflow cases if valid inputs permit them.
5. Continue seeking a real level fixture or runtime trace for slot 8–14 reachability. Keep production changes blocked until that evidence and target-binary parity exist.
