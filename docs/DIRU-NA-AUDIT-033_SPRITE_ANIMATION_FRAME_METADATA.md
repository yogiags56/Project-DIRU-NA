# DIRU-NA-AUDIT-033 — Sprite Animation Tables & Frame Metadata Reconstruction

## Scope and evidence boundary

This audit reads `f_Sprite.java`, `b_SpriteAnimator.java`, and the relevant resource chunks from the read-only candidate `kubikaugustyn/DiamondRushSource` pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. It extracts animation-table metadata for the exact chunks used by the four special encounters. It does not copy image payloads into Project-DIRU-NA or visually identify the characters/frames.

Resource chunk offsets were interpreted using the candidate's `readChunk()` implementation: the one-byte directory count and `count × 8` directory bytes precede the chunk-data area; directory offsets are relative to that data area. Each selected chunk was then parsed following `f_Sprite.parseSprite()` field order.

## 1. Sprite parser structure confirmed

`f_Sprite.parseSprite()` reads these sections in order:

1. Six-byte leading header/signature area, then 16-bit little-endian module count.
2. Module dimensions: width and height bytes per module.
3. Frame-module count (16-bit LE), then records of module index, signed X offset, signed Y offset, and flags (four bytes each).
4. Frame count (16-bit LE), then per-frame module count (byte), one skipped/reserved byte, and first frame-module index (16-bit LE).
5. Frame bounding boxes: four bytes per frame.
6. Animation-frame record count (16-bit LE), then five-byte records: frame index, duration, signed X offset, signed Y offset, flags.
7. Animation count (16-bit LE), then per-animation frame count (byte), one skipped/reserved byte, and first animation-frame index (16-bit LE).
8. Palette and module pixel data sections.

Important source details:
- Animation frame index and duration are read as unsigned bytes when used by the getters/draw path.
- Animation X/Y offsets are signed bytes in `b_SpriteAnimator.applyFrameOffset()` and `drawAnimationFrame()`.
- Frame bounding boxes are stored as four bytes per frame; their semantic ordering is represented as x/y/w/h in the source field comment, but this audit does not establish their collision meaning.
- The parser stores animation frame count as a byte; callers mask with `& 0xFF` in `getAnimationFrameCount()`.

## 2. Encounter chunk metadata

| Encounter resource | Modules | Frames | Animation-frame records | Animation sets |
|---|---:|---:|---:|---:|
| `/mm0.f` chunk 0 | 12 | 21 | 15 | 3 |
| `/mm0.f` chunk 1 | 22 | 24 | 11 | 3 |
| `/b0.f` chunk 0 | 18 | 23 | 29 | 9 |
| `/b1.f` chunk 0 | 72 | 78 | 175 | 14 |
| `/mm1.f` chunk 0 | 104 | 44 | 185 | 15 |

These are exact parsed metadata counts from the pinned candidate resources.

## 3. Falling Torches — `/mm0.f`

### Chunk 0 — animator slot 1

The stage loader creates animator slot 1 and selects animation set 2.

| Animation set | Frame count | Frame IDs | Duration per frame (source ticks) |
|---:|---:|---|---|
| 0 | 8 | 0–7 | 3 each |
| 1 | 2 | 8–9 | 1 each |
| 2 | 5 | 16–20 | 6 each |

Selected animation 2 therefore contains five frames, each held for six animator ticks. Its frame offsets are all `(0, -10)`.

### Chunk 1 — animator slot 2

The stage loader creates animator slot 2 and selects animation set 0.

| Animation set | Frame count | Frame IDs | Durations |
|---:|---:|---|---|
| 0 | 2 | 12, 13 | 2, 2 |
| 1 | 7 | 14, 15, 16, 17, 19, 21, 22 | 20, 8, 2, 2, 2, 2, 2 |
| 2 | 2 | 22, 23 | 1, 1 |

Selected animation 0 contains two frames, each held for two animator ticks. The offsets are `(0,0)`.

## 4. Great Anaconda — `/b0.f` chunk 0

Chunk metadata: 18 modules, 23 frames, 29 animation-frame records, 9 animation sets.

| Animation set | Frame count | Frame IDs | Durations |
|---:|---:|---|---|
| 0 | 1 | 0 | 1 |
| 1 | 3 | 0, 1, 2 | 1, 1, 1 |
| 2 | 1 | 1 | 1 |
| 3 | 2 | 12, 11 | 3, 2 |
| 4 | 4 | 7–10 | 3 each; offset Y = 94 |
| 5 | 4 | 13–16 | 1 each; offset Y = 6 |
| 6 | 8 | 3,4,5,6,5,4,3,2 | 1 each |
| 7 | 4 | 17–20 | 2, 2, 3, 15; offset Y = 94/95 |
| 8 | 2 | 21, 22 | 1, 1; offset Y = 94 |

The inspected loader constructs the animator from chunk 0 but does not explicitly call `setAnimation()` in that branch. `b_SpriteAnimator` fields default to zero, so the initial state is animation 0, frame 0, time 0 unless another code path changes it before drawing. Animation 0 is a single frame (frame 0, duration 1 tick). This is a code-derived default, not a runtime observation.

## 5. Evil Teutonic Knight — `/b1.f` chunk 0

Chunk metadata: 72 modules, 78 frames, 175 animation-frame records, 14 animation sets. The loader explicitly selects animation set 10 for animator slot 5.

| Animation set | Frame count | Duration pattern (source ticks) |
|---:|---:|---|
| 0 | 8 | 3,3,3,3,3,3,3,3 |
| 1 | 8 | 2,2,2,3,2,2,2,3 |
| 2 | 22 | Mostly 1; includes 2 and 6 tick holds |
| 3 | 22 | Same timing as set 2; X offset around 48 |
| 4 | 1 | 3 |
| 5 | 1 | 3 |
| 6 | 8 | 1 each |
| 7 | 8 | 1 each |
| 8 | 17 | Mostly 1; includes 3, 10, and 2 tick holds |
| 9 | 17 | Same timing as set 8; X offset around 48 |
| 10 | 8 | **4,3,3,3,4,3,3,3** |
| 11 | 8 | Same timing as set 10; X offset around 48 |
| 12 | 9 | 4,4,3,3,2,2,1,1,90 |
| 13 | 38 | Mixed 1–6 tick holds, final sequence mostly 2 ticks |

Selected animation 10 uses frame IDs `0,1,2,3,4,3,2,1`. Its eight frames have durations 4,3,3,3,4,3,3,3 ticks and zero X/Y offsets in the animation-frame records. Set 11 is a paired offset variant with the same frame/timing pattern.

## 6. Yetti — `/mm1.f` chunk 0

Chunk metadata: 104 modules, 44 frames, 185 animation-frame records, 15 animation sets. The loader explicitly selects animation set 0 for animator slot 5.

| Animation set | Frame count | Frame IDs | Durations (source ticks) |
|---:|---:|---|---|
| 0 | 13 | 2,3,4,5,2,2,3,4,5,2,6,7,6 | 4,4,4,4,4,4,3,4,5,4,4,3,4 |
| 1 | 13 | Same frame IDs as set 0 | 4,4,4,4,4,4,5,3,4,3,3,2,3 |
| 2 | 15 | 0,2,6–14,9,8,7,6 | 3, then 1 each |
| 3 | 15 | 1,2,6–14,9,8,7,6 | 3, then 1 each |
| 4 | 8 | 22,15–21 | 3,4,3,3,3,4,3,3 |
| 5 | 8 | Same frame IDs as set 4 | 3,4,3,3,3,4,3,3 |
| 6 | 10 | 23–31,29 | 2,1,1,3,2,1,1,1,1,3 |
| 7 | 10 | Same frame IDs as set 6 | 2,1,1,3,2,1,1,1,1,3 |
| 8 | 7 | 32,29,28,27,24,25,26 | 2,4,1,1,2,3,5 |
| 9 | 7 | Same frame IDs as set 8 | 2,4,1,1,2,3,5 |
| 10 | 8 | 22,15–21 | 1,2,1,1,1,2,1,1 |
| 11 | 8 | Same frame IDs as set 10 | 1,2,1,1,1,2,1,1 |
| 12 | 13 | 33,34,33,34,33,34,35,36,37,36,37,36,37 | 1,1,1,1,1,1,2,3,4,5,6,6,6 |
| 13 | 25 | 23,38,24,39,25,40,26,41,27,42,28,43,29,30,31,30,29,30,31,30,29,30,31,30,32 | Mostly 1; one 4 and one 5 tick hold |
| 14 | 25 | Same frame IDs as set 13 | Same timing as set 13 |

Selected animation 0 has 13 frames and variable durations from 3 to 5 ticks. The paired set 1 has similar frame IDs but different timing and a horizontal animation offset of 48 in its records. The source does not label these animation sets with semantic names such as idle, walk, attack, or damage; do not assign those labels without tracing call sites and/or visual verification.

## 7. Animator timing and playback semantics

`b_SpriteAnimator.tick()`:
- Returns immediately when the sprite is null or `crtTime < 0`.
- Reads the current frame duration from the sprite's animation-frame record.
- If duration is zero, the frame does not advance in this method.
- Otherwise increments `crtTime` by one per tick.
- Advances to the next frame when `crtTime` reaches the duration, resets the timer to zero, and loops to frame 0 after the last frame.

`animationEnded()` is true only when the animator is on the last frame and either its duration is zero or `crtTime == duration - 1`. It does not mean the animation automatically stops; `tick()` loops.

`setAnimation()` resets frame/time/offset state only when the requested animation index differs from the current index. Re-selecting the same animation does not restart it.

These durations are engine ticks, not milliseconds. The game loop timing and tick call frequency must be audited before converting them into Android elapsed time.

## 8. Native Android implementation requirements

- Preserve frame index, duration ticks, signed offsets, and frame flags as independent metadata.
- Preserve the source's loop behavior and duration-zero hold behavior unless a separately audited call site overrides it.
- Implement `setAnimation` restart semantics exactly, including the no-op when selecting the already active animation.
- Keep paired animation sets separate; sets with a 48-pixel offset are not necessarily interchangeable with their zero-offset counterpart.
- Resolve game-loop tick frequency before mapping source durations to milliseconds.
- Parse every count/index as unsigned where the source masks it; validate all ranges before indexing.
- Keep bounding boxes distinct from visual frame offsets until collision semantics are established.
- Add deterministic tests for animation advance, loop, duration-zero hold, last-frame detection, offset/flip behavior, and same-animation selection.

## 9. Open work

- Trace every call site that selects animation sets for the encounter animators, especially Great Anaconda's default animation 0 and state-driven changes.
- Decode the full per-frame bounding-box values and identify whether/how gameplay uses them.
- Trace `FLAG_OFFSET_AF`, `FLAG_FLIP_X`, `FLAG_FLIP_Y`, and per-frame flags through all encounter draw paths.
- Establish the main game-loop tick frequency and exact relationship between `tick()` calls and render/update frames.
- Map animation indices to semantic labels only after call-site correlation and lawful visual verification.
- Audit sprite slot 7's origin and its animation set 1 separately.
- Build a bounded, fuzz-tested Native sprite metadata parser; the decompiled parser catches exceptions but may leave partially populated objects.
- Validate selected animation sequences against the original runtime.

## 10. Audit status

Completed at static/binary level: reconstructed sprite metadata section order; parsed five encounter chunks; extracted module/frame/animation counts; extracted all animation set counts for those chunks; recorded exact timing/frame sequences for the selected Falling Torches, Evil Knight, and Yetti animations; established Great Anaconda's constructor-default animation state; documented animator loop/timing semantics.

Open: visual identification; semantic labels for animation sets; complete bounding-box/collision mapping; game-loop tick-to-time conversion; sprite slot 7; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed.