# DIRU-NA-DEV-003A — Player Movement State and Interpolation Contract

**Status:** Static movement contract recorded; implementation intentionally not started  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Audit input:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. Purpose and boundary

Record the movement state and interpolation behavior before implementing player movement in the Native Android core. This is a static-source contract, not a runtime-validated movement specification. The candidate source remains read-only research material; no decompiled code or proprietary assets are copied into this repository.

This step does not implement collision, movement, animation selection, camera behavior, touch mapping, or Android rendering.

## 2. Source anchors

Primary source: `src/main/java/i.java`

| Anchor | Source lines | Observation |
|---|---:|---|
| M01 | 10633–10668 | Special-movement branch and ordinary movement eligibility gate |
| M02 | 10668–10723 | Requested direction copied to active direction; normal movement and collision call |
| M03 | 10724–10746 | Separate direction-6/object-push path initializes progress to 72 |
| M04 | 10747–10793 | Direction-0 idle/settling path decrements positive progress by 6 |
| M05 | 10795–10834 | Fall/hazard checks and state-dependent animation/damage calls |
| M06 | 10836–10867 | Scripted/out-of-stage movement path and its own progress countdown |
| M07 | 12073–12443 | Collision/move helper; destination checks and logical coordinate commit |
| M08 | 11980–12013 | Camera target uses tile coordinate plus progress × direction vector |

The line references are relative to the pinned candidate revision above.

## 3. Distinct movement fields

Keep these source fields separate until their complete call graphs justify semantic renaming:

- `aByte`: requested/pending direction written by the input handler.
- `lByte`: active/current direction consumed by the movement update.
- `jInt`: sub-tile progress value used by camera and movement update.
- `crtPlayerX`, `crtPlayerY`: logical tile coordinates.
- `gByteArr[direction]`, `gByteArr[direction + 8]`: direction-vector components used for X/Y interpolation.
- `kInt & 0x7`: packed direction/orientation bits.
- `kInt & 0x8`, `kInt & 0x70`, `kInt & 0x1000`: independent state gates/flags; do not collapse them into one generic moving boolean.

## 4. Ordinary movement lifecycle

### 4.1 Eligibility to consume a requested direction

At M01–M02, ordinary movement/input consumption is considered when:

`(lByte == 0 || (jInt <= 0 && lByte != 5)) && !xBoolean && (kInt & 0x70) == 0 && aFInt == -1`

The update then copies `aByte` into `lByte`. Additional state, door, bounds, and collision checks still apply. A requested direction is therefore not proof that a move has started.

### 4.2 Directional branch and collision helper

For active direction values 1–4, the source:
- avoids starting the ordinary move for hero animation IDs 40 and 48;
- has a separate `kInt & 0x1000` branch that sets direction bits, initializes `jInt = 18`, and requests an animation;
- otherwise calls the movement/collision helper `aBoolean(-gByteArr[lByte], -gByteArr[lByte + 8], false)`;
- may update animation and movement-related timers according to the helper result and packed state flags.

The helper M07 snapshots movement fields, derives direction from the requested delta, checks destination bounds and layered map/object metadata, and may commit the new logical tile coordinate. When its accepted-move branch is reached with `aWInt == 0`, it sets `jInt = 18` and adds the requested tile delta to `crtPlayerX/Y`. It writes its updated movement fields back only on its non-probe path.

**Important:** logical tile position can be committed at movement start while the rendered/camera position remains interpolated. Do not treat the tile coordinate as the complete visual position.

### 4.3 Progress countdown

In the ordinary direction-0 settling branch at M04, positive `jInt` is reduced by 6 per update. The observed nominal sequence from the common initialization value is:

`18 -> 12 -> 6 -> 0`

When this branch decrements a positive value, it exits that update branch. It is not a wall-clock duration; actual milliseconds depend on the game update cadence and stalls.

The source has other progress paths:
- direction 6 initializes `jInt = 72` for a distinct object/push path;
- direction 5 explicitly sets `jInt = 0`;
- scripted `xBoolean` displacement has its own `jInt = 18` initialization and decrement path at M06;
- other helper/interaction branches may also write `jInt`.

Do not globally replace all progress writes with a single fixed-duration movement timer.

## 5. Camera interpolation contract

At M08, the camera-follow target is calculated from:

- X: `crtPlayerX * 24 + jInt * gByteArr[b]`
- Y: `crtPlayerY * 24 + jInt * gByteArr[b + 8] + 40`

Here `b` is selected from the current packed/active direction state. The camera then smooths and clamps its own position.

This supports the 24-pixel logical tile unit and proves that the camera uses `jInt` as a direction-vector interpolation term. It does not alone establish the exact sprite origin, collision-box dimensions, or all animation offsets.

## 6. Native implementation requirements

1. Keep pending direction, active direction, logical tile position, interpolation progress, and packed movement flags as separate state.
2. Model ordinary movement, direction-5 handling, direction-6/object-push behavior, special movement (`aTInt > 0`), and scripted `xBoolean` displacement as distinct paths.
3. Apply collision and destination checks before committing a normal logical tile move.
4. Preserve the observed 18-to-12-to-6-to-0 update sequence for the ordinary path only, unless additional source evidence proves another path.
5. Derive camera target from the same simulation snapshot used by rendering; do not let render frame rate advance gameplay progress.
6. Do not infer speed in pixels/second or a guaranteed 150 ms duration from the original nominal loop.
7. Keep animation selection as a separate result of movement/state transitions; do not invent semantic names for opaque animation IDs.
8. Preserve bounds checks and all relevant foreground, object-layer, and packed metadata checks.

## 7. Regression cases required before implementation is considered complete

- Requested direction while movement is already active is not consumed as a second simultaneous tile move.
- Accepted ordinary move commits exactly one logical tile delta and initializes progress to 18.
- Blocked destination does not commit the logical tile delta.
- Progress decrements by 6 per applicable update and reaches zero without becoming negative.
- Progress 72 remains exclusive to the separately traced direction-6/object-push path.
- Direction 5 does not accidentally enter ordinary interpolation.
- Scripted `xBoolean` movement remains separate from ordinary input-driven movement.
- Camera target uses the same logical coordinate, direction vector, and progress snapshot as the movement state.
- Stage-edge and metadata-dependent collision cases do not read outside map bounds.

## 8. Current validation boundary

The current repository has source-level input capture, direction priority, and state-aware input dispatch. Those components do not yet move the player. CI run #11 has passed the core test step for the state-aware dispatcher; Android debug APK assembly is a separate step and must be checked independently.

No player movement implementation, movement runtime test, playable APK, or original-device comparison is claimed by this document.

## 9. Next work item

**DIRU-NA-DEV-003B — Direction Vector and Collision Helper Trace.** Resolve the exact `gByteArr` vectors, accepted/blocked result meanings, helper side effects, and state-bit transitions before writing a native movement state machine.
