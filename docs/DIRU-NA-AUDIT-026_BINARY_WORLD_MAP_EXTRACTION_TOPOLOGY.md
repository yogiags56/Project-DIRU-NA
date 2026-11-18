# DIRU-NA-AUDIT-026 — Binary World-Map Extraction & Topology Validation

## Scope and evidence boundary

This audit directly decodes the three binary map resources from the read-only source candidate kubikaugustyn/DiamondRushSource, pinned at commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Files decoded: src/main/resources/map_angkor.out, map_scotland.out, and map_tibet.out. The decoder follows the parser in src/main/java/i.java (aVoid(String)).

This is a binary inventory and structural validation, not an original-game runtime test. It does not identify visual artwork or prove secret-stage unlock requirements. No source files or assets were copied into the active Native Android repository.

## 1. Important parser-layout correction to AUDIT-025

The parser consumes five fixed bytes per node record before the neighbor coordinate pairs, not six:

| Order | Byte | Meaning |
|---:|---|---|
| 1 | b | Grid X |
| 2 | b2 | Grid Y |
| 3 | b3 | Secondary node field, packed into bits 3–5 |
| 4 | dkInt | Stage index, packed into bits 6–10 |
| 5 | b4 | Neighbor count, packed into bits 11–13 |
| 6 onward | X,Y byte pairs | Neighbor coordinates, each packed into 4 bits |

The parser first initializes bits 0–2 to value 1 for each node, then later map initialization rewrites that field according to stage flag 0x40 (with stage index 0 handled specially). Thus the resource file does not contain an explicit first low-three-bit state byte. This corrects the ambiguous field table in AUDIT-025.

## 2. Binary envelope and exact sizes

| Resource | File bytes | Declared payload bytes | Node records | Payload consumed | Trailing bytes |
|---|---:|---:|---:|---:|---:|
| map_angkor.out | 116 | 113 | 13 | 113 | 0 |
| map_scotland.out | 116 | 113 | 13 | 113 | 0 |
| map_tibet.out | 125 | 122 | 14 | 122 | 0 |

All three files have a 2-byte little-endian payload length followed by a 1-byte record count. Every declared record parses successfully, consumes the complete payload, and leaves no trailing bytes.

## 3. Per-world node inventory

### Angkor — map_angkor.out

13 nodes, stage IDs 0–12. 24 directed neighbor references (12 undirected reciprocal links). Grid coordinates:

| Stage ID | X | Y | Secondary field | Neighbors (stage IDs) |
|---:|---:|---:|---:|---|
| 0 | 1 | 3 | 0 | 1 |
| 1 | 4 | 4 | 0 | 0, 2 |
| 2 | 7 | 3 | 0 | 1, 3 |
| 3 | 10 | 4 | 0 | 2, 4 |
| 4 | 8 | 6 | 0 | 3, 5 |
| 5 | 8 | 8 | 0 | 4, 6 |
| 6 | 8 | 10 | 0 | 5, 7, 9 |
| 7 | 5 | 10 | 0 | 6, 8, 12 |
| 8 | 2 | 10 | 0 | 7 |
| 9 | 11 | 10 | 1 | 6, 10 |
| 10 | 11 | 8 | 1 | 9, 11 |
| 11 | 11 | 6 | 1 | 10 |
| 12 | 5 | 8 | 1 | 7 |

### Bavaria UI world — map_scotland.out

13 nodes, stage IDs 0–12. 24 directed neighbor references (12 undirected reciprocal links).

| Stage ID | X | Y | Secondary field | Neighbors (stage IDs) |
|---:|---:|---:|---:|---|
| 0 | 0 | 4 | 0 | 1 |
| 1 | 1 | 7 | 0 | 0, 2 |
| 2 | 3 | 4 | 0 | 1, 3 |
| 3 | 5 | 4 | 0 | 2, 4, 5 |
| 4 | 5 | 6 | 0 | 3, 6 |
| 5 | 5 | 8 | 0 | 4, 6, 7 |
| 6 | 7 | 8 | 0 | 5, 7 |
| 7 | 9 | 8 | 0 | 6, 10 |
| 8 | 11 | 6 | 0 | 10, 9 |
| 9 | 9 | 4 | 0 | 8 |
| 10 | 7 | 6 | 1 | 3 |
| 11 | 7 | 10 | 1 | 6, 12 |
| 12 | 3 | 10 | 1 | 11 |

### Siberia UI world — map_tibet.out

14 nodes, stage IDs 0–13. 26 directed neighbor references (13 undirected reciprocal links).

| Stage ID | X | Y | Secondary field | Neighbors (stage IDs) |
|---:|---:|---:|---:|---|
| 0 | 0 | 9 | 0 | 1 |
| 1 | 2 | 8 | 0 | 0, 2, 11 |
| 2 | 4 | 6 | 0 | 1, 3 |
| 3 | 6 | 5 | 0 | 2, 4 |
| 4 | 8 | 6 | 0 | 3, 5, 12 |
| 5 | 10 | 5 | 0 | 4, 6 |
| 6 | 11 | 3 | 0 | 5, 7 |
| 7 | 8 | 3 | 0 | 6, 8 |
| 8 | 6 | 3 | 0 | 7, 9 |
| 9 | 4 | 3 | 0 | 8, 10 |
| 10 | 2 | 3 | 0 | 9 |
| 11 | 2 | 5 | 1 | 1 |
| 12 | 8 | 8 | 1 | 4, 13 |
| 13 | 6 | 10 | 1 | 12 |

Neighbor lists above are converted from resource coordinates to stage IDs by matching each coordinate to its node record. This conversion is an audit convenience; the source stores neighbor coordinates, not stage IDs.

## 4. Topology validation results

| Validation | Angkor | Scotland/Bavaria | Tibet/Siberia |
|---|---:|---:|---:|
| Node count | 13 | 13 | 14 |
| Directed references | 24 | 24 | 26 |
| Undirected links | 12 | 12 | 13 |
| Duplicate node coordinates | 0 | 0 | 0 |
| Out-of-grid node coordinates | 0 | 0 | 0 |
| Missing neighbor targets | 0 | 0 | 0 |
| Non-reciprocal neighbor links | 0 | 0 | 0 |
| Connected components | 1 | 1 | 1 |

All node coordinates fall within the 12×12 grid. Every neighbor coordinate references an existing node. All links are reciprocal, with no duplicate node coordinates. Each world map forms one connected component.

Node degree distribution:
- Angkor and Scotland: four degree-1 nodes, seven degree-2 nodes, two degree-3 nodes.
- Tibet: four degree-1 nodes, eight degree-2 nodes, two degree-3 nodes.

## 5. Secondary field observation

The resource's secondary field is 0 for the main route nodes and 1 for a small set of additional nodes:
- Angkor: stage IDs 9, 10, 11, 12.
- Scotland/Bavaria: stage IDs 10, 11, 12.
- Tibet/Siberia: stage IDs 11, 12, 13.

This is a strong structural clue that the field distinguishes a subset of nodes, but its exact gameplay meaning is not proven by this extraction alone. It must remain a raw classification field until all readers and runtime behavior are reconciled. Do not label it definitively as Secret Stage.

## 6. Implications for Native Android

- The Native graph can be represented as 13 nodes / 12 links for Angkor, 13 / 12 for Bavaria, and 14 / 13 for Siberia.
- Store stable stage IDs and coordinate pairs; preserve original neighbor ordering because source directional selection may depend on list order.
- Preserve the secondary field as an opaque enum/integer, not a hard-coded secret-stage boolean.
- Node state bits 0–2 remain runtime progression state and are initialized from save flags, not directly read as a resource-file field.
- Validate exact payload consumption, record count, unique coordinates, coordinate bounds, neighbor existence, reciprocal links, and connectivity.
- Keep map resource filename mapping separate from user-facing world names until the Nokia/Gameloft package variant is independently verified.

## 7. Open work

- Trace every reader of the secondary field (packed bits 3–5) and compare the listed node subsets with actual map rendering and stage titles.
- Confirm whether secondary-field-1 nodes correspond to secret/bonus/alternate route stages or another presentation class.
- Correlate all map stage IDs with the world-specific stage-title arrays and stage stream counts.
- Trace directional tie-breaking on the now-known real graph, preserving neighbor list order.
- Decode and identify node sprite frames using legally usable assets.
- Add a deterministic parser/graph test suite using the three extracted record inventories as fixtures.
- Validate UI appearance and navigation against an original legally usable JAR/runtime.

## 8. Audit status

Completed: direct binary decoding of all three world-map resources; exact payload and record counts; full per-world node/neighbor inventories; stage-ID mapping; coordinate and neighbor validation; reciprocity and connectivity checks; correction of AUDIT-025's record-field count ambiguity.

Open: semantic identification of secondary-field-1 nodes; secret-stage proof; sprite mapping; directional tie-break completion; automated tests; original-runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed.