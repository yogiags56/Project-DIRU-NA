# DIRU-NA-AUDIT-008 — Hero Animation & Movement State Reconstruction

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source: `src/main/java/i.java` (hero loading, hero animation selector, nearby-object rendering/update, and player interaction/damage call sites), plus `b_SpriteAnimator.java` for animator behavior.

This is static code analysis only. The numeric animation indices are retained exactly as observed. The source does not provide a reliable semantic name for every index, so this document does not invent labels such as idle/run/jump unless the code path itself establishes that behavior. No artwork was decoded, no runtime was launched, and no handset comparison was performed.

## 1. Hero sprite loading and palette

The candidate loads the hero sprite from resource `/o.f`, chunk 0. It creates a sprite object, parses the chunk, and chooses the palette index using:

`crtHeroPalette = i.iByteArr[8] - 4`

It then creates images for that palette, sets the sprite's active palette, releases the encoded module data, and installs it into `i.aClassbArr[0]` as a `b_SpriteAnimator`.

When the caller requests the additional chunk, `/o.f` chunk 1 is parsed as a separate fire animation and installed into animator slot 3 with animation 0.

Important boundary: save byte 8 is used as a palette selector offset here. This does not establish that byte 8 is health, lives, or a directly named hero statistic.

## 2. Central hero animation selector

The inspected method `gVoid(int n)` is the central animation-selection path for animator slot 0. It reads the current animation index from `i.aClassbArr[0].crtAnimationI`, applies state and environment conditions, and finally calls `setAnimation(n)` on the hero animator.

The method does not set a frame directly; `setAnimation` resets the animator frame and elapsed time when the selected animation index changes. If the animation index is unchanged, the animator is not reset.

### 2.1 Environment- and movement-dependent overrides

The selector can replace the requested animation index based on the player's current tile and surrounding state:

- It checks whether the player is within stage bounds before examining the foreground/object context.
- A special condition involving `eIntArrArr` and the current tile can map a subset of requested state values (0–9, 11, 24–27) to an animation index calculated from the low three bits of `kInt`: `36 + (kInt & 0x7) - 1`.
- Otherwise, when the tile below the player is empty or has object code 14, a switch maps selected incoming state values to alternate indices:
  - 14 → 28
  - 16 → 29
  - 42 → 46
  - 44 → 45
  - 1 → 35
  - 3 → 34
  - 0 → 0
  - 2 → 2
  - 22 → 31
  - 20 → 30
  - 23 → 33
  - 21 → 32
- Other values pass through this mapping branch unchanged.

These are numeric selector transitions. The source excerpt does not justify assigning a universal human-readable animation name to each index.

### 2.2 Special animation index 1000

If the final selected value is 1000, the method sets animator slot 3 to animation 0 and sets bit `0x4000` in `kInt`. It does not set the hero animator to index 1000.

For other values, it calls `i.aClassbArr[0].setAnimation(n)`.

When the selected index differs from the previous hero animation index, the method sets `axInt = 70`. The full downstream meaning of this timer/field is not established here.

## 3. Direction and movement state are coupled to sprite placement

The stage renderer and entity drawing branches use the player's logical tile position and pixel interpolation separately. The code uses a 24-pixel tile unit and camera offsets. For moving entities, direction-vector components and per-cell progress values are used to calculate sub-tile X/Y offsets.

The inspected object rendering paths demonstrate the general pattern: a state/flags word selects direction or orientation, a per-cell progress byte determines movement offset, and a sprite animation/frame is selected independently. This is a useful design clue for the hero renderer, but it is not proof that every hero animation uses the same exact offset formula.

## 4. Nearby animated entities and update context

The hero-related update method `ahVoid()` scans a bounded region around the player's current tile. It updates timed foreground/object effects and handles interactions. Examples in the inspected source include:

- Foreground codes 32 and 36 advance metadata-backed animation counters and may be removed after reaching a frame limit.
- Foreground codes 35 and 37 have movement/transfer behavior involving neighboring cells.
- Foreground code 26 can trigger a door interaction when the player occupies the tile.
- Foreground codes 14 and 33 route through a helper that advances their animation metadata.
- Object code 54 has a separate timed update path that advances a counter, can trigger sound, checks neighboring entities, and clears the cell when its animation-duration threshold is reached.

These are environment/entity updates occurring in the player's nearby simulation region. They should not be conflated with the hero's own `b_SpriteAnimator` timeline.

## 5. Damage and respawn boundary

The inspected source has a `hurtHero(...)` call path and a dedicated hero state selector that can receive alternate numeric state values. However, this audit has not yet completed a full call graph from every damage source through the damage handler, death/retry state, checkpoint restore, and the corresponding hero animation indices.

Therefore this document does **not** claim a definitive damage, death, falling, jump, or respawn animation-index map. That mapping remains an explicit follow-up.

## 6. Native Android state-machine requirements

- Keep the hero's gameplay state separate from the selected visual animation ID.
- Preserve the rule that changing animation resets its frame/timer, while selecting the same animation does not restart it.
- Keep special auxiliary animator slot 3 separate from the hero animator slot 0.
- Preserve the 24-unit logical tile model and camera-relative drawing coordinates.
- Treat animation indices as opaque source IDs until legally usable visual fixtures identify their appearance.
- Ensure environment-dependent animation overrides are deterministic and tested with controlled player tile, neighboring tile, and state-bit inputs.
- Run simulation ticks independently from Android frame callbacks.
- Keep damage, movement, and checkpoint state transitions in gameplay logic; rendering must not own side effects such as damage, sound, or entity removal.

## 7. Open work

- Trace every call site of `gVoid(int)` and name the incoming state values only where caller context proves their meaning.
- Complete the hero movement state map: stationary, directional movement, blocked movement, falling, climbing/transition, and scripted movement if present.
- Trace the complete `hurtHero` → death/retry → checkpoint restore call graph and map any animation changes.
- Identify the exact draw call and lifecycle for hero animator slot 0, including animation ticking and offsets.
- Verify animation indices against legally usable visual fixtures or independently sourced original-package evidence.
- Create synthetic state-transition tests for the environment-dependent override table and the special index-1000 branch.
- Compare movement/animation synchronization against original runtime evidence.

## 8. Audit status

**Completed at static-source level:** hero resource/chunk/palette loading; central hero animation selector; observed environment-dependent index overrides; special auxiliary animator branch; separation between hero animator and nearby entity update logic.

**Not completed:** semantic names for animation indices, complete caller/state map, full damage/death/retry animation path, complete hero rendering/tick path, visual confirmation, synthetic tests, runtime comparison, and licensing clearance.

No Android project build, APK, or runtime test was performed for this audit.
