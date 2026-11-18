# DIRU-NA-DEV-003F — Branch-complete Neighborhood Snapshot Schema

**Status:** Named immutable neighborhood sample model and unit fixtures committed; collision evaluator not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. Purpose and limits

This milestone records the neighboring map cells read by the collision helper as explicit, role-labelled samples. It does not read stage arrays, infer missing cells, decide passability, or mutate the map. Raw foreground, player-layer, packed metadata, and optional auxiliary-grid values remain unnormalized.

## 2. Direct reads in the helper

Source method: `i.aBoolean(dx,dy,probe)`, lines 12073–12443.

| Source expression / condition | Snapshot role or requirement | Notes |
|---|---|---|
| `(crtPlayerX+dx, crtPlayerY+dy)` | `TARGET` | Bounds check occurs before target cell arrays are read |
| `eIntArrArr[target] != 0` | target auxiliary-grid sample | Only when optional grid exists and global gate `iByteArr[10]==0` |
| `crtStageForegrondLayer[target]` | target foreground raw | High bits are also used as branch metadata |
| `crtStagePlayerLayer[target]` | target player-layer raw | Drives player-layer switch |
| `bIntArrArr[target]` | target packed metadata | Read by type 10, type 48, and object-push branches |
| `(playerX+2*dx, playerY+2*dy)` | `TARGET_TWO_STEPS` | Used for player-layer 0/8/9/47 and 48 paths |
| `(targetX, playerY+1)` | `TARGET_ABOVE_PLAYER_ROW` | Used for support/obstruction checks; distinct from target-relative above |
| `(targetX, targetY-1)`, `(targetX,targetY+1)` | `TARGET_ABOVE`, `TARGET_BELOW` | Used by empty-cell/door checks |
| `(targetX-1,targetY)`, `(targetX+1,targetY)` | conditional edge/adjacent checks | Only read in specific empty-cell/door branches; include only when that branch is being captured |

## 3. Player-layer type 0/8/9/47 path

This branch computes the two-step destination `n12=playerX+2*dx`, `n2=playerY+2*dy`. It reads:
- foreground low byte and high metadata at the two-step destination;
- player-layer cell at `(targetX, playerY+1)`;
- foreground at that same support cell;
- the `eBoolean(twoStepX,twoStepY)` helper.

The pinned `eBoolean(x,y)` at lines 14309–14313 returns true only when player-layer value is -1 and foreground low byte is not 14, 33, 5, or 28. This is a separate predicate, not a generic collision result.

If the object-push branch succeeds, it moves a player-layer object from target to one cell beyond target horizontally, writes animation frame 18, changes packed metadata flags, and invokes `kVoid`/ `bVoid`. This is a world mutation, not a player tile commit.

## 4. Player-layer type 48 geometry

For target player-layer type 48:
- the two-step coordinate is computed;
- metadata bit `0x8` chooses a vertical offset pair:
  - set: `n24=0, n26=1`;
  - clear: `n24=-1, n26=0`;
- candidate destination cells are `(twoStepX,twoStepY+n24)` and `(twoStepX,twoStepY+n26)`;
- support cell is `(targetX,targetY+n26+1)`;
- source cells conditionally cleared are `(targetX,targetY+n24)` and `(targetX,targetY+n26)`.

All three destination/support samples and both source-clear samples are represented as named roles. The role names intentionally describe geometry, not whether the cells are passable.

## 5. Empty player-layer (-1) branch

The branch reads:
- stage-edge status of the target coordinate;
- player layer at `(targetX, playerY+1)` and metadata low bits there;
- player-layer values at `(targetX,targetY-1)` and `(targetX,targetY+1)`;
- packed metadata at those adjacent cells;
- movement counters `aNInt` and `aPInt`;
- probe mode.

It can clear local active direction when a nearby door condition is met, and clears packed moving bit `0x8`. Do not interpret an empty target as unconditionally traversable.

## 6. Foreground type 19 scripted path

When probe mode is false, foreground type 19:
1. starts at `(targetX+3,targetY)`;
2. scans upward along that fixed X until player-layer value 39 is found;
3. reads packed metadata at the found anchor;
4. calls `aByte` and `mVoid`;
5. clears consecutive foreground type-19 cells downward from the target column until a non-19 cell is found.

When probe mode is true, this branch exits before the scripted search and mutations. The scan length is data-dependent and has no explicit bound in this method. The snapshot therefore stores an ordered list of scan samples plus the anchor, rather than inventing a fixed scan distance. A future safe loader must validate bounds before reproducing this behavior.

## 7. Snapshot class

`LegacyCollisionNeighborhoodSnapshot` stores named roles:
- target and two-step target;
- target-relative above/below/left/right;
- type-48 candidate destinations, source-clear cells, and support cell;
- optional type-19 anchor and ordered scan samples.

Each `CellSample` stores exact coordinates and raw layer/metadata values. Optional auxiliary-grid presence is distinct from its raw value. Collections are defensive copies and unmodifiable. Missing roles remain missing; no default cell is synthesized.

## 8. Test coverage and boundaries

Tests cover raw-value preservation, repeated type-19 scan ordering, absent samples, optional auxiliary-grid state, and collection immutability. They are synthetic data-structure tests only.

Not implemented:
- source branch execution;
- safe map bounds resolver;
- collision evaluator;
- push-object mutation;
- scripted type-19 map rewrite;
- player movement state machine;
- Android gameplay integration.

## 9. Next work item

**DEV-003G — Branch Input Completeness Audit.** Cross-check every array read in the helper and all called predicates against the named sample schema. Add any missing metadata or neighbor dependencies, then define a branch-input validation layer that refuses to evaluate a branch when required samples are absent. Do not add default values for missing samples.
