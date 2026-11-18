# DIRU-NA-DEV-003B — Direction Vector and Collision Helper Trace

**Status:** Direction-vector core utility and unit tests committed; collision semantics documented, not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source file:** `src/main/java/i.java`

## 1. Exact direction vector table

The pinned source initializes `gByteArr` at line 17722:

`{0, 0, -1, 0, 1, 0, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0}`

The ordinary movement caller passes the negation of the selected pair to the collision helper (line 10702). Therefore the logical tile deltas are:

| Direction code | Input direction | Raw source vector `(g[d], g[d+8])` | Helper delta `(-g[d], -g[d+8])` |
|---:|---|---:|---:|
| 1 | Up | (0, +1) | (0, -1) |
| 2 | Right | (-1, 0) | (+1, 0) |
| 3 | Down | (0, -1) | (0, +1) |
| 4 | Left | (+1, 0) | (-1, 0) |

This matches the input handler's assignments at lines 1234–1240: UP=1, DOWN=3, LEFT=4, RIGHT=2.

## 2. Implemented vector utility

`LegacyDirectionVector` exposes `tileDeltaX(direction)` and `tileDeltaY(direction)`. It returns the logical tile delta after the source's sign inversion. It deliberately does not expose camera pixel offsets or modify player state.

Unknown direction values, including NONE=0, throw `IllegalArgumentException` instead of silently returning a zero vector. Five JUnit tests cover the four directions and invalid NONE.

## 3. Collision helper input and probe mode

The helper is the decompiled method `aBoolean(int dx, int dy, boolean probe)`, lines 12073–12443.

It snapshots selected fields before evaluation:
- packed direction/state `kInt`;
- logical tile `crtPlayerX/Y`;
- active direction `lByte`;
- scripted movement flag `xBoolean`;
- movement-related counter `aXInt`;
- interpolation `jInt`;
- `aWInt`.

It derives packed direction code from the requested delta, computes destination `(crtPlayerX + dx, crtPlayerY + dy)`, then evaluates map bounds, foreground metadata, player-layer content, and secondary metadata.

The boolean `probe` is not a simple dry-run switch: several mutations are conditionally suppressed when true, but the method can still update other shared fields or call helper methods on some branches. Do not assume every probe is side-effect free without tracing each branch.

## 4. Why the helper return is not named canMove

The local return flag `b2` is initialized false but is assigned in multiple semantic branches:

- Out-of-stage destination sets it true.
- Foreground type 5 sets it true and, outside probe mode, enables scripted `xBoolean` movement with `atBoolean=true`.
- Foreground type 3 uses metadata threshold logic: low metadata sets it true; another branch clears it and enables a separate `n10` path.
- Player-layer types 34/35 set it equal to the probe argument.
- Player-layer types 31/49 set it false.
- A collection of player-layer values sets it true.
- Player-layer type 10 can set it true when `xByte == 3`.
- Ordinary traversable/player-layer values can set it based on additional two-tile checks, `eBoolean`, metadata, and pushability.
- Special player-layer type 48 has its own two-cell geometry and state-bit mutations.
- Empty player-layer type -1 has stage-edge and nearby-door/obstacle conditions.
- Foreground type 28 clears active direction and the packed moving bit.

In addition, `b3` records a distinct foreground-type-2 interaction path and affects whether `aDInt` is reset. Local `n3` controls whether player-layer evaluation is entered. Neither is interchangeable with `b2`.

Consequently, a single boolean named `canMove` would erase important distinctions among accepted movement, special movement triggers, interaction, and blocked/edge cases.

## 5. Coordinate commit and animation/state effects

At lines 12417–12443:
- When `!probe && b2 && !b3`, the helper resets `aDInt`.
- When `b2 && aWInt == 0`, it sets local `jInt = 18` and advances local tile coordinates by the requested delta.
- If movement delta is nonzero but the accepted branch is not taken, it may set packed bit `0x8`.
- If packed bit `0x8` is set outside probe mode, animation selection is requested using packed direction.
- Only the non-probe path writes the local snapshot fields back to the player object.

The helper also has an early `return false` for a `kByte == 4` special boundary case (lines 12295–12300), after requesting a direction-dependent animation. The return value alone therefore cannot be used as a complete movement outcome.

## 6. Required native model

Do not implement a single boolean collision API. The next native layer should produce a typed outcome that can preserve at least:
- ordinary tile-step accepted/committed;
- blocked/no tile-step;
- scripted/forced movement trigger;
- push/object interaction;
- foreground interaction/checkpoint trigger;
- probe result, distinct from committed simulation;
- animation/state-bit effects that the caller must apply.

The exact outcome taxonomy remains provisional until the remaining foreground/player-layer cases and the helper caller's handling are traced end-to-end.

## 7. Regression matrix to add before a full movement implementation

- Direction code to tile delta for all four directions.
- Invalid direction rejection.
- Out-of-bounds destination behavior with `aWInt` values 0 and nonzero.
- Probe versus non-probe for every mutation-bearing branch.
- Foreground types 2, 3, 5, 7, 19, 28 and checkpoint type 4.
- Player-layer types 34/35, 31/49, type 10, type 48, empty -1, and the blocking-value set.
- Special early return for `kByte == 4`.
- Interaction path `b3` and `aDInt` reset precedence.
- Packed bit `0x8` and direction animation selection.
- Coordinate commit only under the exact source branch conditions.

## 8. Validation boundary

This commit adds only the direction-vector utility and its unit tests. It does not add a collision engine, stage-grid model, player movement state machine, Android controls, or gameplay behavior. CI results must be checked against the exact commit SHA before calling these tests passed.

## 9. Next work item

**DIRU-NA-DEV-003C — Collision Outcome Taxonomy and Branch Fixtures.** Convert the traced helper branches into a typed outcome proposal and synthetic table-driven fixtures. Keep source-specific grid values and probe/commit side effects explicit; do not wire this into player movement until those fixtures are stable.
