# DIRU-NA-AUDIT-023 — Packed World-Map Node Format & Route-Graph Reconstruction

## Scope and evidence boundary

This audit examines the read-only source candidate kubikaugustyn/DiamondRushSource at pinned commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Inspected src/main/java/i.java around the 12×12 world-map node array, packed-long field getters/setters, neighbor traversal, node-state initialization, route selection, progression advancement, and route animation.

This is a partial static-source reconstruction. The source is decompiled; field names remain opaque. No original world-map binary resource or runtime screenshot was decoded/validated in this pass. No assets were copied and no Android build/APK was produced.

## 1. In-memory map representation

The world map is represented by a 12×12 array of 64-bit values: aLongArrArr[12][12]. A zero value represents an empty grid cell. Nonzero values represent stage nodes.

Each node is addressed by its grid coordinates [x][y]. The map UI positions nodes using a regular 13-pixel grid step, with base drawing offsets x=37 and y=73, and a 6-pixel inset for node artwork/marker. These are source coordinate constants for the existing 240×320 logical screen, not extracted coordinates from an external map file.

## 2. Confirmed packed-long fields

The helper aInt(long value, byte shift, byte width) extracts an unsigned bit field. The companion setter replaces the selected field in place.

| Bit range | Width | Observed use | Confidence |
|---|---:|---|---|
| 0–2 | 3 | Node state/type value; values 0 and 1 are tested in selection, rendering, and route traversal | Partial |
| 3–5 | 3 | Secondary node classification; values 0 and 1 affect map rendering/route logic | Partial |
| 6–10 | 5 | Stage index (0–31); directly used to match current stage, progression index, title, and save records | Confirmed |
| 11–13 | 3 | Number of neighbor-coordinate pairs stored in this node | Confirmed |
| 14–? | 4-bit fields | Repeating neighbor x/y coordinate pairs; each pair consumes 8 bits | Confirmed, storage boundary not fully audited |

The first neighbor pair is read from shifts 14 and 18; subsequent pairs advance by 8 bits. The map code iterates the count extracted from bits 11–13.

Because the packed value is 64 bits, the header plus up to six 8-bit coordinate pairs fits within the word (14 + 6×8 = 62 bits). The source's three-bit neighbor count can encode values through 7; the maximum legal count and malformed-data handling still require validation.

## 3. Neighbor and route traversal

When a node is selected, the code reads its neighbor count and walks its coordinate pairs. Candidate neighbors are filtered by their node state and relative position to the current node, then a destination coordinate is selected for directional input.

The route-transition logic also scans neighbor coordinates from a node and checks candidate node stage indices. When a qualifying later stage is found, it updates the global progression index dZInt, writes stage flag bit 0x40 for that stage, and starts the map transition.

This supports a graph model with explicit neighbor edges rather than assuming stageIndex+1 is always the next node. Branch selection and edge ordering depend on the source's directional filtering logic.

## 4. Node state and stage progression

During map initialization, each nonzero node's stage index is read from bits 6–10. The node's low three-bit state is rewritten based on its stage flag byte: nodes with flag 0x40 set, and the intro node (stage index 0), are assigned state 0; other nodes are assigned state 1.

Selected/current node and progression target coordinates are resolved by scanning the 12×12 grid for the stage indices matching crtStageI and dZInt.

The exact semantic names for low fields 0–2 and 3–5 are not fully established. Do not translate them directly into labels such as completed, locked, secret, or current without tracing every reader/writer.

## 5. Route animation

The route renderer reads neighbor lists and draws route/node sprite frames. It computes screen coordinates from grid positions using the 13-pixel spacing and uses an animation helper to interpolate movement along a path. Route drawing is therefore a presentation of the graph, not the authoritative progression data.

The map code has explicit state for current node coordinates, target node coordinates, interpolation counters, and a list of route marker screen positions. The Native port should keep the graph/progression model independent from this animation state.

## 6. Native Android implementation requirements

- Parse packed map nodes into a typed structure at load time rather than repeatedly extracting bit fields during UI rendering.
- Preserve stable stage IDs and explicit neighbor edges; do not infer linear progression from numeric stage order.
- Validate 12×12 bounds, nonzero-node consistency, neighbor count, coordinate nibbles, duplicate edges, self-edges, and referenced-node existence.
- Treat unknown low-bit state values as opaque until their full semantics are audited.
- Keep stage progression flags and graph node presentation state separate.
- Drive route animation from graph transitions; never use animation completion to commit progression.
- Keep the source's 240×320 logical map coordinate system for the first playable build, while rendering through a viewport transform.
- Build a validated JSON/debug dump of the typed graph before implementing the final Native map renderer.

## 7. Open work

- Enumerate all reads/writes of packed fields bits 0–2 and 3–5 to determine exact node-state/class semantics.
- Determine the maximum valid neighbor count and validate the 64-bit packing boundary.
- Trace direction-to-neighbor selection fully, including multiple candidates in one direction and tie-breaking.
- Determine how secret-stage nodes are encoded in packed node fields and how their prerequisites are evaluated.
- Identify the actual source of the 12×12 node array and whether it is generated from configuration or reconstructed at runtime.
- Compare graph node IDs against all per-world stage counts and stage-title mappings.
- Add parser tests for empty cells, valid nodes, maximum neighbor lists, invalid coordinates, duplicate edges, and disconnected nodes.
- Validate graph topology and route rendering against an original legally usable runtime.

## 8. Audit status

Completed at static-source level: 12×12 packed-long grid; zero/nonnull cell convention; confirmed stage-index field bits 6–10; neighbor-count field bits 11–13; repeated 4-bit x/y neighbor coordinates beginning at bit 14; partial low-field state uses; graph-based route traversal; node initialization from stage flag 0x40; 240×320 map coordinate constants.

Not completed: exact semantics of low fields; maximum legal neighbor count; complete edge selection/tie-breaking; secret-node encoding/prerequisites; graph source provenance; parser tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.