# DIRU-NA-AUDIT-040 — Great Anaconda Native Behavior Specification & Regression Test Design

## 1. Purpose and evidence boundary

This document converts the Great Anaconda findings from AUDIT-033 through AUDIT-039 into a Native Android implementation contract and a deterministic regression-test plan.

It is a design/specification artifact, not an implementation report. It does not claim that the boss, renderer, resource loader, or tests already exist in DIRU-NA.

Primary source input: `src/main/java/i.java` from `kubikaugustyn/DiamondRushSource`, pinned to `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

Relevant audit documents:
- AUDIT-033: sprite animation tables and frame metadata.
- AUDIT-034: animation selection call-site trace.
- AUDIT-035: encounter animation state mapping.
- AUDIT-036: encounter counters and timer lifecycle.
- AUDIT-037: Great Anaconda state machine.
- AUDIT-038: resource/update order, render-coupled gate, and slot-4 double tick.
- AUDIT-039: slot-4 resource provenance and completion lifecycle.

All statements below are static-source findings unless explicitly marked as a Native design recommendation. Runtime cadence and visual equivalence remain unverified.

## 2. Native module boundary

Recommended platform-independent component:

`core/encounter/greatanaconda/`

Suggested responsibilities:

- `GreatAnacondaEncounter`: encounter state, counters, transitions, and ordered update decisions.
- `GreatAnacondaStagePort`: narrow interface for reading/writing stage cells, player position, collision/damage, and sound events.
- `GreatAnacondaAnimatorPort`: access to main and secondary animator operations without binding core logic to Android graphics.
- `GreatAnacondaResourceBinding`: declarative mapping of stage resource IDs to animator slots.
- `GreatAnacondaSnapshot`: checkpoint/restoration state required by the encounter.
- `GreatAnacondaTrace`: optional deterministic event log for tests and source-comparison diagnostics.

Do not put Android Canvas, SurfaceView, bitmap decoding, audio APIs, or touch handling inside the encounter state machine. The encounter should operate in the 240×320 logical gameplay coordinate system and should not know the physical device aspect ratio.

Use descriptive Native names rather than exposing decompiler field names as public API. Keep an evidence mapping table in code comments or documentation so every compatibility field remains traceable.

## 3. State and field contract

### 3.1 State identifiers

| State | Required behavior |
|---:|---|
| 0 | Wait until player X >= 10; then state 6, reset apInt |
| 1 | After apInt > 20, place row-8 two-cell marker; after apInt > 40, enter state 2 |
| 2 | Process marker interaction; success decrements aqInt and enters state 3; timeout after apInt > 30 enters state 4 |
| 3 | After apInt > 40, branch in source order: aqInt <= 0 -> 7; else SInt == 10 -> 9; else SInt == 1 or 2 -> 4 |
| 4 | Run phase helpers; duration is 5 when aqInt <= 1, otherwise 10; at boundary enter state 5 |
| 5 | Render-coupled ZBoolean gate; wait for the frame-derived vertical predicate; then clear/restore and enter state 10 |
| 6 | Wait until apInt > 10; then enter state 1, select main animation 2, reset apInt |
| 7 | Clear marker; wait until apInt > 80; then enter state 8 and call shared completion routine once |
| 8 | No explicit case/exit established; retain state and common animator behavior |
| 9 | Decrement apInt by 2; wait for frame-derived vertical predicate; then state 6 and recalculate arInt |
| 10 | Decrement apInt, run collision helper, increment LInt; animation 7 at LInt == 28; at LInt >= 50 enter state 11 |
| 11 | Decrement apInt, increment LInt; at LInt >= 12 enter state 9; before that, one guarded player-hit test |

### 3.2 Encounter fields

| Source field | Native semantic name (suggested) | Contract |
|---|---|---|
| aoInt | state | Current encounter state identifier |
| aqInt | remainingInteractions | Starts at 3; decremented by successful state-2 marker interaction; not proven HP |
| apInt | stateCounter | Incremented before state dispatch; also decremented in states 9, 10, 11 as source specifies |
| arInt | markerColumnOffset | Feeds dInt() and is recalculated in state 9 |
| SInt | previousInteractionState | Captures current state before state 2 enters state 3 |
| LInt | phaseCounter | Drives state 10/11 animation phases |
| MInt, NInt | markerX, markerY | Two-cell marker coordinates; default -1/-1 |
| jBoolean | hitGate | Prevents repeated state-11 hit during one active window; cleared at state 11 exit |
| kBoolean | secondaryAnimatorEnabled | Controls slot-4 render/encounter-side update; false at init, true on 10->11, false on 11->9 |
| ZBoolean | state5RenderUpdateGate | Update/render coupled gate; set by state-5 update and reset in state-5 render path |
| TInt | unresolvedFieldT | Set to 0 at state-5 transition; no proven semantic role |

Important: do not map `aqInt` to boss health or a health bar. The source evidence only supports a remaining-interaction/completion counter.

### 3.3 Coordinate helper

Preserve exact integer arithmetic:

`dInt = 10 + arInt * (arInt > 0 ? 3 : 2)`

Do not replace with floating-point math or a simplified formula that changes behavior for negative/zero values.

## 4. Resource binding contract

| Slot | Resource | Animation | Role |
|---:|---|---:|---|
| 4 | `/gen1.f`, chunk 0 | 0 initially | Shared stage animator; secondary Great Anaconda channel while kBoolean is true |
| 5 | `/b0.f`, chunk 0 | Constructor/default then state-selected | Main Great Anaconda animator |
| Texture 32 | `/mmv.f`, chunk 3 | n/a | Boss rendering texture |
| Sprite slot 20 | `/gen0.f`, chunk 7 | n/a | Shared sprite resource used by boss render path |

The Great Anaconda stage resource mask enables generated chunk index 10 (bit `0x400`), which maps to `/gen1.f` local chunk 0. Slot 4 must be created by the generic resource loader, not by an invented boss-specific loader.

These paths identify source resource provenance. They do not grant permission to redistribute proprietary assets. Native runtime resource packaging remains subject to licensing review.

## 5. Ordered update contract

The Native implementation must model the source's observable ordering rather than collapsing all animator updates into one generic animation tick.

### 5.1 Per-game-update sequence

1. Run the shared stage update section.
2. If slot 4 exists, execute the shared slot-4 tick.
3. Run remaining common player/stage work in the correct engine order.
4. Dispatch the Great Anaconda encounter branch when the encounter selector is 4.
5. If state is 5, apply the ZBoolean gate:
   - if already true, return from this encounter branch;
   - otherwise set it true and continue.
6. Increment apInt as the source does before evaluating the current state.
7. Execute exactly one state branch and preserve source statement order, including marker writes, counter mutations, helper calls, and state changes.
8. Apply main slot-5 animator action: tick if no animation index was selected; otherwise set the selected animation.
9. If kBoolean is true, apply secondary slot-4 action:
   - tick if no secondary animation index was selected;
   - otherwise set the selected animation.

### 5.2 Slot-4 double-tick compatibility

The shared tick and encounter-side tick are separate source call sites. In the active secondary interval, both may execute during one game update.

Required regression expectations:
- When slot 4 exists and shared update runs, shared tick count increases by one.
- When kBoolean is false, Great Anaconda adds no encounter-side slot-4 tick.
- When kBoolean is true and n22 is unset, encounter-side slot-4 tick adds one more.
- When kBoolean is true and n22 is set to 2 on the 10->11 transition, the encounter action is setAnimation(2), not a second tick on that branch.
- Do not deduplicate the two tick call sites.

A future runtime comparison may revise this compatibility rule, but not this static-source baseline.

### 5.3 Render/update coupling

State 5's ZBoolean reset occurs in the render path, not in a generic state-entry/exit callback. Keep this as a distinct render-side event until original update/render cadence is measured. Tests should model update and render as separate operations.

## 6. State transition and side-effect contract

### State 0 / 6 / 1
- State 0 checks player X >= 10.
- State 6 waits for apInt > 10, then selects main animation 2 and enters state 1.
- State 1 places a two-cell marker at (dInt, 8) after apInt > 20.
- State 1 enters state 2 after apInt > 40.
- The marker pair is represented by MInt/NInt and stage-player-layer values written by hVoid.

### State 2 / 3
On successful bBoolean(dInt,8):
1. Decrement aqInt.
2. Save SInt from the current state value (2 in this branch).
3. Clear the stored marker through gVoid.
4. Reset MInt/NInt to -1.
5. Set state 3.
6. Select main animation 3.
7. Play sound 10.

If state 2 reaches apInt > 30, enter state 4, reset apInt, select main animation 0, and clear/reset marker.

State 3 waits until apInt > 40, then evaluates branches in this exact precedence:
1. aqInt <= 0 -> state 7, reset apInt.
2. else SInt == 10 -> state 9, reset apInt.
3. else SInt == 1 or 2 -> state 4, reset apInt, select main animation 2.
4. otherwise remain in state 3.

### State 4 / 5
- State 4 phase duration is 5 if aqInt <= 1, otherwise 10.
- Preserve the intermediate animation-1 selection condition and source comparison against current animation.
- While state 4 is active, call eBoolean() and abVoid() in source order.
- At duration boundary, enter state 5 and select main animation 4.
- State 5 retains the ZBoolean render gate.
- When the frame-derived vertical condition is met, set TInt=0, call acVoid(), enter state 10, store marker coordinates (dInt,4), and place the two-cell marker using hVoid.
- The exact frame-derived predicate must be ported from the source expression, not approximated from sprite height.

### State 7 / 8
- State 7 clears MInt/NInt marker cells and resets coordinates.
- Wait for apInt > 80.
- On that threshold, set state 8 and call alVoid() once, in that order.
- State 8 has no explicit encounter case or proven exit. Do not invent a completion animation or transition.
- Common main animator behavior continues according to the source update path.

### State 9 / 10 / 11
- State 9 decrements apInt by 2, calls abVoid(), and waits for the frame-derived vertical condition. On success, enter state 6, reset apInt, and recalculate arInt from player position using the source's integer/time adjustment.
- State 10 decrements apInt, calls abVoid(), increments LInt. At LInt == 28, select main animation 7. At LInt >= 50, reset LInt, enter state 11, clear marker cells, select main animation 8 and secondary animation 2, and set kBoolean true.
- State 11 decrements apInt and increments LInt. At LInt >= 12, reset LInt, enter state 9, select main animation 4, clear jBoolean and kBoolean. Otherwise, if jBoolean is false and the source's row-4/X-range condition is met, apply the source damage call and set jBoolean true.

## 7. Stage-grid and shared callback contract

Keep stage-grid operations behind a narrow port so they can be tested without Android rendering.

- `hVoid(x,y)`: writes marker value 50 into two adjacent cells.
- `gVoid(x,y)`: if either coordinate is negative, does nothing; otherwise clears two adjacent cells to -1.
- `bBoolean(x,8)`: interaction scan over two columns and rows 8/7; preserve its helper side effects and return semantics.
- `eBoolean()`: scan region derived from the main animator frame metadata through row 10; preserve its helper side effects.
- `acVoid()`: sets bjInt=30 and performs its conditional cell restoration.
- `abVoid()`: performs the frame-derived player collision check and invokes hurtHero with the source's distinct direction/offset arguments.
- `alVoid()`: special encounter gate: return for kByte 3/4/5 while aqInt > 0; otherwise, if cmInt >= 0, decrement shared counter, play sound 8 at zero, and perform the source's foreground-cell scan/metadata updates.

Do not label aqInt as health. Do not reinterpret alVoid as a boss-death callback. Keep the two overloaded source methods `jVoid(int,int)` and `jVoid()` separate in Native naming and responsibility.

## 8. Deterministic regression test plan

Use a fake stage grid, fake player, fake sound sink, and fake animator ports. Tests must operate on logical grid/frame data, not screenshots or Android device timing.

| ID | Scenario | Setup / action | Expected result |
|---|---|---|---|
| GA-001 | Initial state | Construct encounter | state=0, aqInt=3, arInt=0, apInt=0, marker=-1/-1, kBoolean=false |
| GA-002 | State-0 threshold | Player X below then equal to 10 | Remains 0 below; enters 6 at X>=10 |
| GA-003 | State-6 delay | Advance apInt through 10 then 11 | Remains 6 at 10; enters 1 and selects anim 2 above 10 |
| GA-004 | State-1 marker timing | Advance state 1 through apInt 20, 21, 40, 41 | No marker at 20; marker at 21; enters state 2 only after 40 |
| GA-005 | Marker footprint | Place marker at (x,8) | Exactly cells (x,8) and (x+1,8) receive 50 |
| GA-006 | Successful interaction | Make bBoolean return true in state 2 | aqInt decrements once; marker clears; state 3; anim 3; sound 10 |
| GA-007 | Interaction timeout | No interaction; advance beyond apInt 30 | State 4; apInt reset; anim 0; marker cleared |
| GA-008 | State-3 precedence | Set aqInt<=0 and SInt==2 at threshold | State 7 wins; must not take state-4 branch |
| GA-009 | State-3 saved-state branch | aqInt>0, SInt==10 at threshold | State 9 |
| GA-010 | State-3 normal branch | aqInt>0, SInt==2 at threshold | State 4; anim 2 |
| GA-011 | State-4 duration | aqInt<=1 and aqInt>1 variants | Duration boundaries 5 and 10 respectively |
| GA-012 | State-5 render gate | Update state 5 twice without render between | Second encounter update returns at gate; render resets gate; next update may proceed |
| GA-013 | State-5 frame threshold | Feed frame metadata just before/at source predicate | No transition before; state 10 and marker row 4 at predicate |
| GA-014 | State-7 completion wait | apInt=80 then 81 | No transition at 80; state 8 and exactly one alVoid call after 80 |
| GA-015 | State-8 persistence | Repeated updates while state 8 | No invented encounter transition; main animator follows common tick path |
| GA-016 | State-9 counter | apInt value and state 9 update | Decrement by two before predicate evaluation as source specifies |
| GA-017 | State-10 animation marker | LInt=27 then update | Animation 7 selected at exactly 28 |
| GA-018 | State-10 to 11 | LInt=49 then update | State 11, main anim 8, slot4 anim 2, kBoolean=true, marker cleared |
| GA-019 | Slot-4 double tick | kBoolean=true, n22 unset | One shared slot-4 tick plus one encounter-side tick in same update |
| GA-020 | Slot-4 transition selection | Trigger 10->11 | Shared tick occurs; encounter side selects animation 2, not extra tick |
| GA-021 | State-11 hit gate | Player inside hit condition for consecutive updates | Damage applied once while jBoolean remains true |
| GA-022 | State-11 exit | LInt reaches 12 | State 9; main anim 4; jBoolean=false; kBoolean=false |
| GA-023 | alVoid gate | kByte=4, aqInt>0 | Immediate return; shared counter untouched |
| GA-024 | alVoid counter | kByte=4, aqInt=0, cmInt>=0 | Decrement selected shared counter; sound 8 only when it reaches zero |
| GA-025 | alVoid no counter | cmInt<0 | No counter/cell side effects |
| GA-026 | Checkpoint restore | Restore Great Anaconda checkpoint | Encounter baseline and kBoolean reset match source |
| GA-027 | dInt integer mapping | Test arInt negative, zero, positive | Exact piecewise integer results; no float rounding |
| GA-028 | Marker clear guard | gVoid with either coordinate negative | No grid writes |

### Test implementation notes

- Use explicit fake animator counters to distinguish shared tick from encounter-side tick.
- Capture ordered event traces, not just final field values. For example, the 10->11 trace must show shared slot-4 tick before encounter state action and `setAnimation(2)`.
- Test update and render separately for ZBoolean.
- Use source-extracted frame metadata fixtures only when licensing/provenance permits. Until then, use synthetic metadata fixtures that exercise the same predicate shape.
- Preserve integer overflow/byte semantics only where source types require them; do not silently widen or clamp.
- Every expected behavior should cite the relevant audit/source location in the eventual test file.

## 9. Implementation acceptance criteria

The Great Anaconda Native module is not considered complete until:

1. All state branches and exact source-order side effects are represented.
2. Resource slots 4 and 5 are bound from the correct, legally usable resource inputs.
3. Shared and encounter animator tick paths are independently represented and tested.
4. State-5 update/render coupling is modeled explicitly.
5. Marker placement, interaction, clearing, and checkpoint restoration pass deterministic tests.
6. State-8 behavior remains conservative and does not invent an exit.
7. Shared alVoid gate/counter/tile effects are tested.
8. Event-trace regression tests pass for all documented transitions.
9. A comparison against an authorized original runtime is recorded before claiming visual/gameplay equivalence.
10. Build, APK, and device validation are separately reported only after they actually occur.

## 10. Current status

**Completed:** behavior specification and test design at static-source level.

**Not completed:** Native implementation, test code, resource packaging, Android build, APK, original-runtime comparison, or device validation.

The next implementation-ready work item is to design the encounter's pure Kotlin/Java data model and fake-port test harness inside the approved DIRU-NA architecture, once the Android/core project skeleton exists. This audit does not create those modules or claim that they exist.
