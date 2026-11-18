# DIRU-NA-AUDIT-011 — Main Menu, World Selection & Stage Selection Reconstruction

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source ranges in `src/main/java/i.java` include `TVoid()`, `iVoid()`, the state-21 world-map preparation path, menu/world-selection input branches, stage-progress reconciliation, and stage-map drawing/selection helpers.

This is a partial static-source reconstruction. The decompiled source uses numeric state fields and obfuscated names; this document keeps those names where semantic meaning is not proven. It does not claim a complete menu option list or complete stage-map interaction model.

## 1. Main menu input routine

The title/menu input routine is `TVoid()`, dispatched when global state `i.bByte == 4`.

Observed behavior:

- Up/down key inputs call menu movement helpers `mVoid()` and `nVoid()`.
- The movement helpers may be called twice for one input depending on `boInt`, `bqInt`, and `XBoolean`.
- The confirm key initializes a selection/confirmation sub-state using `dbInt = 0) and records a timestamp in `gLong`, unless a confirmation state is already active.
- Cancel behavior depends on `boInt`:
  - Case 5 has special behavior for states 2 and 4, including returning to state 4 and playing title music.
  - Case 2 transitions to state 9 and initializes a sub-state.
  - Case 4 calls a menu-selection helper with argument 2.

This confirms a menu navigation routine with context-sensitive confirm/cancel behavior, but the meaning of every menu row is not fully established by this method alone.

## 2. World selection and world unlock

The candidate exposes three world indices:

| World index | Map resource | Seal position constant |
|---:|---|---|
| 0 | `/map_angkor.out` | `SEAL_POS_ANGKOR` |
| 1 | `/map_scotland.out` | `SEAL_POS_BAVARIA` |
| 2 | `/map_tibet.out` | `SEAL_POS_SIBERIA` |

The source's world-selection/seal navigation path `iVoid()` handles a confirmed selection by world position:

- Angkor: sets world index 0, releases current-player resources, clears a mode flag, sets global state 15, initializes the current progression marker from `bInt(worldIndex)`, and starts world preparation.
- Bavaria/Scotland: checks `isSealPositionUnlocked`; if unlocked, sets world index 1, resets stage index to 0, enters state 15, initializes progression marker, and updates progression byte 9 to at least 1.
- Siberia/Tibet: checks unlock condition; if unlocked, sets world index 2, resets stage index to 0, enters state 15, initializes progression marker, updates progression byte 9 to at least 2, and may raise save byte 8 to 8.
- Shop: transitions to state 18.

The source naming is inconsistent between seal constants and map filenames: the seal positions are called Bavaria and Siberia, while the loaded map resources are Scotland and Tibet. This audit preserves both names rather than silently reconciling them.

Unlock logic is implemented by `isSealPositionUnlocked(int)`: Angkor and the shop are always unlocked; other seal positions require the saved 16-bit currency/progression value at offset 6 to meet the corresponding `a_Config.worldPrices` threshold.

A second selection path in the state-specific input dispatcher maps choice 0/1/2 to world indices 0/1/2 and starts the same state-15 → state-21 preparation route. Choice 3 calls the shop/menu helper. The exact UI context for this branch still needs tracing.

## 3. World-map preparation and stage selection data

After a world is selected, `avVoid()`:

- Clears a transition flag and releases prior world-map sprites.
- Resets map-related arrays.
- Sets global state 21.
- Resets the incremental loading step counter `bsInt`.
- Sets a stage/progression bound field `dKInt = 100).
- Clamps `crtStageI` to the maximum stage value returned by `bInt(crtWorldIndex)).
- Calls `awVoid()` to allocate world-map node storage.

State 21 then loads the selected world map resource, world-map sprites, world-specific header, and crystal/diamond marker resources. At a later loading step it iterates the map-node array and reconciles node status against per-world stage progress flags. It identifies map-node coordinates corresponding to the current stage and latest unlocked stage, then transitions to state 15.

The map is therefore not merely a static background: the game builds a node/state representation from map data and saved progression.

## 4. Stage progression and unlock evidence

The source uses several distinct values which should not be conflated:

- `crtWorldIndex`: selected world index.
- `crtStageI`: current stage index.
- `dZInt`: a progression/map marker index used when reconciling and highlighting map nodes.
- `bInt(worldIndex)`: per-world stage/progression bound returned by a helper.
- `aByte(worldIndex, stageIndex)`: per-stage flag byte.
- `bInt(worldIndex, stageIndex)` and `cInt(worldIndex, stageIndex)`: per-stage values used in stage-completion comparison paths.

Observed stage flag checks include bit `0x2` in the next-stage record and bit `0x40` in stage/map-node reconciliation. The full semantic meaning of all bits is not yet established.

The map-node renderer `axVoid()` checks whether the current/latest stage marker should be shown as active and uses per-node packed fields to choose node state and marker sprite slots. It also consults stage completion counters when deciding node appearance. This supports a data-driven stage map rather than a fixed linear menu.

## 5. Native Android requirements

- Represent worlds with stable semantic IDs while retaining original source indices in an audit mapping.
- Preserve the three-world order and exact map-resource association pending confirmation of publisher/variant naming.
- Implement world unlock rules from a typed save model, not hard-coded menu visibility.
- Treat map nodes as data records with coordinates, stage index, unlock/completion state, and marker state.
- Keep `crtWorldIndex`, `crtStageI`, and progression marker equivalents separate in the native model.
- Reproduce map selection/confirmation and cancel behavior with a state-driven input controller.
- Load map and marker resources incrementally or asynchronously; support cancellation and cleanup when changing worlds.
- Do not infer that every map node corresponds to a playable stage until the map parser and node records are fully mapped.
- Preserve unknown progression flag bits in the audit specification until their writers/readers are traced.

## 6. Open work

- Trace `mVoid()`, `nVoid()`, `hVoid()`, and the map-node selection handlers to reconstruct exact cursor movement and selection.
- Identify the menu options and map/world screen paint branches.
- Trace the exact entry route from title/menu to the world seal/map and from map node to stage launch.
- Decode the meaning of stage flag bits `0x2` and `0x40` through all writers/readers.
- Trace the `bInt(world)`, `bInt(world, stage)`, and `cInt(world, stage)` helpers and their storage ranges.
- Confirm the seal-name/map-name differences (Bavaria vs Scotland; Siberia vs Tibet) against the exact JAR variant and original package metadata.
- Validate node coordinates, cursor movement, unlocks, and map transitions against a legally usable original runtime.
- Add deterministic tests for unlock thresholds, node-state reconciliation, and stage selection.

## 7. Audit status

**Completed at static-source level:** title/menu input routine; three world indices and map resource names; seal/world unlock threshold path; state-15/state-21 world preparation; partial map-node and stage-progress reconciliation; distinction among current stage and progression marker fields.

**Not completed:** full menu layout/options; complete map cursor/selection; complete stage launch route; full stage flag semantics; world naming/variant confirmation; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
