# DIRU-NA-AUDIT-035 — Encounter Animation State Transition Mapping

## Scope and evidence boundary

This audit correlates encounter state variables and animation-selection indices in the candidate source file src/main/java/i.java, pinned to kubikaugustyn/DiamondRushSource commit 215c71d04057d66a1b08419b1a4b1043ae3e0881.

It focuses on the update branches for:
- Yetti (kByte == 3)
- Great Anaconda (kByte == 4)
- Evil Teutonic Knight (kByte == 5)

The audit is static source analysis. It does not assign undocumented human-readable attack names, decode new proprietary assets, or claim original-runtime validation.

## 1. Important interpretation rule

The local variables n10, n21, n22, and n26 are per-update animation-selection outputs. They are not persistent state variables. The persistent encounter state is primarily aoInt, while the local output is assigned when a transition requests a new animation.

The source frequently assigns the same numeric value to aoInt and the animation index, but that relationship must not be generalized to every state. Some states intentionally wait, tick the current animation, or use a separate animator.

The animator's setAnimation method does not restart playback when the requested index is already active (AUDIT-033). This affects how repeated state assignments behave.

## 2. Yetti — kByte 3

### 2.1 Entry and activation

The loader initializes animator slot 5 from /mm1.f chunk 0 and selects animation 0. The encounter starts with aoInt = -1. Once the player reaches crtPlayerX * 24 >= 336, the update changes aoInt to 0 and selects animation 0.

### 2.2 State-to-animation mapping

For the main animator in slot 5, the update's local n10 output is selected as follows:

| Current state aoInt | Condition / event | Next state | Animation selected |
|---:|---|---:|---:|
| -1 | Player X threshold reached (336 world units) | 0 | 0 |
| 0 | Position test against asInt + 24 | 4 or 5 | 4 or 5 |
| 1 | Position test against asInt + 24 | 5 or 4 | 5 or 4 |
| 2 | Current animation ends | 4 or 13 | 4 or 13 |
| 3 | Current animation ends | 5 or 14 | 5 or 14 |
| 4 | Boundary/timer/player-position condition | 10 or 5, or remain | 10 or 5 |
| 5 | Boundary/timer/player-position condition | 11 or 4, or remain | 11 or 4 |
| 10 | Current animation ends; timer/player vertical/horizontal checks | 13, 4, or 6 | 13, 4, or 6 |
| 11 | Current animation ends; timer/player vertical/horizontal checks | 14, 5, or 7 | 14, 5, or 7 |
| 6 / 13 | Special movement/attack sequence completes | 4 | 4 |
| 7 / 14 | Special movement/attack sequence completes | 5 | 5 |
| 12 | Completion delay exceeds 100 update counts | 15 | Current animator is ticked in the completion branch |
| 15 | Terminal branch | 15 | No further encounter animation transition in this branch |

Notes:
- State 0 and 1 are movement/boundary states that adjust asInt by one unit per update when no transition is selected.
- States 4 and 5 also adjust asInt while active; states 10 and 11 adjust it by two units while their animation has not ended.
- States 6/7 and 13/14 share a transition block but use different collision extents and different timer behavior.
- For states 13/14, reaching the last animation frame also refreshes kLong and jLong timers.
- Damage checks are tied to animation frame thresholds and player coordinates; animation selection alone does not describe the entire attack.
- State 12 repeatedly plays the mine sound during its delay branch and eventually transitions to state 15.

### 2.3 Confirmed semantic limits

The source supports movement, timed state transitions, collision/damage checks, and a terminal state. It does not prove human-readable names such as idle, walk, swipe, jump, or roar for individual animation indices. Those labels remain prohibited in the Native implementation until visual/runtime evidence is available.

## 3. Great Anaconda — kByte 4

### 3.1 Animator arrangement

- Main animator: slot 5, loaded from /b0.f chunk 0.
- Secondary animator: slot 4, initialized separately in the encounter resource-loading path.
- Main animation selection output: n21.
- Secondary animation selection output: n22.
- The secondary animator is ticked/selected only when kBoolean is true.

This is a two-animator encounter path. Native code must not collapse both into one animation channel.

### 3.2 Main animator selection table

The following animation selections are explicit in the inspected update branch:

| Encounter state / condition | Main animation output n21 | State or additional effect |
|---|---:|---|
| State 6; apInt > 10 | 2 | State becomes 1; apInt resets |
| State 2; interaction succeeds | 3 | aqInt decrements; state becomes 3; sound 10 |
| State 2; apInt > 15 and current animation is not 6 | 6 | State remains 2 |
| State 2; apInt > 30 | 0 | State becomes 4; interaction markers are cleared |
| State 3; apInt > 40 and SInt is 1 or 2 | 2 | State becomes 4 |
| State 4; timer threshold reached | 4 | State becomes 5 |
| State 4; intermediate animation/timer condition | 1 | State remains 4 |
| State 10; LInt reaches 28 | 7 | State remains in its current branch |
| State 10; LInt reaches 50 | 8 | State becomes 11; n22 becomes 2; kBoolean becomes true |
| State 11; LInt reaches 12 | 4 | State becomes 9; jBoolean and kBoolean reset |

The source also includes states 0, 1, 5, 7, 8, and 9 whose transitions primarily modify stage objects, timers, player interaction, or encounter state. A transition table for those states must include their surrounding conditions before assigning animation semantics.

### 3.3 Secondary animator

When kBoolean is true, slot 4 is updated independently:
- If n22 remains -1, slot 4 is ticked.
- If n22 is assigned, slot 4 receives that animation index.
- The inspected state-10 to state-11 transition explicitly selects animation 2 for slot 4 and enables kBoolean.
- The state-11 completion path disables kBoolean.

The slot-4 resource identity and exact visual role remain subject to the resource/call-site mapping. Do not label it as a particular body part or effect without evidence.

### 3.4 Counter and state caveats

aqInt is decremented by successful encounter interactions in the inspected branch and controls certain transitions, but the source audit does not justify naming it "health" without tracing all initialization, reads, and writes. Preserve its raw role as an encounter counter until the complete data-flow audit is finished.

## 4. Evil Teutonic Knight — kByte 5

### 4.1 Entry and activation

The loader initializes animator slot 5 from /b1.f chunk 0 and selects animation 10. The encounter can begin with aoInt = -1; when crtPlayerX * 24 >= 360, the update sets aoInt to 10 and selects animation 10.

### 4.2 Main animator selection mapping

The local n26 output controls slot 5:

| State / condition | Next state | Animation selected |
|---|---:|---:|
| Initial state -1; player reaches X threshold 360 | 10 | 10 |
| State 13; current animation ends | 0 | 0 |
| State 4; current animation ends | 0 | 0 |
| State 5; current animation ends | 1 | 1 |
| State 10/11; horizontal relation to moving coordinate | 1 or 0 | 1 or 0 |
| State 6; player reaches the relevant side/boundary | 8 or 0 | 8 or 0 |
| State 7; player reaches the relevant side/boundary | 9 or 1 | 9 or 1 |
| State 0; player/arena condition | 6, 2, or 1 | 6, 2, or 1 |
| State 1; player/arena condition | 7, 3, or 0 | 7, 3, or 0 |
| State 8; animation ends | 10 | 10 |
| State 9; animation ends | 11 | 11 |
| State 3; animation ends | 11 | 11 |
| State 2; animation ends | 10 | 10 |
| Encounter counter aqInt <= 0 | 12 | 12 |

Additional behavior:
- States 0/1 move atInt and branch based on player position, vertical coordinate, and the adBoolean timing flag.
- States 2/3 inspect animation frames 5 and 7 for timing and collision behavior.
- States 8/9 transition to states 10/11 after animation completion and use frame thresholds for contact checks.
- States 6/7 move atInt by two units in their active path and can transition into states 8/9.
- State 13 holds animation 13 and can tick it conditionally; when it ends the state returns to 0.
- State 12 is a completion-delay branch; after apInt exceeds 100 it changes to state 15 and invokes jVoid(11, 11).
- The encounter counter aqInt can decrement when stage objects are affected; when it reaches zero, the update selects animation 12 and enters state 12.

### 4.3 Source anomaly to preserve cautiously

One collision predicate in the inspected decompiled code compares n27 against n29 - 24 in both its lower and upper bounds. This is a single-coordinate equality check, not a range. Do not silently "correct" it in Native code. Record it as source behavior and verify against an exact original runtime before deciding whether it is a decompiler artifact or original logic.

## 5. Cross-encounter implementation requirements

1. Keep aoInt as a raw source-compatible encounter state until a complete transition graph is proven.
2. Keep animation index selection as an explicit output of each state transition.
3. Preserve animator slot/resource identity by encounter, not by global slot number.
4. Preserve the independent Great Anaconda secondary animator and its kBoolean gate.
5. Preserve tick-vs-setAnimation behavior and same-index no-op semantics.
6. Keep source tick counts, game-loop timing, and wall-clock timing separate.
7. Keep collision frame thresholds and stage-object mutations in the same deterministic update sequence as the source.
8. Do not invent semantic animation labels or reinterpret aqInt as health.
9. Preserve suspicious source predicates verbatim in compatibility tests until runtime evidence resolves them.

## 6. Remaining work

- Complete Great Anaconda's full state 0–11 transition graph, including all branches not selecting n21/n22.
- Trace initialization and every read/write of aqInt, apInt, SInt, LInt, jBoolean, and kBoolean.
- Trace the origin and lifecycle of the slot-4 animator.
- Complete Yetti's state graph with exact movement/timer predicates and all stage-cell mutations.
- Trace Evil Knight's state 0–15 graph, including all object interactions and collision predicates.
- Decode and map bounding-box values and establish their relationship to the frame-based collision checks.
- Establish game-loop tick frequency and reproduce deterministic encounter tests.
- Validate against the exact original JAR/runtime when a legally usable package is available.

## 7. Audit status

Completed at static-source level:
- Correlated the primary animation output variables n10, n21, n22, and n26 with encounter state branches.
- Established the Yetti main animator's state-driven animation selection behavior.
- Established that Great Anaconda uses separate main and secondary animator channels.
- Mapped multiple explicit Great Anaconda animation outputs and secondary animation 2 selection.
- Mapped the principal Evil Teutonic Knight animation outputs and terminal transition.

Open:
- Full state graphs and all timer/counter data flow.
- Slot-4 resource identity.
- Semantic animation names.
- Bounding-box/collision mapping.
- Tick-rate conversion.
- Original-runtime validation and licensing clearance.

No Android build, APK, or runtime test was performed.
