# DIRU-NA-AUDIT-013 — Player Spawn, Camera System & Movement Initialization

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source: `src/main/java/i.java`, including stage preprocessing for player-start marker 79, gameplay input dispatch, camera-follow update `aVoid()`, camera transition interpolation `gBoolean()`, and map/world coordinate calculations.

This is static-source analysis only. It does not validate behavior in an emulator or on an original Nokia handset. Decompiled field names remain opaque unless behavior is directly demonstrated. No assets or source files were copied.

## 1. Player spawn marker and coordinate model

During stage preprocessing, player/object-layer code 79 is treated as the player-start marker. For a marker at tile coordinate `(x, y)`, the source:

- sets `crtPlayerX = 0`;
- sets `crtPlayerY = y`;
- stores `ayInt = x`;
- clears the marker cell in the player/object layer by writing `-1`;
- initializes camera target/current offsets for X to 0;
- initializes camera target/current Y offset to `y * 24 - 160`.

This is an unusual but explicit source behavior: the marker's X coordinate is retained in `ayInt`, while the logical hero X begins at tile 0. The audit does not rename `ayInt` as a spawn offset or scripted-path variable without tracing all of its later uses.

The game uses a 24-pixel tile unit in the inspected movement, camera, and rendering calculations. Logical player coordinates are tile-based integers; sub-tile movement is represented separately through `jInt` and direction vectors.

## 2. Camera coordinate system

The gameplay viewport is 240×320 in the inspected source. Camera offsets are pixel coordinates:

- horizontal camera maximum: `stageWidth * 24 - 240`;
- vertical camera maximum in the camera-follow method: `stageHeight * 24 - 240`;
- a separate camera-transition clamp uses `stageHeight * 24 - 320 + 80`, algebraically the same vertical maximum.

The source uses two pairs of offsets:
- `cInt` / `dInt`: camera-follow/intermediate offsets used by the smooth-follow routine;
- `aInt` / `bInt`: rendered camera offsets.

The player anchor is calculated from tile coordinates and movement interpolation:
- X anchor: `crtPlayerX * 24 + jInt * directionX`;
- Y anchor: `crtPlayerY * 24 + jInt * directionY + 40`.

The camera-follow routine uses horizontal thresholds around 96 and 120 pixels relative to the viewport origin, and vertical thresholds around 96 and 160 pixels. It adjusts the intermediate camera offsets gradually, clamps them to stage bounds, then copies them to the rendered offsets.

## 3. Camera transition interpolation

The camera transition helper `gBoolean()` moves `aInt` and `bInt` toward target coordinates `ciInt` and `cjInt` by `ckInt) per update. It clamps the result to stage bounds.

When both axes reach their target or a boundary, it synchronizes `cInt`/`dInt` with `aInt`/`bInt`, resets `axInt` to 70, and reports completion.

The inspected `agVoid()` state machine uses this helper for a multi-step camera/interaction transition. It sets a camera target based on the current player tile (`playerTile * 24 - 108` on both axes) and changes interpolation speed in different sub-states. This is separate from the normal camera-follow routine.

## 4. Movement and input abstraction

The MIDP Canvas receives `keyPressed` and `keyReleased` callbacks. Both translate device key codes through `getKeyFromKeyCode()` and update a shared bitmask `keysPressed`.

The gameplay input dispatcher checks this bitmask and maps directional/action key groups into movement state. The source also clears the bitmask when movement, interaction, modal overlays, or selected animation states should suppress additional input.

This gives the Native port a clear compatibility target: physical keys and touchscreen controls should feed one shared action-state abstraction, rather than implementing independent movement logic for touch and keyboard.

The full key-to-action table and all movement-state transitions remain open; this audit records the shared bitmask mechanism and does not claim a complete control mapping.

## 5. Native Android implementation requirements

- Preserve the 24-pixel logical tile unit and tile-based player coordinates in the gameplay model.
- Keep spawn marker parsing separate from the runtime hero coordinate; preserve the source's marker-X-to-`ayInt` behavior until fully understood.
- Represent camera state with typed world-pixel coordinates and explicit target/intermediate/render offsets.
- Keep the source's 240×320 logical viewport for the first playable build; do not change it to landscape or stretch it.
- Implement normal camera follow and scripted camera transitions as separate controllers.
- Clamp camera offsets safely for stages smaller than the viewport; the source formulas can produce negative maxima for undersized maps, so Native behavior needs an explicit tested policy.
- Feed touchscreen and physical input into one action bitmask/state layer.
- Run movement and camera updates on deterministic simulation ticks, not directly from Android draw callbacks.
- Keep rendering transforms separate from logical player position and collision coordinates.

## 6. Open work

- Trace all reads/writes of `ayInt` to establish the meaning of the spawn marker's X coordinate.
- Trace complete player movement update and `jInt` interpolation semantics.
- Extract the full key-code-to-action mapping and document repeat/edge-trigger behavior.
- Trace camera update call cadence and all scripted camera transition entry points.
- Verify behavior on maps smaller than 240×320 and at stage edges.
- Add deterministic tests for spawn marker conversion, camera follow thresholds, clamping, transition interpolation, and touch/key equivalence.
- Validate player spawn and camera behavior against original runtime evidence.

## 7. Audit status

**Completed at static-source level:** marker-79 spawn handling; 24-pixel tile coordinate evidence; camera offset pairs and viewport thresholds; camera follow and scripted interpolation behavior; shared keyboard bitmask input mechanism.

**Not completed:** semantic meaning of `ayInt`; complete movement interpolation and key mapping; all camera entry points/cadence; small-stage edge policy; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
