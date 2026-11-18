# DIRU-NA-AUDIT-006 — Sprite Resource Mapping & Animation Reconstruction

## Scope and evidence boundary

This document records static-source findings from the read-only candidate repository `kubikaugustyn/DiamondRushSource`, pinned for this audit at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Primary inspected files: `src/main/java/f_Sprite.java`, `src/main/java/b_SpriteAnimator.java`, and `src/main/java/i.java`.

No resource payload was copied, decoded into distributable assets, or executed. The mappings below are code-path evidence, not visual identification. Slot meanings are intentionally left unknown unless the source explicitly establishes them. Resource ownership/licensing remains unresolved.

## 1. Sprite data model

The candidate parser in `f_Sprite.parseSprite(byte[], int)` reads these structures in order:

1. Module count and module dimensions (width, height).
2. Frame-module records: module index, X/Y offsets, flags.
3. Frame table: module count per frame, first module index, and four-byte bounding-box records.
4. Animation-frame records: frame index, duration/time, X/Y offsets, flags.
5. Animation table: frame count per animation and first animation-frame index.
6. Palette section: pixel format, palette count, colors per palette, palette data.
7. Module pixel data: encoded-data format and length-prefixed module payloads.

The parser has no comprehensive bounds validation in the inspected implementation and catches broad exceptions. The Native implementation must use a bounded cursor, checked arithmetic, explicit endianness, and fail-closed validation before allocating buffers or indexing tables.

### Pixel formats observed in source

| Pixel format | Source constant | Interpretation in source |
|---|---:|---|
| 8888 | 0x8888 | 32-bit RGBA-like packed palette entries |
| 4444 | 0x4444 | 4-bit alpha/red/green/blue expanded to 8-bit channels |
| 1555 | 0x5515 | 1-bit alpha plus 5-bit RGB |
| 0565 | 0x6505 | RGB565; source treats 0xF81F as transparent |

### Encoded module formats observed

| Encoding | Source constant | Decoder behavior |
|---|---:|---|
| I256 | 0x5602 | One palette index per pixel |
| I16 | 0x1600 | Two 4-bit palette indices per byte |
| I4 | 0x0400 | Four 2-bit palette indices per byte |
| I2 | 0x0200 | Eight 1-bit palette indices per byte |
| I256RLE | 0x56F2 | Run/literal decoding using a high-bit marker |
| I127RLE | 0x27F1 | Palette-index run or literal decoding |

These names and constants are copied from the parser's own declarations; their actual binary coverage and malformed-input behavior still require independent fixtures.

## 2. Frame composition and animation timing

A frame is composed from one or more modules. Each frame-module record contains a module index, signed byte offsets, and low-bit transform flags. The renderer applies horizontal/vertical flips and offsets, then draws each module.

An animation frame record contains:
- frame index (byte),
- duration/time (byte),
- X offset (byte),
- Y offset (byte),
- flags (byte).

The animator tracks animation index, frame index, elapsed tick, position, and flip flags. `setAnimation` resets frame/timer/offset state only when the animation index changes. `tick` advances when the stored duration is reached; duration 0 is treated as non-advancing. The sequence loops to frame 0 after the last frame. `animationEnded()` reports the final tick of the final frame; it does not itself stop or hold the animation.

The constructor accepts sprite and position/parent arguments but the inspected body initializes position to zero and parent to null. This discrepancy must be preserved as an observed implementation quirk until call-site behavior is fully traced.

## 3. Resource chunk to runtime slot mappings

### 3.1 `gen0.f` through `gen4.f`

The loader iterates a flattened generated-resource chunk index across five files, with ten chunks per file except the final partial group. The source maps selected chunk IDs to runtime sprite slots or texture arrays:

| Flattened chunk ID | Resource chunk | Runtime destination | Notes |
|---:|---|---|---|
| 0 | gen0.f / 0 | slot 33 | Sprite |
| 1 | gen0.f / 1 | slot 34 | Sprite |
| 2 | gen0.f / 2 | textures[24], conditionally textures[25] | Conditional water-breathing/world palette path |
| 3 | gen0.f / 3 | slot 7 | Sprite |
| 4 | gen0.f / 4 | slot 3 only outside world index 2 | Conditional |
| 5 | gen0.f / 5 | slot 58 | Sprite |
| 6 | gen0.f / 6 | textures[21] | Texture array |
| 7 | gen0.f / 7 | slot 20 | Sprite; palette path changes in world index 2 |
| 8 | gen0.f / 8 | slot 5 | Sprite |
| 9 | gen0.f / 9 | slot 12 | Sprite |
| 10 | gen1.f / 0 | animator slot 4, animation 0 | Animated sprite |
| 11 | gen1.f / 1 | slot 11 | Sprite |
| 12 | gen1.f / 2 | textures[10] | Texture array |
| 13 | gen1.f / 3 | slot 1 | Sprite |
| 14 | gen1.f / 4 | textures[6] | Texture array |
| 15 | gen1.f / 5 | slot 4 when mode bit set; slot 21 built with palette 1 under another mode bit | Conditional |
| 16 | gen1.f / 6 | textures[13] | Texture array |
| 17 | gen1.f / 7 | slot 4 when mode bit set; slot 21 built with palette 1 under another mode bit | Conditional |
| 18 | gen1.f / 8 | slot 37 | Sprite |
| 19 | gen1.f / 9 | textures[7] | Texture array |
| 20 | gen2.f / 0 | slot 2 | Sprite |
| 21 | gen2.f / 1 | slot 3 only outside world index 2 | Conditional |
| 22 | gen2.f / 2 | slot 8 | Sprite |
| 23 | gen2.f / 3 | slot 30 | Sprite |
| 24 | gen2.f / 4 | slot 57; palette 1 also created | Sprite/texture condition |
| 25 | gen2.f / 5 | slot 17 | Sprite |
| 26 | gen2.f / 6 | slot 19 | Sprite |
| 27 | gen2.f / 7 | `i.dClassf` | Special sprite reference |
| 28 | gen2.f / 8 | slot 45 | Sprite; palette 1 conditionally created |
| 29 | gen2.f / 9 | textures[26] | Texture array |
| 30 | gen3.f / 0 | slot 15 | Sprite |
| 31 | gen3.f / 1 | textures[29] | Texture array |
| 32 | gen3.f / 2 | slot 32 | Sprite |
| 33 | gen3.f / 3 | slot 22 | Sprite |
| 34 | gen3.f / 4 | slot 27 | Sprite |
| 35 | gen3.f / 5 | slot 28 | Sprite |
| 36 | gen3.f / 6 | slot 35 | Sprite |
| 37 | gen3.f / 7 | slot 29 | Sprite |
| 38 | gen3.f / 8 | slot 36 | Sprite |
| 39 | gen3.f / 9 | slot 6 | Sprite |
| 40 | gen4.f / 0 | slot 2 | Sprite; distinct generated chunk |
| 41 | gen4.f / 1 | slot 38 | Sprite |
| 42 | gen4.f / 2 | slot 39 | Sprite |

The loader only creates/assigns selected resources when the corresponding load mask requests them. Some slots are palette- or world-dependent. The table describes assignment destinations, not a claim that all resources are simultaneously resident.

### 3.2 `cm.f`

| Chunk | Runtime destination | Notes |
|---:|---|---|
| 0 | textures[11] | Palette 0 image array |
| 1 | slot 56 | World-dependent palette replacement for world indices 1 and 2 |
| 2 | slot 59 | Palette 0 plus palette 1 |
| 3 | slot 13 | Calls `rVoid()` after assignment |
| 4 | textures[5] | Palette 0 image array |
| 5 | textures[18] | Palette 0 image array |
| 6 | slot 43; textures[20] cleared | Sprite |
| 7 | Not assigned in inspected switch | Chunk still parsed; no destination shown |

The code's range check is `n57 < 8`, despite the comment saying chunks 0–8. Treat chunk 8 as unconfirmed/unreachable through this branch pending deeper control-flow inspection.

### 3.3 Other directly observed resource assignments

| Resource | Chunk | Destination | Evidence |
|---|---:|---|---|
| `ui.f` | 0 | animator slot 0, animation 0 | UI animation |
| `ui.f` | 2 | slot 0 | Top/bottom UI load path |
| `ui.f` | 3 | slot 18 | UI sprite |
| `ms.f` | 0 | slot 17 | Sprite |
| `ms.f` | 1 | slot 23 | Sprite |
| `mmv.f` | 1 | slot 54 | Sprite; dimensions used for half-width/height |
| `mmv.f` | 2 | slot 53 | Sprite |
| `mmv.f` | 3 | slot 52 | Sprite |
| `mm1.f` | 0 | animator slot 5, animation 0 | Animated sprite |
| `mm0.f` | 2 | animator slot 2, animation 0 | Animated sprite |
| `mm0.f` | 1 | animator slot 1, animation 2 | Animated sprite |
| `o.f` | 0 | animator slot 0 | Hero sprite; palette = save byte 8 minus 4 |
| `o.f` | 1 | animator slot 3 | Fire animation, only when requested |
| `0.f`, `1.f`, `2.f` | 0–3 | slots 60, 16, 42 or textures[8] depending on chunk | Conditional generated load path |

The hero is loaded from `o.f` chunk 0. The palette index is derived from `i.iByteArr[8] - 4`. This establishes a palette selection dependency; it does not establish the gameplay meaning of save byte 8.

## 4. Renderer implications for Native Android

- Preserve the distinction between modules, composite frames, and animation timelines. Do not flatten every animation into unrelated bitmaps.
- Store signed offsets and transform flags explicitly; test horizontal and vertical flips independently and in combination.
- Keep per-resource palette selection explicit. Several resource paths switch palettes by world or mode.
- Preserve per-frame duration and offsets. A single global animation FPS is not equivalent to the source data.
- Use deterministic update ticks independent of Android draw frequency. Rendering should interpolate only if it does not change discrete frame/tick behavior.
- Separate decoded pixel buffers from Android bitmap caches. Cache keys must include resource identity, module, and palette.
- Keep resource slot IDs as temporary compatibility identifiers in the audit layer only; the Native design should introduce semantic asset IDs once visual mapping is supported by permitted fixtures.
- Validate all counts, offsets, dimensions, palette indices, encoded lengths, and output pixel counts before decoding.

## 5. Open questions and follow-up

- Decode or inspect legally usable visual fixtures to identify actual hero, enemy, object, UI, and environmental artwork. No visual identity is assigned in this document.
- Trace each sprite slot's creation mask and lifetime, including resource unload/reload behavior.
- Confirm whether frame bounding boxes are used for collision or only rendering/metadata.
- Resolve signedness and transform semantics for animation-frame offsets and frame-module offsets with fixtures.
- Validate each encoding with synthetic byte fixtures, including truncated streams and runs that exceed expected pixel count.
- Trace hero animation indices and enemy/object state-to-animation transitions.
- Confirm the `cm.f` chunk 8 discrepancy and all chunk-count boundaries.
- Rights/provenance review remains a prerequisite to redistributing original assets or decompiled code.

## 6. Audit status

**Completed at static-source level:** parser structure, palette/encoding constants, frame composition, animator timing, and partial resource-chunk-to-slot mappings.

**Not completed:** visual asset identification, complete slot semantic names, binary fixture validation, runtime comparison, complete animation-state mapping, and licensing clearance.

No Android project build, APK, or runtime test was performed as part of this audit.
