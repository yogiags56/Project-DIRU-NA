# DIRU-NA-DEV-003AN — aLongArr Offset-54 Reference Encoding Audit

**Status:** Static source audit completed; encoding defect/aliasing behavior identified, runtime reachability unverified  
**Parent:** DEV-003AM  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Executive finding

The offset-54 field is **not proven to be a safe 15-slot reference**. The source passes zero-based bLongArr indices (0–14) into aLongArr's six-argument allocator, but stores them through a 3-bit field setter. The getter reads only three bits. Values 8–14 therefore alias values 0–6 on read.

The write helper also does not mask its input to the declared width before OR-ing it into the 64-bit word. For values above 7, shifted high bits can spill beyond the nominal field into bits 57 onward. This is a concrete source-level encoding hazard, not merely a theoretical capacity mismatch.

No remapping from 0–14 to 0–7 was found in the traced call paths. Do not change the field width or invent a remapping until the original binary/runtime invariant is established.

## 2. Exact packed-store helpers

The int-indexed aLongArr getter is:

`aInt(id, offset, width) = (int)(aLongArr[id - 1] >>> offset & ((1L << width) - 1))`

Source lines 15899–15901. It extracts exactly the requested width; offset 54, width 3 returns only bits 54–56.

The int-indexed setter at lines 15903–15907:
1. reads the old field using the requested width;
2. decrements the one-based ID;
3. XOR-clears the old extracted field at the offset;
4. ORs the unmasked input value shifted to the offset.

In particular, the final expression is:
`(word ^ ((long) oldValue << offset)) | ((long) value << offset)`

It does **not** AND-mask the new value to the declared width. The byte-indexed bLongArr helper is separate and must not be conflated with this setter.

## 3. Producer: six-argument aLongArr allocator

Source lines 16719–16747. The allocator scans aLongArr IDs 1–15, clears the selected 64-bit word, and then:
- for type 7, writes type 7 at offset 28 and does not write offset 54;
- otherwise, writes type 1 at offset 28;
- writes argument `b4` to offset 54, width 3;
- writes the bLongArr slot reference `b6` to another packed field at offset 0, width 3;
- writes X, Y, group, and direction fields.

The offset-54 write is therefore specifically the supplied `b4` argument on the non-type-7 path.

## 4. Proven value flow into offset 54

Relevant callers:

| Source line | Value passed as final argument | Provenance |
|---:|---|---|
| 16289 | `aInt(b36, 54, 3)` | Copies the existing three-bit value from another aLongArr record |
| 16495 | `bByte(aInt11, aInt12)` | bLongArr coordinate lookup; returns zero-based slot 0–14 or -1 |
| 16810 | `bByte(n, n2)` | bLongArr coordinate lookup; returns zero-based slot 0–14 or -1 |

At line 16810, the caller reaches the allocator only if the earlier lookup at line 16801 succeeded, but it performs a second lookup for the final argument. At line 16495, no local nonnegative guard is visible at the call site. The coordinate lookup itself scans all 15 bLongArr slots and returns its first matching zero-based index.

The six-argument allocator's return value is a separate one-based aLongArr ID (1–15); it is not the bLongArr reference stored at offset 54.

## 5. Three-bit aliasing consequences

Because the getter reads only bits 54–56:

| bLongArr slot supplied | Read back from offset 54 |
|---:|---:|
| 0–7 | 0–7 |
| 8 | 0 |
| 9 | 1 |
| 10 | 2 |
| 11 | 3 |
| 12 | 4 |
| 13 | 5 |
| 14 | 6 |

Thus the movement path at line 16090 casts the three-bit getter result to a byte and uses it as a zero-based bLongArr index. A stored source index 8–14 will resolve to a different slot 0–6, if those values reach this path.

The unmasked setter adds another effect: writing 8–14 at offset 54 sets bits above bit 56 as well. The getter still returns only the low three bits. The allocator clears the entire aLongArr word before this write, so those spill bits are initially zero and then may become set. Their semantic impact depends on the active aLongArr record layout and subsequent mutations; this audit does not claim they are always harmless.

## 6. Additional offset-54 operations

- Line 16090 reads offset 54, width 3, and uses the result as a bLongArr slot.
- Line 16253 reads offset 54 from one aLongArr record and writes the same three-bit result into another record.
- Line 16289 forwards the existing three-bit value to the six-argument allocator.
- Line 16736 writes the supplied final argument at offset 54, width 3.

No explicit conversion table, modulo mapping, range check, or alternate slot bank was found in these source-level operations.

## 7. What is established vs. unresolved

**Established from source:**
- bLongArr has 15 zero-based slots.
- Coordinate lookup can return any index 0–14.
- Two call paths feed coordinate-lookup results to the six-argument aLongArr allocator.
- Offset 54 is written with width 3 and read with width 3.
- The getter truncates the observable value to three bits.
- The setter does not mask its input before shifting/OR-ing.
- Values 8–14 therefore alias 0–6 on read, with additional high-bit spill on write.

**Not established:**
- Whether normal gameplay can produce a matching bLongArr coordinate in slots 8–14 at either caller.
- Whether a runtime invariant limits linked bLongArr records to slots 0–7.
- Whether bits 57 onward are intentionally reserved/used in the active aLongArr layout.
- Whether the decompiled source exactly matches the original shipping JAR.
- Whether this behavior is present in the original binary or is a source/decompiler artifact.

## 8. Required next work

1. Trace all bLongArr allocator call paths and prove whether slot indices 8–14 are reachable at the two offset-54 producer call sites.
2. Inspect all aLongArr layout fields at offsets 54–63, including overlapping layouts, to determine the effect of setter spill bits.
3. Compare the exact original JAR bytecode for the getter, setter, and six-argument allocator; the upstream compiled classfile is known to predate the latest source and is not sufficient as parity evidence.
4. Build a small exact-semantics test for offset-54 writes with values 0–14, including raw 64-bit word output and getter readback.
5. Do not widen the field, mask the setter, clamp indices, or remap slots in production code until binary/runtime evidence supports that compatibility decision.

## 9. Next stage — DEV-003AO

Prioritize the bLongArr slot reachability proof and the complete aLongArr bit-range overlap audit. The current finding is a concrete encoding hazard; the intended original behavior remains unresolved. No runtime code or gameplay integration was changed in this stage.
