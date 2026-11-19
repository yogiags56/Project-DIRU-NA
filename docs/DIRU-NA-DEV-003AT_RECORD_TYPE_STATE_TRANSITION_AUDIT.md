# DIRU-NA-DEV-003AT — bLongArr / aLongArr Type-State Transition Audit

**Status:** Source transition graph documented; no production changes  
**Source:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. bLongArr type writers

A full-source scan of the audited revision found exactly four writes to the bLongArr type field (offset 0, width 4):

| Source line | New type | Context |
|---:|---:|---|
| 15875 | 1 | shared 15-slot bLongArr allocator |
| 16144 | 3 | completion of an aLongArr type-2 movement record when its linked value reaches zero |
| 16605 | 2 | movement-transition path after allocating a bLongArr record |
| 16802 | 2 | `mVoid(x,y)` coordinate lookup and relocation path |

No direct bLongArr type-0 writer exists in the scanned source other than whole-word clearing at line 16469. That consumer clears only records whose type is 3.

## 2. bLongArr state interpretation

Observed lifecycle:
- **0 — free/zero word:** eligible for first-free allocation. Whole-word clear also returns a slot to this state.
- **1 — newly allocated:** produced by the shared allocator. The type-1 record carries subtype, movement value, and coordinates.
- **2 — active movement/relocation record:** produced by the two type-2 setters. It is not cleared by the type-3 consumer.
- **3 — completed/ready-for-consumption:** written by the aLongArr type-2 movement path when the linked bLongArr value is zero. A separate consumer reads subtype and coordinates, then clears the complete bLongArr word at line 16469.

This is a source-derived interpretation, not an assertion that every type-2 record follows one identical gameplay path.

## 3. aLongArr state transitions relevant to bLongArr

The aLongArr type field is at offset 28, width 3. The six-argument allocator creates:
- type 1 for every non-type-7 request, writing offset 54 from its final argument;
- type 7 for the special request, clearing a 9-bit tile metadata group and setting render state 24, without writing offset 54.

The movement loop handles aLongArr type 2 separately. It reads the reference at offset 54 (three bits), derives a bLongArr slot, and processes that record. When the linked value reaches zero, it:
1. sets the aLongArr record's type to 3;
2. if the linked bLongArr movement value is zero, sets bLongArr type to 3;
3. changes the aLongArr record to type 5;
4. may set the movement controller into a completion state.

The aLongArr type-3 cleanup and bLongArr type-3 cleanup are distinct paths. Do not conflate their record tables or ID conventions.

## 4. Important type-2 reactivation path

The aLongArr type-1/type-6/type-7 movement handler can encounter a destination tile group whose packed tile type is 7. For non-type-7 object records it then:
- reads a bLongArr slot index from the destination tile group's low three bits;
- copies the object's offset-54 value into the referenced bLongArr record at offset 54;
- sets that bLongArr record type to 2;
- updates its movement/direction/coordinate fields;
- clears the current aLongArr object through `fVoid`.

This is a further path that mutates a bLongArr type-2 record; it is not a generic release. The source writes the same three-bit reference value into bLongArr offset 54, which is a separate field from aLongArr offset 54.

## 5. Allocator capacity boundary

The six-argument aLongArr allocator starts IDs at 1 and checks whether ID exceeds 15 inside the loop. After the loop it unconditionally indexes `aLongArr[id - 1]`. If all 15 records are occupied, the loop appears to leave ID 16 and then indexes element 15.

The isolated type-7 model currently returns -1 on full capacity rather than reproducing this apparent out-of-bounds access. This is a deliberate safety boundary in the model, but means it is not exact source parity for the full-table case. A future compatibility decision must be based on original bytecode/runtime evidence, not on the safer model behavior.

## 6. Key conclusions

1. bLongArr type 2 is not released by the type-3 consumer.
2. bLongArr type 3 is the only typed state directly cleared by the observed consumer.
3. Both type 2 and type 3 are set in movement-related paths, but only type 3 has the observed whole-word release.
4. aLongArr type 2 is the active processor that can transition a linked bLongArr record to type 3.
5. The three-bit offset-54 reference remains a separate unresolved correctness issue. The state machine does not make indices 8–14 representable.

## 7. Next stage — DEV-003AU

- Build a small explicit transition model for bLongArr types 1, 2, 3 and whole-word release, preserving separate aLongArr type state.
- Add fixtures for type-2 persistence, type-2-to-type-3 completion, and type-3 clearing/reuse.
- Model the aLongArr type-1/type-6/type-7 destination-tile handoff as a separate transition.
- Determine whether the tile group's three-bit bLongArr reference is itself constrained to 0–7 and whether that can explain the offset-54 width.
- Keep production behavior unchanged pending target-JAR and live-level evidence.
