# DIRU-NA-DEV-003D — Movement Caller and Collision Integration Matrix

**Status:** Static caller/helper matrix committed; no movement engine implementation  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source:** `src/main/java/i.java`

## 1. Purpose

Trace the normal movement caller around `aBoolean(dx, dy, probe)` and document how its return value, state flags, coordinates, timers, and animation calls are consumed. This document is a static-source contract only. It does not implement a movement state machine or a collision simulator.

## 2. Caller gate and path selection

The relevant caller block is approximately lines 10633–10867.

| Path | Entry condition | Behavior |
|---|---|---|
| Special current-cell path | `aTInt > 0` | Inspect current player-layer cell and cells below/diagonal-below; may advance logical Y (and X) when a player-layer value 9 with direction metadata 3 is found; set packed direction to 3; obtain a frame/state value from `bByteArrArr`. Does not call ordinary collision helper in this branch. |
| Ordinary direction/update path | `(lByte == 0 || (jInt <= 0 && lByte != 5)) && !xBoolean && (kInt & 0x70) == 0 && aFInt == -1` | Copy requested `aByte` into active `lByte`, apply door/forced-direction logic, then dispatch on `lByte`. |
| Scripted displacement/settling path | ordinary gate false | If `xBoolean && jInt <= 0`, select `pByte` or `qByte` from `atBoolean`, derive direction and commit scripted tile delta, set `jInt=18`, request animation; then process packed-state gate and `jInt -= 6` settling. |

Do not combine these paths under one generic `move(direction)` method without preserving their distinct preconditions and state writes.

## 3. Pre-switch direction overrides

Within the ordinary path:

1. `lByte = aByte`.
2. If `ayInt > 0`, override active direction to 2 and decrement `ayInt`. When it reaches zero, close the door head, clear `aByte`, and clear the global held-key mask.
3. Otherwise, if the caller's boolean parameter `b` is true, call `kVoid(crtPlayerX, crtPlayerY)`.
4. Dispatch the resulting active direction.

The meaning of caller parameter `b` is not renamed here; its use is tracked as an opaque source parameter.

## 4. Ordinary caller switch matrix

| Active `lByte` | Caller behavior | Collision helper? | Follow-up |
|---:|---|---|---|
| 0 | Clear packed bit `0x8`; select idle animation based on current hero animation and packed bit `0x4000`; set `aXInt` to 6 or 0 depending on `aClassc`; decrement positive `jInt` by 6 | No | Continue to hazard and periodic idle checks |
| 1 | Ordinary direction branch | Yes, unless animation is 40/48 or packed bit `0x1000` is set | On helper return true, set `lInt=40`, `cBoolean=false`, choose animation from packed bit and direction |
| 2 | Ordinary direction branch | Same as direction 1 | If packed bit `0x8` is set, animation 8; otherwise direction-specific animation path |
| 3 | Clear packed bit `0x8`, then fall through to ordinary direction branch | Same as direction 1 | Same caller handling as 1/2/4 |
| 4 | Ordinary direction branch | Same as direction 1 | If packed bit `0x8` set, animation 9; otherwise direction-specific animation path |
| 5 | Set `jInt=0`; if in bounds request animation `(kInt & 7) + 12` | No | Special state, not ordinary interpolation |
| 6 | Determine horizontal side from packed direction; if caller bounds flag is true and adjacent player-layer cell is empty, create/push player-layer type 32 with metadata, set `jInt=72`, clear requested `aByte`, request animation 20 or 22 | No | Separate push/object path |

For directions 1–4:
- Hero animation IDs 40 and 48 stop the direction branch before collision evaluation.
- If `kInt & 0x1000` is set, write active direction into packed state, set `jInt=18`, and request animation `lByte-1` (clamped at zero). This branch does not call the collision helper.
- Otherwise call `aBoolean(-gByteArr[lByte], -gByteArr[lByte+8], false)`.
- Recompute the caller bounds flag using `isPlayerWithinStageBounds()` after the helper call.
- If helper return is false, skip the caller's subsequent `lInt`/animation setup.
- If helper return is true, set `lInt=40`, `cBoolean=false`; if packed bit `0x8` is clear, request animation `(lByte-1 clamped at 0)+4`; if bit `0x8` is set, request animation 8 for direction 2 and 9 for the other directions.

**Critical distinction:** caller-local `aBoolean` is the raw helper return flag `b2`. It is not a semantic `canMove` result. The caller uses it to select a follow-up state/animation path, while the helper may also alter local movement fields and special state.

## 5. Collision helper outcome matrix

The helper is `aBoolean(int dx, int dy, boolean probe)`, approximately lines 12073–12443.

| Helper branch | Raw flags/effects | Caller implication |
|---|---|---|
| Destination outside stage bounds | `b2=true`; skips foreground/player-layer evaluation | Caller may still take the helper-true animation path; final helper commit block is independently gated by `b2 && aWInt==0` |
| Foreground 1 | Non-probe timer/counter and helper call; exits foreground switch block with n3 still 0, so player-layer switch follows | Do not collapse into ordinary floor |
| Foreground 2 | Non-probe `b3=true`; item/metadata-dependent `aDInt/aEInt` | `b3` affects the helper's `aDInt` reset condition; it is not the return flag |
| Foreground 3 | Metadata threshold can set `b2`; alternate path sets local `n10` | Requires metadata-aware fixture |
| Foreground 4 | Non-probe checkpoint index/save-state update; player-layer switch follows | Checkpoint side effect is distinct from tile passability |
| Foreground 5 | `b2=true`; non-probe enables `xBoolean` and `atBoolean=true` | Forced/scripted displacement path |
| Foreground 7 | Metadata nibble controls local `n10` | May continue into player-layer logic |
| Foreground 19 | Non-probe scripted path can mutate map/player state; probe exits before this scripted block | Probe fixture must not claim this branch mutates the map |
| Foreground 28 | Non-probe enables `xBoolean`, sets `atBoolean=false` | Distinct forced-displacement path |
| Player layer 34/35 | `b2=probe` | Probe and non-probe return differ |
| Player layer 31/49 | `b2=false` | Explicit special values |
| Blocking-value set | `b2=true` | Keep exact source values as a table, not a guessed generic wall category |
| Player layer 10 | Conditional on `xByte==3`; may mutate metadata and set `b2` | State/metadata-dependent |
| Player layer 48 | Two-cell geometry; non-probe map movement and metadata writes; `b2=true` on accepted branch | Dedicated object/push behavior |
| Player layer 0/8/9/47 | Two-tile lookahead, edge/door checks, push checks; may clear local active direction and packed moving bit | Includes special `kByte==4` early return after animation request |
| Player layer -1 | Stage-edge and nearby door/obstacle conditions; clears packed moving bit | Empty cell is not unconditionally passable |
| Player layer 28 | Clears active direction and packed moving bit | Distinct stop condition |
| Default player-layer value | Sets local delta to zero and `b2=false`; may request direction animation | Return false can still accompany an animation request |

## 6. Helper commit and caller post-processing

At helper exit:
- If `!probe && b2 && !b3`, reset `aDInt=-1`.
- If `b2 && aWInt==0`, set local `jInt=18` and apply the requested delta to local tile coordinates.
- If a nonzero delta was not committed and this is non-probe, set packed bit `0x8`.
- If packed bit `0x8` is set in non-probe mode, request animation 8 or 9 based on packed direction.
- Write the local snapshot back only when `probe==false`.

After the direction switch, the caller may:
- run hazard checks when its bounds flag is true;
- if helper return flag is true, clear `yBoolean` and leave the current caller block;
- otherwise require bounds and a periodic stage tick before running four directional probe calls using the positive raw `gByteArr` vector values;
- use those probe results plus the player-layer cell above to control an idle/turn animation path (including animation 19).

Therefore helper probes are part of caller behavior and must not be mistaken for ordinary move attempts.

## 7. Implementation boundary

No collision algorithm, player state machine, stage grid, object-push engine, or Android movement integration is implemented in this milestone. The next implementation must consume a typed trace/outcome that retains:
- raw helper `b2` and `b3`;
- probe mode and whether player snapshot was committed;
- branch identity and metadata inputs;
- logical coordinate delta and whether it was persisted;
- animation/state effects;
- world/object mutations;
- caller follow-up branch.

Do not reduce the helper to a boolean interface.

## 8. Regression requirements

Before the first native collision simulator is accepted, tests must cover:
1. Every caller direction 1–4 with ordinary collision path.
2. Animation IDs 40/48 bypassing helper invocation.
3. Packed `0x1000` path bypassing helper and initializing progress.
4. Caller helper-true and helper-false follow-up behavior.
5. Bounds flag recomputation after helper.
6. Direction 3 packed-bit clear before fall-through.
7. Direction 5 and 6 distinct from ordinary movement.
8. `ayInt` door override and key-mask clearing at zero.
9. Probe calls using positive raw vectors, not the negated ordinary movement vectors.
10. Foreground 2 `b3` effects and `aDInt` reset suppression.
11. Probe early animation request for `kByte==4`.
12. Scripted `xBoolean` movement and its separate progress lifecycle.
13. Hazard and idle/turn checks after the direction switch.

## 9. Next work item

**DEV-003E — Collision Input Snapshot and Typed Branch Observation.** Define immutable synthetic grid-cell fixtures and a source-oriented input snapshot for one helper call. Keep map lookup, metadata, probe mode, caller flags, and writeback effects explicit. Only then build a pure-core collision evaluator.


### 10.1 Foreground labeled-exit correction (2026-11-27)

The source's `break Label_0738` exits the entire foreground block, bypassing the trailing `n3 = n10` assignment. Since `n3` was initialized to zero, the player-layer switch still executes. This applies to foreground types 1, 2, 4, 5, 6 and 28 (and the low-metadata type-3 path). Do not model these labeled exits as `SKIP_PLAYER_LAYER`.

By contrast, type 7 with nibble < 2 and type 3 with signed high byte >= 3 set `n10=1`, reach `n3=n10`, and skip player-layer evaluation. The high-metadata type-3 path also sets `b2=false`.

This control-flow distinction is now reflected in the type-156, type-2 and type-428 pure evaluator flow outputs and their unit-test expectations.
