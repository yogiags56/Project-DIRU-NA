# DIRU-NA-AUDIT-030 — Special Stage Identification & Cross-Resource Reconciliation

## Scope and evidence boundary

This audit correlates the decoded stage streams, map node records, title mapping arrays, and explicit special-stage branches in src/main/java/i.java from the read-only candidate kubikaugustyn/DiamondRushSource pinned at commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. It is static source/binary analysis only; no original JAR runtime was executed.

## 1. Confirmed special-stage constants

The source declares these explicit stage constants:

| Constant | World index | Stage index | Source-specific behavior |
|---|---:|---:|---|
| STAGE_ANGKOR_FALLING_TORCHES | 0 | 5 | Special Angkor stage configuration: kByte=1, alInt=816, amInt=0 |
| STAGE_ANGKOR_GREAT_ANACONDA | 0 | 8 | Special boss/configuration branch; initializes multiple gameplay fields and flags |
| STAGE_ANGKOR_DEMO | 0 | 13 | Demo-specific configuration: kByte=2 and tBoolean=false |
| STAGE_BAVARIA_EVIL_TEUTONIC_KNIGHT | 1 | 9 | Calls yVoid() during stage initialization |
| STAGE_SIBERIA_YETTI | 2 | 10 | Calls adVoid(), sets asInt=360, kByte=3, and gameplay flags |

These constants identify special gameplay handling, not separate stage-file formats. The records remain ordinary entries in their world's stage stream.

## 2. Angkor stage index 13: identified as demo stage

Three independent source clues identify Angkor stage index 13 as a demo stage:

1. The constant `STAGE_ANGKOR_DEMO` is explicitly assigned value 13.
2. A startup/demo flow sets `crtWorldIndex=0` and `crtStageI=STAGE_ANGKOR_DEMO`.
3. Stage initialization has a dedicated branch for world 0, stage 13, and applies demo-specific configuration. Additional input/exit handling also checks the same world/stage pair.

Cross-resource reconciliation:
- `/w0.bin` contains stage records 0–13, so stage 13 is a real stage payload.
- `map_angkor.out` contains map nodes 0–12 only.
- Angkor title mapping contains 13 entries, for node stage IDs 0–12 only.

Conclusion: the extra Angkor stage-stream record is not an unexplained missing World Map node. It is explicitly identified by source code as the Angkor demo stage and is launched through a demo flow rather than represented as a regular map node. Its exact commercial-JAR packaging/availability still requires variant verification.

## 3. Special stages within the normal world catalogs

The other named special-stage constants fall inside the normal stage index ranges:

- Angkor stage 5: Falling Torches.
- Angkor stage 8: Great Anaconda.
- Bavaria UI / Scotland resource stage 9: Evil Teutonic Knight.
- Siberia UI / Tibet resource stage 10: Yetti.

These are special gameplay scenarios within existing stage records. They do not explain the map/title count mismatches because their stage indices already have map nodes and title mapping entries.

## 4. Tibet/Siberia title mapping entry 14 remains unresolved

The static title mapping for world index 2 contains 15 values:
`1,2,3,4,5,6,7,8,9,10,11,12,13,14,15`.

However:
- `/w2.bin` contains 14 stage records, indices 0–13.
- `map_tibet.out` contains 14 nodes, stage IDs 0–13.
- No stage index 14 is present in the decoded world-2 stage stream or map node inventory.
- The final mapping value 15 would select generated title array index 14, which is Secret stage 4.

The source contains this extra mapping entry, but this audit found no corresponding stage payload or map node. It may be a stale/reserved mapping, a removed secret stage, or a variant mismatch. None of these explanations is proven. Do not add a synthetic Siberia stage 14 or Secret Stage 4 to the Native catalog.

## 5. Title generation and demo title are separate concerns

`generateStageTitles()` creates 16 strings: normal Stage 1–11, Secret Stage 1–4, and an Intro Stage string at index 15. The Angkor demo stage index 13 is not part of the map title mapping; its demo flow uses a separate UI path. Do not infer that the demo stage should be titled "Stage 14" or "Secret Stage 4".

## 6. Reconciled resource catalog

| World | Stage stream | Map nodes | Title map | Interpretation |
|---|---:|---:|---:|---|
| Angkor | 14 records (0–13) | 13 (0–12) | 13 | Stage 13 is explicitly the demo stage; normal map stages align |
| Bavaria UI / Scotland resource | 13 records (0–12) | 13 (0–12) | 13 | Counts align |
| Siberia UI / Tibet resource | 14 records (0–13) | 14 (0–13) | 15 entries | Extra title mapping entry 14 / Secret Stage 4 unresolved |

## 7. Native Android implementation requirements

- Represent the Angkor demo stage as a distinct special/demo entry, not a normal World Map node.
- Keep special gameplay configuration metadata separate from stage identity and title mapping.
- Do not automatically expose demo content in the retail stage selector; gate it behind an explicitly verified product/variant policy.
- Do not create Siberia stage index 14 without a real stage payload and corroborating map/runtime evidence.
- Keep localization title generation separate from stage identity; stage names should be attached through explicit catalog metadata.
- Preserve source world resource naming separately from UI world labels until the target JAR variant is confirmed.

## 8. Open work

- Trace demo launch/exit flow and determine whether the demo stage is always present in the intended Nokia JAR variant.
- Determine whether the extra Siberia title mapping entry is stale, reserved, removed, or variant-specific by checking additional independently sourced JAR builds.
- Trace per-world maximum stage/save bounds and confirm whether they include the Angkor demo record.
- Decode stage-specific object and foreground distributions to build special-stage mechanic profiles.
- Validate named special stages against original runtime evidence.
- Add catalog integrity tests that permit an explicitly classified demo-only stage but reject phantom map/title references.

## 9. Audit status

Completed at static-source level: identified five named special-stage constants; confirmed Angkor stage 13 is a demo stage through constant, launch flow, and dedicated initialization/input branches; reconciled normal map-stage counts; isolated the unresolved Siberia stage-index-14 title mapping entry.

Open: exact target-JAR variant validation; demo packaging policy; Siberia mapping anomaly; full progression bounds; gameplay/runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed.