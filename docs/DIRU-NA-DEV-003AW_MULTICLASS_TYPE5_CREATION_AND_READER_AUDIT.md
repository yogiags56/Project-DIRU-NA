# DIRU-NA-DEV-003AW — Multi-Class Type-5 Creation and Reader Audit

**Status:** Pinned-source multi-class inventory completed; type-5 creation path clarified; no production changes  
**Parent:** DEV-003AV  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Scope and method

The recursive Git tree for the pinned source revision contains 329 Java files. The game/application source files outside the bundled `com/`, `javax/`, and `org/` compatibility/runtime namespaces were fetched and searched for:
- `aLongArr`, `bLongArr`, `cLongArr`, and `dLongArr`;
- the cleanup helper `fVoid(`;
- direct references to the record arrays.

The non-library top-level source set includes `GloftDIRU.java`, `a_Config.java`, `b_SpriteAnimator.java`, `c.java`, `d.java`, `e_Utils.java`, `f_Sprite.java`, `g.java`, `h.java`, `i.java`, `j_SoundManager.java`, and `k.java` (plus the desktop launcher). All direct array-field hits were in `i.java`. No other game top-level source file directly references these four record arrays or the cleanup helper.

This is a source-level cross-class inventory for the pinned revision, not a proof about the original shipping bytecode or reflective/native access.

## 2. Type-5 creation path clarified

At source lines 16571–16575, a collision/line traversal path performs the following sequence:

1. Calls the six-argument allocator `aByte(n31, n30, (byte) 8, (byte) -2, (byte) 2, (byte) 0)`.
2. The allocator at 16719–16747 first selects the first record whose type field at offset 28 is zero, clears the whole word, and—because its mode argument is 8, not 7—initially writes a normal type-1 record.
3. The caller immediately overwrites that allocated record's type at offset 28 with type 5 (line 16573).
4. The caller writes X and Y coordinates at offsets 14 and 20 (lines 16574–16575).
5. Subsequent calls write the associated tile/collision metadata.

Thus type 5 is not a separate allocator branch. It is a caller-side reclassification of a record returned by the ordinary non-type-7 allocator. The allocator's first-free policy still treats this type-5 record as occupied thereafter.

## 3. Cross-class reader/clear result

Within the pinned decompiled Java source set:
- the only direct owner and accessor of the four long-array fields is `i.java`;
- the main type dispatcher at 16075 has cases 3, 2, 1, 6, and 7, with no case 5;
- the explicit whole-word clear helper at 16658–16660 sets `aLongArr[id - 1]` to zero;
- its known dispatch entry is type 3 (16075–16078), with additional explicit calls in movement branches;
- no type-5-specific branch or type-5-specific call to the whole-word clear helper was found in the pinned source.

The source evidence therefore supports a narrow conclusion: **type-5 records are not consumed or released by the audited aLongArr dispatcher/cleanup paths in this source revision.** It does not establish whether they are intentionally persistent for the remainder of a level or are cleared through a lifecycle path not visible in the searched Java source.

## 4. Capacity and full-table implications

The six-argument allocator scans IDs 1 through 15 and treats only type 0 as free. Type 5 is therefore occupied. The type-5 creation path at 16573 does not check the allocator result before using it.

If all 15 slots are nonzero, the allocator loop can advance to ID 16 and then access `aLongArr[15]` at line 16730. The collision/line traversal caller does not visibly guard against a failed allocation because the allocator has no visible failure return. This makes the full-table out-of-bounds candidate relevant to the type-5 creation path, but legal gameplay reachability remains unknown.

## 5. What this audit does and does not prove

**Proven from the pinned source text:**
- array references are confined to `i.java` among the fetched game top-level Java files;
- type-5 creation reuses the ordinary allocator and then changes the record type;
- type 5 consumes allocator capacity;
- the main dispatcher has no type-5 case;
- no type-5-specific whole-word clear was found in the audited source.

**Not proven:**
- that type-5 records survive every level transition or save/restore path;
- that no hidden bytecode/native/reflection path exists in the original MIDlet;
- that 15 simultaneous nonzero records can occur in a legal level;
- that the shipping JAR exhibits the same control flow as the later decompiled Java source;
- that the full-table allocator fault is reachable in normal gameplay.

## 6. Next stage — DEV-003AX

1. Trace all writes to the type-5 records' coordinate fields (offsets 14 and 20) and their corresponding reads, to determine whether these records serve as persistent collision/line markers.
2. Inspect the complete type-5 creation loop boundaries and reset conditions to establish whether the number of type-5 records per level is naturally bounded.
3. Construct a source-derived occupancy simulation for the type-5 line traversal, with conservative assumptions and explicit unknowns.
4. Compare the relevant methods against the historical classfile or a verified original JAR before proposing a cleanup or capacity guard.
5. Keep production changes blocked until the target binary/runtime confirms the intended lifetime and overflow behavior.

## 7. Validation

- This document is committed to the Project-DIRU-NA repository on `main`.
- Existing capacity regression tests remain field-level tests; no new runtime test is claimed in this stage.
- No Gradle test execution or successful CI run is claimed.
- No production code or codec behavior was changed.
