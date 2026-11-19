# DIRU-NA-DEV-003AJ — Typed bLongArr Producer/Consumer Validation

**Status:** Static source audit committed; coordinate provenance remains unresolved  
**Parent:** DEV-003AI  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Typed overload call graph

The source has separate overloads for two packed stores:

| Signature | Backing store | Slot convention |
|---|---|---|
| `aInt(byte slot, byte offset, byte width)` | `bLongArr[slot]` | zero-based |
| `aVoid(byte slot, byte value, byte offset, byte width)` | `bLongArr[slot]` | zero-based |
| `aInt(int id, byte offset, byte width)` | `aLongArr[id - 1]` | one-based |
| `aVoid(int id, byte value, byte offset, byte width)` | `aLongArr[id - 1]` | one-based |

The compile-time type of the first argument selects the store. Six-argument `aVoid` is a separate tile-grid helper and must not be mistaken for a bLongArr write.

## 2. Confirmed direct bLongArr writes

In the audited source revision, only two direct calls to the byte-indexed field setter were found:

| Line | Slot expression | Field |
|---:|---|---|
| 16144 | `b21` | value 3 at offset 0, width 4 |
| 16605 | `b81 = bByte(n24, n26)` | value 2 at offset 0, width 4 |

No direct call was found that writes bLongArr offsets 4, 13, or 20. The generic setter supports arbitrary fields, but that does not prove that those fields are populated at runtime.

## 3. Coordinate provenance is unresolved

The type-3 consumer at lines 16465–16468 reads:
- type from offset 0, width 4;
- coordinate A from offset 13, width 7;
- coordinate B from offset 20, width 7;
- subtype from offset 4, width 2.

The coordinate lookup at lines 15917–15929 also reads offsets 13 and 20.

However, the complete direct-reference inventory for `bLongArr` in this source revision shows:
- allocation of the 15-word array;
- the byte-indexed getter and setter;
- coordinate lookup;
- two direct field writes, both to offset 0;
- whole-word clear;
- snapshot/restore;
- teardown.

No explicit writer for the coordinate or subtype fields was found. This is a genuine source-level gap in the current reconstruction. Possible explanations such as implicit zero values, omitted code paths, or decompiler/source mismatch are hypotheses only and are not adopted as facts.

Consequently, the current codec's `findFirstByCoordinates` is a faithful representation of the lookup helper, but its use as a populated coordinate index is not yet validated.

## 4. Sentinel behavior at line 16605

The lookup helper returns -1 when no coordinate pair matches. At line 16605, the caller passes the result directly to the byte-indexed setter:

`aVoid(b81 = bByte(n24, n26), (byte) 2, (byte) 0, (byte) 4)`

There is no local `>= 0` guard at that call site. Since Java array access with slot -1 is invalid, the caller must rely on an invariant that a matching slot exists, or the original code has a failure path if that invariant is broken. The audit does not silently clamp -1 or redirect it to slot 0.

The separate `mVoid` path at lines 16798–16810 does check the coordinate lookup result before writing. These two call paths therefore have different local sentinel handling.

## 5. Corrected interpretation

- Type-3 creation is observed as a low-field write at offset 0.
- The consumer reads coordinate and subtype fields, but their producer has not been located.
- The coordinate lookup is first-match and does not filter record type.
- A zeroed word can match coordinates (0,0).
- The caller at line 16605 assumes a match without a local guard.
- No runtime integration or coordinate initialization behavior is claimed.

This supersedes any earlier implication that the producer audit had already established a complete coordinate-bearing queue record.

## 6. Validation status

This is a source audit only. No new gameplay code or tests are added because the missing coordinate producer makes a synthetic producer/consumer test misleading. The existing codec tests validate bit operations and lookup behavior, not the source's complete state-machine invariant.

## 7. Next stage — DEV-003AK

Resolve coordinate provenance and sentinel preconditions:
1. Inspect all 15-word array writes and possible array copies at bytecode level, not only decompiled source text.
2. Compare the exact classfile descriptors for the overloaded getter/setter methods.
3. Verify whether the source file revision and compiled game binary are the same build.
4. Trace the runtime invariant that guarantees a match at line 16605.
5. Inspect the `mVoid` caller path and explain why it checks -1 while the other path does not.
6. Only after those checks, decide whether bLongArr requires a revised record schema or the decompiled source needs correction.

Do not add coordinate setters, allocator methods, or gameplay integration until this discrepancy is resolved.
