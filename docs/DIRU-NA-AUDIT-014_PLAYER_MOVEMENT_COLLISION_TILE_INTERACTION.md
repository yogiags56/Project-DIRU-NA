# DIRU-NA-AUDIT-014 — Player Movement, Collision & Tile Interaction Reconstruction

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source: `src/main/java/i.java`, especially the gameplay input dispatcher, movement update path, player collision/move helper calls, interaction-key branches, entity interaction code, and camera-follow method.

This is a partial static-source reconstruction. It records only behaviors visible in inspected source ranges. The decompiled names are not semantic API names. It does not establish a complete collision matrix, complete tile-code meanings, or visual animation labels. No runtime/emulator test was performed and no candidate assets were copied.

## 1. Direction and logical movement model

The gameplay input dispatcher translates the shared key bitmask into a requested direction stored in `aByte`:

| Direction | Input | Stored value |
|---|---|---:|
| Up | `KEY_UP` | 1 |
| Right | `KEY_RIGHT` | 2 |
| Down | `KEY_DOWN` | 3 |
| Left | `KEY_LEFT` | 4 |

The movement update later transfers the requested direction into `lByte` when the player is eligible to begin a move. Movement is suppressed while certain state bits are active, while scripted/interaction states are running, or while selected hero animation states are active.

The source uses direction-vector arrays `gByteArr` for X and Y movement. The logical player location is stored in tile coordinates `crtPlayerX` and `crtPlayerY`; the movement progress value `jInt` is used in pixel-space calculations at 24 pixels per tile.

## 2. Movement progression and interpolation

The inspected movement path initializes `jInt` to 18 for a movement start in selected branches and uses animation/state conditions to advance or complete movement. The camera-follow method calculates the player anchor from:

- `crtPlayerX * 24 + jInt * directionX`
- `crtPlayerY * 24 + jInt * directionY + 40`

This demonstrates that tile position and sub-tile movement progress are separate. The source also checks the current movement direction and movement state before accepting another input.

The full `jInt` progression, all move-start/complete branches, and timing relationship to animator frames have not yet been exhaustively traced. Do not assume a fixed velocity or frame duration from the observed initial value alone.

## 3. Collision and movement feasibility

The source calls a movement/collision helper with the proposed neighboring tile and current direction. The helper evaluates player/object layer, foreground state, background-derived metadata, and current player state. The surrounding movement code also checks whether the player remains within stage bounds.

Observed collision behavior includes:

- A move can be rejected based on occupied object-layer cells or special foreground states.
- Some occupied cells are treated as interactable or traversable depending on object code and packed metadata.
- Door foreground code 7 has special conditions; a door state/metadata nibble can block or permit passage.
- Object code 48 has special metadata checks during movement/path evaluation.
- Out-of-bounds movement is guarded by stage-coordinate checks in the inspected paths.

This is not a complete tile collision table. The same object code can have state-dependent behavior; Native collision must not be reduced to a simple static solid/non-solid property.

## 4. Action/confirm interactions

The confirm key (`KEY_OK`) is separate from directional movement. Observed behavior includes:

- If the player is at the active checkpoint tile and the foreground code is 4, confirm plays the checkpoint sound and restores the checkpoint.
- Otherwise, confirm can trigger special interactions based on the current tile and progression byte 9.
- Higher progression tiers scan adjacent cells and evaluate object types, metadata, and direction to select an interaction.
- The inspected code contains special handling for multiple object-layer codes, including 9, 18, 30, 46, 49, 50, 19, 43, and 45.
- Some interaction branches set state bits, advance object states, remove or transform entities, trigger sound/vibration, and request hero animation changes.

The source therefore treats confirm as a context-sensitive action, not a universal “use” operation on every adjacent tile.

## 5. Interaction with moving entities and hazards

The inspected movement/update region includes entity-specific behavior:

- Certain object-layer entities are moved or transformed based on direction and adjacent free cells.
- Some interactions use animation frame/timer conditions before applying world changes.
- Object code 9 and related states can cause a special interaction/transition path.
- Object codes 18 and 30 have separate interaction handling.
- Codes 19, 43, 45, 46, and 49 appear in adjacent-cell resolution logic with metadata-dependent behavior.
- Failed or dangerous interactions can trigger vibration, sound, and a direction-dependent hero animation request.

These examples confirm that collision, interaction, and entity simulation are coupled through gameplay state. A full per-code behavior matrix remains open.

## 6. Camera synchronization

The normal camera-follow routine uses the logical tile coordinate plus the sub-tile movement progress `jInt`. It follows the interpolated player anchor, then clamps the camera to stage bounds. This should be updated from the deterministic simulation tick so the camera follows movement consistently.

Camera movement should not be implemented as an independent approximation based only on the final tile coordinate; doing so would lose the source's sub-tile tracking behavior.

## 7. Native Android implementation requirements

- Use one direction/action input abstraction for touchscreen and physical controls.
- Separate requested direction, committed movement direction, tile coordinate, and sub-tile progress.
- Implement movement as deterministic simulation state, independent of render frame rate.
- Build collision queries from all relevant layers and metadata, not only a foreground tile ID.
- Model context-sensitive confirm interactions as explicit interaction handlers.
- Keep object state transitions and side effects (sound, vibration, item removal, tile mutation) in gameplay logic.
- Preserve 24-pixel logical tile coordinates for the initial 240×320 build.
- Keep camera follow tied to the interpolated player anchor.
- Add bounds checks before all neighbor reads; use safe map-edge behavior rather than relying on source array assumptions.
- Keep unknown object codes as explicit unresolved cases; do not invent collision semantics.

## 8. Open work

- Trace the complete movement helper and all return/side-effect branches.
- Map every `jInt` update and determine movement timing and completion.
- Build a full collision matrix for foreground, player/object layer, and packed metadata.
- Trace object pushing, climbing, swimming, door, ladder, and hazard interactions separately.
- Complete the confirm/action behavior across progression tiers.
- Extract all direction-vector and key-code mappings, including diagonal/automatic movement if any.
- Add deterministic tests for blocked moves, valid moves, edge bounds, state-dependent doors, moving entities, and touch/key parity.
- Validate movement and collision behavior against original runtime evidence.

## 9. Audit status

**Completed at static-source level:** directional input values; separation of tile coordinates and movement progress; use of direction vectors; movement eligibility gates; examples of metadata-dependent collision and confirm interactions; camera tracking of sub-tile movement.

**Not completed:** full movement timing, full collision matrix, complete entity interaction semantics, complete action mapping, synthetic tests, runtime validation, and licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
