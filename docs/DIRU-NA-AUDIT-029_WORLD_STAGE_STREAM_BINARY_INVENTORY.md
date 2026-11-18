# DIRU-NA-AUDIT-029 — World Stage-Stream Binary Extraction & Inventory

## Scope and evidence boundary

This audit directly decodes /w0.bin, /w1.bin, and /w2.bin from the read-only candidate kubikaugustyn/DiamondRushSource at pinned commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. The format was cross-checked against the stage-loading and save-initialization code in src/main/java/i.java. This is static binary inspection, not a game-runtime test. No source or binary assets were copied into Project-DIRU-NA.

## 1. Container format reconstructed

Each file begins with:

1. One leading byte with value 0x01 in all three inspected files. Its semantic meaning is not named by the inspected loader; retain it as an opaque prefix/version-like byte.
2. One byte containing the number of stage records.
3. For each stage record:
   - Width: unsigned 16-bit little-endian.
   - Height: unsigned 16-bit little-endian.
   - Player/object layer: width × height bytes.
   - Background layer: width × height bytes.
   - Foreground layer: width × height bytes.

The source indexes layer data as x + y × width and copies it into [x][y] arrays. The three layers are contiguous and uncompressed in these resources. There is no per-stage delimiter between layers beyond the known dimensions.

## 2. Whole-file validation

| Resource | File bytes | Prefix | Stage count | Parsed stages | Consumed bytes | Trailing bytes |
|---|---:|---:|---:|---:|---:|---:|
| /w0.bin (Angkor) | 47,686 | 0x01 | 14 | 14 | 47,686 | 0 |
| /w1.bin (Scotland resource / Bavaria UI) | 46,956 | 0x01 | 13 | 13 | 46,956 | 0 |
| /w2.bin (Tibet resource / Siberia UI) | 56,887 | 0x01 | 14 | 14 | 56,887 | 0 |

All declared stage records parse successfully using the reconstructed format. Every file is consumed exactly, with no truncation and no trailing bytes.

## 3. Complete stage dimension inventory

### Angkor — /w0.bin

| Stage index | Width × height | Tiles per layer |
|---:|---:|---:|
| 0 | 26 × 21 | 546 |
| 1 | 27 × 24 | 648 |
| 2 | 27 × 26 | 702 |
| 3 | 40 × 23 | 920 |
| 4 | 51 × 24 | 1,224 |
| 5 | 30 × 75 | 2,250 |
| 6 | 26 × 45 | 1,170 |
| 7 | 44 × 29 | 1,276 |
| 8 | 35 × 14 | 490 |
| 9 | 50 × 30 | 1,500 |
| 10 | 50 × 31 | 1,550 |
| 11 | 46 × 31 | 1,426 |
| 12 | 46 × 31 | 1,426 |
| 13 | 68 × 11 | 748 |

Total: 14 stages, 15,876 tiles per layer, 47,628 layer bytes. Width range 26–68; height range 11–75.

### Scotland resource — Bavaria UI — /w1.bin

| Stage index | Width × height | Tiles per layer |
|---:|---:|---:|
| 0 | 45 × 24 | 1,080 |
| 1 | 40 × 24 | 960 |
| 2 | 33 × 30 | 990 |
| 3 | 43 × 30 | 1,290 |
| 4 | 45 × 33 | 1,485 |
| 5 | 43 × 33 | 1,419 |
| 6 | 36 × 43 | 1,548 |
| 7 | 53 × 27 | 1,431 |
| 8 | 34 × 32 | 1,088 |
| 9 | 35 × 25 | 875 |
| 10 | 37 × 24 | 888 |
| 11 | 60 × 20 | 1,200 |
| 12 | 23 × 60 | 1,380 |

Total: 13 stages, 15,634 tiles per layer, 46,902 layer bytes. Width range 23–60; height range 20–60.

### Tibet resource — Siberia UI — /w2.bin

| Stage index | Width × height | Tiles per layer |
|---:|---:|---:|
| 0 | 60 × 26 | 1,560 |
| 1 | 39 × 35 | 1,365 |
| 2 | 35 × 42 | 1,470 |
| 3 | 38 × 38 | 1,444 |
| 4 | 46 × 27 | 1,242 |
| 5 | 49 × 28 | 1,372 |
| 6 | 55 × 30 | 1,650 |
| 7 | 51 × 20 | 1,020 |
| 8 | 51 × 37 | 1,887 |
| 9 | 104 × 17 | 1,768 |
| 10 | 35 × 25 | 875 |
| 11 | 45 × 28 | 1,260 |
| 12 | 35 × 32 | 1,120 |
| 13 | 35 × 26 | 910 |

Total: 14 stages, 18,943 tiles per layer, 56,829 layer bytes. Width range 35–104; height range 17–42.

## 4. Layer-size and memory implications

Across all three worlds:
- 41 stage records.
- 50,453 tile cells per layer when summing all stage maps.
- 151,359 raw bytes across the three layers.
- Largest single stage by tile count: Angkor stage 5 at 30 × 75 = 2,250 cells per layer.
- Widest stage: Siberia stage 9 at 104 × 17.

The runtime allocates byte[width][height] for the player/object and background layers, and int[width][height] for the foreground layer after loading. A Native implementation should preserve the logical dimensions and layer order but use checked multiplication and bounded reads before allocation.

## 5. Reconciliation with World Map nodes and title mappings

| World | Stage-stream records | Map nodes | Map node IDs | Title mapping entries |
|---|---:|---:|---|---:|
| Angkor | 14 | 13 | 0–12 | 13 |
| Bavaria UI / Scotland resource | 13 | 13 | 0–12 | 13 |
| Siberia UI / Tibet resource | 14 | 14 | 0–13 | 15 |

Confirmed observations:
- Scotland/Bavaria aligns one-to-one: 13 stage records, 13 map nodes, and 13 title mapping entries.
- Angkor has one more stage-stream record than map nodes: stage record index 13 exists in /w0.bin, but map_angkor.out has nodes only for IDs 0–12 and the title mapping has 13 entries.
- Tibet/Siberia has 14 stage records and 14 map nodes (IDs 0–13), but the title mapping has 15 entries. Its mapping entry for index 14 has no corresponding stage-stream record or map node in the decoded files.

These are two distinct mismatches. Do not synthesize a map node or stage record to force the arrays to match. The extra Angkor stage record and extra Tibet/Siberia title mapping entry require source/runtime investigation; neither is proven to be a secret stage or an intro stage by this audit.

## 6. Parser robustness requirements for Native Android

- Treat the leading 0x01 byte as opaque until its meaning is proven.
- Read the stage count only after verifying the prefix exists.
- Read each four-byte dimension header exactly.
- Reject zero or unreasonable dimensions and use overflow-safe width × height calculations.
- Verify that 3 × width × height bytes remain before allocating or reading layer buffers.
- Preserve layer order: player/object, background, foreground.
- Preserve row-major source indexing x + y × width when converting into native arrays.
- Require exact file consumption unless a future verified format revision documents trailing data.
- Keep stage-stream inventory, map-node inventory, title mapping, and save progression as separate data catalogs linked by explicit IDs.

## 7. Open work

- Inspect the extra Angkor stage record index 13 and determine whether it is a bonus, unused, hidden, or otherwise special record.
- Resolve the Tibet/Siberia title mapping entry index 14 and identify why it has no stage resource or map node.
- Trace how the leading byte 0x01 is used by other versions or loaders, if any.
- Correlate all 41 stage records with stage title arrays and map-node IDs without assuming index+1 title semantics.
- Build a native fixture manifest containing per-stage dimensions and hashes, without copying source payloads into the app before provenance review.
- Add tests for truncated files, invalid dimensions, overflow, wrong layer lengths, and count mismatches.
- Validate selected stages and map navigation against an original legally usable JAR/runtime.

## 8. Audit status

Completed at static/binary level: all three world stage streams decoded; 41 stage records inventoried; exact dimensions and per-layer byte counts recorded; full-file byte consumption verified; map-node/title-count discrepancies isolated.

Open: semantic explanation of the two count anomalies; leading-byte meaning; automated parser tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed.