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

This is strong static evidence that the reviewed candidate is configured for a portrait logical canvas of 240 × 320, not the historical 320 × 240 landscape target. However, this does not by itself establish how every original Nokia handset rotated or presented the game. Before choosing Android orientation, compare original Nokia device/build evidence and inspect actual game screenshots or a legally usable original binary. Keep the logical viewport configurable until that verification is complete.

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
| Viewport | Multiple 240 × 320 drawing/camera constants; 24 px tile calculations | Original Nokia device orientation/presentation |
| Movement | Direction values, state gating, contextual action/checkpoint/respawn paths | Complete movement, collision, and interaction state machine |
| Camera | Interpolated/clamped camera helper in `c.java` | All event opcodes and camera timing semantics |

No binary stage package has been extracted or decoded in this pass, and none of these observations has been runtime-tested.
