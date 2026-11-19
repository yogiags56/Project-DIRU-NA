# DIRU-NA-DEV-003AB — Packed Tile Group-Clear Caller Audit

**Status:** Source-level caller audit committed; integration intentionally deferred  
**Parent:** DEV-003AA  
**Implementation:** `LegacyPackedTileGroupClear`  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Overload separation

The decompiled class contains several unrelated methods named `cVoid`. This audit concerns only the **static three-argument** overload at lines 16712–16716:

```java
private static void cVoid(int row, int column, byte group) {
    int word = eIntArrArr[row][column];
    byte shift = (byte) (group * 9);
    eIntArrArr[row][column] =
        word ^ ((word >>> shift & 0x1FF) << shift);
    cByteArrArr[row][column] = 24;
}
```

Do not confuse it with:
- instance `cVoid(int,int,int)` at line 11009, which appends a coordinate/type record to entity/object arrays;
- instance `cVoid(int,int)` at line 6423, which is a rendering/collision drawing routine;
- instance `cVoid(byte)` at line 9039, which prepares sprite animation/render state;
- other zero-argument and boolean overloads.

The source also contains a static `aInt(...)` helper that may call the group-clear overload internally. Method name alone is not a safe identifier; signature and static dispatch matter.

## 2. Direct static caller inventory

The source revision has five direct unqualified call sites to the static group-clear overload:

| Line | Caller context | Condition / observed purpose |
|---:|---|---|
| 15971 | Repeated segment propagation loop | Clears the current group before examining/updating the next segment cell; the loop advances across a multi-cell span and wraps group 2 to group 0 while advancing the column |
| 16276 | Movement/segment transition branch | Clears the current group when the branch flag `b40` is true; otherwise writes segment value 15 into the 4-bit subfield |
| 16674 | `aByte(...)` movement helper | Clears the destination group's metadata when boolean `b5` is true; otherwise derives a segment state and writes its 4-bit subfield |
| 16732 | Object/segment allocation helper | When caller argument `b == 7`, clears the source tile group before marking the allocated object record as type 7 |
| 16776 | Static `aInt(...)` lookup/validation helper | When the selected 4-bit state is 6 but the encoded object coordinates/group do not match the requested coordinates/group, clears that stale group |

These call sites show that group-clear is used both as an explicit movement/segment transition and as stale metadata cleanup. It is not merely a generic tile erase.

## 3. Important behavioral findings

### 3.1 Only one cell's byte state is refreshed

Every direct call reaches the static helper, which writes `cByteArrArr[row][column] = 24`. It does not touch `column + 1`. This confirms the separate side-effect contract implemented by DEV-003AA and distinguishes it from DEV-003Y's six-argument setter.

### 3.2 The 9-bit group is cleared atomically

The XOR expression clears exactly the selected 9-bit region while preserving all other bits of the 32-bit packed word. The group index is used as a 9-bit stride. The direct call sites pass group values derived from state/control-flow variables; the code paths shown use the three group bands 0–2.

### 3.3 Group clear is coupled to entity movement/state

The call sites in the movement helper and allocator indicate that the packed group can represent an object/segment record associated with a tile coordinate. A clear can be part of moving or relocating that record, or removing a stale record. The precise semantic label for every group subfield is still not established.

### 3.4 Adjacent-cell behavior differs by operation

- Six-argument setter: refresh current cell and next column cell.
- Three-argument group clear: refresh current cell only.
- Entity queue `cVoid(int,int,int)`: unrelated queue write; does not mutate `eIntArrArr` through this helper.

Do not unify these methods or normalize their side effects.

## 4. Implementation assessment

`LegacyPackedTileGroupClear.clear(row, column, group)` matches the observed static helper's core mutation order and single-cell state refresh for valid coordinates and group values. Its bounds validation is a defensive wrapper behavior, not an assertion about the original runtime's exception semantics.

Regression tests cover:
- clearing one selected 9-bit group;
- preserving all other packed bits;
- refreshing only the addressed cell state;
- rejecting invalid group/cell inputs before mutation.

## 5. Next stage: DEV-003AC

Before gameplay integration, trace the object-record lifecycle around:
- the allocator at lines 16719–16747;
- the static lookup/validation helper at lines 16750–16781;
- the movement helper at lines 16662–16705;
- the packed segment propagation loop around lines 15938–16006.

The next stage should identify record ownership and transition invariants, then add an isolated lifecycle model only where the source proves the contract. Do not assign final gameplay names to the packed fields based solely on decompiler variable names or numeric values.

No integration into the runtime, collision engine, or rendering loop is claimed in this stage.
