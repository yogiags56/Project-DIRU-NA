# DEV-003BM — Auxiliary Field Semantics & Cross-Table Transition Matrix

## Source and method

Static audit of `kubikaugustyn/DiamondRushSource/src/main/java/i.java`, pinned to commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

The source file was re-fetched and scanned for every textual occurrence of `bLongArr` and the packed access helpers. This is decompiled-source evidence only; no shipping-JAR bytecode comparison or runtime trace was performed.

## 1. Confirmed auxiliary entry layout

The auxiliary table is a 15-element `long[]`. The packed reader/writer operate on zero-based entry indices.

| Bit range | Width | Observed interpretation | Evidence |
|---|---:|---|---|
| 0..3 | 4 | Entry state / discriminator | State-4 controller filters value 3; other call sites set value 2 |
| 4..5 | 2 | Direction code | State-4 controller switch reads this field |
| 6..12 | 7 | No meaning established | No reader found in the audited auxiliary-table call sites |
| 13..19 | 7 | X coordinate | State-4 controller and coordinate lookup read this field |
| 20..26 | 7 | Y coordinate | State-4 controller and coordinate lookup read this field |
| 27..63 | 37 | No meaning established | No reader found in the audited auxiliary-table call sites |

The packed helper is:
- reader: `(bLongArr[index] >>> bitOffset) & mask(width)`;
- writer: replace the selected bit range while preserving the other bits.

Do not assign semantics to the unclassified ranges without further source/runtime evidence.

## 2. Coordinate lookup

`bByte(x, y)` scans indices 0 through 14 and returns the first entry whose X field (bits 13..19) and Y field (bits 20..26) match. It does not filter by the low-nibble state. It returns -1 when no coordinate match is found.

Consequences:
- duplicate coordinates are ambiguous; the first matching index wins;
- a matching entry in any state can be returned;
- the caller must not assume that coordinate lookup means the entry is in a particular lifecycle state.

## 3. Observed state and direction transitions

### State 3: controller-state-4 consumer

In `asVoid()` controller state 4 (around lines 16455–16500), the code scans all 15 entries and processes only entries whose low nibble equals 3.

It reads:
- X from bits 13..19;
- Y from bits 20..26;
- direction from bits 4..5.

It then clears the entire auxiliary entry to `0L` before dispatching direction behavior.

| Direction | Coordinate change | Resulting behavior |
|---|---|---|
| 0 | Y += 1 | Calls Type-7 creation/attachment path at resulting coordinate |
| 1 | X += 1 | Continues controller traversal with positive X direction |
| 2 | X -= 1 | Continues controller traversal with negative X direction |
| 3 | No movement | Calls Type-7 creation/attachment path at current coordinate |

For the Type-7 path, the controller calls the six-argument object allocator with type 7 and passes the coordinate lookup result from `bByte(x,y)`. The auxiliary entry is already cleared before this call. The allocator writes an object record in `aLongArr` and updates tile metadata; it is not an auxiliary-table allocator.

### State 2: observed writers

Two call sites explicitly write low-nibble state 2:
- around line 16605, after controller processing and coordinate selection, using the result of `bByte(x,y)`;
- around line 16802 in `mVoid(x,y)`, when a matching auxiliary coordinate is found.

The second path also changes a stage-player-layer cell, switches the controller to state 1, and invokes the six-argument object allocator with type 0.

### State 0

Zero is the cleared/empty value: the state-4 consumer explicitly assigns `0L` to a processed entry. Stage initialization also allocates a zero-filled array.

### State 1 and unknown values

The four-argument byte-indexed allocator at lines 15863–15884 creates a new auxiliary entry with state 1, then writes subtype, movement value, and both coordinates.

A separate movement transition writes state 3 at line 16144:
`aVoid(b21, (byte) 3, (byte) 0, (byte) 4)`.
Here `b21` is the zero-based auxiliary slot index read from the linked aLongArr record's offset-54, width-3 field. The surrounding branch has already resolved the linked movement entry and updates the object/movement state before marking the auxiliary entry as state 3.

Therefore, the producer for state 3 is identified in the current decompiled source. This corrects the preliminary conclusion of this DEV-003BM draft. State 3 is a transition state on an existing linked auxiliary entry, not a state written by the two state-2 call sites.

Values 4..15 are still not semantically classified. The exact bytecode parity of this transition remains unverified because the checked-in upstream classfile is historical and not proven to match the 2025 source revision.

## 4. Cross-table transition matrix

| Trigger/path | Auxiliary `bLongArr` | Object table `aLongArr` | Tile metadata |
|---|---|---|---|
| Stage subsystem initialization (`arVoid`) | Allocate 15 zero entries | Allocate 15 zero entries | Metadata grid managed separately |
| Checkpoint save | Copy all 15 entries to `dLongArr` | Copy all 15 entries to `cLongArr` | Copy metadata grid to checkpoint grid |
| Checkpoint restore | Restore all 15 entries from `dLongArr` | Restore all 15 entries from `cLongArr` | Restore metadata grid |
| Controller state 4, entry state 3 | Read coordinates/direction, then clear entry | Type-7 record may be allocated/attached through six-argument allocator | Type-7 attachment path calls tile metadata cleanup/update |
| Movement transition near line 16144 | Linked entry state 1 becomes 3 | Uses auxiliary slot index stored in aLongArr offset 54 | Associated movement/object record is updated |
| Controller path near line 16605 | Set matched entry state to 2 | Subsequent processing may create or attach an object record | Updated by corresponding traversal helpers |
| `mVoid(x,y)` | Set matched entry state to 2 | Calls six-argument allocator with type 0 | Stage-player-layer cell is set to -1 |
| Teardown | Set reference to null | Set reference to null | Metadata arrays also released |

The table establishes coordinated checkpoint lifetime, not a one-to-one identity mapping between an auxiliary slot and an object-record slot. The coordinate lookup is by X/Y and does not return an object-record ID.

## 5. Object-record allocator distinction

The six-argument `aByte(x,y,type,...,auxIndex)` method allocates from `aLongArr`, scans one-based object IDs 1..15, initializes the object record, and returns its object ID. Its optional auxiliary-index argument is written into object-record bits 54..56.

That auxiliary-index field is distinct from the auxiliary table's own packed state, X/Y, and direction fields. The Type-7 controller path passes the result of coordinate lookup to this allocator, but this does not establish that auxiliary slot number and object-record ID are interchangeable.

## 6. Implementation decision

No typed auxiliary-record model was added in this milestone. The current native harness continues to preserve the 15 raw 64-bit entries bit-for-bit.

Reason: state-3 production is not accounted for, fields 6..12 and 27..63 remain unexplained, and coordinate lookup accepts any state. Introducing a typed state machine now would encode assumptions as facts.

## 7. Correction and next milestone: DEV-003BN — State-3 Transition Preconditions & Binary Provenance

1. Prove the state-1 to state-3 transition preconditions around line 16144 and the source of b21.
2. Validate the three-bit aLongArr offset-54 link against the zero-based bLongArr slot domain.
3. Inspect the exact original shipping JAR/class set when supplied; the upstream historical target/classes/i.class is not a matching binary.
4. Trace the consumer's behavior after state 3, including direction-specific branches and whole-entry clear.
5. Keep bits 6..12 and 27..63 explicitly unknown unless further readers/writers establish meaning.
6. Add typed transition tests only for semantics supported by source and binary evidence.

No gameplay code was changed in DEV-003BM. No CI or runtime pass is claimed by this source-audit document.
