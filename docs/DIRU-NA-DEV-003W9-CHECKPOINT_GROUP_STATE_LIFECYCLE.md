# DIRU-NA DEV-003W9.8 — Checkpoint and group-state lifecycle audit

## Source identity
Static source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.
Static decompiled-source audit only; no binary stage fixture or retail runtime validation was performed.

## 1. Stage allocation and record type 17

- At stage setup, `cmInt` is initialized to -1 and `mByteArr`/`lByteArr` are cleared to null before loading the stage resource.
- The stage parser allocates both arrays to the same length, derived from the highest group bit/index found in its packed stage metadata.
- During the Y-major then X-major foreground scan, record/tile code 17 with a nonnegative group index writes:
  - `mByteArr[group]` from `hashtable2[group]`, default 0, narrowed to Java byte.
  - `lByteArr[group]` from `hashtable3[group]`, default 57, narrowed to Java byte.
- This is not a direct flat serialized array record; the two values are resolved through parsed group hash tables while walking foreground cells.
- Exact parser ordering matters: arrays are allocated before the scan, and each encountered group record assigns its indexed entries. Repeated group IDs may write the same slot; the final value depends on the parsed table value (not cell order) because lookup is keyed by group ID.

## 2. lByteArr consumer scope

- The inspected source has one direct indexed consumer: `texts[lByteArr[cmInt]]`, used to select the localized prompt for the currently active group.
- `lByteArr` is therefore a localized text-index table/metadata, not a countdown and not a second mutable timer.
- No writer after stage parse was found in the inspected source file. It is stage metadata and should remain immutable during ordinary gameplay unless further source evidence appears.
- Its byte is used as an array index after Java signed-byte promotion. Values outside the valid `texts` index range may fail; do not silently reinterpret as unsigned or clamp without fixture evidence.

## 3. Checkpoint capture order

The audited `saveCheckpoint()` order is:
1. Capture scalar player/world/game state fields.
2. Capture active group selector: `cnInt = cmInt`.
3. If `mByteArr != null`, copy the full countdown array into preallocated `oByteArr`.
4. Copy mutable stage grids column-by-column: background/object int grid, cByte grid, player layer, foreground layer.
5. If `lBoolean` is active, capture additional mode-specific grids, long arrays, flags and scalar state.

The stage setup path allocates `oByteArr` to `mByteArr.length` when group arrays exist. The capture path assumes that allocation/length relationship.

There is no corresponding checkpoint copy of `lByteArr`: it is parsed stage metadata, not mutable group progress.

## 4. Checkpoint restore order

The audited `restoreCheckpoint()` order is:
1. Restore player/camera and scalar state, including player coordinates.
2. Apply mode-specific restoration branches (some may call `yVoid()` or set mode counters).
3. Restore active group selector: `cmInt = cnInt`.
4. If `mByteArr != null`, copy `oByteArr` back into the full countdown array.
5. Restore stage grids column-by-column.
6. Recompute cByte neighborhoods over interior cells where the restored player/foreground values qualify.
7. Restore additional mode-specific state if `lBoolean` is active.
8. Continue later restoration logic, including transient object/animation handling.

This ordering means group selector/countdown are restored before grid restoration and neighborhood recomputation. Do not rebuild countdown from foreground cells after the copy.

## 5. SaveCheckpoint call sites and lifecycle

The inspected file has two direct save call sites:
- Stage-transition/setup flow after event queue cursors `adInt` and `aeInt` are both reset to zero.
- A gameplay path after sound ID 9 and a short 100 ms delay.

The queue cursors are reset before the stage-transition checkpoint call. The save routine itself does not capture the event queue arrays/cursors. This supports treating the event animation queue as transient, not checkpoint-persistent state.

The second call site is a normal gameplay save; exact user-facing semantic meaning should be named only after tracing its surrounding game-state branch.

## 6. State ownership matrix

| State | Owner/lifecycle | Checkpoint behavior |
|---|---|---|
| `mByteArr` | Mutable per-stage group countdown | Captured to `oByteArr`, restored |
| `lByteArr` | Parsed per-stage localized text index | Not copied; retained as stage metadata |
| `cmInt` | Active group selector | Captured to `cnInt`, restored |
| `cnInt` | Checkpoint scalar | Written during save |
| `oByteArr` | Checkpoint copy of group countdowns | Written during save, read during restore |
| Event ring arrays/cursors | Transient animation work list | Not captured; stage-transition cursors reset |
| Stage grids | Mutable stage state | Copied into checkpoint grids and restored |

## 7. Native implementation/test implications

- Parse stage group tables before processing record-17 group references; allocate arrays from validated group index bounds.
- Keep `GroupCountdownState` mutable and `GroupTextIndexTable` immutable/separate.
- Preserve Java signed-byte narrowing and promotion. Add tests for table values around 0x7F/0x80/0xFF and invalid localized text indices.
- Capture/restore group selector and countdown array atomically with the stage-grid snapshot.
- Test save while a group countdown is partially elapsed, restore, and verify both selector and remaining countdown.
- Test no-group stages (`mByteArr == null`) and matching null snapshot behavior.
- Do not snapshot transient event-animation queue unless new source evidence shows it is required.
- Preserve save and restore ordering; do not derive mutable group state from restored foreground grid.

## 8. Milestone boundary and next

W9.8 is documentation/source audit only. No production group-countdown or checkpoint code is added.

Next: close DEV-003W9 with a source-coverage matrix and readiness review. Identify any remaining unresolved source questions, then define the smallest deterministic native implementation slice and tests. Do not begin full gameplay integration merely because this audit is complete.
