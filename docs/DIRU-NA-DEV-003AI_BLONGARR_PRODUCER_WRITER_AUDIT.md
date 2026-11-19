# DIRU-NA-DEV-003AI — bLongArr Producer & Writer Audit

**Status:** Confirmed writer call sites documented; no codec/runtime integration  
**Parent:** DEV-003AH  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Overload distinction

Two four-argument setters share the obfuscated name `aVoid`:

- `aVoid(int slot, byte value, byte offset, byte width)` updates `aLongArr[slot - 1]` (source lines 15903–15907).
- `aVoid(byte slot, byte value, byte offset, byte width)` updates `bLongArr[slot]` (source lines 15913–15915).

The first argument's compile-time type is decisive. Calls with an explicit `(int)` cast target aLongArr, not bLongArr. Six-argument calls target the tile metadata helper operating on `eIntArrArr` and `cByteArrArr`; they are not bLongArr writes.

The bLongArr setter's exact expression is:

`bLongArr[slot] = (bLongArr[slot] ^ ((long) current << offset)) | (long) (value << offset)`

The `value << offset` expression is evaluated as a signed Java `int` shift before widening to `long`. The isolated codec preserves this behavior.

## 2. Confirmed bLongArr field-write call sites

Only two direct calls to the byte-indexed four-argument setter were confirmed in the inspected source revision:

| Source line | Slot provenance | Write | Context |
|---:|---|---|---|
| 16144 | `b21`, a byte derived from an aLongArr object's field at offset 54 | Type 3 at offset 0, width 4 | Marks the related bLongArr entry as a queued transition record |
| 16605 | `b81 = bByte(n24, n26)`, first matching coordinate slot | Value 2 at offset 0, width 4 | Marks the coordinate-matched entry before the subsequent state transition |

These two sites are distinct from:
- int-indexed setters such as lines 16140–16145 and 16356–16380, which update aLongArr;
- six-argument setters such as lines 15988, 16417, and 16423, which update packed stage tile metadata;
- direct whole-word clear at line 16469, which consumes a bLongArr record.

## 3. Type-3 producer

At line 16144, the code writes type 3 into the bLongArr slot whose index is held in `b21`. That byte value is extracted from the associated aLongArr record's field at offset 54, then used to read another packed field at offset 6. The write occurs in a movement/segment state branch after the related aLongArr object's type/state fields have been updated.

This confirms a producer for type-3 records. It is not a standalone bLongArr allocator: it reuses a slot identifier derived from the object/movement record state.

## 4. Coordinate lookup and consumer interaction

The helper `bByte(x, y)` scans bLongArr indices 0–14 and returns the first record whose fields at offsets 13 and 20 match. It does not filter by type. The type-3 consumer at lines 16458–16470 scans the same 15 entries, reads type 3, coordinates, and subtype, then clears the whole selected word before dispatch.

At line 16605, the result of `bByte(n24, n26)` is passed directly to the byte-indexed setter to change the low type field to 2. This call is not guarded locally against -1. The next audit must follow the caller's state invariant and determine whether a matching record is guaranteed there; do not silently rewrite the sentinel behavior.

## 5. Corrected exclusions

The following previously listed examples are explicitly **not** bLongArr setter calls:
- line 16145: explicit `(int) b19`, therefore aLongArr;
- line 16380: explicit `(int) b36`, therefore aLongArr;
- lines 16417 and 16423: six-argument tile metadata helper;
- line 16439: six-argument tile metadata helper.

This correction supersedes the preliminary writer inventory in the earlier draft of DEV-003AI.

## 6. Next stage — DEV-003AJ

Build a typed producer/consumer transition model:
- prove the relationship between b21 and the bLongArr slot it selects;
- trace how the bLongArr slot obtains coordinate fields at offsets 13 and 20;
- prove the precondition for the line-16605 coordinate lookup to return a valid slot;
- map type 2 and type 3 transitions and their consumer order;
- add focused tests for valid-slot and no-match sentinel behavior without inventing a producer.

No gameplay integration or runtime behavior change is claimed.
