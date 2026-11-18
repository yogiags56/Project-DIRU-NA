# DIRU-NA-AUDIT-032 — Special Encounter Sprite & Animation Resource Mapping

## Scope and evidence boundary

This audit traces sprite loading and rendering for the four named encounters and the Angkor Falling Torches sequence in src/main/java/i.java from read-only candidate kubikaugustyn/DiamondRushSource pinned at 215c71d04057d66a1b08419b1a4b1043ae3e0881. The audit identifies file/chunk-to-runtime-slot relationships where explicit source code exists. It does not decode the binary image payloads or visually verify character identity, individual frame meaning, or animation counts.

## 1. Encounter resource mapping

| Encounter | Runtime sprite / texture | Source resource | Confirmed use |
|---|---|---|---|
| Falling Torches (Angkor 5) | animator slots 1 and 2; texture slot 27 | `/mm0.f`, chunks 0, 1, 2 | chunk 0 creates animator slot 1 and starts animation 2; chunk 1 creates animator slot 2 and starts animation 0; chunk 2 supplies texture slot 27 |
| Great Anaconda (Angkor 8) | animator slot 5; sprite slot 40; texture slot 32; sprite slot 20 | `/b0.f` chunks 0 and 1; `/mmv.f` chunk 3; `/gen0.f` chunk 7 | b0 chunk 0 creates animator slot 5; chunk 1 creates sprite slot 40; mmv chunk 3 supplies texture slot 32; gen0 chunk 7 supplies sprite slot 20 |
| Evil Teutonic Knight (Bavaria 9) | animator slot 5; texture slot 30; sprite slot 20 | `/b1.f` chunk 0; `/mmv.f` chunk 2; `/gen0.f` chunk 7 | b1 chunk 0 creates animator slot 5 and starts animation 10; mmv chunk 2 supplies texture slot 30; gen0 chunk 7 supplies sprite slot 20 |
| Yetti (Siberia 10) | animator slot 5; texture slot 31 | `/mm1.f` chunk 0; `/mmv.f` chunk 1 | mm1 chunk 0 creates animator slot 5 and starts animation 0; mmv chunk 1 supplies texture slot 31 |

## 2. Falling Torches resource loading

When kByte is 1, the stage resource loader calls dVoid(chunkIndex) for successive chunks. dVoid opens `/mm0.f`, reads its chunk directory, parses each selected chunk as an f_Sprite, and creates images using palette 0.

Observed switch fall-through:
- chunk 0 creates `aClassbArr[1]` and starts animation 2.
- chunk 1 creates `aClassbArr[2]` and starts animation 0.
- chunk 2 assigns `textures[27]` from the parsed image data.

The stage update/render code uses animator slot 1 for a repeating animated pattern and animator slot 2 for the related moving/transition sequence. The exact semantic identity of texture slot 27 and individual frames remains unverified.

## 3. Great Anaconda resource loading

When kByte is 4, the loader uses cVoid(chunkIndex) with `/b0.f`:
- chunk 0 creates animator slot 5.
- chunk 1 creates sprite slot 40.

The same loading branch also loads `/mmv.f` chunk 3 into texture slot 32 and `/gen0.f` chunk 7 into sprite slot 20.

Encounter rendering uses animator slot 5 for the main moving entity. Sprite slot 40 is drawn as part of the stage scene. Sprite slot 20 is loaded in the same special mode, but this audit has not established every draw call or its exact visual role. Texture slot 32 is loaded, but its exact visual identity is not established here.

An encounter HUD-like counter is drawn from aqInt as a row of small rectangles. It is source-visible UI behavior, but aqInt's underlying meaning (health, remaining hits, or phases) remains unresolved.

## 4. Evil Teutonic Knight resource loading

When kByte is 5, the loader:
- Loads `/mmv.f` chunk 2 into texture slot 30.
- Loads `/gen0.f` chunk 7 into sprite slot 20.
- Loads `/b1.f` chunk 0 into animator slot 5 and sets its initial animation to 10.

The stage render path draws animator slot 5 at the moving coordinate `atInt`, relative to camera position. When aoInt reaches 12, it additionally draws animation 1 from sprite slot 7 and calls fVoid at the same effect position. The audit confirms the draw linkage but does not establish the visual meaning of sprite slot 7 or the individual frames.

## 5. Yetti resource loading

When kByte is 3, the loader loads `/mmv.f` chunk 1 into texture slot 31 and `/mm1.f` chunk 0 into animator slot 5, initially animation 0.

The render path positions animator slot 5 using `asInt` and camera offset. In aoInt state 12, it additionally draws animation 0 from sprite slot 7 (animation set 1) and invokes fVoid at that position. The precise visual identity of the entity/effect and the meaning of each animation index require image/frame inspection.

## 6. Cross-encounter shared slots

Runtime slots are context-dependent and reused:
- `aClassbArr[5]` is the main encounter animator in all four modes, but its backing sprite resource differs by mode: `/b0.f`, `/b1.f`, or `/mm1.f`.
- `aClassfArr[20]` is loaded from `/gen0.f` chunk 7 for both Great Anaconda and Evil Teutonic Knight.
- `aClassfArr[7]` is referenced for additional encounter effects in Yetti and Evil Knight render paths; its origin is not resolved in this audit.
- `textures[30]`, `[31]`, `[32]`, and `[27]` are stage-mode-specific loaded texture slots, not global semantic identifiers.

Native implementation must key assets by encounter plus resource identity, not by legacy slot number alone.

## 7. Animation metadata status

The source proves initial animation selections:
- Falling Torches: animator slot 1 starts animation 2; slot 2 starts animation 0.
- Great Anaconda: animator slot 5 is constructed from b0 chunk 0; its first animation selection is not explicitly shown in the inspected cVoid branch.
- Evil Teutonic Knight: animator slot 5 starts animation 10.
- Yetti: animator slot 5 starts animation 0.

Animation frame counts, per-frame durations, offsets, bounding boxes, and visual labels were not extracted in this audit. These values live in parsed f_Sprite metadata and require a dedicated sprite-chunk/frame-table audit.

## 8. Native Android renderer requirements

- Build an explicit asset manifest keyed by world, stage ID, source resource, chunk ID, palette, and logical role.
- Keep encounter-local sprite references; do not assume legacy slot 5 means the same character across modes.
- Preserve animation index, frame duration, offsets, and flip flags from f_Sprite/b_SpriteAnimator metadata once audited.
- Keep effect rendering separate from entity rendering and collision logic.
- Ensure stage-mode asset loading is bounded and failure-safe; the original loader catches and suppresses exceptions, which Native code should not silently imitate.
- Avoid copying candidate payloads into the app until source provenance/licensing review is complete.

## 9. Open work

- Inspect `/mm0.f`, `/b0.f`, `/b1.f`, `/mm1.f`, `/mmv.f`, and `/gen0.f` chunk directory sizes and hashes without copying payloads into the active app.
- Decode frame/animation tables for the identified encounter chunks.
- Trace the origin of sprite slot 7 and its animation set 1.
- Determine initial animation state for Great Anaconda's b0 chunk 0 animator.
- Visually identify entity/effect frames only after lawful asset provenance is established.
- Trace every draw call for sprite slot 20, 40, texture slots 27/30/31/32.
- Add an automated asset-manifest consistency test.
- Validate encounter animations against the original JAR/runtime.

## 10. Audit status

Completed at static-source level: mapped encounter-specific resource files/chunks to legacy runtime animator, sprite, and texture slots; recorded confirmed initial animation selections; identified reused slot numbers and unresolved sprite-slot-7 origin.

Not completed: frame table extraction, frame counts/durations/offsets, visual identification, complete draw-call mapping, binary payload validation, runtime validation, licensing clearance.

No Android build, APK, or runtime test was performed.