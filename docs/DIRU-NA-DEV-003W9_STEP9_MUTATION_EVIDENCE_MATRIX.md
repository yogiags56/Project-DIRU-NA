# DIRU-NA DEV-003W9 — Step-9 mutation evidence matrix

## Evidence source

Pinned static candidate: `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`, `src/main/java/i.java`, stage initialization step 9. The pass scans `x = 0..width-1` outer and `y = 0..height-1` inner.

This document records directly inspected branches only. It is not a complete semantic tile catalog and does not claim runtime validation.

## Per-cell prelude

For every `(x,y)`, before branch processing:
- `bByteArrArr[x][y] = 0`
- `cByteArrArr[x][y] = 0`
- Read foreground, background-runtime and player/object values for the current coordinate.

The pass mutates these arrays in place. A Native implementation must preserve the scan order and model neighbor reads against the current, potentially already-mutated state.

## Foreground branch matrix

| Authored foreground low byte | Observed writes/effects | Neighbor / dependency |
|---|---|---|
| 31 | Set feature bit; repack current foreground using background in upper bits; falls through to 19 packing | Current background |
| 19 | Repack as `(background << 8) | 0x13` | Current background |
| 17 | May remove/repack current foreground; may alter foreground above; records counts/types in maps | `(x,y-1)` foreground and player; background-keyed maps |
| 14, 33 | Normalize based on mode; set feature bit; optional collected-item transformation to player code 41/background 10, or remove player and set foreground bit 0x100 | Stage/world/mode and collected-item lookup |
| 2 | Set feature bits; repack foreground with background | Background low value 0/1 affects feature flags |
| 8, 9 | Set environment flags; 9 enables water-breathing-potion requirement; count by background; repack | Background |
| 7 | Store door coordinates indexed by background; repack foreground | Background must be a valid door index; later pass also inspects neighbors |
| 30, 1, 26 | 30 sets feature bit/counter; all repack with background | Background |
| 0 | Increment entity counter; repack with background | Background |
| 4 | Increment counter; set feature bit; repack | Background |
| 5, 28 | Store stage-specific background values in scalar bytes | Background |
| 3 | Set cByte to 127; if background > 0, repack with background+1 | Background |
| 6 | Count by background; set feature bit; repack | Background |
| 20..25 | Retain foreground code; set world-specific feature bit | World index |
| 34 | Set feature bit | None observed in this branch |
| other values 0..79 | Remove unsupported foreground values in default branch | Exact code-specific coverage remains incomplete |

## Player/object branch matrix

The following effects are directly visible in the inspected switch:

| Player/object code | Observed state effects |
|---|---|
| 48 | Set background runtime to 16 or 0 based on low bits; increment entity count; duplicate/convert cell above to code 48 with background 8 |
| 47 | cByte=48; background=0; set feature bit |
| 46 | background=0; cByte=24; bByte=0; set feature bit |
| 45 | background=0; cByte=24; set feature bit |
| 44 | cByte=24; background=0; set feature bit |
| 42 | Increment counters; set flags; call entity registration helper |
| 41 | Ensure background positive; add it to collected/retained total |
| 40 | Increment counters; set flags; call entity registration helper |
| 12 | Remove player-layer marker; record coordinates and background in stage scalar fields |
| 36 | Normalize background unless equal to 1; set feature bit |
| 18 | Reset two scalar counters; set feature flags |
| 34 | Remove player-layer cell; set foreground to 15; set feature bit |
| 35 | Preserve player code; remove foreground; set feature flag and fan-pot requirement |
| 39 | Set stage boolean and feature bit |
| 38 | Set stage boolean; set foreground to 27; set feature flags |
| 14 | Set feature bit; normalize background; cByte=24 |
| 28 | Set feature bit; for background >10 divide by 11 and set bit 0x8; cByte=24 |
| 79 | Set player start coordinates and camera-related scalar positions; remove spawn marker |
| 11 | Set background to 16 or 0 depending on authored background; cByte=48; set feature bit |
| 49 | cByte=48 |
| 43 | Set world-dependent feature; update background flags; cByte=48 |
| 19 | Set world-dependent feature and counter; cByte=48 |
| 22, 23 | Set feature bits; cByte=48 |
| 30 | Set feature bit; background=0 |
| 37 | Set feature bit; background=0 |
| 10 | Set background=0; request resource mask bit 0x2 |
| 16 | May duplicate code 16 into cell above with copied background; set feature bit |
| 6, 7 | Entity registration/counters and background normalization |
| 26, 24, 27 | Increment counters; set flags; register entity |
| 8, 4, 5, 2, 0, 1 | Fall-through chain updates feature bits, counters, cByte=48, background=0, resource mask and retained-item count |

Default player/object codes in the range 0..79 may be cleared from the player layer. Do not infer semantic meanings from the numeric values alone.

## Derived data after the first scan

The inspected code allocates or prepares:
- `dByteArr` sized from the entity counter.
- `aClasscArr` and `pByteArr` sized from another entity counter.
- `kByteArr` sized from door/checkpoint-related counts.
- `mByteArr` and `lByteArr` sized from a background-keyed maximum.
- `eByteArr` sized to three bytes per selected entity.

A subsequent scan populates foreground-derived coordinate/metadata tables and entity records. That second scan is not fully mapped in this milestone.

## Required safety conditions

- Preserve x-major/y-minor mutation order.
- Do not read `y-1`, `y+1`, `x-1`, or `x+1` without explicit source-compatible bounds handling.
- Validate background-derived indices before using them to index door arrays.
- Preserve Java signed-byte behavior for byte grids and player/object values.
- Keep authored StageGrid data distinct from transformed runtime layers.
- Keep source mode/world-dependent behavior explicit.
- Do not silently replace unknown codes with generic solid/empty behavior.

## Remaining gaps

- Full second scan and all derived table population.
- Exact helper semantics for entity registration.
- Complete branch/fall-through audit for all foreground and player values.
- Exact neighbor boundary behavior and map-edge preconditions.
- Complete player spawn/camera ordering and scalar ownership.
- Synthetic tests against every branch and source-compatible edge case.
- Runtime validation against a legally usable original build.

## Next

DEV-003W10: implement a narrowly scoped, testable preprocessing pass in phases, starting with the unconditional per-cell reset and only those foreground branches whose neighbor requirements are fully bounded. Keep unimplemented branches explicit and fail closed; do not produce a production collision seed from a partial pass.
