# DIRU-NA-AUDIT-005 — Tile and Entity Dictionary Reconstruction

**Status:** Partial static reconstruction; complete visual/name mapping and runtime validation remain open  
**Audit input:** `kubikaugustyn/DiamondRushSource`  
**Reviewed revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Primary sources:** `src/main/java/i.java`, `src/main/java/f_Sprite.java`, `src/main/java/b_SpriteAnimator.java`  
**Development target:** `yogiags56/Project-DIRU-NA` only

> This audit records what the decompiled source establishes. Numeric tile/object IDs are not assigned familiar gameplay names unless source behavior supports the label. No binary map or sprite asset was copied, decoded into the active project, or redistributed. No runtime or handset test was performed.

## 1. Stage map container and layer layout

The stage loader in `i.java` selects one of three world files:

- `/map_angkor.out`
- `/map_scotland.out`
- `/map_tibet.out`

The loader reads a leading byte, then a stage count for each world section. For each stage it reads a four-byte dimension header:

- Width: unsigned 16-bit little-endian
- Height: unsigned 16-bit little-endian

For the selected stage, it allocates and reads three consecutive byte arrays, each of size `width * height`:

1. **Player/object layer** — stored in `crtStagePlayerLayer`
2. **Background layer** — stored in `bIntArrArr`
3. **Foreground layer** — stored initially in `crtStageForegrondLayer`

The loader converts the flat byte stream into arrays addressed as `[x][y]`, while the source stream index is `x + y * width`.

Important distinction: the background values are subsequently used as metadata/parameters by initialization logic, not simply as decorative pixels. The foreground array is converted to a packed integer in many cases: low byte is a foreground type code, upper bits carry per-cell metadata. The player/object layer is also transformed during stage initialization.

The original loader uses single `InputStream.read` and `skip` calls. A Native Android parser must use bounded read-fully/skip-fully operations, validate dimensions and multiplication overflow, and reject truncated or malformed data.

## 2. Layer semantics

### 2.1 Player/object layer

Despite the original field name, this layer is not only the hero. It stores dynamic and static entities, object footprints, stage markers, and other interactive elements. Values are signed Java bytes in the source; comparisons and switch statements therefore require careful unsigned/signed interpretation.

The empty-cell sentinel is commonly `-1` (byte 0xFF). Values below 80 are generally treated as ordinary map/entity codes in several initialization and rendering paths, while values at or above 80 are used by other special logic. This is a branch-specific convention, not a universal format guarantee.

### 2.2 Background layer

The background layer begins as one byte per cell. During stage initialization, values are used as:

- metadata paired with foreground cells,
- object configuration and orientation flags,
- counters/weights/parameters,
- inputs to entity initialization.

For some entities the code rewrites background values or packs flag bits into an integer state array (`bIntArrArr`). Do not flatten this into a simple visual background tile map.

### 2.3 Foreground layer

The foreground layer starts as one byte per cell and is expanded to a 32-bit integer during initialization. The low byte identifies the foreground type. Upper bits are reused for different metadata depending on the type: indexes, counters, state flags, and animation selection.

Some values are replaced or removed during stage initialization based on the player/object layer in the same or neighboring cells. Therefore, the runtime map is not always byte-for-byte identical to the serialized map.

## 3. Foreground code catalog — partial

The following table uses source initialization, interaction, and rendering branches. “Confirmed behavior” is intentionally narrow.

| Low-byte code | Observed source behavior | Classification |
|---:|---|---|
| 0 | Counted during stage initialization; assigned a dynamic update slot | Dynamic/animated foreground; exact visual identity open |
| 1 | Preserved with background metadata; collision helper has a dedicated interaction branch and updates a timer/counter | Interactive foreground; visual identity open |
| 2 | Sets progression/world-state flags; upper metadata is read during player interaction | Progression/gate-related foreground; exact graphic/name open |
| 3 | Initializes a timer/state byte; upper metadata is adjusted; renderer uses an animation/frame index derived from upper byte | Animated foreground; exact visual identity open |
| 4 | Increments checkpoint count; sets checkpoint flag; records checkpoint coordinates; activation schedules an in-memory checkpoint snapshot | **Checkpoint** — behavior confirmed |
| 5 | Stores a stage transition coordinate/parameter; collision path sets scripted-transition state | Transition/exit-related trigger; exact displayed object name open |
| 6 | Allocates a dynamic update slot and sets a stage capability flag; renderer computes vertical animation/offset | Animated/hazard-related foreground; exact identity open |
| 7 | Stores door coordinates by metadata index; renderer draws from sprite resource index 56; interaction/progression logic reads metadata | **Door** — behavior confirmed |
| 8 | Sets a water-related capability flag and is rendered with a dedicated sprite family | Water/liquid-related foreground; exact subtype open |
| 9 | Sets a water-breathing-potion requirement flag and is rendered with a dedicated sprite family | Water/liquid-related foreground; exact subtype open |
| 14 | Uses animation resource index 8 and metadata as animation frame; source can mark its upper byte as 0xFF during initialization | Animated special foreground; exact name open |
| 17 | Neighbor-dependent initialization; may be removed or transformed based on object-layer contents | Structural/interactive tile; exact identity open |
| 19 | Preserved with background metadata and has special movement/interaction handling | Special foreground; exact identity open |
| 26 | Preserved with metadata and has a dedicated rendering/interaction path in later code | Special foreground; exact identity open |
| 28 | Stores a stage transition coordinate/parameter; collision path sets scripted-transition state | Transition/exit-related trigger; exact displayed object name open |
| 30 | Counted and allocated a dynamic update slot; shares part of the initialization path with code 0 | Dynamic foreground; exact identity open |
| 31 | Sets a stage capability flag and is normalized to foreground type 31 with background metadata | Special foreground; exact identity open |
| 33 | Uses animation resource index 22 and metadata as animation frame; can be transformed by initialization | Animated special foreground; exact name open |
| 34 | Sets a stage capability flag; renderer uses a special animation from sprite resource index 27 | Special animated foreground; exact identity open |

This table is not a complete list of all possible codes. It records only codes with traceable behavior in the reviewed initialization, collision, and rendering paths.

## 4. Player/object layer code catalog — partial

| Code | Observed source behavior | Classification |
|---:|---|---|
| -1 / 0xFF | Empty sentinel used when clearing cells and testing occupancy | Empty cell |
| 0–9 | Several values take part in push/movement and dynamic object initialization; meanings differ by code and state | Object family; individual labels open |
| 10 | Initializes a state flag in the separate object state array | Special entity/state |
| 11 | Sets object state and dynamic timer; contributes a stage capability flag | Dynamic entity; exact identity open |
| 12 | Removed from the layer and its coordinates/background value saved into dedicated fields | Stage marker/entity; exact identity open |
| 14 | Sets stage capability flag and initializes a dynamic timer | Dynamic entity; exact identity open |
| 16 | Has a vertical multi-cell relationship with the adjacent cell and uses animation/timer data | Multi-cell entity/structure; exact identity open |
| 18 | Initializes dedicated counters and stage capability flags | Special stage entity; exact identity open |
| 19 | Contributes a world-specific capability flag and a dynamic update timer | Dynamic entity; exact identity open |
| 22–23 | Contribute stage capability flags and dynamic timers | Dynamic entity family; exact identity open |
| 24, 26, 27 | Create dynamic update entries and stage capability flags; code 26 also enables an additional capability | Dynamic entity family; exact identity open |
| 28 | Sets a stage capability flag; may encode additional state in background metadata | Dynamic entity; exact identity open |
| 30 | Sets a stage capability flag and resets associated state | Special entity/state |
| 31, 33 | Retained in the object layer during initialization; semantics depend on later branches | Special entity/marker |
| 34 | Removed from the object layer and replaced with foreground code 15 | Initialization marker/transform |
| 35 | Retained in the object layer, removes foreground at same cell, sets a stage capability flag and fan-pot loading flag | Fan-pot-related marker/entity; exact visible behavior open |
| 36 | Normalizes associated background state and sets a stage capability flag | Special entity/state |
| 37 | Sets a stage capability flag and resets background state | Special entity/state |
| 38–39 | Set special stage capability flags; code 38 is transformed into foreground code 27 | Initialization marker/entity |
| 40 | Creates a dynamic update entry, sets stage capability flags, and enables a mode flag | Dynamic entity; exact identity open |
| 41 | Adds a background-derived quantity to a stage counter | Collectible/quantity-bearing entity; exact identity open |
| 42 | Creates a dynamic update entry and sets capability flags | Dynamic entity; exact identity open |
| 43 | Sets a world-specific capability and dynamic timer; updates packed state bits | Dynamic entity; exact identity open |
| 44–46 | Initialize timers/state and capability flags | Dynamic entity family; exact identity open |
| 47 | Initializes a timer and stage capability flag | Dynamic entity; exact identity open |
| 48 | Creates a vertically adjacent second cell and participates in special two-cell movement logic | **Two-cell movable entity** — footprint behavior confirmed; visual identity open |
| 49 | Sets a stage capability flag and dynamic timer | Dynamic entity; exact identity open |
| 53–54 | Appears in state/animation branches, but its initialization semantics are not yet fully traced | Special state/entity; identity open |
| 79 | Sets player start X/Y and camera initialization coordinates, then clears its own cell | **Player start marker** — behavior confirmed |

The code values above are not a complete dictionary. Some values are consumed by the stage initialization pass and no longer remain in the runtime layer. Others are dynamic entities whose state is spread across the background/object state arrays.

## 5. Rendering linkage established so far

The renderer scans the visible stage grid, reads the foreground low byte and object-layer byte, then selects sprite resources and animation frames.

Examples directly visible in the source:

- Foreground code 7 draws a frame from `aClassfArr[56]`; metadata determines the frame index.
- Foreground code 14 uses `aClassfArr[8]`, animation 0, and metadata-derived frame.
- Foreground code 33 uses `aClassfArr[22]`, animation 0, and metadata-derived frame.
- Foreground code 34 uses `aClassfArr[27]`, animation 2, frame 0.
- Foreground codes 8 and 9 use sprite resources 22 and 23 respectively; upper metadata selects animation states.
- Foreground codes 5 and 28 share a renderer path using sprite index 11, with selected stages changing a frame parameter.
- Foreground codes 15 and 16 use animation/effect state and are drawn conditionally.
- Object code 16 is treated as a two-cell vertical structure in initialization/rendering.
- Object code 48 is treated as a multi-cell movable object with extra state in the object-state array.

These resource indexes are runtime sprite slots, not original filenames. The resource-loading table that maps every slot to a file/chunk/palette must be documented separately before naming visual entities.

## 6. Sprite binary format — parser structure

The candidate's `f_Sprite.parseSprite()` reads a structured sprite container. The parser exposes these sections:

1. Module count and module dimension records.
2. Frame-module records: module index, X/Y offsets, flags.
3. Frame table: module count, first module index, and frame bounding boxes.
4. Animation-frame table: frame index, duration, X/Y offsets, flags.
5. Animation table: frame count and first animation-frame index.
6. Palette section: pixel format, palette count, colors per palette, then palette data.
7. Module image data section: data format followed by per-module length and payload.

Pixel formats explicitly handled in the parser:

- 8888
- 4444
- 1555
- 0565

The parser's module data format dispatch includes the previously inventoried packed image encodings (including indexed and RLE variants). Full validation of every decoder branch, malformed-length behavior, and palette edge cases remains open.

The renderer composes frames from modules, applies flips/offsets, and uses animation-frame durations to advance the animation. `b_SpriteAnimator` tracks animation index, current frame, timer, offsets, and optional parent animator.

**Native implication:** preserve the distinction between module, frame, animation, and palette. A sprite is not necessarily one image per tile/entity. The Android renderer should reconstruct composed frames from parsed modules and animation tables only after a format parser is independently validated.

## 7. Map and sprite interpretation limits

- No image-to-code mapping was performed in this audit because binary resources were not decoded or visually inspected.
- No claim is made that foreground code 8/9 maps to a particular water visual, that object code 48 is a boulder, or that any opaque object code is a snake/spider/trap without sprite/resource correlation.
- Gameplay descriptions from external databases can help build a list of hypotheses, but cannot replace code/resource evidence for this exact candidate package.
- World labels in the source file names are Angkor, Scotland, and Tibet. Other descriptions may call the second region Bavaria or use another regional name; this audit retains the source filenames and does not reconcile localization/version differences.
- The project’s 240×320 portrait initial viewport is a separate implementation decision. The map format and 24-unit tile scale do not independently prove the original device display profile.

## 8. Native Android data model proposal

The source's packed arrays should not become the Native Android public save format. Proposed typed model:

- `StageMap`: width, height, source-format version, and validated layer arrays.
- `ForegroundCell`: type code, raw metadata, decoded metadata when known.
- `EntityCell`: type code, raw parameters, runtime state, footprint identity.
- `EntityFootprint`: stable entity ID and one or more occupied grid cells.
- `TileDefinition`: collision policy, interaction policy, render binding, animation binding.
- `SpriteDefinition`: modules, frames, animations, palettes, pixel format.
- `ResourceBinding`: logical asset key mapped to a source file/chunk/palette, with provenance/license status.
- `UnknownCode`: preserve unknown code and raw bytes without assigning an invented meaning.

The parser must validate dimensions, cell count, section boundaries, offsets, counts, palette indexes, module indexes, frame references, and animation references. Unknown but structurally valid codes should be preserved as opaque values, not silently discarded.

## 9. Follow-up work required

1. Complete the sprite-slot-to-resource-file/chunk/palette table.
2. Validate and document every sprite image decoder, including RLE and indexed formats.
3. Build a source-code-to-sprite-frame mapping for the foreground codes listed above.
4. Build the same mapping for player/object-layer codes and multi-cell footprints.
5. Trace the dynamic update system that changes entity codes during gameplay.
6. Trace all interactions for water, traps, doors, chests, exits, enemies, collectibles, and movable objects.
7. Decode only legally usable fixtures and preserve provenance; do not copy assets into Project DIRU-NA at this stage.
8. Build synthetic map fixtures for parser and collision tests.
9. Validate map parser behavior for truncated streams, zero/oversized dimensions, and integer overflow.
10. Verify the resource format against original Nokia JAR/JAD evidence if independently available.

## 10. Evidence links

- Pinned candidate revision: https://github.com/kubikaugustyn/DiamondRushSource/tree/215c71d04057d66a1b08419b1a4b1043ae3e0881
- Stage loader and entity initialization: https://github.com/kubikaugustyn/DiamondRushSource/blob/215c71d04057d66a1b08419b1a4b1043ae3e0881/src/main/java/i.java
- Sprite parser and renderer: https://github.com/kubikaugustyn/DiamondRushSource/blob/215c71d04057d66a1b08419b1a4b1043ae3e0881/src/main/java/f_Sprite.java
- Sprite animation controller: https://github.com/kubikaugustyn/DiamondRushSource/blob/215c71d04057d66a1b08419b1a4b1043ae3e0881/src/main/java/b_SpriteAnimator.java
- External gameplay description (general mechanics only): https://www.mobygames.com/game/26446/diamond-rush/

## 11. Validation status

| Area | Result |
|---|---|
| Three world map filenames | Confirmed |
| Stage header and three-layer layout | Confirmed statically |
| Foreground packed metadata | Confirmed statically |
| Foreground code catalog | Partial |
| Object/entity code catalog | Partial |
| Player start marker | Confirmed by initialization behavior |
| Checkpoint foreground code 4 | Confirmed by AUDIT-004 and initialization |
| Door foreground code 7 | Confirmed by coordinate and renderer paths |
| Two-cell object code 48 | Footprint behavior confirmed; visual identity open |
| Sprite parser section layout | Partially documented |
| Sprite slot-to-file mapping | Incomplete |
| Code-to-visual mapping | Not performed |
| Binary fixture decoding | Not performed |
| Runtime/handset validation | Not performed |
| Native Android implementation | Not started by this audit |

No APK, build, runtime test, decoded asset pack, or playable milestone was produced by this audit.
