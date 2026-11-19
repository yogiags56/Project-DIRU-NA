# DIRU-NA-DEV-003AG — bLongArr Codec Reconstruction

**Status:** Isolated codec and regression tests committed; gameplay integration deferred  
**Parent:** DEV-003AF  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Storage identity and lifecycle

`bLongArr` is a separate static 15-element `long[]`, not an alias of `aLongArr`.

Observed lifecycle:
- Allocated as 15 words in static initialization (line 17753).
- Reallocated together with `aLongArr` in `arVoid()` when `lBoolean` is true (lines 15840–15845).
- Snapshot copied independently into `dLongArr` (lines 15227–15230).
- Restored independently from `dLongArr` (line 15354).
- Set to null during teardown (line 4910).

No dedicated allocator for bLongArr was found in the inspected references.

## 2. Getter/setter layout

The source defines:
- Getter `aInt(byte slot, byte offset, byte width)` at lines 15909–15911:
  `(int) (bLongArr[slot] >>> offset & ~(-1L << width))`
- Setter `aVoid(byte slot, byte value, byte offset, byte width)` at lines 15913–15915:
  `old ^ ((long) current << offset) | (long) (value << offset)`

The setter's right-hand value is shifted as a Java `int` before widening to `long`. The codec preserves that exact behavior. In this source revision, no direct call site to this setter overload was found; it is retained as a low-level parity primitive, not presented as an observed runtime mutation path.

## 3. Observed fields

The source's coordinate lookup and type-3 consumer read:

| Offset | Width | Observed role |
|---:|---:|---|
| 0 | 4 | Record type; consumer selects value 3 |
| 4 | 2 | Direction/subtype used by consumer switch |
| 13 | 7 | First coordinate |
| 20 | 7 | Second coordinate |

The coordinate lookup at lines 15917–15929 scans zero-based indices 0 through 14 and returns the first matching coordinate pair. It does not check the type field, so an all-zero slot can match coordinate pair (0,0). This is preserved by `findFirstByCoordinates`.

The consumer at lines 16458–16470 scans all 15 words. For entries whose type field at offset 0 equals 3, it reads coordinates from offsets 13 and 20 and subtype from offset 4, then clears the entire selected word to zero before processing the movement/transition. This confirms whole-word release for the observed type-3 path.

## 4. Implementation

Added `LegacyByteIndexedLongRecordCodec`:
- fixed 15-word backing array;
- zero-based byte-compatible slot indexing;
- generic field getter;
- source-style generic setter with int-shift behavior;
- observed type/subtype/coordinate accessors;
- first coordinate-pair lookup, irrespective of type;
- whole-word clear.

Regression tests cover field reads, first-match lookup, (0,0)-style empty slot semantics, whole-word clear isolation, in-range write behavior, no-match result, and invalid slot rejection.

## 5. Boundaries and open questions

- Do not merge this codec with either aLongArr layout.
- Do not infer a bLongArr allocator from its 15-slot capacity.
- No source call to the generic bLongArr setter overload was found in the audited revision; actual observed mutations are whole-word clears in the type-3 consumer.
- The source's no-type-filter coordinate lookup can return an empty zeroed slot for coordinates (0,0). This is legacy behavior, not necessarily a desired redesign.
- CI status and actual test execution are not verified. No gameplay integration is claimed.

## 6. Next stage — DEV-003AH

Audit the full type-3 consumer state machine:
- map each subtype value read from offset 4;
- trace coordinate increments/decrements and wrap/bounds behavior;
- determine how each branch updates stage tile/player layers;
- verify whether a matched coordinate record is always consumed/cleared;
- add transition-level tests around the pure codec behavior without moving game logic prematurely.
