# DIRU-NA-AUDIT-002 — Gameplay and Resource Specification (Initial Static Pass)

**Status:** In progress — static-source specification only  
**Audit input:** `kubikaugustyn/DiamondRushSource`  
**Reviewed source revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Development target:** `yogiags56/Project-DIRU-NA` only  
**Scope:** sprite container internals, animation state, map data structure, audio IDs/priorities, localization loading behavior, and gameplay-state facts exposed by the audited source.

> This document records what can be supported by static inspection. It does not claim that any game asset has been extracted, decoded, built, or tested on Android. Candidate source and proprietary game resources remain read-only research inputs pending rights review.

## 1. Sprite resource structure

Primary source: `src/main/java/f_Sprite.java`.

### 1.1 Metadata layout observed

The parser advances through the sprite payload in this order:

1. A 8-byte initial header area; the module count is assembled from bytes at offsets 6 and 7 as a little-endian unsigned 16-bit value.
2. Module dimensions: two bytes per module (width, height).
3. Frame-module record count: little-endian unsigned 16-bit.
4. Frame-module records: four bytes each, interpreted by the drawing path as module index, X offset, Y offset, and flags.
5. Frame count: little-endian unsigned 16-bit.
6. Per-frame metadata: one byte frame-module count, one skipped/reserved byte, then a little-endian unsigned 16-bit first frame-module index.
7. Per-frame bounding boxes: four bytes per frame.
8. Animation-frame record count: little-endian unsigned 16-bit.
9. Animation-frame records: five bytes each — frame index, duration, X offset, Y offset, and flags.
10. Animation count: little-endian unsigned 16-bit.
11. Per-animation metadata: one byte animation-frame count, one skipped/reserved byte, then a little-endian unsigned 16-bit first animation-frame index.
12. Palette section (only when module count is positive): pixel-format ID, palette count, colors per palette, then palette color entries.
13. Module pixel encoding ID.
14. Per-module pixel-data lengths (little-endian unsigned 16-bit) and the corresponding encoded byte streams.

The source has explicit TODO comments questioning whether the skipped bytes in frame and animation metadata are actually reserved bytes or part of a wider count. Do not treat those interpretations as definitively resolved until real binary fixtures are examined.

### 1.2 Palette pixel formats

The source declares these palette entry formats:

| Format ID | Observed interpretation |
|---|---|
| `0x8888` | 32-bit RGBA-like packed color; alpha is retained |
| `0x4444` | 16-bit RGBA 4-bit channels expanded to 8-bit channels |
| `0x5515` | 16-bit 1-bit alpha + 5-bit RGB (1555) |
| `0x6505` | 16-bit RGB 565; `0xF81F` is treated as transparent |

The exact channel ordering should be confirmed against known visual samples. The table describes the decoder's observed operations, not an independently validated file-format standard.

### 1.3 Indexed pixel encodings

| Encoding ID | Decoder behavior observed |
|---|---|
| `0x5602` | I256: one palette index per pixel |
| `0x1600` | I16: two 4-bit palette indices per byte, high nibble first |
| `0x0400` | I4: four 2-bit palette indices per byte, most-significant pair first |
| `0x0200` | I2: eight 1-bit palette indices per byte, most-significant bit first |
| `0x56F2` | I256RLE: run/literal control byte followed by palette-index data |
| `0x27F1` | I127RLE: control values above 127 repeat the following palette index; values 0–127 are emitted as a single palette index |

The decoder writes into a fixed 4096-pixel temporary array. The parser/decoder does not visibly enforce all payload bounds or output-count bounds before reading/writing. A native decoder must validate dimensions, multiplication overflow, palette indexes, module lengths, RLE run lengths, and input/output bounds before decoding.

### 1.4 Frame and draw semantics

- A frame is a collection of module references with signed-looking byte offsets and low-nibble transform flags.
- Frame draw iterates its module records, applies X/Y offsets, adjusts origin for horizontal/vertical flips using module dimensions, and combines frame flags with module flags using XOR.
- Animation-frame draw selects a frame index and may apply animation-frame offsets when `FLAG_OFFSET_AF` is set.
- Horizontal and vertical flip flags are `0x01` and `0x02`; the animation-offset flag is `0x20`.
- The source draws each decoded module as a separate image region. A native renderer can instead upload decoded modules to textures/bitmaps, but must preserve the same origin, offset, transparency, and flip behavior.

## 2. Animation state behavior

Primary source: `src/main/java/b_SpriteAnimator.java` and `f_Sprite.java`.

Observed animator state includes sprite reference, X/Y position, flags, current animation index, current animation frame, elapsed frame time, and optional parent animator.

- `setAnimation(index)` resets frame index, elapsed time, and accumulated frame offsets only when the requested animation differs from the current animation.
- `tick()` reads the current frame duration. Duration zero freezes the current frame. Otherwise it increments elapsed ticks and advances once the duration threshold is reached.
- Advancing past the final animation frame loops to frame zero and clears stored frame offsets.
- `animationEnded()` reports true only on the last frame when its final tick is reached, or when the frame duration is zero.
- `draw()` adds parent animator positions before drawing. It selects animation-frame, module, or composite-frame rendering according to the animator's current state.
- `applyFrameOffset()` applies per-animation-frame offsets to the animator position, reversing signs when the corresponding flip flag is set.

**Native implication:** separate animation data from renderer objects. Keep animation advancement deterministic and test frame-duration boundaries, looping, zero-duration frames, parent offsets, and flip combinations. Do not assume the nominal game-loop sleep is the animation time unit until runtime observations confirm it.

## 3. World and map data

Primary source: `src/main/java/i.java`.

The current static findings establish the following map resource mapping:

| Resource | World name represented by game constants/call sites |
|---|---|
| `/map_angkor.out` | Angkor |
| `/map_scotland.out` | Bavaria |
| `/map_tibet.out` | Siberia |

The parser reads a little-endian 16-bit length, a one-byte record count, then a sequence of records. Each record begins with five fixed bytes and is followed by a variable number of byte pairs. The exact semantic meaning of each field, coordinate units, record boundaries, and variable-pair interpretation have not yet been established.

The game class also reads `/w0.bin`, `/w1.bin`, and `/w2.bin` in world initialization/progress paths. Their structures and relationship to map records remain unresolved.

**Required next investigation:** inspect legally usable binary samples; record file length and SHA-256; parse with bounds checks; correlate records to observed map positions and stage transitions; document unknown fields rather than guessing.

## 4. Gameplay and input facts exposed by the source

Primary source: `src/main/java/i.java`.

The decompiled game class exposes these explicit input bit values:

| Action | Bit value |
|---|---:|
| Up / Down / Left / Right | 1 / 2 / 4 / 8 |
| Center | 16 |
| Left soft key / Right soft key | 32 / 64 |
| Alternate center | 128 |
| Star / Pound | 256 / 512 |
| Number keys 0–9 | 1024 through 524288, powers of two |

Combined action masks in the source include:
- Confirm: center or numeric 5 (with alternate center/left-soft-key included in a broader confirm mask).
- Cancel: right soft key.
- Respawn: star.
- Directional movement: matching D-pad or numeric 2/8/4/6.

World-selection constants identify Angkor, Bavaria, Siberia, and Shop. The source also names selected special stages (Angkor falling torches, great anaconda, demo; Bavaria evil Teutonic knight; Siberia yetti).

These constants are useful anchors for a future state-machine map, but they do not by themselves define the full movement physics, collision rules, hazards, inventory, puzzle logic, or stage progression. Those require tracing the relevant update methods and correlating them with reproducible gameplay observations.

**Native implication:** define a logical action layer shared by touchscreen and physical-key input. Preserve held-state semantics where required, but make the native input system robust to pointer cancellation, focus loss, app pause, and simultaneous touches.

## 5. Audio resource and event map

Primary source: `src/main/java/j_SoundManager.java`.

The manager declares 21 logical sound IDs: 15 effects (0–14) and 6 music tracks (15–20).

| ID | Logical event |
|---:|---|
| 0 | Switch |
| 1 | Riddle |
| 2 | Death |
| 3 | Chest 1 |
| 4 | Chest 2 |
| 5 | Hero hurt |
| 6 | Hammer hit unbreakable |
| 7 | Mine |
| 8 | Working |
| 9 | Checkpoint |
| 10 | Enemy hurt |
| 11 | Break |
| 12 | Hooking |
| 13 | Water |
| 14 | Boulder |
| 15 | Level clear music |
| 16 | Angkor music |
| 17 | Bavaria music |
| 18 | Siberia music |
| 19 | Title music |
| 20 | Game-over music |

The manager skips one initial byte in `/snd.f`, then reads 21 entries × 8 bytes (168 bytes) as an index/header. It obtains a 16-bit length from offset `(soundID * 8) + 4` for each payload. The source reads audio payloads as MIDI and creates Java ME `Player` instances.

Priority classes observed:
- Priority 30: riddle, death, chest 2, level-clear, world music, title, game-over.
- Priority 20: chest 1, mine, working, checkpoint, break, hooking, water.
- Priority 10: switch, hero hurt, hammer hit unbreakable, enemy hurt, boulder.

The exact meaning of all other 8-byte header fields and the payload offsets/ordering is not yet confirmed. Existing stream reads are not guaranteed to fill the requested byte count. A native implementation must use validated bounded reads and an explicit audio event queue/priority policy; it must not port the Java ME thread/player lifecycle directly.

## 6. Localization timing decision

The candidate source contains 14 locale-specific resource files plus `lang.xx` fallback. Its loader resolves a system locale and then falls back to `lang.xx`; it does not currently expose an explicit in-game language selection workflow.

**Project decision:** do not add or replace language payloads during AUDIT-002 or the initial playable implementation. Preserve the catalog and existing loader behavior as an audit reference only. After the Android APK reaches a verified playable state, perform a separate read-only audit of the original Nokia Diamond Rush language resources, then design and validate a user-selectable language system. Do not copy files from the previous Native Android project build, and do not claim that `lang.xx` is English without decoding and verifying it.

See [LANGUAGE_RESOURCE_CATALOG.md](LANGUAGE_RESOURCE_CATALOG.md).

## 7. Implementation rules derived from this pass

1. Treat all numeric fields as unsigned where the source masks them with `& 0xFF` or `& 0xFFFF`; preserve signed-byte behavior only where the source intentionally uses signed offsets.
2. Use explicit little-endian readers and reject truncated or out-of-range data.
3. Never trust counts, dimensions, offsets, run lengths, or palette indexes from a binary resource.
4. Keep decoding, animation timing, gameplay state, rendering, input, audio, and storage as separate modules.
5. Do not use AWT, MIDP Canvas, Java ME Player, or FreeJ2ME as the Android runtime.
6. Do not import source files, generated artifacts, binaries, build configuration, or build lineage from the archived Native Android project.
7. Do not copy candidate/original game code or assets into the active project until provenance and licensing review permits the intended use.

## 8. Validation status and open work

| Area | Static-source result | Still required |
|---|---|---|
| Sprite metadata | Field order and several record widths observed | Binary fixture validation; uncertain skipped bytes |
| Palette decoding | Four palette formats identified | Channel-order and transparency visual tests |
| Pixel encoding | Six indexed encodings identified | Bounds-safe decoder and golden image fixtures |
| Animation | State and tick behavior documented | Timing-unit validation and edge-case tests |
| Maps | Three world resources and outer record shape observed | Field semantics, coordinates, variable records |
| World data | Three `w*.bin` resources identified | Full format and save/progress relationship |
| Input | Key bit masks and menu/world constants observed | Full action-to-state transition map |
| Audio | 21 IDs and priority classes documented | Header offset semantics, payload validation, Android playback tests |
| Localization | Existing loader/fallback behavior documented | Defer original Nokia resource audit until APK is playable |

No APK, build, decoded asset, gameplay test, or Android runtime test is produced by this audit pass. The milestone remains **in progress**.


## 9. Stage package and tile-layer loading — additional static findings

Primary source: `src/main/java/i.java`, stage initialization path (the decompiled `jVoid()` state sequence).

The source selects a world package using `worldFiles[crtWorldIndex]`. The exact filename mapping behind `worldFiles` still needs to be confirmed from its static initialization, but the selected package is parsed as a sequence of stages.

Observed package traversal:

1. One initial byte is read before the stage traversal loop.
2. A stage-count byte is read for the current world/package section.
3. Each stage begins with a 4-byte header: unsigned little-endian 16-bit width followed by unsigned little-endian 16-bit height.
4. For the requested stage, the loader allocates three grid layers, each with `width * height` cells:
   - player/object layer (`byte[][]`)
   - background layer (`int[][]`, populated from one byte per cell)
   - foreground layer (`int[][]`, populated from one byte per cell)
5. The three raw layers are each read as `width * height` bytes.
6. If a stage is not the requested stage, the loader skips `width * height * 3` bytes after its header.

The byte stream is traversed in row-major order using source index `x + y * width`, then stored in arrays indexed as `[x][y]`. A native port should preserve this coordinate convention explicitly and should not assume the source arrays are contiguous in the same order as the serialized bytes.

**Robustness concern:** the candidate uses single `read(byte[])` calls for stage headers and layer buffers, and `skip(width * height * 3)` for non-target stages. These calls are not guaranteed to consume the full requested amount. The native parser must use read-fully and skip-fully loops, validate dimensions/counts before allocation, cap memory use, and detect truncated packages or impossible stage indexes. The current source's loop can continue indefinitely when a requested stage does not exist; the native implementation must fail with a structured load error instead.

## 10. Grid semantics and player spawn — partially established

The stage loader and subsequent initialization pass reveal these runtime structures:

| Structure | Observed role |
|---|---|
| `crtStagePlayerLayer[x][y]` | Dynamic/object/entity tile codes, including the player spawn marker |
| `bIntArrArr[x][y]` | Background tile/property values; later processing also packs auxiliary bits/IDs into integer values |
| `crtStageForegrondLayer[x][y]` | Foreground tile/object values; later processing packs background-derived data into upper bits |
| `bByteArrArr[x][y]` | Auxiliary per-cell byte state initialized during stage setup |
| `cByteArrArr[x][y]` | Auxiliary per-cell animation/timing/state byte layer |

The player spawn marker observed in the object layer is value `79`. During stage initialization, the loader sets `crtPlayerX = 0`, `crtPlayerY = y` for that marker, records the marker's original X in `ayInt`, clears the marker cell, and initializes camera positions around the spawn row.

Other object-layer values trigger setup behavior for entities and level mechanics (examples include moving/animated objects, water-related elements, doors, checkpoints, collectibles, and enemies). Their complete mapping is not yet established. Do not assign friendly names to every numeric tile ID until each code is traced through update and render call sites.

The initialization pass scans every cell, counts or records certain object types, normalizes some foreground values, builds door-coordinate arrays, and allocates per-object/per-cell state arrays. This confirms that the stage package is not just a flat image: it is a multi-layer logical grid with runtime entities and derived metadata.

## 11. Logical coordinate system and viewport evidence

The candidate's rendering and camera code repeatedly uses a **240 × 320** logical drawing area:

- Stage cell size is treated as 24 logical pixels in camera/player-position calculations.
- Camera clamping uses `stageWidth * 24 - 240` horizontally and `stageHeight * 24 - 320 + 80` vertically.
- The demo/event renderer fills or clips against 240 × 320.
- Gameplay UI draws top and bottom bars at 42 pixels high in the 240 × 320 coordinate space.
- Menu layout calculations also center content using 240 and 320.

This is strong static evidence that the reviewed candidate is configured for a portrait logical canvas of 240 × 320. **Project decision for DIRU-NA:** use 240×320 portrait as the initial logical viewport and preserve it in the first Native Android build. This is an explicit baseline for this independent project; it does not inherit the archived Native Android project's 320×240 target. The candidate evidence informs this project decision, but does not prove how every original Nokia handset rotated or presented the game.

Adaptive aspect-ratio support remains a later development goal. It must retain the 240×320 logical gameplay coordinate system and adapt viewport, camera, and UI layout for wider/taller device screens without stretching sprites or changing collision geometry. The initial baseline and later adaptive presentation are separate milestones.

## 12. Movement and interaction observations — incomplete trace

The in-stage input branch confirms the following high-level behavior:

- Directional actions are mapped to four movement intents; the source's direction-state values are 1 (up), 2 (right), 3 (down), and 4 (left).
- The source stores player grid coordinates separately from pixel/camera offsets.
- Some movement is temporarily blocked while an interaction/animation timer is active, while a transition flag is set, while a scripted event is active, while the player is in a non-alive state, or during a specific animation.
- During a temporary hurt/stun state, directional input changes small velocity/offset values rather than immediately issuing a normal grid step.
- Confirm/OK can activate nearby interactables, use a checkpoint when the player is on a checkpoint tile, or select an adjacent object/action depending on inventory/ability state.
- Respawn/star handling can restore a checkpoint or trigger death/game-over behavior depending on the current animation and foreground cell.
- The source uses neighboring grid cells and per-cell bit fields to determine whether a contextual action can be performed. Some special actions scan up to three cells horizontally or inspect a defined set of nearby offsets.

The current code excerpt is not enough to reconstruct all movement timing, collision response, push/pull rules, climbing, swimming, hazards, or object-specific interaction rules. The full state/update methods and entity handlers must be mapped before native gameplay implementation. For now, these are behavioral anchors, not a completed physics specification.

## 13. Camera and transition behavior — partial evidence

A separate event/transition helper class `c.java` contains camera interpolation logic. It interpolates camera target coordinates over a configured duration and clamps the result against the stage dimensions in 24-pixel tile units. It also handles scripted visual overlays and event sequences. This helper appears to combine camera behavior with demo/cutscene/script rendering; its responsibilities should be separated in the native design.

The source contains multiple hard-coded drawing bounds and animation counters. A native implementation should model camera target, interpolation progress, clamping, and scripted events as separate testable systems rather than porting the helper class as one unit.

## 14. Additional validation status

| Area | New static evidence | Remaining uncertainty |
|---|---|---|
| Stage package | 4-byte width/height header and three cell layers per stage | Initial section header meaning; exact world package mapping |
| Grid layout | Serialized row-major order; runtime arrays addressed as `[x][y]` | Full numeric tile/object ID dictionary |
| Player spawn | Object-layer marker 79; X set to 0 and Y taken from marker row | Whether all worlds/stages use identical spawn convention |
| Viewport | Multiple 240 × 320 drawing/camera constants; 24 px tile calculations; DIRU-NA initial baseline formally set to 240×320 portrait | Adaptive viewport implementation and original Nokia device presentation details |
| Movement | Direction values, state gating, contextual action/checkpoint/respawn paths | Complete movement, collision, and interaction state machine |
| Camera | Interpolated/clamped camera helper in `c.java` | All event opcodes and camera timing semantics |

No binary stage package has been extracted or decoded in this pass, and none of these observations has been runtime-tested.

## 15. Stage entity and foreground initialization — additional trace

Primary source: `src/main/java/i.java`, stage initialization scan and follow-up entity registration pass, reviewed at source revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This section records numeric codes only where their initialization effects are visible. It is deliberately **not** a complete semantic tile dictionary: numeric values are not assigned guessed names such as "enemy", "key", or "door" unless the code path provides direct evidence.

### 15.1 Player/object-layer codes with visible initialization effects

| Raw code | Observed initialization effect | Caveat |
|---:|---|---|
| 79 | Sets initial player grid X to 0 and Y to marker row; stores marker X separately; clears marker cell; initializes camera offsets | Explicit spawn branch |
| 48 | Increments a counter; initializes a related cell above; sets feature flags; follow-up may register a paired coordinate when a background bit is set | Identity unknown |
| 47 | Sets auxiliary animation byte to 48, clears background value, sets feature flag | Identity unknown |
| 46 | Clears background; sets animation byte 24 and auxiliary byte 0; sets feature flag | Identity unknown |
| 45 | Clears background; sets animation byte 24; sets feature flag | Identity unknown |
| 44 | Sets animation byte 24, clears background, sets feature flag | Identity unknown |
| 43 | Sets world-dependent feature bit, sets one `buInt` bit, modifies background flags, sets animation byte 48 | Identity unknown |
| 42 | Increments counters, sets feature flags, invokes `iVoid(x,y)` registration | Identity unknown |
| 41 | Ensures background quantity is at least 1, then adds it to a stage total | Identity unknown |
| 40 | Sets feature flags, increments counters, enables boolean feature, invokes `iVoid(x,y)` | Identity unknown |
| 39 | Enables boolean feature and sets feature bit | Identity unknown |
| 38 | Enables boolean feature, rewrites foreground to 27, sets feature bits | Identity unknown |
| 36 | Normalizes background value and sets feature bit | Identity unknown |
| 35 | Preserves object code 35, clears foreground cell, sets feature flag and `crtShouldLoadFanPot` | Rendered identity unresolved |
| 34 | Clears object-layer cell, writes foreground value 15, sets feature bit | Identity unknown |
| 30 | Sets feature bit and clears background value | Identity unknown |
| 28 | Sets feature bit; for some background values scales data and sets background flag; initializes animation byte 24 | Identity unknown |
| 27, 26, 24 | Registers entity-like entries and increments counters; 26 also sets feature bit | Identity unknown |
| 23, 22 | Set feature bits and animation byte 48 | Identity unknown |
| 19 | Sets world-dependent feature bit, sets a `buInt` bit, initializes animation byte 48 | Identity unknown |
| 18 | Resets two counters and sets feature bits | Identity unknown |
| 16 | May copy matching tile/background value to cell above and sets feature bit | Identity unknown |
| 14 | Sets feature bit, conditionally changes background value, initializes animation byte 24 | Identity unknown |
| 12 | Clears object-layer cell and stores X, Y, and background data in dedicated fields | Semantic identity unresolved |
| 11 | Sets background based on its current value, animation byte 48, and feature bit | Identity unknown |
| 10 | Clears background value and sets a stage flag | Identity unknown |
| 8, 7, 6, 5, 4, 2, 1, 0 | Participate in a fall-through switch sequence initializing counters, per-cell animation state, flags, or calling `iVoid(x,y)` | Must trace combined fall-through behavior |
| 49 | Sets feature bit and animation byte 48 | Identity unknown |
| 37 | Clears background value and sets feature bit | Identity unknown |
| 53, 52, 51 | Explicitly ignored in this initialization switch | Does not prove unused at runtime |

The Java switch contains fall-through. A native port must reproduce the observed combined behavior, not translate each case as an isolated branch without verifying control flow.

### 15.2 Foreground-layer initialization effects

The foreground layer is read as one byte per cell and then packed with background-derived values into an integer.

| Foreground code | Observed effect |
|---:|---|
| 8 | Enables a boolean feature, then falls through into code 9 setup |
| 9 | May request water-breathing-related resource loading depending on neighboring/background state; sets feature bit and counts matching background IDs |
| 7 | Registers door-coordinate slots using background value as an index; later scans neighboring cells to derive a variant/connection value |
| 30 | Sets feature bit and increments an object counter |
| 1, 26 | Preserve foreground code while packing background value |
| 0 | Preserves code and increments an object counter |
| 4 | Increments a counter and sets feature bit |
| 5, 28 | Store background-derived values in dedicated fields |
| 3 | Initializes per-cell byte to 127 and may adjust packed background value |
| 6 | Counts background IDs, sets feature bit, packs background and foreground |
| 34 | Sets feature bit |
| 20–25 | Special group that can set world-dependent feature flags |
| Other values below 80 | Some are normalized to `-1`; do not treat every value below 80 as a valid foreground entity |

A second pass builds lookup arrays for foreground codes 0, 30, 4, 5, and 17. Code 7 uses neighboring foreground values to derive additional packed bits and may set animation state. The raw foreground byte alone is not the full runtime state.

### 15.3 Dynamic registration and derived arrays

The initialization pass allocates arrays based on counts gathered during the scan, including entity/event coordinate storage, per-entity byte state, door coordinate pairs, paired/moving-object coordinate triples, and foreground lookup/animation indexes.

The exact class/array names are decompiler artifacts and should not become the Native Android public API. A new implementation may eventually use typed structures such as `StageCell`, `EntitySpawn`, `DoorLink`, and `StageFeatureFlags`, but only after semantics are validated.

### 15.4 Sprite chunk-to-runtime-slot mapping — partial

The stage loader reads sprite chunks from `0.f`, `1.f`, `2.f`, `gen0.f` through `gen4.f`, and `cm.f` according to flags/counts derived from the stage. Examples directly visible in the loader:

| Generated chunk index | Runtime sprite slot |
|---:|---:|
| 0 | 33 |
| 1 | 34 |
| 30 | 15 |
| 33 | 22 |
| 34 | 27 |
| 35 | 28 |
| 36 | 35 |
| 37 | 29 |
| 38 | 36 |
| 40 | 2 |
| 41 | 38 |
| 42 | 39 |

This is a resource-loading map, not a confirmed entity-name map. It must be correlated with entity update/draw call sites and legally usable asset samples before assigning semantic labels.

## 16. Audit-002 follow-up priorities

1. Trace `iVoid(x,y)` and methods it calls to determine what entity records are created.
2. Trace per-tick update and draw paths for object codes 0–53, prioritizing frequently registered codes 24, 26, 27, 40, 42, and 48.
3. Trace foreground code 7 neighbor/door-link logic and code 17 lookup path.
4. Identify world-package filenames behind `worldFiles` and correlate each world index with map/resource names.
5. Trace save/checkpoint arrays and their relationship to stage initialization before designing Native Android persistence.
6. Only then create a versioned tile/entity dictionary; keep unresolved IDs explicitly marked unknown.

No source or asset payload has been copied into Project-DIRU-NA. These are static observations from the read-only candidate revision only.

## 17. Correction and clarification: helper methods and event dispatch

A deeper trace of `i.java` at candidate revision `215c71d04057d66a1b08419b1a4b1043ae3e0881` corrects an earlier interpretation in §15:

- `iVoid(x,y)` is **not** an entity registration routine. It sets feature bit `0x400000L`, clears the low byte of `crtStageForegrondLayer[x][y]`, and writes foreground code `14`. Calls to this method therefore normalize/replace a foreground cell as part of stage setup or gameplay logic.
- `kVoid(x,y)` is a distinct event/controller dispatch helper. It resets two local state bytes, searches `pByteArr` for the supplied event/demo ID, assigns the corresponding `c` controller instance, then invokes its `aVoid()` method. This is evidence of a stage event/demo controller table, not a generic tile constructor.
- `iVoid()` with no arguments is a separate method used in seal/world-map navigation logic; it must not be confused with either overload above.

Accordingly, references in §15 to `iVoid(x,y)` as "entity-like registration" are superseded by this correction. The dynamic arrays described in §15.3 are derived during stage initialization, but their exact semantics still require separate tracing.

### 17.1 Checkpoint snapshot and restore

The source contains explicit checkpoint snapshot and restore paths.

Snapshot behavior copies:
- player coordinates and selected movement/world state,
- camera and related player state,
- background/object state array `bIntArrArr`,
- animation/timer array `bByteArrArr`,
- object/player layer `crtStagePlayerLayer`,
- foreground layer `crtStageForegrondLayer`,
- selected optional stage feature arrays and world-state flags when the corresponding feature is active.

Restore behavior copies those arrays back into the live stage, restores optional feature state, then scans the stage for object code 48 and reinitializes those objects through either `dVoid(48)` or `aiVoid()` depending on a background flag. Some hard-coded special-cell adjustments also occur in the restore path.

This demonstrates that checkpoint state is substantially more than just the player's X/Y position. Native Android persistence/checkpoint design must preserve the relevant mutable stage state, or intentionally reproduce the original checkpoint reconstruction behavior. The exact minimal state set remains to be established.

### 17.2 Collision and occupancy predicates — partial

Three static predicates were identified in the source:

- `dBoolean(x,y)`: checks whether the player/object-layer value and foreground code combination qualifies for one class of traversable/processable cell. It excludes several explicit object and foreground codes.
- `eBoolean(x,y)`: requires an empty player/object-layer cell and excludes foreground codes 14, 33, 5, and 28.
- `fBoolean(x,y)`: requires an empty player/object-layer cell and excludes foreground codes 14, 33, 4, and 32; foreground code 7 is conditional on packed background flags.

These are not yet a complete collision system. They are cell-state predicates used by different movement/interaction paths. Do not collapse them into one generic `isWalkable` method until every call site and movement context has been mapped.

### 17.3 Moving object code 48 — partial behavior

The source shows code 48 is a mutable moving-object family rather than a static decorative tile:
- initialization may pair its coordinate with a neighboring cell based on background flags;
- runtime handling dispatches either to `dVoid(48)` or `aiVoid()`;
- `dVoid` may move the code-48 object into the cell below, update packed direction/state bits, set animation bytes, and trigger player damage if the player occupies a hazardous destination;
- `aiVoid` updates a paired-coordinate record for the moving object based on direction/adjacent-cell state;
- checkpoint restore reconstructs code-48 objects from their saved state.

The exact visual identity and full state machine remain unresolved. This should be documented as a moving/paired object behavior, not assigned a semantic object name yet.

### 17.4 Updated audit priorities

1. Trace all call sites of `kVoid(x,y)` and map event IDs to controller behaviors.
2. Trace `dVoid`, `aiVoid`, and related methods for code 48 to complete its state machine.
3. Map call sites of `dBoolean`, `eBoolean`, and `fBoolean` before defining collision categories.
4. Enumerate every mutable array included in checkpoint snapshot/restore and identify which are essential versus recomputable.
5. Trace foreground code 7 and code 17 handling, then correlate generated sprite slots with actual draw calls.

## 18. Demo/event controller format — parser and opcode behavior

Primary source: `src/main/java/c.java`, parsed from `/demo.f` and sprite references from `/demoSpr.bin`, at candidate revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 18.1 Event/controller selection

The game stores an array of controller instances (`c[]`) and a parallel byte array of event/demo IDs. The dispatch helper `kVoid(id)` searches the ID array, selects the matching controller, and calls its `aVoid()` update method. This is a lookup by event ID, not by tile ID. A separate `aVoid(id)` method on class `c` parses the corresponding demo record from `demo.f`.

The parsed demo record includes:
- a list of sprite IDs needed by the event; these are resolved from `demoSpr.bin`,
- a count of event commands,
- typed command records with their own payload layouts,
- one or more command groups that can be nested/packed into a composite command buffer.

The parser reads the demo record from a chunk in `demo.f`, then sorts sprite IDs before loading them. It constructs a sprite cache indexed by the IDs in `demoSpr.bin`. The implementation has unchecked stream reads and should not be copied as a native parser.

### 18.2 Command types observed

The parser recognizes these command opcodes and builds normalized runtime records. The semantic descriptions below are based on the update/render handlers; some are still only partially understood.

| Opcode | Parsed payload (source-level) | Observed runtime role |
|---:|---|---|
| 0 | Nested command count, followed by nested command records | Composite/group container; parser flattens children into a packed command list |
| 1 | X, Y, duration, optional target camera X/Y (16-bit each) | Camera interpolation toward a target; sentinel 10000 means use current camera target |
| 2 | Text length, Y, text length/data fields | Timed text/dialogue display with vertical placement and scrolling/advance state |
| 4 | Seven 16-bit fields | Sprite-frame drawing/interpolation command; includes sprite and frame identifiers plus position/timing data |
| 5 | X, Y, one-byte object code | Writes a player/object-layer value at a grid coordinate |
| 6 | 32-bit duration/value | Timed wait command |
| 7 | No payload | Parsed as a short command marker; exact independent behavior unresolved |
| 8 | X, Y, 16-bit value | Shares the object-layer update path with opcode 9 in the update handler |
| 9 | X, Y, 16-bit value | Writes a player/object-layer value at a grid coordinate; update switch falls through from 8/9 handling |
| 10 | One-byte input/action value | Updates current scripted input/action state and calls the game's action/update path |
| 11 | Two 16-bit values | Timed command; completion is gated by controller tick count |
| 12 | X, Y (16-bit) | Camera/player-follow visual rectangle effect; renderer interpolates a white rectangle around the player |
| 13 | X, Y, duration (16-bit) | Camera interpolation between coordinates |
| 14, 15 | No payload | Short marker commands; update logic treats them as completed/visual state transitions |
| 16, 17 | 16-bit value + one-byte duration/state | Timed visual/effect commands; update processing occurs on alternating controller ticks |
| 18 | One-byte state + 24-bit color | Full-screen color overlay/fade |
| 25 | X, Y, two one-byte values | Writes a packed foreground-layer cell value |
| 26 | X, Y, 32-bit value | Writes a packed background/object-state cell value |
| 27 | Text length + text bytes | Timed dialogue/text panel; text is mapped through demo text mapping |

Some command handlers intentionally fall through to subsequent cases in the decompiled switch. The table describes combined observed behavior and is not a guarantee that each opcode is an isolated operation.

### 18.3 Update/render lifecycle

- `aVoid()` advances the active event command(s) according to a wall-clock throttle (static interval initialized to 1000 ms) and controller state.
- `bVoid()` advances command progression and updates command completion flags.
- `cVoid(command)` performs per-command tick/update behavior, including timers, input state, map-cell writes, and dialogue progression.
- `aVoid(Graphics)` renders the active command(s), including camera transitions, sprite animation frames, text/dialogue panels, and color overlays.
- The event controller can expose completion via `aBoolean()`; composite commands are considered complete only when their child command records are complete.

This helper is therefore a small scripted cutscene/demo interpreter, not simply a camera helper. It combines command parsing, timing, gameplay input, stage mutation, camera movement, and rendering. The native architecture should split these concerns into a validated event bytecode parser, deterministic event runner, stage command interface, camera controller, and presentation layer.

### 18.4 Native implementation constraints

1. Preserve the original 240×320 logical coordinate space for event rendering.
2. Parse command lengths and nested groups with strict bounds checks; reject unknown opcodes safely or preserve them as opaque records.
3. Do not trust sprite IDs or text indexes until they are validated against the loaded catalog.
4. Use a monotonic clock and deterministic tick inputs rather than relying on the original 1000 ms wall-clock gate.
5. Separate commands that mutate gameplay state from commands that only draw overlays.
6. Do not copy `demo.f`, `demoSpr.bin`, or proprietary sprite payloads into DIRU-NA until provenance and licensing review allows it.

## 19. Save and preferences storage — confirmed storage mechanism, layout still open

The candidate uses Java ME RMS record stores:
- `DiamondRush`, record ID 1: main progress/state byte array `iByteArr`.
- `Preferences`, record ID 1: preferences byte array `jByteArr`.

The main progress buffer is initialized with 1001 bytes in one new-game path, but a separate `cwInt` tracks the number of meaningful bytes written/read. The game updates the main record through `setRecord(1, ...)` or `addRecord(...)`. Preferences use a separate RMS store.

**Native implication:** use Android app-private storage (for example, a versioned database or bounded binary file) with atomic writes and migration support. Do not map RMS record bytes directly into the new format until every offset, field type, version marker, and checksum/length rule has been traced. Preserve independent progress and preferences domains.

## 20. Audit-002 follow-up priorities

1. Trace event ID source and map stage/demo IDs to each controller instance.
2. Validate every demo command record length against parser consumption and nested group boundaries.
3. Trace stage mutation opcodes 5, 8, 9, 25, and 26 to establish how scripted events alter gameplay.
4. Trace all uses of `iByteArr` and `jByteArr` with exact offsets to produce a save/preferences schema.
5. Determine whether checkpoint snapshots are temporary in-memory state only or are persisted across application restarts.
6. Keep event/cutscene interpretation independent from the core gameplay state machine.

## 21. Main save record — partial byte-layout reconstruction

Source trace: `i.java` at candidate revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`. This section records only fields whose use is visible in the current static trace; it is not yet a complete compatibility specification.

### 21.1 Record envelope and header

The main save is RMS store `DiamondRush`, record 1. New-game initialization allocates a 1001-byte buffer, initializes bytes 3, 8, and 9, and sets a separate meaningful-length counter. The buffer is not necessarily 1001 bytes of meaningful data.

| Offset | Observed use | Confidence / caveat |
|---:|---|---|
| 0 | Incremented while scanning map tiles whose map byte equals 2; used as a count/header value during save construction | High for count behavior; semantic label provisional |
| 1 | Bit field during world/unlock initialization; bit 0 is set when a world unlock condition is met | High for bit-field behavior; exact mapping still open |
| 2 | Bit field: low bits feed gameplay configuration; bits 3 and 4 are checked/set in item/shop progression; bits 5–7 encode a tier/value | High for bitfield use; exact names and masks still open |
| 3 | Current/selected progression index copied from/to `azInt` | High |
| 4–5 | Little-endian 16-bit value `aZInt`; read/written by shop logic | High for storage; UI label/units need confirmation |
| 6–7 | Little-endian 16-bit secondary value `bbInt`; shop code increments it and checks world-price thresholds | High for storage; semantic label provisional |
| 8 | Hero progression value: initialized to 4, used as hero palette offset (`value - 4`), and passed to damage handling | High for behavior; do not assume this is simply health |
| 9 | Progression/unlock tier: values 1, 2, and 8 assigned at story/seal milestones and used to gate features | High |
| 10 | Boolean-like state used by gameplay and checkpoint restore; initialized to 0 and toggled in gameplay | High for flag behavior; semantic name open |
| 11 | Small state byte `rByte`, persisted by `wVoid()`/`xVoid()` | High |
| 12 | Small progression counter incremented and clamped to 3 in a story flow | High |
| 13 | Boolean-like tutorial/first-run flag set to 1 by `bVoid(boolean)` | High |
| 14–19 | Three little-endian 16-bit offsets, one per world data block | High |

### 21.2 World and stage subrecords

The game has three world resources (Angkor, Scotland, Tibet). Header offsets at bytes 14, 16, and 18 point to each world's block. Each world block contains:
- a world header/progress byte,
- a maximum/current stage progress byte,
- a stage-offset table (16-bit little-endian entries),
- per-stage records.

The stage record base is resolved from the selected world's offset table plus a stage index multiplied by two. A stage record exposes at least:
- a stage flag byte (offset +2),
- a completion/collected counter (offset +3),
- a required/target count (offset +4),
- coordinate pairs beginning at offset +5, two bytes per pair.

The coordinate pairs track map locations for a stage. Matching a coordinate can clear that pair and increment the stage record's counter. The source uses this structure for map/seal-like collectible tracking. Exact coordinate meaning and the first two bytes' full semantics remain under audit.

### 21.3 Save construction and migration implications

During new-save construction, the game reads each world map, scans dimensions and map layers, counts cells with marker value 2, and records coordinates for marker values 14 or 33. It writes per-stage offsets into the world's stage-offset table. This is a generated index over map content, not a simple fixed list of stage completion booleans.

Native DIRU-NA should not import this byte layout as its own storage model. Recommended approach:
- Define a typed, versioned native save schema.
- Store world/stage progress and collected coordinate identities as explicit records.
- Preserve source mapping only in a migration/import adapter if lawful and technically necessary.
- Add integrity checks, bounded parsing, atomic writes, and forward-compatible unknown-field handling.
- Do not use the old candidate's 1001-byte allocation as a native schema requirement.

### 21.4 Remaining open questions

1. Trace all reads/writes of header bytes 0–13 to give stable semantic names.
2. Confirm exact world block layout and stage table bounds for all three worlds.
3. Determine whether the coordinate list represents seals, map pickups, or multiple object classes.
4. Confirm whether `jByteArr` preferences remain one byte for all source configurations; visible initialization uses one byte and only explicitly writes index 0.
5. Trace save write timing and failure recovery around RMS operations.

## 22. Checkpoint persistence boundary — in-memory snapshot confirmed

The source methods `saveCheckpoint()` and `restoreCheckpoint()` were inspected directly.

Observed snapshot contents include:
- selected player/game counters and player grid coordinates,
- currency/score-like counters and selected gameplay state bytes,
- copies of the active stage's background/object state, animation/timer bytes, player layer, and foreground layer,
- optional feature-specific arrays and moving-object state when the corresponding mode is active.

Restore copies those arrays back into the active stage and rebuilds selected transient effects/animation state. It also resets some transient input/animation fields and recenters the camera around the saved player coordinates.

**Persistence conclusion:** the checkpoint snapshot is held in static arrays and instance fields in memory. The inspected `saveCheckpoint()` and `restoreCheckpoint()` methods do not serialize the snapshot into RMS or call the save-record writer. Therefore, this checkpoint is a gameplay respawn snapshot, not evidence of a durable checkpoint across application termination. The ordinary progress save and the temporary checkpoint snapshot must remain separate concepts in DIRU-NA.

Native implication: model checkpoint snapshots as an in-memory stage-state snapshot with explicit copy/restore semantics. App termination should reload durable progress and stage initialization, not silently promise resuming the last temporary checkpoint, unless a later product decision deliberately adds a new native feature.
