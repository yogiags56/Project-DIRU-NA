# DIRU-NA DEV-003W9.3 — Runtime array consumer and persistence audit

## Source identity

Static evidence target: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This is a source-level lifecycle audit, not retail-runtime validation.

## 1. eByteArr — mutable runtime span records

### Allocation and initial population
- Static `byte[] eByteArr` is allocated as `new byte[awInt * 3]` after Pass A counters are complete.
- Pass B writes one three-byte slot per counted record and stores the slot index in the high byte of the corresponding `bIntArrArr[x][y]` cell.
- Initial Pass B branch for player-layer code 48 writes:
  - byte 0 = candidate x
  - byte 1 = candidate x
  - byte 2 = y when `dInt(candidateX,y) >= 0`, otherwise -1.
- **Correction to the earlier W9.2 wording:** the equal first two coordinates are only the initial form for that branch. They are not an invariant of the runtime record.

### Runtime readers and writers
- `EVoid()` iterates records in steps of three and reads all three bytes to render a horizontal special-object span in the visible viewport. The first two bytes are interpreted as horizontal endpoints; byte 2 is the row.
- `VVoid()` iterates active records (row byte > 0), adjusts both horizontal endpoints through `aByte(...)`, then calls `aBoolean(x,y)` for every x in the resulting inclusive interval. Thus records are actively mutated as the object moves/updates.
- `aBoolean(x,y)` calls the player-layer classifier; for recognized special object cells it applies the neighborhood cByte update helper and associated feature state.
- Player interaction logic around the later source section reads the record row and horizontal endpoints to detect whether a target cell lies adjacent to a special span.
- Special-object mutation code writes record bytes through the index stored in the high byte of `bIntArrArr`. The inspected `dVoid(int,int)` path can set row byte to -1 when removing/clearing the special object. Other movement paths update endpoint and row bytes.
- Consequently, `eByteArr` is not merely a load-time descriptor table. It is mutable runtime state used by rendering, collision/interaction and object movement.

### Checkpoint behavior
- `saveCheckpoint()` copies stage grids `bIntArrArr`, `bByteArrArr`, player layer and foreground layer into checkpoint arrays. It also copies selected derived arrays/scalars such as `mByteArr`, and optional-mode arrays/longs.
- It does **not** copy `eByteArr`.
- `restoreCheckpoint()` restores those stage grids, performs a cByte neighborhood rebuild, then scans restored player-layer cells for code 48. For each such cell it sets the current coordinates and dispatches to `dVoid((byte)48)` or `aiVoid()` depending on background flag bit 0x8. Those routines reconstruct/update the corresponding three-byte record using the record index in the high byte of `bIntArrArr`.
- Therefore the observed design is **reconstruction from checkpointed stage/object grids**, not direct persistence of the eByteArr byte array. This dependency must be preserved in the native restore order.

## 2. pByteArr — stage-derived resource type map

### Allocation and population
- Static `byte[] pByteArr` is allocated to `n11`, matching `aClasscArr` length.
- Pass B populates it from foreground group payloads (codes 0 and 30) and player-layer object cases (26, 42, 24, 27, 40), using the corresponding legacy type tags.
- This is a stage-derived type/resource map. It is paired by index with `aClasscArr`.

### Readers
- The asynchronous loading path uses each `pByteArr[index]` to initialize the corresponding `c` resource/sprite object via `aVoid(type)`.
- The special loading completion branch also initializes remaining entries from index 16 onward using the same type map.
- Runtime `kVoid(type)` scans `pByteArr`, selects the matching `aClasscArr[index]`, and invokes its animation/start method.
- The scan does not break on a match; if duplicate tags exist, the last matching index wins. Preserve this behavior unless a fixture proves duplicates cannot occur.
- The source clears `pByteArr` and the paired resource array during teardown and resets the static map during class initialization.

### Checkpoint behavior
- `saveCheckpoint()` and `restoreCheckpoint()` do not copy `pByteArr`.
- Its contents are derived from the loaded stage and resource metadata, rather than per-checkpoint mutable gameplay progress. Resource instances are lifecycle-managed separately.

## 3. Native architecture consequences

1. Keep eByteArr conceptually separate from immutable preprocessing output. It is mutable runtime state.
2. Model each eByteArr entry as a three-byte record only behind an adapter that preserves exact byte encoding, signed-byte behavior, record indexing, and mutation order.
3. Checkpoint capture must include the source arrays that make eByteArr reconstructible, especially the high-byte record index in background/object grid and player-layer markers.
4. Restore order must be: restore source stage/object grids and player layer; rebuild cByte neighborhood state; scan player-layer code 48 in source order; rebuild/update eByteArr via the matching branch. Do not restore stale eByteArr bytes from a snapshot.
5. pByteArr is immutable stage-derived metadata for a loaded stage. Pair it index-for-index with its resource objects; do not put it into gameplay checkpoint snapshots.
6. Duplicate pByteArr tags require explicit last-match-wins semantics in the runtime selector.
7. Signed Java byte values must not be accidentally widened as unsigned when reading the row marker or -1 sentinel.

## 4. Remaining evidence gates

- Trace every caller of `VVoid()`, `EVoid()`, `aBoolean(x,y)`, `dVoid(...)`, and `aiVoid()` to determine lifecycle timing and whether record rebuild is complete for every object subtype.
- Confirm the exact high-byte record index encoding and bounds for all eByteArr writers.
- Audit save/restore scalar fields and all checkpoint arrays beyond the subset inspected here.
- Trace `c` resource class initialization and ownership before designing native resource lifetime.
- Build fixture stages with multiple special-object spans, duplicate pByteArr tags, negative row sentinels, and checkpoint restore cases.

## 5. Milestone status

W9.3 documents consumer and persistence semantics only. No eByteArr runtime adapter, checkpoint implementation, production preprocessing executor, collision activation, or GameSession integration is introduced by this document.
