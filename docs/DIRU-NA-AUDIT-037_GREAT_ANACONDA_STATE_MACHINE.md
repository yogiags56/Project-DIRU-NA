# DIRU-NA-AUDIT-037 — Great Anaconda Complete State-Machine Reconstruction

## Scope and source pin

Static source audit of `src/main/java/i.java` from `kubikaugustyn/DiamondRushSource`, pinned to commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This document focuses on Great Anaconda (`kByte == 4`), its `aoInt` states, main animator slot 5, secondary animator slot 4, stage-grid helper methods, and phase counters. It is not a runtime-verified behavior specification.

## 1. Encounter initialization

The stage setup branch initializes Great Anaconda with:
- `kByte = 4`
- `aoInt = 0`
- `aqInt = 3`
- `arInt = 0`
- `apInt = 0`
- `kBoolean = false`

The main animator is loaded from `/b0.f` chunk 0 into slot 5. The resource loader also loads texture slot 32 from `/mmv.f` chunk 3 and sprite resource slot 20 from `/gen0.f` chunk 7.

Slot 4 is obtained from a separate generic sprite-loading branch that constructs an animator from a loaded `sprite` and selects animation 0. The inspected Great Anaconda loader branch does not itself show a dedicated `/b0.f` resource being assigned to slot 4. Therefore the exact semantic identity of slot 4 remains unresolved.

## 2. State transition overview

The encounter update increments `apInt` once per update before processing the current state. Main animator selection is emitted through local `n21`; secondary animator selection through `n22`.

| State | Entry / active behavior | Exit condition | Next state | Main animation |
|---:|---|---|---:|---:|
| 0 | Wait for player X >= 10 | Player reaches threshold | 6 | unchanged |
| 6 | Wait for `apInt > 10` | Delay elapsed | 1 | 2 |
| 1 | At >20, place stage markers at `(dInt,8)` and `(dInt+1,8)`; at >40 advance | `apInt > 40` | 2 | unchanged |
| 2 | Test interaction with marker cells; may trigger interaction animation 3; timeout/alternate animation logic | successful interaction | 3 | 3 |
| 2 | If `apInt > 15` and main animation != 6 | intermediate animation selection | 2 | 6 |
| 2 | If `apInt > 30` | timeout/reset markers | 4 | 0 |
| 3 | Wait until `apInt > 40`; choose next branch from counter and saved state | `aqInt <= 0` | 7 | unchanged |
| 3 | Saved `SInt == 10` | phase selection | 9 | unchanged |
| 3 | Saved `SInt == 1 || SInt == 2` | phase selection | 4 | 2 |
| 4 | Short phase; duration 5 if `aqInt <= 1`, otherwise 10; run `eBoolean()` and `abVoid()` | `apInt >= duration` | 5 | 4 |
| 4 | Mid-phase and current animation != 1 | phase animation | 4 | 1 |
| 5 | Wait for main animator's frame-derived vertical condition; then reset/clear arena markers and enter falling phase | vertical threshold reached | 10 | unchanged |
| 7 | Clear marker cells; periodic sound; wait | `apInt > 80` | 8 | unchanged |
| 8 | No explicit case in this switch | No explicit transition in inspected switch | unresolved | animator tick path |
| 9 | Decrement `apInt` by 2; collision/marker processing | frame-derived vertical threshold reached | 6 | unchanged |
| 10 | Decrement `apInt`; run collision helper; increment `LInt` | `LInt == 28` | 10 | 7 |
| 10 | Phase threshold | `LInt >= 50` | 11 | 8 |
| 11 | Decrement `apInt`; increment `LInt`; one-shot player hit test | `LInt >= 12` | 9 | 4 |

The switch has no explicit `case 8` in the inspected Great Anaconda update block. State 8 is entered from state 7 after `apInt > 80`, where `alVoid()` is invoked. Do not invent an exit transition for state 8; inspect the broader encounter lifecycle and stage-completion code before implementing it.

## 3. State details and stage-grid helpers

### 3.1 Marker-cell operations

The encounter repeatedly operates on a two-column marker at row 8:
- `bBoolean(dInt, 8)` checks two columns (`dInt` and `dInt+1`) and rows 8 and 7 for stage-player-layer cells equal to 0.
- When found, it invokes `dVoid`, `kVoid`, clears the cell to -1, invokes `bVoid`, and returns true if at least one cell was processed.
- `hVoid(x,y)` writes value 50 to two adjacent stage-player-layer cells.
- `gVoid(x,y)` clears two adjacent cells to -1, but does nothing if either coordinate is negative.
- `eBoolean()` computes a vertical tile coordinate from the current main animator frame's bounding-box offset, then scans two columns from that row through row 10, removing cells equal to 0 using the same helper sequence.
- `acVoid()` sets `bjInt = 30` and conditionally restores cells at the stored encounter coordinates.

These helper names are obfuscated. The behavior above is the proven contract; do not rename them as "spawn", "destroy", or "damage" without further call-site evidence.

### 3.2 dInt and arInt

`dInt()` returns:
`10 + arInt * ((arInt > 0 ? 1 : 0) + 2)`

Therefore:
- if `arInt <= 0`, the result is `10 + 2*arInt`;
- if `arInt > 0`, the result is `10 + 3*arInt`.

In state 9, `arInt` is recalculated from `crtPlayerX - 10`, divided by 3, with a small time-based adjustment when the remainder is 2. This affects the two-column marker target in later states. Preserve integer division and the conditional adjustment exactly.

### 3.3 State 1 to 2 and interaction

State 1 starts marker placement after `apInt > 20`, and transitions to state 2 after `apInt > 40`. State 2 calls `bBoolean(dInt,8)`; when it returns true:
- `aqInt` decrements;
- `SInt` captures the current state value;
- marker cells at `MInt,NInt` are cleared;
- `aoInt` becomes 3;
- main animation 3 is selected;
- sound 10 plays.

The source contains a second `SInt = aoInt` assignment immediately before `aoInt = 3`; preserve the order exactly. Since this assignment occurs while state 2 is active, the saved value is 2 in this branch.

### 3.4 State 3 follow-up

After `apInt > 40`:
1. If `aqInt <= 0`, enter state 7 and reset `apInt`.
2. Else if `SInt == 10`, enter state 9 and reset `apInt`.
3. Else if `SInt == 1 || SInt == 2`, enter state 4, reset `apInt`, and select animation 2.
4. Otherwise remain in state 3.

Because the normal state-2 interaction captures `SInt = 2`, the third branch is the normal follow-up for that path.

## 4. States 4, 5, 9, 10, and 11

### State 4
Duration is selected from the remaining encounter counter:
- 5 updates when `aqInt <= 1`;
- otherwise 10 updates.

When `apInt` exceeds half the duration and current animation is not 1, animation 1 is selected. At the duration boundary, state 5 is entered and animation 4 is selected. Both `eBoolean()` and `abVoid()` run while state 4 is active.

### State 5
The transition condition reads the current main animator frame's fourth metadata byte (the frame Y/bounds-related field used elsewhere in the source) and compares it to the stage/player vertical offset. When the condition is met:
- `TInt = 0`;
- `acVoid()` runs;
- state becomes 10;
- `MInt = dInt`, `NInt = 4`;
- `hVoid(MInt,NInt)` writes the two marker cells.

`eBoolean()` and `abVoid()` also run in state 5.

There is an entry guard at the top of the Great Anaconda branch: when `aoInt == 5`, if `ZBoolean` is already true the update returns; otherwise it sets `ZBoolean = true`. This one-time gate must be preserved. The full lifecycle/reset of `ZBoolean` still needs tracing.

### State 9
Decrements `apInt` by 2, calls `abVoid()`, and waits for the animator's frame-derived vertical threshold. When reached, it returns to state 6, resets `apInt`, and recalculates `arInt` from the player's X coordinate.

### State 10
Decrements `apInt`, calls `abVoid()`, increments `LInt`.
- At exactly 28, selects main animation 7.
- At 50 or later, resets `LInt`, enters state 11, clears marker cells, selects main animation 8 and secondary animation 2, and enables `kBoolean`.

### State 11
Decrements `apInt`, increments `LInt`.
- At 12 or later, resets `LInt`, returns to state 9, selects main animation 4, and clears `jBoolean` and `kBoolean`.
- Before that threshold, if `jBoolean` is false and the player is at row 4 within the calculated X interval, it damages the player and sets `jBoolean = true`.

## 5. Collision and damage helper

`abVoid()`:
- calculates `dInt()`;
- only checks player columns `dInt` or `dInt+1`;
- obtains the main animator's vertical extent from its current Y and frame metadata;
- compares the player animator's Y against that interval;
- calls `hurtHero(1,48,4)` for the first column or `hurtHero(1,48,2)` for the second, if `jBoolean` is false.

Note: the state-11 damage branch uses a separate direct player-grid condition and sets `jBoolean` true after a hit. The `abVoid()` helper itself does not set `jBoolean`; do not conflate the two damage paths.

## 6. Slot 4 — important unresolved lifecycle

Evidence:
- A generic loader branch creates `aClassbArr[4]` from a local `sprite` and selects animation 0.
- A shared update path ticks slot 4 when non-null.
- Great Anaconda state 11 enables `kBoolean`, then its encounter branch can select animation 2 on slot 4 and tick it while enabled.
- A rendering helper draws slot 4 at player-relative coordinates and flips its horizontal flag for one tile type.
- Other code reads slot 4's animation frame to derive a small state value.

Therefore slot 4 is not proven to be a dedicated Great Anaconda resource. It appears to be a shared animator whose identity depends on the active loaded sprite. The encounter uses it as a secondary channel when `kBoolean` is true, but its actual visual identity and lifecycle must be resolved from the loader's local `sprite` provenance and all call sites.

Potential update-order hazard:
- A shared update path ticks slot 4 when non-null.
- The Great Anaconda encounter branch also ticks slot 4 when `kBoolean` is true and `n22 == -1`.
- If both paths execute in the same frame, slot 4 may receive two ticks. The exact call order and conditions must be tested before porting. Do not silently remove either tick.

## 7. Implementation requirements

1. Preserve all raw state numbers and the missing explicit state-8 case.
2. Preserve exact update ordering: increment `apInt`, process state, set/tick main animator, then conditionally set/tick secondary animator.
3. Preserve two-column stage-grid operations and exact marker values (0, 50, -1).
4. Preserve integer division and time-based `arInt` adjustment.
5. Preserve state-2 counter decrement and `SInt` capture order.
6. Preserve state-4 duration selection (5/10), state-10 `LInt` threshold 28/50, and state-11 threshold 12.
7. Preserve `ZBoolean` one-time guard until its reset lifecycle is known.
8. Do not assume slot 4 belongs exclusively to Great Anaconda.
9. Investigate possible double ticking of slot 4.
10. Do not rename `aqInt` as HP or assign semantic animation names without further evidence.

## 8. Remaining work

- Trace every write/reset of `ZBoolean`, `MInt`, `NInt`, `TInt`, and `arInt`.
- Resolve the local `sprite` source for slot 4 and all loader paths that can populate it.
- Verify whether slot 4 is ticked twice in the actual frame order.
- Resolve state 8's lifecycle through the shared stage-completion and encounter flow.
- Trace `alVoid()` and `jVoid(11,11)` side effects for completion.
- Establish the original game's tick frequency and deterministic update ordering.
- Build tests for state transitions, counter boundaries, marker-cell updates, and checkpoint restoration.
- Validate against a legally usable original JAR/runtime.

## 9. Audit status

Completed at static-source level:
- Full explicit Great Anaconda switch cases 0–7 and 9–11 mapped.
- State-2 interaction and `aqInt` decrement path identified.
- Marker-cell helper behavior documented.
- State-4 duration and state-10/11 phase thresholds documented.
- Slot-4 shared-resource ambiguity and possible double-tick hazard identified.

Open:
- State 8 lifecycle.
- Slot-4 sprite provenance and frame-order verification.
- `ZBoolean` reset lifecycle.
- Remaining field write/read traces.
- Runtime validation.

No Android build, APK, or runtime test was performed.
