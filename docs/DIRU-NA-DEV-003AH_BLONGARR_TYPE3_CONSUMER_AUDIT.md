# DIRU-NA-DEV-003AH — bLongArr Type-3 Consumer State-Machine Audit

**Status:** Consumer transition audit committed; gameplay integration deferred  
**Parent:** DEV-003AG  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Consumer scan and record ownership

The state-machine branch at source lines 16458–16510 scans `bLongArr` using zero-based indices 0 through 14. It selects records whose low 4-bit type field (offset 0, width 4) equals 3.

For each selected record it reads:
- X-like coordinate: offset 13, width 7;
- Y-like coordinate: offset 20, width 7;
- movement subtype: offset 4, width 2.

The entire selected word is cleared immediately, before the movement transition is processed. Therefore the consumer is destructive/one-shot: it consumes the queued record before dispatching its movement logic.

## 2. Subtype transition map

The source switch on the 2-bit subtype is:

| Subtype | Coordinate effect | Control state |
|---:|---|---|
| 0 | Increment second coordinate | Set first movement delta to 0; continue with phase value 0 |
| 1 | Increment first coordinate | Set first movement delta to +1; continue with phase value 2 |
| 2 | Decrement first coordinate | Set first movement delta to -1; continue with phase value 2 |
| 3 | No coordinate delta in this switch | Call the movement helper with current coordinates, type 7, current deltas, and a fresh coordinate lookup; stop processing this record |

The first three branches continue through the loop, updating the small control-state bytes and applying the selected coordinate delta. The default branch dispatches to the movement helper and exits the current record's loop.

The labels “first” and “second” coordinate are deliberately used here rather than assigning row/column or world-axis semantics beyond what the caller proves.

## 3. Coordinate lookup semantics

The static lookup helper `bByte(x, y)` scans slots 0–14 and returns the first record whose fields at offsets 13 and 20 match the supplied pair. It does not check type, so:
- an empty zero word can match (0,0);
- if multiple records share coordinates, the earliest slot wins;
- it returns -1 if no pair matches.

The subtype-3 dispatch passes this lookup result to the movement helper. The helper's full behavior when the lookup returns -1 must remain guarded by its own source logic; do not silently convert -1 to slot 0.

## 4. Record mutation contract

Observed order:
1. Read type and fields from selected slot.
2. If type equals 3, read coordinates/subtype.
3. Clear the complete selected `bLongArr` word.
4. Process subtype and update local coordinates/control state.
5. For subtype 3, call movement helper with a fresh coordinate lookup.

This means a failure or early exit in the subsequent movement helper does not restore the consumed queue entry. That is part of the source order and should be preserved if this logic is later ported.

## 5. Boundaries and unproven semantics

- This audit proves the subtype dispatch and coordinate arithmetic, but does not yet prove the complete valid-coordinate bounds or wrap policy for every stage dimension.
- It does not prove the gameplay names of the two coordinate fields.
- It does not prove a producer/allocator for `bLongArr`; the generic setter overload exists, but no direct call site to it was found in the audited source.
- It does not prove whether every consumed type-3 record reaches a successful movement helper outcome.
- No integration into the game loop, rendering, or collision runtime is claimed.

## 6. Next stage — DEV-003AI

Trace the producer side and the movement helper contract:
- find every assignment to `bLongArr` beyond the generic setter declaration and whole-word clear;
- trace all call sites that can create type-3 entries;
- inspect the movement helper's handling of coordinate lookup -1;
- map valid coordinate boundaries against stage dimensions;
- add a pure transition model and tests only for source-proven branches.

If no producer exists in the inspected source revision, record that explicitly rather than inventing one.
