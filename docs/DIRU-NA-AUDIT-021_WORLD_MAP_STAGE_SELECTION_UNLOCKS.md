# DIRU-NA-AUDIT-021 — World Map, Stage Selection & Stage Unlock Conditions

## Scope and evidence boundary

This audit examines the read-only source candidate kubikaugustyn/DiamondRushSource at pinned commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Inspected src/main/java/i.java around world-map initialization, stage node rendering/navigation, per-world save block accessors, stage completion aggregation, stage title generation, and world-stage count tables.

This is static-source analysis only. It does not validate the map visually or in an original runtime. No assets were copied; no Android build, APK, or runtime test was performed.

## 1. Stage catalog per world

The source defines stage-title lookup arrays with these entries:

| World | Stage-title mapping entries |
|---|---:|
| Angkor | 13 |
| Bavaria | 13 |
| Siberia | 15 |

The mapping arrays are gIntArrArr: Angkor {1,2,3,4,5,6,7,8,9,12,13,14,15}; Bavaria {1,2,3,4,5,6,7,8,9,10,12,13,14}; Siberia {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}. These values are title/resource indices, not proof that every stage is initially unlocked.

Stage title generation creates regular titles for the first 11 positions and secret-stage titles for later positions. A separate title entry is reserved for the intro stage. The displayed title for a world-map node is looked up through the world-specific mapping.

## 2. World map node grid and stage identity

The world map uses a 12×12 long-value grid. Each nonzero node stores packed fields, including a stage index in bits 6–10, node type/state bits, and additional neighbor/route metadata. The map is not a simple linear list of stages.

The current and next-stage node positions are resolved by scanning this packed grid. The selected node displays a stage title and a completion count formatted as current/target. The current count is clamped so it cannot display above the target.

## 3. Per-world stage save blocks

The legacy save header stores a 16-bit offset for each world at offsets 14, 16, and 18. The accessor computes the address of a stage record from that world offset and the stage index.

Each stage record exposes at least:
- A flag byte at record offset +2.
- A collected/completion counter at +3.
- A target/count byte at +4.
- A sequence of coordinate pairs beginning at +5.

The accessors read the current count and target from the record. Another helper searches the coordinate-pair list; when a matching coordinate is collected, it clears that pair and increments the record counter. This is persistent per-stage coordinate collection, not just a global counter.

## 4. Stage unlock and route progression

The world-map completion routine sets stage flag bits when result conditions are met. The observed result flags include 0x04, 0x08, 0x10, and 0x20, based on separate stage-result counters/conditions. The source also uses 0x02 as a stage progression/unlock-related bit and 0x40 in the map-node construction/availability logic.

The map transition logic compares the current stage index with the global progression index and inspects stage flags to determine which nodes are selectable/current/locked. It builds a route graph from packed node metadata, then uses an animated path/selection marker to move between node coordinates.

The exact semantic meaning of every node-state bit and all secret-stage unlock conditions is not fully reconstructed in this pass. Do not treat all completion flags as equivalent or assume that finishing a stage always unlocks the numerically next stage.

## 5. World stage limits and loading

Before world-map initialization, the source clamps the selected stage index to the maximum stored stage index for the chosen world. World resource loading then scans the selected world's stage stream until it reaches that index and loads its dimensions and three map layers.

The static stage-title arrays and the save-record offsets are separate structures: titles provide localized presentation mapping, while per-world save records hold progression/count/coordinate data.

## 6. Native Android implementation requirements

- Represent each world's stage catalog as explicit data, separate from localized stage titles.
- Model the world map as a graph/grid with stable stage IDs and explicit route edges, not a linear stage list.
- Use typed per-stage records for completion flags, current/target counters, and collected coordinate identities.
- Preserve distinct flag meanings; do not collapse all stage flags into a single completed boolean.
- Implement stage-node availability from verified progression rules and explicit prerequisites.
- Animate the selection marker along graph nodes while keeping gameplay/progression state independent of rendering.
- Validate stage index against each world's actual stage catalog before loading.
- Keep secret stages and intro-stage presentation separate from ordinary numbered stage titles.
- Use a versioned native save model rather than importing legacy byte offsets or packed node longs.

## 7. Open work

- Fully decode the 12×12 packed world-map node format and every field/bit.
- Trace every writer and reader for stage flag bits 0x02, 0x04, 0x08, 0x10, 0x20, and 0x40.
- Determine exact prerequisites and route behavior for every stage and secret stage.
- Reconcile stage-title mapping indices with the world resource stage counts and stage names.
- Trace all stage coordinate-pair collection writers/readers and reset behavior.
- Extract exact world-map node coordinates, route paths, and marker animation behavior from legally usable assets/runtime.
- Add deterministic tests for stage availability, completion flags, collectible coordinate persistence, route navigation, and world-specific bounds.
- Validate the reconstructed map and unlock rules against original runtime evidence.

## 8. Audit status

Completed at static-source level: per-world stage-title mapping counts and indices; generated regular/secret/intro title labels; 12×12 packed world-map grid; selected node fields and current/target display; per-world save offsets and stage-record layout; coordinate-pair collection persistence; stage result flag writes; world-specific stage-index clamping.

Not completed: complete packed node schema; full flag semantics; all route and secret-stage prerequisites; exact map visuals; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.