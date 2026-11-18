# DIRU-NA-AUDIT-009 — Damage, Death, Retry & Checkpoint Reconstruction

## Scope and evidence boundary

This audit examines the read-only candidate source `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source ranges in `src/main/java/i.java`: `hurtHero`, player input/update handling, stage initialization, checkpoint snapshot/restore, retry handling, and checkpoint-triggered save paths.

Static source analysis only. No runtime, emulator, or handset validation was performed. Field names are decompiler names and are not semantic identifiers. Conclusions below are limited to behavior directly visible in inspected code.

## 1. Damage handler behavior

The method `hurtHero(int n, int hInt, int n2)`:

- Clears several transient action/state fields before processing.
- Selects animator slot 0 or slot 3 depending on bit `0x4000` in `kInt`.
- Ignores further damage while the selected animator is already at indices 40, 48, or 47.
- Applies a set of gating conditions involving timers, movement/interactions, `lByte`, bits in `kInt`, and `aTInt`.
- When accepted, increments a damage-related counter, updates a bounded value through `aVoid((byte)(nByte - n))`, and plays the hero-hurt sound.
- Writes the supplied state bits into `kInt` using mask `0xFFFFFF8F`.
- For `hInt == 16`, clears two counters, sets `aTInt = 5`, changes the player-layer cell to code 9, writes a special object-state value, and clears selected state bits. Control then falls through into the following case.
- For `hInt == 64`, requests the special animation selector value 1000; control then falls through to request animation index 10.
- For other accepted damage values, requests animation index 10.
- If `n2` is nonzero, searches directions using the direction-vector arrays for a free destination cell. On success, moves the logical player tile, sets `jInt = 18`, clears `aByte`, and updates direction/state bits.

The code also returns early for already-active hero animation indices 40, 48, and 47. The complete visual meaning of those indices is not established by this audit.

## 2. Damage sources and parameter families

Inspected call sites show multiple environmental and entity interactions call `hurtHero`, with differing arguments. Examples include:

- Damage from object/environment collision using `(1, 64, 0)`.
- Directional knockback variants using `(1, 48, direction)`.
- Higher-severity variants using `(2, 48, direction)`.
- Special state/health-dependent calls using `i.iByteArr[8]` as the first argument.
- A separate `(0, 16, 0)` path.

These argument patterns are evidence of different damage/response contexts, but the first argument must not be renamed “damage points” or “health loss” without tracing all call sites and the meaning of `nByte`. Similarly, the third argument is used as a direction search/knockback hint when nonzero; zero means that displacement branch is skipped.

## 3. Checkpoint snapshot semantics

The method `saveCheckpoint()` stores a broad in-memory snapshot:

- Player tile coordinates and multiple player/gameplay counters and state fields.
- The current checkpoint-related value from save byte 10.
- Several world/stage progress and inventory-like fields.
- The active stage object-state integer grid, per-cell byte timers, player/object layer, and foreground layer.
- Optional mode-specific state arrays and long-value arrays when `lBoolean` is enabled.
- Additional mode-specific bytes and counters.

The snapshot arrays are allocated during stage setup. The stage initialization path calls `saveCheckpoint()` at its checkpoint/setup stage. Another gameplay path guarded by `shouldSaveCheckpoint` also calls `saveCheckpoint()` after playing a sound and a short delay.

**Important persistence boundary:** this checkpoint snapshot is held in static arrays and instance fields. The inspected `saveCheckpoint()` and `restoreCheckpoint()` methods do not serialize this full snapshot to RMS. It is an in-memory respawn/checkpoint snapshot, not a durable app-restart save format.

## 4. Checkpoint activation and restore

The input/update path checks whether the player is standing on foreground code 4:

- Pressing the confirm/action key while at the checkpoint tile plays the checkpoint sound and calls `restoreCheckpoint()`.
- Pressing the respawn key also restores the checkpoint when the current player tile has foreground code 4, for the animation-state branches inspected.
- Otherwise, the respawn key may play the death sound and request animation index 19, depending on the current hero animation index.

The restore method:

1. Resets transient interaction/animation fields and sets camera offsets from the saved player tile.
2. Restores the checkpoint-related byte 10 and the captured player/progression fields.
3. Sets hero animator slot 0 to animation index 2.
4. Resets movement/state fields and restores the saved player tile coordinates.
5. Restores the stage grids: object-state integers, per-cell timers, player/object layer, and foreground layer.
6. Rebuilds dirty/update markers around relevant cells.
7. Restores optional mode-specific arrays and long counters.
8. Reconstructs special multi-cell object state for object code 48 after the grid restore.

The source therefore treats checkpoint restore as a full gameplay-world rollback to a captured in-memory state, not merely moving the hero to a coordinate.

## 5. Death and retry / lives flow

A retry handler `XVoid()` decrements `azInt` and increments `bdInt`. If `azInt` remains nonnegative, it restores the checkpoint, resets the hero's bounded value from save byte 8, clears selected movement/damage fields, and returns. If the value becomes negative, it changes the global game state to 12.

This demonstrates a retry-count gate around checkpoint restore. The exact user-facing label for `azInt` should be confirmed from UI text and all writers before naming it lives/continues.

The respawn key path also distinguishes checkpoint presence from ordinary death handling: at checkpoint foreground code 4 it calls `restoreCheckpoint()`; otherwise, for selected hero animation indices, it plays the death sound and selects animation index 19. This is a separate input path from the retry handler and should be modeled separately until the complete global state transition graph is documented.

## 6. Native Android architecture requirements

- Separate damage evaluation, knockback/relocation, death state, retry accounting, and checkpoint restore into explicit gameplay systems.
- Preserve invulnerability/duplicate-hit gates and their timers; do not apply repeated damage every render frame.
- Preserve the distinction between the durable player save and the in-memory checkpoint snapshot.
- Implement checkpoint snapshots as bounded, versioned native data structures in memory; if the Native game later chooses durable checkpoint persistence, treat that as an intentional product behavior change rather than assuming the source does it.
- Restore all gameplay grids and mode-specific state atomically; avoid partial world rollback.
- Rebuild derived caches/dirty regions after restoring grids.
- Keep sound and animation requests as side effects of gameplay state transitions, not drawing code.
- Keep the retry-count semantics unresolved until UI strings and all writers/readers are traced.
- Add deterministic tests for damage gating, knockback direction search, checkpoint capture/restore, retry underflow, and stage-grid restoration.

## 7. Open work

- Trace all writes and reads of `nByte`, `aTInt`, `azInt`, and `bdInt` before assigning semantic names.
- Trace the full global state transition after `i.bByte = 12` and the UI flow when retry count is exhausted.
- Trace the full death animation and terminal state transitions for animation indices 19, 40, 47, and 48.
- Verify all damage sources and parameter meanings.
- Confirm exactly when `shouldSaveCheckpoint` is set and what gameplay event it represents.
- Test checkpoint restoration against an original runtime, including optional mode state and moving multi-cell entities.
- Confirm whether any other code serializes checkpoint state; the methods inspected here do not.
- Resolve source/assets provenance before redistributing any candidate material.

## 8. Audit status

**Completed at static-source level:** damage handler gating and state changes; examples of damage parameter families; in-memory checkpoint snapshot contents; checkpoint activation and restore behavior; retry-count branch behavior.

**Not completed:** complete damage argument semantics, full death/global-state transition graph, UI meaning of retry count, all checkpoint trigger paths, synthetic tests, runtime validation, and licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
