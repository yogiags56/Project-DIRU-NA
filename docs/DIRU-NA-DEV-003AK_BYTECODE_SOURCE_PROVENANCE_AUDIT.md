# DIRU-NA-DEV-003AK — Bytecode/Source Provenance & bLongArr Completeness Audit

**Status:** Provenance mismatch documented; no runtime change  
**Parent:** DEV-003AJ  
**Target source:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Historical source:** same file at `e8c5a63f67e11cf189126d159a9a79799d914c9a`

## 1. Critical artifact provenance finding

The upstream repository contains compiled class files under `target/classes`, including `i.class` and `j.class`. These files must not automatically be treated as bytecode corresponding to the latest decompiled source.

Git history establishes:
- `target/classes/i.class` was last changed in the 2023-04-01 initial commit `e8c5a63f67e11cf189126d159a9a79799d914c9a`.
- `src/main/java/i.java` was subsequently changed, most recently in commit `215c71d04057d66a1b08419b1a4b1043ae3e0881` on 2025-06-07.
- The checked-in `compile.bat` compiles `src/main/java/*.java` into `build/`, not `target/classes/`.

Therefore, the checked-in `target/classes/i.class` is a historical artifact and is not proven to correspond to the 2025 source revision. Its constant pool and line tables do not provide a reliable bytecode validation of the latest decompiled source. A future bytecode audit requires the exact original game JAR/class set or a reproducible binary known to match the current source.

## 2. Historical classfile inspection

The 2023 `target/classes/i.class` contains a method reference set for the older class layout, including the packed helper descriptors. However, its source line table and method call graph differ from the current 2025 decompiled file. In particular, the 2025 source contains edits and rewritten names/paths not represented by the historical class artifact.

The 2023 class file is retained as a historical comparison artifact only. No semantic conclusion about the 2025 `bLongArr` producer is derived from it.

## 3. Exhaustive bLongArr source-reference inventory

A full text scan of `i.java` at both the initial 2023 revision and the latest 2025 revision shows the same categories of direct references:

| Operation | Current source location | Meaning |
|---|---:|---|
| Field declaration | 490 | Static `long[15]` store |
| Teardown | 4910 | Set live array to null |
| Snapshot allocation/copy | 15227–15230 | Copy live store to `dLongArr` |
| Restore | 15354 | Copy snapshot back |
| Runtime allocation | 15844 | Allocate 15 words |
| Getter | 15910 | Read arbitrary packed field |
| Setter | 15914 | Write arbitrary packed field |
| Coordinate lookup | 15925 | Compare offsets 13 and 20 |
| Type-3 consumer | 16465–16469 | Read type/coordinates/subtype, then clear word |
| Static allocation | 17753 | Allocate 15 words |

The two explicit byte-indexed setter call sites in the current source are:
- line 16144: write value 3 to offset 0, width 4;
- line 16605: write value 2 to offset 0, width 4.

No explicit bLongArr field write to offset 4, 13, or 20 was found in either the 2023 initial source or the 2025 latest source. The setter supports arbitrary offsets, but the existence of the helper does not prove those offsets are ever written.

This confirms that the current source representation leaves coordinate/subtype provenance unresolved. It does **not** prove the runtime records are always zero, because the exact original runtime binary and all possible external mutation paths have not been matched to this source revision.

## 4. Implications for the existing codec

The current `LegacyByteIndexedLongRecordCodec` remains a valid low-level representation of the observed getter/setter semantics and coordinate lookup:
- 15 zero-based slots;
- packed field reads;
- source-style signed-int shift behavior in writes;
- first-match coordinate lookup without a type filter;
- whole-word clear.

However, methods `readSubtype`, `readX`, `readY`, and `findFirstByCoordinates` are faithful interpretations of consumers/getters, not proof of a complete record producer schema.

Do not add a synthetic allocator or coordinate writer to make the record model appear complete.

## 5. Required artifact before a definitive bytecode comparison

The next source of truth must be one of:
1. the exact original Diamond Rush JAR for the same game/device build represented by this decompilation; or
2. the complete original class directory with provenance/hash tied to that JAR; or
3. a reproducible build whose input source revision and compiler/runtime dependencies are pinned and whose output is explicitly tied to the 2025 source commit.

The historical `target/classes` directory alone does not meet this requirement.

## 6. Next stage — DEV-003AL

Proceed with a source-only state invariant audit while preserving the bytecode mismatch as a blocker:
- trace every assignment to `b21` and the field from which its slot ID is derived;
- trace the state transitions surrounding the type-3 write;
- trace the type-3 consumer's coordinates when the selected record fields have not been explicitly initialized;
- verify all paths that call the unguarded coordinate lookup at line 16605;
- distinguish proven behavior from decompiler/source artifacts;
- do not modify runtime code until the exact original binary is available.

No bytecode parity, runtime test, or gameplay integration is claimed by this stage.
