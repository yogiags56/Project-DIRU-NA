# DIRU-NA-AUDIT-007 — Entity Animation & State Mapping

## Scope and evidence boundary

This audit follows static control flow in the read-only candidate source repository `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Primary inspected file: `src/main/java/i.java`, especially the stage rendering loop, foreground update paths, and object-specific rendering branches. Animator mechanics are cross-referenced with `b_SpriteAnimator.java` and `f_Sprite.java`.

This is a code-path audit only. It does not decode or redistribute game artwork, execute the game, validate behavior on a Nokia handset, or establish licensing permission. Slot IDs and numeric entity codes are preserved as source identifiers; visual identities are not inferred from them.

## 1. Rendering is split across multiple passes

The inspected stage renderer does not draw every entity in one uniform pass.

1. It visits a viewport window around the camera and reads the player/object layer and foreground layer at each tile.
2. It draws foreground entries with codes below 38 in an early pass, including selected static frames, animated foreground sprites, and temporary per-tile animation state.
3. It handles player/object-layer entities in a separate branch, selecting sprite slots, animation indices, frame indices, and movement offsets based on object code and per-cell state arrays.
4. It later draws another foreground pass for selected codes (20–25, 31, 32, 36 and codes at/above 80), plus an additional high-bit animation overlay.
5. Some entities trigger transient overlay effects or draw special sprites outside the ordinary per-tile sequence.

**Native implication:** preserve explicit render layers/pass ordering. A single tile-to-sprite lookup table cannot reproduce all overlap and foreground occlusion behavior.

## 2. Confirmed foreground animation/render mappings

| Foreground code | Sprite/slot path | State or frame selection observed | Confidence |
|---:|---|---|---|
| 3 | slot 12 | Metadata high byte minus one selects frame when non-negative | Direct code evidence |
| 5, 28 | slot 11 | Frame 0 or 1 selected by world/stage combinations | Direct code evidence |
| 6 | slot 26, frame 0 | Vertical/position offset depends on nearby/player occupancy and movement state; clipping is applied in part of the branch | Direct code evidence |
| 7 | slot 56 | Frame selected from metadata bits 4–7, adjusted to a non-negative value; upper neighbor water codes 8/9 suppress drawing | Direct code evidence |
| 8, 9 | texture indices 23, 22 respectively | Frame/texture index chosen from metadata bits 0x100 and 0x200; code 9 uses texture index 22 and code 8 index 23 in this branch | Direct code evidence; visual meaning remains unresolved |
| 14, 33 | slots 8, 22 | Animation 0; metadata high byte selects animation frame, with 255 normalized to 0 | Direct code evidence |
| 15 | slot 14 via texture/frame selection | Uses `ceInt` to select a frame during a bounded state window | Direct code evidence |
| 16 | slot 15 via texture/frame selection | Uses `ceInt` to select a frame; values outside a defined range suppress this branch | Direct code evidence |
| 20–25 | slot 3 | Animation index = foreground code − 20; frame = global tick-derived value modulo that animation's frame count | Direct code evidence |
| 31 | slot 15 | Animation index from metadata high byte; frame from global tick modulo selected animation length | Direct code evidence |
| 32 | slot 16 | Animation 0; frame from metadata high byte | Direct code evidence |
| 34, 37 | slot 27 | Animation 2, frame 0 | Direct code evidence |
| 36 | slot 20 | Animation 0; frame derived from metadata using integer arithmetic | Direct code evidence |

The high nibble of the 32-bit foreground cell value is also used as an independent animation overlay counter in the later rendering pass. The update path increments it on selected global ticks, compares it against animation 0's frame count in slot 13, and clears the high-nibble field when the animation completes. This overlay must not be confused with the low-byte foreground code or the middle metadata bits.

## 3. Confirmed player/object-layer animation selections

The renderer uses per-cell state arrays in addition to the object code. The following are selected code paths, not a complete entity catalog.

| Object code | Sprite slot | Animation/frame behavior observed | Additional state |
|---:|---:|---|---|
| 49 | slot 38 | Chooses animation index from direction bits; animation frame is global tick modulo that animation's frame count | Cell flags and progress bytes determine orientation/offset; calls an entity update helper |
| 48 | slot 32 | Chooses animation index 0, 1, or 2 from direction/orientation bits | Movement progress byte offsets sprite along direction vector; an additional state bit gates rendering |
| 21 | slot 29 | Chooses animation from direction and a state bit; one branch uses metadata and per-cell progress to select a frame | Direction cases include distinct animation indices and offsets |
| 54 | slot 7 | Animation 0; frame derived from per-cell integer state through helper `aInt(...)` | When its state reaches the animation frame count, object layer cell is cleared; midpoint triggers sound and neighboring-cell interactions |
| 16, 19, 43, 49 | Not one common sprite mapping in the inspected interaction code | Some are removed/cleared by a neighboring interaction branch | State/foreground effects vary by context |
| 30, 37 | Not established as one sprite mapping by the inspected excerpts | A neighboring interaction branch changes state for these codes | Needs dedicated state tracing |

For code 49, the branch uses direction bits and a movement progress value to calculate X/Y offsets from a direction vector table. Depending on the occupancy pattern and state flags, it selects different animation indices or a frame derived from cell metadata.

For code 48, the branch selects a sprite from slot 32 and uses the low three direction bits plus a state flag to select one of three animation indices. The per-cell progress byte is multiplied by direction-vector components to place the sprite between tile positions.

For code 54, a counter in the per-cell integer array advances through slot 7's animation 0. At the first step and midpoint the source triggers sounds and checks neighboring object codes; once the count reaches the animation frame count, the cell is cleared. The code establishes a timed multi-step entity behavior, but does not establish its visual identity.

## 4. Foreground state transitions that advance animation

A helper that updates foreground codes 14 and 33 writes a new metadata frame index when the associated per-cell timer reaches zero. It chooses slot 8 for code 14 and slot 22 for code 33, checks animation 0's frame count, and advances metadata while the current index is below the last frame. A separate condition checks player occupancy and elapsed time to trigger a state change and sound.

This is evidence that some foreground animation frames are stored in the stage foreground cell's metadata, rather than solely in a global animator object.

The high-nibble overlay mechanism described above is another independent animation state channel. The Native implementation should model both channels explicitly.

## 5. Native animation/state architecture requirements

- Represent entity state independently from its rendering command. State transitions should choose a semantic animation, then a renderer resolves it to frames.
- Preserve render pass ordering and foreground occlusion; do not draw all layers in arbitrary entity order.
- Keep per-cell animation metadata, per-cell timers, global tick-driven animation, and actor animator timelines distinct.
- Preserve source update cadence and frame-duration semantics. Android display refresh rate must not change simulation state.
- Treat source slot numbers as audit aliases only. Introduce semantic identifiers after visual fixtures are legally available and positively identified.
- Model object movement offsets from the same logical tile coordinate system and direction vectors used by gameplay.
- Ensure animation completion side effects (clearing an entity, advancing a state, sound triggers) are owned by gameplay/state logic, not by the drawing routine.
- Add deterministic tests for direction selection, frame progression, completion, timer pause, overlay reset, and render ordering.

## 6. Open work

- Trace complete hero animation state selection and connect it to movement, damage, idle, and interaction states.
- Trace all enemy codes, enemy AI state transitions, and their animation selection.
- Trace object codes 0–79 to completion, including hazards, collectibles, switches, movable objects, and multi-cell structures.
- Identify the semantic meaning of animation indices only with permitted visual fixtures or independent original-package evidence.
- Confirm every per-cell state array's meaning and reset/restore behavior.
- Create synthetic deterministic tests for all observed animation selection branches.
- Validate render-pass order and occlusion against an original runtime/handset when available.
- Resolve rights and provenance before any original artwork or decompiled source is included in a distributable build.

## 7. Audit status

**Completed at static-source level:** multi-pass rendering structure; selected foreground code-to-slot/animation mappings; selected object code-to-slot/animation mappings; examples of per-cell animation progression and completion side effects.

**Not completed:** full entity catalog, hero/enemy state machines, visual identity, all animation indices, synthetic test fixtures, runtime comparison, and licensing clearance.

No Android project build, APK, or runtime test was performed for this audit.
