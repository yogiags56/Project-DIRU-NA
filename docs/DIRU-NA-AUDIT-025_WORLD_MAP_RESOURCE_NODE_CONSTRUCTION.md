# DIRU-NA-AUDIT-025 — World-Map Resource Records, Node Construction & Stage Mapping

## Scope and evidence boundary

This audit examines the read-only source candidate kubikaugustyn/DiamondRushSource at pinned commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Inspected src/main/java/i.java around world-map resource selection/loading, the map parser aVoid(String), packed-node setters, world-map grid allocation, and legacy save initialization from per-world stage streams.

This is static-source analysis only. The parser was inspected, but the binary map files were not independently decoded or runtime-validated in this pass. The exact contents of every map record, complete topology, and all secret-stage prerequisites remain unverified. No assets were copied and no Android build/APK was produced.

## 1. Three world-map resources

The source selects one map resource by world index:

| World index | Resource |
|---:|---|
| 0 | /map_angkor.out |
| 1 | /map_scotland.out |
| 2 | /map_tibet.out |

The names Scotland and Tibet are the resource filenames used by the source; the world-selection UI labels are separately mapped to Bavaria and Siberia. Preserve this distinction in documentation and do not rename source resources based on UI labels.

## 2. Binary map-file envelope

The parser reads:
- Two bytes as a little-endian payload length.
- One byte as the number of node records.
- Exactly that many records from the payload buffer.

The payload is loaded into fByteArr, then parsed sequentially. The record count controls the number of node entries. The current implementation reads the declared payload into a fixed byte array in one read call and does not visibly validate every boundary before indexing; Native parsing must be stricter.

## 3. Node record layout

The parser consumes the following fields for each node:

| Field order | Width | Meaning |
|---|---:|---|
| 1 | 1 byte | Grid X coordinate |
| 2 | 1 byte | Grid Y coordinate |
| 3 | 1 byte | First packed node field, written to bits 0–2 |
| 4 | 1 byte | Secondary packed node field, written to bits 3–5 |
| 5 | 1 byte | Stage index, written to bits 6–10 |
| 6 | 1 byte | Neighbor count, written to bits 11–13 |
| 7 onward | 2 bytes per neighbor | Neighbor X then neighbor Y, each packed into 4 bits |

The parser initializes the low three-bit field to 1 for every record, then writes the record's supplied first packed field over it. It writes the secondary field, stage index, neighbor count, and neighbor coordinate pairs using the packed-long setter.

Therefore, the packed-long node structure from AUDIT-023 is not invented dynamically from stage order: it is constructed from explicit records in the world's map resource.

## 4. Grid construction and dimensions

Before loading a map, the game allocates a 12×12 long array and clears the route-node coordinate buffer. Each record's X/Y values are used directly as indices into that grid. The parser does not derive arbitrary grid dimensions from the file envelope in the inspected method.

The parser does not show a full validation layer for X/Y bounds, duplicate node coordinates, duplicate neighbor edges, missing neighbor nodes, self-links, or neighbor-count overflow. These must be validated before a Native graph is accepted.

## 5. Relationship to stage resources and save data

The map record's stage index is written to packed bits 6–10. The same stage index is later used to:
- Match the currently selected stage node.
- Match the progression target node.
- Look up the localized stage title through the per-world title-index array.
- Address the per-world stage save record.

The per-world map resource is therefore the topology/presentation index, while stage resources contain gameplay layers and save records contain persistent progress. These are related by stage index but are not the same data structure.

The source also scans each world stage stream while constructing the save record, stores a per-world stage count/max index, counts map marker value 2, and records coordinates for markers 14 and 33. That save initialization is a separate parse path from the world-map node resource parser.

## 6. Secret-stage identification: current evidence

The map parser exposes a first packed field and secondary classification field, but this audit does not establish a definitive secret-stage encoding from their values. Secret-stage title labels exist, and stage-title mapping arrays include non-sequential title indices, but those facts alone do not prove which map nodes are secret or their unlock prerequisites.

Keep secret-stage identity as unresolved metadata until map resource records are decoded and correlated with stage-title indices, stage streams, and all relevant progression checks.

## 7. Native Android parser requirements

- Parse the two-byte little-endian payload length and one-byte record count with exact bounded reads.
- Require the payload length to match the available resource bytes and ensure all records consume the payload exactly (or document permitted trailing bytes).
- Validate each X/Y coordinate is within 0..11 before indexing.
- Validate each neighbor count against the packed format capacity and remaining record bytes.
- Validate each neighbor coordinate is within 0..11 and references an existing node after all nodes are loaded.
- Reject or explicitly resolve duplicate node coordinates, duplicate edges, self-edges, and missing targets.
- Preserve raw record fields alongside decoded typed fields for auditability.
- Keep world-map resource topology separate from stage gameplay resources and native save progression.
- Map world UI labels to resource filenames explicitly: Angkor/Angkor file, Bavaria/Scotland file, Siberia/Tibet file, pending confirmation against original package/runtime.
- Do not infer secret-stage identity from an unverified packed-field value.

## 8. Open work

- Decode the three map resource files independently and publish exact record inventories.
- Validate each map's record count, payload length, coordinate bounds, neighbor count, and graph connectivity.
- Produce per-world node/edge tables and compare stage IDs with stage-title mappings and stage resource counts.
- Identify the semantic meaning of the two packed classification fields using all readers and the actual records.
- Establish secret-stage node identities and prerequisites, if encoded in map data.
- Verify the resource filename-to-world-label mapping against an original legally usable package.
- Add parser tests for truncated payloads, malformed counts, out-of-range coordinates, missing targets, duplicate nodes/edges, and maximum neighbor lists.
- Validate graph topology and navigation against an original runtime.

## 9. Audit status

Completed at static-source level: three map resource paths; binary envelope (16-bit little-endian payload length plus one-byte record count); node record field order and widths; mapping of record fields into packed-long bits; 12×12 grid construction; stage-index relationship to map selection/title/save addressing; distinction between map topology and stage gameplay/save structures.

Not completed: independent binary decoding of map resources; exact per-world node/edge inventory; full packed classification semantics; secret-stage encoding/prerequisites; parser tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.