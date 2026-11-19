# DIRU-NA-DEV-003AI — bLongArr Producer & Writer Audit

**Status:** Producer paths identified from source; no codec/runtime integration  
**Parent:** DEV-003AH  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Correction to the previous stage's open question

The generic `bLongArr` setter is not unused. Its signature is:

`aVoid(byte slot, byte value, byte offset, byte width)`

It is called from several gameplay movement paths. The previous DEV-003AG note saying no direct call site was found is superseded by this audit.

The setter updates a field in-place using the source expression:

`old ^ ((long) current << offset) | (long) (value << offset)`

The final `value << offset` is evaluated as a signed Java `int` shift before widening to `long`. This behavior is intentionally preserved in `LegacyByteIndexedLongRecordCodec.write`.

## 2. Confirmed producer/write paths

The source contains multiple byte-indexed setter calls which target `bLongArr`. Representative writes:

| Source line | Field write | Context |
|---:|---|---|
| 16144 | type = 3, offset 0, width 4 | Creates/marks a type-3 transition record in a movement path |
| 16145 | type = 5, offset 28, width 3 | Writes a separate `aLongArr` record; not bLongArr |
| 16154–16155 | offset 47 and 26 | These use the int-indexed `aLongArr` setter; not bLongArr |
| 16380 | offset 45, width 2 | Byte-indexed state update in movement processing |
| 16423 | offset 3, width 4 | Byte-indexed record field update |
| 16439 | offset 7, width 2 | Byte-indexed record field update |
| 16605 | type = 2, offset 0, width 4 | Marks a coordinate-matched bLongArr record for subsequent processing |

The overload distinction matters: calls whose first argument is an `int` target `aLongArr`; calls whose first argument is a `byte` target `bLongArr`. The same obfuscated method name is overloaded for both stores. Do not classify a call by method name alone.

## 3. Type-3 producer evidence

The strongest confirmed type-3 write is at line 16144:

`aVoid(b21, (byte) 3, (byte) 0, (byte) 4)`

Here `b21` is a byte slot ID derived from an existing movement/object record. The surrounding code also changes the related aLongArr record's type at offset 28 and may update another byte-indexed record field. This demonstrates that bLongArr type-3 records are generated as part of a larger movement transition, rather than by a standalone allocator.

Other byte-indexed writes update subtype and movement fields while processing those transitions. The producer is therefore coupled to the movement state machine; a general-purpose allocator cannot be inferred from these setter calls.

## 4. Consumer and coordinate lookup interaction

The type-3 consumer remains the scan documented in DEV-003AH:
- scan byte-indexed slots 0–14;
- select type 3 from offset 0, width 4;
- read coordinates from offsets 13 and 20, width 7;
- read subtype from offset 4, width 2;
- clear the selected word before dispatch.

The coordinate lookup helper also scans slots 0–14 and compares offsets 13 and 20 without checking type. The consumer and producer therefore share a compact queue-like record representation, but source evidence still does not establish a dedicated allocation/free-list protocol.

## 5. Important exclusions

Several visually similar calls in the same movement methods target other stores:
- int-indexed four-argument setters target `aLongArr`;
- six-argument tile metadata setters mutate `eIntArrArr` and `cByteArrArr`;
- direct `bLongArr[slot] = 0L` is whole-record consumption/release.

Only byte-indexed calls to the four-argument overload are bLongArr field writes. This audit records representative confirmed call sites; a complete typed call graph is still required before refactoring.

## 6. Movement helper boundary

The type-3 subtype-3 branch invokes the movement helper with a fresh coordinate lookup. The lookup can return -1. The called helper's contract must be inspected before deciding whether -1 is a valid sentinel or an unsafe index. No change is made to this behavior in DEV-003AI.

## 7. Next stage — DEV-003AJ

Build a typed call-site matrix for all bLongArr writes:
- classify each overloaded setter by compile-time first-argument type;
- identify slot provenance and producer branch;
- map fields written at offsets 0, 4, 13, 20, and any additional offsets;
- trace every path that can create type 3;
- inspect movement helper handling of a -1 lookup;
- add producer/consumer transition tests only after the exact source state preconditions are known.

No gameplay integration or runtime behavior change is claimed.
