# DIRU-NA-AUDIT-027 — Stage ID, Localized Title Mapping & Secret-Route Correlation

## Scope and evidence boundary

This audit examines the pinned read-only candidate kubikaugustyn/DiamondRushSource at commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Relevant code in src/main/java/i.java includes generateStageTitles(), gIntArrArr initialization, world-map node title rendering, packed secondary-field reads, and progression flag handling. It correlates those findings with the decoded map node inventories in AUDIT-026.

This is static source analysis. It does not establish the exact unlock requirements for every secret stage, validate title strings in every locale, or confirm behavior in an original JAR runtime. No source or asset was copied into Project-DIRU-NA.

## 1. Generated title table

generateStageTitles() allocates 16 entries:
- Indices 0–10: localized normal titles generated from texts[66] (commented as "Stage %U"), numbered 1 through 11.
- Indices 11–14: localized secret titles generated from texts[62] (commented as "Secret stage %U"), numbered 1 through 4.
- Index 15: localized intro title from texts[31] (commented as "Intro stage").

If localization lookup or formatting throws, the corresponding title is set to "E"; the intro title falls back to a blank space.

## 2. Per-world stage-title mapping

World-map rendering uses stageTitles[gIntArrArr[worldIndex][nodeStageIndex] - 1]. Thus gIntArrArr maps a node's stage index to a 1-based title number; it is not itself the title table.

Source arrays:

| World resource | gIntArrArr values by node stage index |
|---|---|
| Angkor | 1,2,3,4,5,6,7,8,9,12,13,14,15 |
| Scotland resource / Bavaria UI | 1,2,3,4,5,6,7,8,9,10,12,13,14 |
| Tibet resource / Siberia UI | 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 |

## 3. Correlation with decoded map nodes

### Angkor — map_angkor.out

13 nodes; stage IDs 0–12.

| Node stage ID | Title number | Generated title | Secondary field |
|---:|---:|---|---:|
| 0–8 | 1–9 | Stage 1–9 | 0 |
| 9 | 12 | Secret stage 1 | 1 |
| 10 | 13 | Secret stage 2 | 1 |
| 11 | 14 | Secret stage 3 | 1 |
| 12 | 15 | Secret stage 4 | 1 |

### Scotland resource / Bavaria UI

13 nodes; stage IDs 0–12.

| Node stage ID | Title number | Generated title | Secondary field |
|---:|---:|---|---:|
| 0–8 | 1–9 | Stage 1–9 | 0 |
| 9 | 10 | Stage 10 | 0 |
| 10 | 12 | Secret stage 1 | 1 |
| 11 | 13 | Secret stage 2 | 1 |
| 12 | 14 | Secret stage 3 | 1 |

### Tibet resource / Siberia UI

14 nodes; stage IDs 0–13.

| Node stage ID | Title number | Generated title | Secondary field |
|---:|---:|---|---:|
| 0–10 | 1–11 | Stage 1–11 | 0 |
| 11 | 12 | Secret stage 1 | 1 |
| 12 | 13 | Secret stage 2 | 1 |
| 13 | 14 | Secret stage 3 | 1 |

## 4. What this proves about secondary field value 1

Across all three decoded maps, every node whose secondary field equals 1 maps through gIntArrArr to a generated Secret stage title. All nodes with secondary field 0 map to normal Stage titles.

This is strong, consistent source evidence that secondary field value 1 classifies secret-stage nodes in these three map resources. It is no longer merely a visual-class hypothesis.

However, this does not prove the complete unlock prerequisites. The route progression code separately checks node state bits 0–2 and stage save flag 0x40, and writes 0x40 to a qualifying higher-index neighbor. The exact secret-stage availability lifecycle and whether all secret nodes are reached by the same condition remain to be traced.

## 5. Important unresolved mapping anomaly

The Tibet/Siberia gIntArrArr contains 15 entries for node stage indices 0–14, but map_tibet.out contains only 14 node records with stage IDs 0–13. The final mapping value 15 would resolve to generated title index 14 (Secret stage 4), but there is no stage-ID-14 node in the decoded map resource.

Do not invent a missing node. This may represent an unused/reserved mapping entry, a removed node, or a mismatch between this source candidate's map and its stage-title array. Further cross-reference with world stage streams, title drawing, and progression code is required.

## 6. Native Android implications

- Keep node stage ID distinct from localized title number and title-table index.
- Model per-world stage-title mappings as explicit arrays; do not derive titles from stage ID + 1.
- Generate normal and secret title labels from localization templates, retaining fallback behavior.
- Preserve secondary field as an enum/classification; value 1 may be represented as SecretStageCandidate/SecretStage only within this audited map/title mapping, while keeping raw source value for traceability.
- Keep unlock/progression state separate from node classification. A secret-stage node being identified does not mean it is unlocked.
- Treat the Tibet stage-ID-14 mapping as unresolved and do not create a synthetic node.

## 7. Open work

- Trace all readers/writers of node secondary field bits 3–5 and verify no other values have title semantics.
- Trace save flags and route progression to determine exact unlock prerequisites and flag lifecycle for secret stages.
- Inspect the per-world stage streams /w0.bin, /w1.bin, /w2.bin and compare actual stage counts and stage offsets with map/title mappings.
- Resolve the extra Tibet/Siberia mapping entry at stage index 14.
- Verify localized text indices 66, 62, and 31 against decoded language resources, including lang.xx, without assuming lang.xx is English.
- Validate secret-stage names and route behavior against an original legally usable JAR/runtime.

## 8. Audit status

Completed at static-source level: generated title table structure; normal/secret/intro title ranges; all three world-specific title mapping arrays; complete node-to-title crosswalk for all 40 decoded map nodes; strong source correlation between secondary field 1 and Secret stage titles.

Open: exact secret unlock requirements and flag lifecycle; extra Tibet mapping entry; world stage-stream reconciliation; localized resource verification; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed.