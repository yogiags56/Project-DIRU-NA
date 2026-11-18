# DIRU-NA-AUDIT-012 — Stage Loading, Gameplay Initialization & Resource Lifecycle

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Primary inspected source: `src/main/java/i.java`, especially the stage initialization dispatcher (steps 7–10), stage stream parsing, layer preprocessing, resource-loading continuation, and world-map resource cleanup. The earlier map/resource parser findings are cross-referenced but not re-proven here.

This is static-source analysis only. The candidate was not executed. The source uses single `InputStream.read/skip` calls in several places; the audit records the current behavior but does not treat those calls as robust parsing. No source assets were copied.

## 1. Stage initialization is incremental

The inspected stage initialization dispatcher is divided into numbered steps. Relevant observed steps:

- Step 7: reads the stage's configured value using `cInt(world, stage)`.
- Step 8: initializes a 264×264 offscreen image/graphics surface, resets many gameplay counters and temporary fields, reads the stage flag byte, and applies world/stage-specific setup.
- Step 9: preprocesses the loaded stage layers and builds runtime state from tile codes/background metadata.
- Step 10: opens the selected world's stage file (`0.f`, `1.f`, or `2.f`), reads its chunk count and directory entries, then proceeds into chunk loading.
- Later default-branch iterations load chunks from the selected world file, `gen0.f`–`gen4.f`, and `cm.f`, using `bsInt`/resource counters to advance the incremental load process.

This confirms that entering a stage is a multi-update pipeline, not one synchronous function call. The exact complete step sequence, including every skipped/conditional step, remains open.

## 2. Stage binary record loading

At stage step 8, the source opens the resource named by `worldFiles[crtWorldIndex]`. It reads an initial byte, then repeatedly reads a world-stage count, 4-byte stage headers, and stage payloads until the selected stage index is found.

For each stage header:
- width = unsigned 16-bit little-endian value at header offset 0
- height = unsigned 16-bit little-endian value at header offset 2

For the selected stage, the source allocates arrays sized width × height and reads three consecutive one-byte-per-cell layers:

1. Player/object layer → `crtStagePlayerLayer[x][y]`
2. Background layer → `bIntArrArr[x][y]`
3. Foreground layer → `crtStageForegrondLayer[x][y]`

The flat source index is `x + y * width`, then data is copied into arrays addressed as `[x][y]`.

For non-selected stages, the source skips `width * height * 3` bytes. The inspected implementation uses one `skip` call and single `read` calls rather than looping until the requested byte count has been consumed. This is a parser robustness issue to avoid repeating in Native Android.

## 3. Layer preprocessing and runtime transformation

Stage step 9 scans every tile and resets/initializes per-cell runtime arrays. It uses foreground code, background value, and player/object code together.

Observed transformations include:

- Foreground codes 31 and 19 are repacked with background metadata into the upper bits.
- Foreground code 17 examines the tile above and the player/object code above; it may be removed or repacked depending on neighboring entity type and background metadata.
- Foreground codes 14 and 33 may be converted/normalized, contribute to feature flags, and interact with a stage-specific collected/retained-item check.
- Foreground codes 8 and 9 set environment capability flags; code 9 specifically enables the water-breathing-potion requirement flag in the inspected branch.
- Foreground code 7 records door coordinates in door-index arrays using the background value as the index.
- Foreground codes 30, 1, and 26 are repacked with background metadata; code 30 also increments an entity counter.
- Several object-layer codes initialize `bByteArrArr`, `cByteArrArr`, and `bIntArrArr` runtime state, convert multi-cell objects, or set global feature bits in `cLong`.

The source uses multiple arrays for the same tile coordinate: authored player/object layer, authored background values, transformed foreground values, byte timers/state, and integer state/metadata. The Native model must preserve these as distinct layers/state channels rather than flattening them into one tile enum.

## 4. Player spawn and stage-derived state

The inspected stage preprocessing contains player-start and entity conversion logic. The source's player start marker is processed in the broader layer initialization path and establishes player/camera start coordinates; see the earlier tile/entity audit for the marker code. This audit does not claim the exact full ordering of every spawn-related mutation because the complete step-9 method has not yet been mapped line-by-line.

Stage-specific setup also applies special configuration for selected stages/worlds, including dedicated state flags, counters, and mode values. Examples include special handling for particular Angkor, Bavaria, and Siberia stage constants. These branches demonstrate that a stage loader must accept per-stage configuration in addition to generic map layers.

## 5. Resource loading and cleanup

The incremental resource path loads:
- World-specific stage data file: `0.f`, `1.f`, or `2.f`
- Generated sprite chunks from `gen0.f` through `gen4.f`
- Common sprite chunks from `cm.f`
- Additional previously mapped resources such as `ms.f`, `mmv.f`, and other UI/gameplay resources in the broader world preparation path.

The source releases world-map sprites and references when changing worlds via `auVoid()`, clearing slots 23–26 and 17, map-node storage, and map image/graphics references before requesting garbage collection. Other state-21 steps also release selected sprite slots and texture references under mode conditions.

Resource lifetime is therefore tied to world/stage transitions and load flags. The Native implementation should use explicit ownership and cache lifetimes rather than relying on garbage collection and nullable global arrays.

## 6. Important source limitations and Native parser requirements

The candidate loader has unsafe assumptions:
- Single `read` calls may return fewer bytes than requested.
- Single `skip` calls may skip fewer bytes than requested.
- Width × height and width × height × 3 are not guarded by checked arithmetic in the inspected path.
- Stage index search loops until a stage is found; invalid/missing stage indices can cause a non-terminating read loop or stream exhaustion behavior.
- Layer arrays are allocated before comprehensive validation of the stage dimensions and resource bounds.
- Tile preprocessing indexes neighboring cells in some branches, so edge conditions must be validated against map borders.

Native Android requirements:
- Use a bounded binary cursor with `readFully` and `skipFully`.
- Validate stage count, selected stage index, dimensions, multiplication overflow, and remaining bytes before allocating.
- Enforce configurable maximum stage dimensions and total cells.
- Parse into temporary immutable stage data, then commit it atomically to active gameplay state only after validation succeeds.
- Preserve the three authored layers and all runtime state channels separately.
- Make stage initialization cancellable and deterministic; dispose partial resources on failure.
- Build explicit stage resource manifests and release resources by owner/world/stage scope.
- Keep rendering and simulation paused until stage data and required resources are fully ready.

## 7. Open work

- Trace the entire numbered stage initialization dispatcher and document all steps and conditional branches.
- Complete the step-9 object/foreground preprocessing transformation table.
- Trace exact player spawn marker ordering and camera initialization.
- Map the resource load mask `dLong` to every chunk loaded in the selected world path.
- Trace all resource unload paths and ownership across stage retry, world switch, app background, and failed load.
- Verify stage file count/header layout and malformed-input behavior with synthetic fixtures.
- Add parser tests for short reads, short skips, invalid dimensions, overflow, missing stage indices, and truncated layers.
- Validate stage entry and resource lifetime against a legally usable original runtime.

## 8. Audit status

**Completed at static-source level:** incremental initialization stages 7–10; world stage-file selection; 4-byte dimension header; three-layer extraction and array orientation; selected layer preprocessing rules; selected resource-loading and world-map cleanup behavior; parser robustness risks.

**Not completed:** full initialization-step map; complete preprocessing table; full spawn/camera order; complete resource load-mask and unload graph; synthetic parser tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
