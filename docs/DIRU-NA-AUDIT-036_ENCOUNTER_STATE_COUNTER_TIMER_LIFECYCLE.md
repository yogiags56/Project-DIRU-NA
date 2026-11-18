# DIRU-NA-AUDIT-036 — Encounter State Counters & Timer Lifecycle

## Scope and source pin

Static analysis of `src/main/java/i.java` from `kubikaugustyn/DiamondRushSource`, pinned to commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This audit traces field declarations, constructor/reset defaults, encounter initialization, update-branch reads/writes, and checkpoint restoration for selected fields. It does not infer undocumented semantic names beyond what the code proves.

## 1. Field inventory

| Field | Type | Verified role in audited branches | Confidence |
|---|---|---|---|
| `aoInt` | int | Persistent encounter state selector | High |
| `aqInt` | int | Encounter counter used by special encounter logic; decremented by object/interaction events and gates completion | High for behavior; semantic label unknown |
| `apInt` | int | State-local timer/counter, reused across encounter states | High |
| `jLong` | long | Yetti absolute game-tick deadline, initialized with randomized delay | High |
| `kLong` | long | Yetti auxiliary absolute deadline; also gates a delayed arena-cell update | High |
| `SInt` | int | Great Anaconda snapshot of prior `aoInt` used to choose follow-up state | High |
| `LInt` | int | Great Anaconda phase counter in states 10/11 | High |
| `jBoolean` | boolean | Great Anaconda one-shot player-hit gate in state 11 | High |
| `kBoolean` | boolean | Great Anaconda secondary animator enable gate | High |
| `arInt` | int | Great Anaconda selected horizontal target/column in state 6 path | Medium |

## 2. Initialization and reset evidence

### 2.1 Constructor defaults

The constructor initializes:
- `LInt = 0`
- `SInt = 0`
- `jBoolean = false`
- `kBoolean = false`
- `jLong = 0L`
- `kLong = 0L`

These are generic object defaults, not encounter-specific starting states.

### 2.2 Encounter initialization

In the stage configuration branch:
- Great Anaconda sets `kByte = 4`, `aoInt = 0`, `aqInt = 3`, `arInt = 0`, `apInt = 0`, and `kBoolean = false`.
- Evil Teutonic Knight enters through `yVoid()`, which sets `aoInt = 13`, `aqInt = 4`, `atInt = 408`, and `kByte = 5`.
- Yetti is configured in the Siberia branch; its initial encounter counter is set to 5 through `adVoid()` in the audited initialization/update path. `adVoid()` also sets `aoInt = -1`.

Checkpoint restoration repeats the encounter-specific setup:
- Evil Knight calls `yVoid()`.
- Yetti sets `aqInt = 5`.
- Great Anaconda resets `aoInt = 0`, `arInt = 0`, `aqInt = 3`, and `kBoolean = false`.

## 3. aqInt — encounter counter, not yet a proven health value

### 3.1 Confirmed writes

The pinned source contains the following relevant writes:
- Great Anaconda initialization: `aqInt = 3`.
- Evil Knight initialization via `yVoid()`: `aqInt = 4`.
- Yetti initialization/reset via `adVoid()`: `aqInt = 5`.
- Yetti encounter branch: when the counter reaches zero or below, state 12 is entered and `apInt` resets.
- Great Anaconda: a successful interaction in state 2 decrements the counter.
- Evil Knight: certain stage-cell/object interactions decrement the counter.
- Yetti: the inspected branch decrements the counter when specific stage objects in its encounter arena are hit/removed.

### 3.2 Confirmed reads

The counter is used to:
- choose whether one or two encounter state-processing iterations are performed in a game update (`aSInt % aqInt == 0`, when positive);
- choose a Great Anaconda phase duration (`aqInt <= 1 ? 5 : 10`);
- choose which Falling Torches/encounter arena cells are placed or activated at thresholds such as `aqInt <= 2`;
- enter the completion state when `aqInt <= 0);
- block the shared `alVoid()` stage-completion routine while a special encounter has `aqInt > 0).

This is enough to classify `aqInt` as a progression/remaining-object counter in these paths. It is not enough to call it boss health: the decrement sites are tied to stage objects and interactions, and no direct damage/HP relationship has been established.

## 4. apInt — state-local counter

The same field is reused and reset at multiple state boundaries, so it must not be modeled as one global encounter duration.

### Yetti
- State 12 increments `apInt`; after it exceeds 100, the encounter enters state 15 and calls `jVoid(11, 11)`.
- This is a completion delay, not an animation duration.

### Great Anaconda
- Incremented once at the beginning of each state-4 update.
- State 6 waits until `apInt > 10`, then transitions to state 1 and resets it.
- State 1 uses thresholds `>20` and `>40`.
- State 7 waits until `>80`, then transitions to state 8 and calls `alVoid()`.
- State 3 waits until `>40` before choosing a follow-up based on `aqInt` and `SInt`.
- State 2 uses `>15` and `>30` thresholds.
- State 4 uses a duration of 5 or 10 based on `aqInt`.
- States 9, 10, and 11 decrement `apInt` while using `LInt` for their own phase timing.

### Evil Knight
- State 12 increments `apInt`; after it exceeds 100, it enters state 15 and calls `jVoid(11, 11)`.

## 5. Yetti absolute timers

### 5.1 jLong

When `jLong == 0` and `nByte != 0`, it is initialized to:
`aSInt + random(340, 441)`.

The update compares current `aSInt` against this absolute deadline. Once the deadline passes, movement states 2/3/4/5/10/11 can transition into the corresponding special states 13/14. After special state 13/14 animation completion:
- `kLong = aSInt + 40`
- `jLong = aSInt + random(340, 441)`

The randomized interval is in game-tick units, not milliseconds. The source's global `aSInt` increment is gated by `bByte != 2`; tick-rate conversion remains open.

### 5.2 kLong

In Yetti state 13/14 completion, it is assigned `aSInt + 40`. The draw/update path checks `aSInt >= kLong + 80`, then rewrites a fixed arena row (x=14..21, y=15) and clears `kLong` to zero.

The observed deadline plus the additional 80-tick comparison means the arena mutation occurs no earlier than the stored deadline plus 80 game ticks. Preserve this arithmetic literally; do not simplify it to a 40- or 80-tick delay without tracing the exact lifecycle and call order.

## 6. Great Anaconda phase fields

### 6.1 SInt

In state 2, when the interaction succeeds:
- `SInt = aoInt` is captured before the transition to state 3.
- The source then assigns `aoInt = 3`.
- A later state-3 branch checks `SInt == 10` to choose state 9, or `SInt == 1 || SInt == 2` to choose state 4 and select main animation 2.

A second assignment of `SInt = aoInt` occurs after the interaction cleanup in the same state-2 block. The exact prior-state meaning is therefore tied to the current control-flow path and should be preserved in sequence.

### 6.2 LInt

- State 10 increments `LInt`; at 28 it selects main animation 7.
- At 50 it resets `LInt` to zero, enters state 11, clears interaction markers, selects main animation 8 and secondary animation 2, and enables `kBoolean`.
- State 11 increments `LInt`; at 12 it resets `LInt`, enters state 9, selects main animation 4, and disables both `jBoolean` and `kBoolean`.

### 6.3 jBoolean and kBoolean

- In state 11, `jBoolean` prevents repeated player damage: after the positional hit condition is met, it becomes true. It is reset when state 11 completes.
- `kBoolean` controls whether animator slot 4 is updated. It becomes true on the state-10 to state-11 transition and false when state 11 completes.
- Checkpoint restore resets `kBoolean` to false. Generic constructor defaults also set both flags false.

## 7. Implementation constraints

1. Keep `aqInt` as a raw encounter progression counter; do not rename it to health.
2. Treat `apInt` as a state-local counter with explicit reset/decrement semantics per state.
3. Store `jLong` and `kLong` as source-tick deadlines, not wall-clock timestamps.
4. Preserve the exact `kLong + 80` comparison and reset sequence pending runtime validation.
5. Preserve Great Anaconda's `SInt` capture order and state-3 branch tests.
6. Preserve `LInt` thresholds 28, 50, and 12 exactly.
7. Keep `jBoolean` one-shot damage gating separate from `kBoolean` animator gating.
8. Checkpoint restoration must reinitialize encounter state and counters consistently with the source.
9. Do not convert any of these fields to semantic names in production code until their full lifecycle is documented.

## 8. Remaining work

- Trace all writes to `aqInt` outside the three encounter branches, including helper calls and object callbacks.
- Inspect Great Anaconda states 0, 1, 5, 7, 8, and 9 end-to-end.
- Resolve the exact Great Anaconda slot-4 resource and its rendering/position lifecycle.
- Trace all `jLong`/ `kLong` reset sites and global tick progression.
- Establish deterministic update ordering for counter decrement, state transition, animation set, and animator tick.
- Build source-derived unit tests for checkpoint reset and all counter/timer boundaries.
- Validate behavior against the original runtime when a legally usable JAR/runtime is available.

## 9. Audit status

Completed:
- Verified declarations and constructor defaults for audited fields.
- Verified Great Anaconda, Yetti, and Evil Knight encounter counter initialization/reset values.
- Mapped primary `aqInt` decrement/read behavior.
- Mapped major `apInt` thresholds in the three encounter branches.
- Mapped Yetti `jLong` and `kLong` deadline behavior.
- Mapped Great Anaconda `SInt`, `LInt`, `jBoolean`, and `kBoolean` use.

Open:
- Complete helper/callback data flow.
- Complete all-state Great Anaconda graph.
- Exact tick-rate conversion.
- Runtime validation.

No Android build, APK, or runtime test was performed.
