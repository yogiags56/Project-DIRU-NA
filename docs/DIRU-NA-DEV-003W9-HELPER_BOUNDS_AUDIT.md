# DIRU-NA DEV-003W9.5 — Helper semantics and grid-boundary audit

## Source identity
Static source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This is decompiled-source evidence, not a retail runtime test.

## 1. Important overload/name disambiguation

There are multiple unrelated methods with similar names:
- `jVoid()`: a no-argument periodic state/cleanup routine with a switch over an internal counter.
- `jVoid(x,y)`: a coordinate helper used by object cleanup. It ORs foreground cell metadata with `0x10000000` and invokes `alVoid()`.
- `cVoid(boolean)`: a separate state/update method.
- `cVoid(x,y,type)`: appends a three-field event into fixed ring buffers `aByteArr`, `cByteArr`, and `bByteArr`, then advances `aeInt = (aeInt + 1) & 7`. It is not a cByte-grid neighborhood writer.
- `aByte(worldIndex,stageIndex)`: unrelated static stage-record accessor.
- `aByte(x,direction,bound,widthRow)`: special-object endpoint expansion helper used by `VVoid()`.

Never bind these methods by short name alone in native code; use descriptive names and retain overload distinctions.

## 2. `jVoid(x,y)` coordinate helper

Exact observed behavior:
1. Reads foreground cell `crtStageForegrondLayer[x][y]`.
2. Preserves the low 28 bits.
3. ORs `0x10000000` into the cell.
4. Calls `alVoid()`.

It does not clear the cell, write cByte, or directly mutate eByteArr. Its callers often perform the actual foreground/player-layer clearing before invoking it. The helper marks a foreground metadata flag and triggers the associated `alVoid()` side effect.

The no-argument `jVoid()` is a distinct periodic routine; do not port it as the coordinate helper.

## 3. `cVoid(x,y,type)` event-ring helper

Exact observed behavior:
- Writes X and Y as bytes into `aByteArr[aeInt*2]` and `aByteArr[aeInt*2+1]`.
- Writes type into `cByteArr[aeInt]`.
- Resets `bByteArr[aeInt]` to zero.
- Advances the 3-bit ring cursor: `aeInt = (aeInt + 1) & 7`.

This is an eight-entry event queue/ring, not a stage-grid cByte mutation. In `aBoolean(x,y)`, the call `cVoid(x,y,1)` queues an event after the player-layer and packed background/object state have been changed.

Native implementation must preserve byte narrowing, ring capacity 8, wraparound, overwrite behavior and cursor order. Consumer timing and meaning of the event type still need separate tracing before choosing a public API.

## 4. `aByte(x,direction,bound,row)` endpoint expansion

Observed behavior:
- Starts at `x + direction`.
- Initial candidate is eligible when either:
  - `x > 0 && bound == 0`, or
  - `x < bound && bound > 0`.
- Initial candidate cell is traversable when player-layer value is negative, equals 31, or is recognized by `dInt(candidateX,row)`.
- Once entered, it continues in the same direction while the next candidate satisfies the loop boundary condition and is either negative, recognized by dInt, or code 31.
- Returns the last accepted X as a byte.
- Left expansion uses direction -1 and bound 0; right expansion uses direction +1 and bound width-1.
- The initial condition and loop condition are not identical: code 31 is accepted in the initial test, while subsequent loop test accepts negative, recognized, or 31 (same categories but ordering differs); preserve exact expression and evaluation order.
- The boundary condition is asymmetric and must not be replaced by a conventional `0 <= x < width` clamp.

### Safety implications
- The source accesses the candidate array cell as part of the condition; malformed geometry can cause an out-of-bounds access depending on the boundary expression and direction.
- The native port must establish the stage border invariant from source assets or reject malformed geometry before running the update. Do not silently clamp, wrap, or skip.
- Java byte narrowing is part of the legacy return behavior; test widths/coordinates near signed-byte limits before selecting native coordinate storage.

## 5. Special-object call sequence clarified

Within `VVoid()`:
1. Iterate eByteArr in stable 3-byte slots.
2. Only records with row > 0 are active.
3. Expand left endpoint with `aByte(left,-1,0,row)`.
4. Expand right endpoint with `aByte(right,+1,width-1,row)`.
5. Store both returned endpoints back into the same record.
6. Call mutating `aBoolean(x,row)` for every inclusive X in the expanded span.

Within `aBoolean(x,y)`, for recognized player-layer types:
1. Set cByte neighborhood via `kVoid(x,y)` (distinct helper).
2. Change player-layer cell to 9.
3. Pack classified object type into background/object grid, preserving selected flags.
4. Queue event with `cVoid(x,y,1)`.
5. Independently check player overlap and possibly damage hero.

Thus cVoid is an event queue; kVoid is the neighborhood-grid operation. Do not conflate them.

## 6. Native design implications

- Suggested descriptive native names:
  - `markForegroundMetadataForObjectRemoval(x,y)` for coordinate `jVoid`.
  - `enqueueStageObjectEvent(x,y,type)` for three-argument `cVoid`.
  - `expandSpecialObjectEndpoint(x,direction,bound,row)` for static four-argument `aByte`.
- Keep no-argument jVoid and boolean cVoid outside this helper set; audit them independently if needed.
- Keep event queue state (8 slots + cursor) separate from stage-grid byte-state.
- Add tests for ring wraparound, overwrite order, byte narrowing, endpoint expansion over empty/code31/recognized/unrecognized cells, and both left/right boundaries.
- The full downstream consumer of the event ring and `alVoid()` still needs tracing before runtime integration.

## 7. Milestone boundary

W9.5 documents helper semantics and bounds only. No native event queue, endpoint expander, object runtime, collision activation, checkpoint implementation, or GameSession wiring is added.
