# DIRU-NA-DEV-003H — Coordinate Binding and Bounds Proof

**Status:** Coordinate resolver, bounds guards, corrected type-19 fixture, and unit tests committed; no collision evaluator  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. CI correction before continuing

DEV-003G runs #20 and #21 failed. Run #20 exposed a test compilation error from an obsolete `EnumSet.plus(...)` expression. Run #21 compiled but one fixture still omitted the mandatory `TARGET` sample for foreground type 19. Both failures were inspected in workflow logs. The fixture now supplies target, anchor, upward scan, foreground clear-run, and `L_BOOLEAN` context. Neither failed run is counted as passing.

## 2. Coordinate equations from the pinned helper

For player origin `(px,py)`, requested delta `(dx,dy)`, and target `T`:
- target: `T=(px+dx, py+dy)`;
- two-step: `T2=(px+2dx, py+2dy)`;
- target-above-player-row: `(px+dx, py+1)`;
- target-relative above/below: `(Tx,Ty-1)`, `(Tx,Ty+1)`.

Type-19:
- anchor scan column: `(Tx+3, Ty)`;
- upward scan successor: `(scanX, scanY-1)`;
- foreground clear-run successor: `(Tx, currentY+1)`, where the source reuses the anchor scan's final Y as the starting Y.

Type-48, where metadata bit 0x8 chooses offsets:
- set: `upperOffset=0, lowerOffset=1`;
- clear: `upperOffset=-1, lowerOffset=0`;
- destinations: `(T2x,T2y+upperOffset)`, `(T2x,T2y+lowerOffset)`;
- source clear cells: `(Tx,Ty+upperOffset)`, `(Tx,Ty+lowerOffset)`;
- support: `(Tx,Ty+lowerOffset+1)`.

## 3. Checked resolver

`LegacyCollisionCoordinateResolver` computes these source-derived coordinates using long intermediate arithmetic. It rejects integer overflow and rejects any coordinate outside the supplied positive stage dimensions before a caller can index a grid.

It exposes coordinate-only functions. It does not read a stage grid, decide collision, or silently clamp an invalid coordinate.

## 4. Dynamic type-19 scan safety

The legacy upward scan has no explicit bound in the original helper. The native path must not copy that unbounded loop literally. A safe caller must:
1. start at the checked anchor-column coordinate;
2. read one in-bounds player-layer sample at a time;
3. stop only when sentinel value 39 is observed;
4. if the next upward coordinate is outside stage bounds before finding 39, return an invalid/incomplete input result;
5. never fabricate an anchor or wrap to another row.

The downward foreground run is separate and begins at anchorY+1 on targetX. Each next coordinate must be checked independently. If the run reaches the bottom edge without observing a non-19 terminator, reject the fixture as malformed/incomplete rather than reading outside the stage.

## 5. Test coverage

- target and two-step formulas;
- target-above-player-row and target-relative vertical neighbors;
- separate type-19 upward and downward coordinate progression;
- type-48 offset geometry;
- out-of-bounds target, scan, and support rejection;
- integer overflow rejection;
- invalid stage dimensions.

The DEV-003G type-19 fixture now includes the required target sample as well as both scan lists and anchor.

## 6. Boundaries

No grid access, collision branch evaluation, stage mutation, object movement, animation, Android UI, or playable gameplay is implemented by this milestone. The resolver only proves coordinate validity for supplied dimensions.

## 7. Next work item

**DEV-003I — Collision Branch Read Plan and Safe Neighborhood Capture.** Define a branch-specific capture plan that requests only required samples, checks each coordinate before reading, records whether each dynamic scan terminated correctly, and returns explicit incomplete/out-of-bounds reasons. This will form the safe input boundary for the first pure-core evaluator.
