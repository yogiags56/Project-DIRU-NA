# DIRU-NA-AUDIT-004 — Gameplay State Machine and Movement Reconstruction

**Status:** In progress — first static gameplay pass  
**Audit input:** `kubikaugustyn/DiamondRushSource`  
**Reviewed source revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Primary source:** `src/main/java/i.java`  
**Related source:** `src/main/java/b_SpriteAnimator.java`  
**Development target:** `yogiags56/Project-DIRU-NA` only  
**Scope:** input handling, player grid movement, animation coordination, collision/interaction predicates, damage, checkpoint, respawn, and game-loop timing.

> This is a static decompilation analysis, not a verified behavioral specification. Decompiled identifiers are often opaque; field and tile meanings are named only where call sites provide sufficient evidence. No game runtime, original handset, APK, or automated gameplay test was used.

## 1. Execution loop and input capture

### 1.1 Main loop

The `run()` method contains the main MIDlet game loop.

Observed behavior:

- Initializes timing and reads the separate `Preferences` RMS store.
- Repeats until the game termination flag is set.
- Calls `handleKeyPresses()` to process current input and update game state.
- Calls `repaint()` during the normal loop.
- Targets approximately 50 ms per iteration using `sleep(50 - elapsed)`.
- If a loop iteration exceeds 65 ms, it sets a timing-related flag and skips the ordinary repaint/sleep path for that iteration.
- The loop catches exceptions from input/state processing, prints the exception, and waits 5 seconds before continuing.

**Interpretation:** nominal scheduling is approximately 20 updates per second, but this is not a guaranteed fixed timestep. A native port should use a monotonic clock and explicit elapsed-time policy rather than reproduce the original sleep loop literally. Animation and gameplay timing must be separated and validated.

### 1.2 Key-state model

The game stores current key state in a shared bitmask, `keysPressed`.

- `keyPressed(keyCode)` converts the device key code to a game bit and ORs it into the mask.
- `keyReleased(keyCode)` clears the corresponding bit.
- `isKeyPressed(mask)` checks whether any requested bit is currently held.
- The handler clears key state in several modal/gameplay transitions and when input must be suppressed.

The source supports simultaneous held bits at the capture layer, but many gameplay decisions use ordered `if / else if` checks. Do not assume that diagonal movement is supported merely because multiple key bits can coexist.

### 1.3 Direction mapping

During normal in-stage control (`bByte == 1`), the source maps key combinations to a pending direction byte:

| Logical action | Source mask | Pending direction |
|---|---:|---:|
| Up | 4097 | 1 |
| Right | 65544 | 2 |
| Down | 262146 | 3 |
| Left | 16388 | 4 |

The mapping is confirmed by the key handler at approximately lines 1258–1265. These are combined masks for D-pad and numeric keypad alternatives, not raw Java ME key codes.

The pending direction is consumed later by the gameplay update path. The direction values are not themselves pixel velocities.

### 1.4 Other in-stage controls

| Input | Observed behavior |
|---|---|
| Center / numeric 5 (mask 32784) | Context action; if standing on a checkpoint tile at the stored checkpoint position, restores the checkpoint snapshot |
| Broad confirm mask (32944) | Opens/enters the contextual action path; in normal stage control it invokes `aeVoid()` when no direction/other action takes precedence |
| Star (256) | Manual respawn/death action path; on a checkpoint tile it restores the checkpoint, otherwise it plays death sound and requests death animation |
| Direction during special movement state | Adjusts special movement offsets; it is not processed as ordinary tile movement |
| Right soft key (64) | Used for cancel/menu paths in other game states; not the ordinary in-stage directional control |

The full semantic meaning of the broad confirm mask and every menu-state key mapping remains outside this pass.

## 2. Player coordinate and movement model

### 2.1 Grid coordinates

The player position is stored as integer tile coordinates:

- `crtPlayerX`
- `crtPlayerY`

The source uses a 24-pixel tile unit in the rendering/camera calculations. For example, the camera-follow path calculates player pixel position using `crtPlayerX * 24` and `crtPlayerY * 24`.

This is strong evidence of a tile-based logical world with 24-pixel cells in the candidate implementation. It does not independently prove the physical resolution or orientation of the original Nokia handset.

### 2.2 Movement interpolation

The source separates logical tile position from a short movement/interpolation timer:

- `jInt` is set to 18 when a movement step begins.
- In the ordinary update path, it is reduced by 6 on successive updates until it reaches zero.
- The player/camera pixel position includes a term derived from `jInt` and the direction vector.
- Movement animation is selected through `gVoid(animationIndex)`.

This suggests a three-update interpolation window at the nominal 50 ms loop rate, but that timing should be treated as an observed implementation pattern, not a guaranteed 150 ms movement duration. Delayed frames and state gates can alter the actual wall-clock duration.

### 2.3 Direction vectors and animation

The source uses direction-indexed arrays `gByteArr` and `gByteArr[direction + 8]` to derive coordinate offsets. It also combines direction into the low three bits of `kInt`.

The `gVoid(int)` helper selects the player animator's animation index, with additional substitutions based on:
- current animation,
- whether the player is within stage bounds,
- special tile/interaction state,
- whether a tile below the player permits a particular transition.

The helper includes special animation index 19 and a sentinel request 1000 that resets another animator and sets a state flag. The exact semantic label for every animation index is not assigned in this audit.

**Native implication:** implement logical direction, tile destination, movement phase, and animation selection as separate state. Avoid embedding game rules directly into drawing code.

## 3. Movement state gates

Ordinary directional input is not always accepted. The input handler clears or suppresses input when conditions include:

- an active cutscene/interaction object,
- a door/transition timer,
- a scripted movement or out-of-stage transition,
- a zeroed/disabled player status counter,
- death animation state 19,
- another special action/state flag,
- a special temporary movement mode.

The main update path also checks animation and action flags before accepting a new movement direction. A native state machine should model these as explicit gates rather than relying on the original opaque bitmask.

## 4. Collision and interaction checks

The helper `aBoolean(dx, dy, probeOnly)` evaluates a candidate neighboring cell and may update gameplay state when `probeOnly` is false. It is not a simple boolean wall test.

### 4.1 Bounds

The player is considered within the ordinary stage interior when:

`0 < crtPlayerX < stageWidth - 1` and `0 < crtPlayerY < stageHeight - 1`.

The collision helper also checks candidate destination coordinates against the full array bounds before accessing tile layers.

### 4.2 Foreground tile checks

The helper inspects the low byte of the foreground cell and, in some cases, its upper metadata bits.

Observed special cases:

| Foreground low-byte value | Evidence-based behavior |
|---:|---|
| 4 | Checkpoint. Crossing a checkpoint with a sufficiently new checkpoint index sets `shouldSaveCheckpoint`; snapshot capture is deferred until the movement timer reaches zero |
| 5 | Sets a scripted/out-of-stage transition flag and a direction-related flag |
| 28 | Sets the same broad scripted-transition flag, with a different direction mode |
| 1 | In the non-probe path, updates a timer/counter and clears connected foreground cells of the same value |
| 2 | Reads upper metadata and progression tier; can set a gate/progression state |
| 3 | Uses upper metadata/progression tier to decide whether the candidate cell blocks movement or activates a special state |
| 19 | Enters a special handling path that searches for a related entity and changes level state; exact world-object semantics remain unresolved |

Do not treat this as a complete foreground-tile dictionary. Several values have context-dependent meanings and upper metadata bits matter.

### 4.3 Player/object layer checks

The helper also checks the player/object layer. Static switch cases distinguish groups of values that block movement, permit movement, or trigger special handling.

Observed examples:

- Values `0`, `8`, `9`, and `47` enter a passable/special interaction path.
- Values `31` and `49` are handled as non-blocking in this branch.
- Many other values (including `1`, `2`, `4`–`7`, `11`, `14`, `19`, `24`, `26`, `27`, `33`, `40)–`43`, and `45`) set a blocking result in this particular branch.
- Values `34` and `35` depend on whether the helper is in probe-only mode.
- Values `0`, `8`, `9`, and `47` may initiate a push/interaction path by examining the cell beyond the target.

These are local branch outcomes, not universal semantic labels for every occurrence of those values.

### 4.4 Pushable entity behavior

The movement helper has explicit branches for object-layer values `0`, `8`, `9`, `47`, and `48).

- The `0/8/9/47` branch may move the encountered object to a cell two grid positions away, update its state arrays, and trigger a corresponding animation/interaction.
- The `48` branch checks a state flag (`0x8`) and two destination cells, then moves a two-cell footprint before updating its metadata and animation timer.
- Both paths require bounds/occupancy checks and are suppressed during probe-only evaluation.

The exact in-game identity of every object code is not fully established here. The code-48 entity should be documented as a special two-cell movable entity until resource-to-visual correlation confirms its identity.

### 4.5 Collision outcomes

When a movement attempt is accepted, the update path:
- updates the logical player tile coordinates,
- sets the movement interpolation timer,
- updates direction/state bits,
- selects a movement animation.

When blocked, it may still update facing direction or select a blocked/interaction animation. This means “input accepted” and “player position changed” are separate outcomes.

## 5. Damage and death flow

### 5.1 Damage entry point

`hurtHero(amount, stateMask, knockbackDirection)` is the central observed damage handler.

Observed behavior:

- Rejects repeated damage while certain hurt/death animations or invulnerability/action states are active.
- Updates a counter and modifies the player's status value through a clamping helper.
- Applies a bitmask to the player state.
- Plays the hero-hurt sound.
- Selects hurt/death-related animations.
- For selected damage types, may place a temporary marker into the player/object layer and set a short special movement timer.
- If a knockback direction is supplied, searches candidate neighboring cells and moves the player into a free cell when one is found.

The field `nByte` is bounded against `iByteArr[8]`; its exact user-facing meaning is not conclusively identified in this pass. Do not name it health, lives, or stamina until all initialization, UI rendering, save/load, and decrement/increment call sites are correlated.

### 5.2 Manual respawn

The star key (mask 256) enters a manual respawn path:

- If the player is on a checkpoint foreground cell (low-byte value 4), restore the in-memory checkpoint.
- Otherwise, play the death sound and request the death animation.
- A related branch handles specific death/fall animations and follows the same checkpoint-versus-death distinction.

### 5.3 Automatic death / out-of-stage handling

The source contains a separate path for a player marked as being in a scripted/out-of-stage state. When coordinates move well outside the stage bounds, it selects different game-state transitions depending on flags and stage progression, then resets related state.

This path must be correlated with actual level scripts and cannot be reduced to a generic “fall off map = lose life” rule.

### 5.4 Retry/checkpoint transition

`XVoid()` decrements `azInt` and increments another counter. If `azInt` remains nonnegative, it restores the checkpoint and resets several temporary movement fields. Otherwise it switches to another game state.

Although this resembles a retry/continuation budget, the exact UI-facing meaning of `azInt` must be verified from all initialization and display call sites before assigning a user-facing label.

## 6. Checkpoint behavior

### 6.1 Activation

When the collision helper encounters a foreground cell with low-byte value 4 in a non-probe movement path, it checks the checkpoint metadata against the current checkpoint index.

If the checkpoint is new:
- sets `shouldSaveCheckpoint`,
- increments the checkpoint index,
- stores a short delay/tick marker.

The main update path waits until the movement interpolation timer is no longer active, plays the checkpoint sound, and then calls `saveCheckpoint()`.

### 6.2 Snapshot contents

The snapshot includes:

- player tile coordinates,
- selected player/gameplay counters and progression-related values,
- active stage player/object layer,
- background and foreground layers,
- additional stage state arrays,
- optional mode-specific arrays and moving-object state,
- selected timer/animation and interaction state.

The exact semantics of every copied field remain partially unresolved, but the copied arrays show that checkpoint restore is broader than simply moving the player back to a coordinate.

### 6.3 Restore behavior

`restoreCheckpoint()` resets transient interaction fields, restores the saved player coordinates and selected state, resets player animation/state, restores the stage arrays, and recenters camera-related coordinates.

As established in AUDIT-002, these checkpoint snapshots are held in memory. They are not serialized into RMS by `saveCheckpoint()` / `restoreCheckpoint()`. Do not confuse temporary checkpoint restoration with durable save-game persistence.

## 7. Camera follow

The camera-follow helper uses the player's grid coordinate multiplied by 24, adds interpolation offsets, and adjusts the camera toward the player with threshold regions. It clamps camera position to the stage's pixel extents.

The camera logic is separate from the player tile state. For DIRU-NA, preserve this separation so that later adaptive viewport support does not alter collision geometry or logical tile coordinates.

## 8. Initial native state-machine proposal

This is an implementation-facing abstraction derived from the observed code, not a claim that the original game uses these exact named states.

```text
READY
  ├─ directional input → MOVE_REQUEST
  │    ├─ destination valid → MOVE_INTERPOLATING
  │    ├─ destination blocked → FACE_OR_INTERACT
  │    └─ special tile/entity → SCRIPTED_INTERACTION
  ├─ confirm input → CONTEXT_ACTION
  └─ star input → RESPAWN_REQUEST

MOVE_INTERPOLATING
  ├─ movement timer completes → SETTLE_TILE
  │    ├─ checkpoint activated → CHECKPOINT_CAPTURE_PENDING
  │    ├─ hazard/damage → HURT_OR_KNOCKBACK
  │    └─ ordinary tile → READY
  └─ scripted transition → SCRIPTED_INTERACTION

CHECKPOINT_CAPTURE_PENDING
  └─ movement settles → SNAPSHOT_IN_MEMORY → READY

RESPAWN_REQUEST
  ├─ current tile is checkpoint → RESTORE_CHECKPOINT
  └─ otherwise → DEATH_ANIMATION → RETRY_OR_GAME_OVER

RESTORE_CHECKPOINT
  └─ restore stage snapshot and player state → READY
```

A production native state machine must include explicit cancellation, app pause/resume, focus loss, and safe input reset behavior.

## 9. Native implementation requirements

1. Use integer tile coordinates as authoritative gameplay position; do not derive collision from rendered pixel coordinates.
2. Keep the observed 24-pixel tile unit in the compatibility model, but make it a logical unit rather than a hard-coded physical screen size.
3. Model movement as a stateful tile transition with a separate interpolation timer and animation selection.
4. Centralize tile/object collision and interaction rules in testable functions.
5. Preserve probe-only collision evaluation as a side-effect-free operation. The original helper has side effects in the non-probe path; the native implementation should make that distinction explicit.
6. Represent object footprints and dynamic occupancy, including the special two-cell entity path, rather than treating every object as a single solid tile.
7. Separate damage/status counters from UI labels until their semantics are proven.
8. Keep checkpoint snapshots in memory and durable save-game data as separate systems.
9. Use a fixed or bounded timestep with deterministic simulation; do not port the original sleep-based timing literally.
10. Keep input capture independent of Android touch controls. Touch and physical keys must feed the same logical action layer.
11. Do not embed MIDP, AWT, FreeJ2ME, or Nokia API compatibility stubs in the Android runtime.
12. Do not copy candidate source/assets into the active project until provenance and licensing review permits the intended use.

## 10. Open questions for the next audit pass

- Trace every write/read of `nByte`, `iByteArr[8]`, `azInt`, and related UI rendering to identify the player status/retry semantics.
- Decode the complete direction vector tables and correlate each animation index with sprite animation names/visual frames.
- Trace all foreground and object-layer tile codes against map/resource fixtures to build a semantic tile dictionary.
- Determine which collision branches are probe-only and identify every call site that uses each mode.
- Trace all call sites of `hurtHero()`, including enemy, trap, water, falling-object, and scripted damage.
- Correlate scripted/out-of-stage transitions with actual stage boundaries and map exits.
- Validate checkpoint capture/restore using legally usable test fixtures and deterministic snapshots.
- Determine whether the game loop's 50 ms target corresponds to animation ticks, simulation ticks, or both.
- Verify the exact Nokia handset orientation/display profile from original package or handset evidence; this audit does not change the project's separately approved 240×320 portrait development baseline.

## 11. Validation status

| Area | Static finding | Remaining work |
|---|---|---|
| Main loop | Approximately 50 ms target; 65 ms slow-loop branch | Runtime timing validation |
| Input | Bitmask capture and four-direction mapping documented | Full key map and simultaneous-input behavior |
| Grid movement | Integer tile coordinates, 24-pixel unit, interpolation timer | Deterministic test fixtures and edge cases |
| Animation | Direction/state-based animation selection observed | Full animation dictionary and visual correlation |
| Collision | Bounds, foreground and object-layer branch behavior recorded | Complete semantic tile dictionary |
| Push interactions | Two movement/push branches identified, including code 48 footprint | Validate against visual/resource fixtures |
| Damage | Central damage handler and knockback behavior identified | Status counter meaning and all damage sources |
| Death/respawn | Manual star path, checkpoint restore, retry transition observed | Full retry/game-over semantics |
| Checkpoint | Deferred capture and broad in-memory snapshot confirmed | Snapshot fixture tests |
| Camera | Grid-to-pixel follow and stage clamping observed | Visual comparison |
| Android implementation | Not started in this audit | Separate DEV milestone |

No Android project build, APK, runtime test, original handset test, or extracted proprietary asset was produced by this audit. AUDIT-004 remains **in progress**.
