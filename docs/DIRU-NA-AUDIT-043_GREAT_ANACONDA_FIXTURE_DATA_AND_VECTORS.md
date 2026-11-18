# DIRU-NA-AUDIT-043 — Great Anaconda Test Fixture Data Model & Deterministic Scenario Vectors

## 1. Purpose and evidence boundary

This audit defines reusable synthetic fixtures and deterministic before/after update vectors for GA-001 through GA-028.

Candidate source pin:
- Repository: `kubikaugustyn/DiamondRushSource`
- File: `src/main/java/i.java`
- Commit: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

Supporting documents: AUDIT-033 through AUDIT-042, especially the source anchors and test mapping in AUDIT-042.

All fixture values in this document are **synthetic test inputs**, not extracted proprietary sprite/resource payloads and not claims about original level contents. Frame metadata values are chosen to make predicates controllable and repeatable. They must not be used as production animation data.

No test source code was added or executed. No Android build/APK/runtime validation is claimed.

## 2. Fixture model

### 2.1 ScenarioFixture

Every test case should be expressible as an immutable fixture plus a sequence of operations.

Conceptual schema:

```text
ScenarioFixture {
  id: String
  encounter: EncounterFixture
  player: PlayerFixture
  stageGrid: StageGridFixture
  mainAnimator: AnimatorFixture
  secondaryAnimator: AnimatorFixture?
  sharedCounters: SharedCounterFixture
  operations: List<ScenarioOperation>
  assertions: List<ExpectedObservation>
}
```

Use immutable fixture definitions and create a fresh mutable fake world for each test. Tests must not share mutable grids, counters, or animator operation lists.

### 2.2 EncounterFixture

```text
EncounterFixture {
  state: Int
  remainingInteractions: Int       // source aqInt; not HP
  stateCounter: Int                // source apInt
  markerColumnOffset: Int           // source arInt
  previousInteractionState: Int     // source SInt
  phaseCounter: Int                 // source LInt
  markerX: Int                      // source MInt
  markerY: Int                      // source NInt
  hitGate: Boolean                  // source jBoolean
  secondaryAnimatorEnabled: Boolean // source kBoolean
  state5RenderUpdateGate: Boolean   // source ZBoolean
  unresolvedFieldT: Int             // source TInt; retain only for parity
}
```

Defaults for a newly initialized Great Anaconda encounter:
- state=0
- remainingInteractions=3
- stateCounter=0
- markerColumnOffset=0
- previousInteractionState=0
- phaseCounter=0
- markerX=-1, markerY=-1
- hitGate=false
- secondaryAnimatorEnabled=false
- state5RenderUpdateGate=false
- unresolvedFieldT=0

These defaults combine the stage initialization and constructor defaults established in the earlier audits. A test that constructs only the encounter object must not assume stage initialization has also run unless its fixture explicitly invokes that setup.

### 2.3 PlayerFixture

```text
PlayerFixture {
  logicalX: Int
  logicalY: Int
  animatorY: Int
}
```

Keep player logical grid coordinates separate from animator pixel-space Y. The source collision helpers use both domains in different predicates.

### 2.4 StageGridFixture

Use separate layers because the source uses different arrays for the boss marker/interactions and foreground metadata.

```text
StageGridFixture {
  width: Int
  height: Int
  playerLayer: Int[width][height]
  foregroundLayer: Int[width][height]
  cellTimers: Int[width][height]
  cellTypeMetadata: Int[width][height]
}
```

Recommended synthetic baseline:
- width=24, height=16 (test dimensions only);
- playerLayer initialized to -1 (empty);
- foregroundLayer initialized to 0;
- cellTimers initialized to 0;
- cellTypeMetadata initialized to 0.

Use a test helper to seed exact cells. Do not infer that a 24×16 fixture is the original stage dimensions.

**Bounds rule:** helpers hVoid/gVoid check negative x/y but the source does not visibly clamp x+1 or validate the upper edge in those methods. Keep test coordinates in bounds. Add separate defensive-boundary tests only when the Native adapter's policy is designed; do not falsely attribute upper-bound checks to the source.

### 2.5 AnimatorFixture

```text
AnimatorFixture {
  present: Boolean
  currentAnimation: Int
  currentFrame: Int
  y: Int
  frameMetadata: FrameMetadata
  tickAdvancesFrame: Boolean
  operationLog: List<AnimatorOperation>
}
```

`tickAdvancesFrame=false` is the default for state-machine unit tests. It prevents accidental time progression. Tests that specifically exercise animation-frame changes should provide an explicit scripted frame sequence.

### 2.6 FrameMetadataFixture

The source indexes five integers per animation frame and reads field offset +3 for the vertical extent used in Great Anaconda predicates. Use a typed representation:

```text
FrameMetadata {
  field0: Int
  field1: Int
  field2: Int
  verticalExtentField3: Int
  field4: Int
}
```

Synthetic baseline example:
- field0=0
- field1=0
- field2=0
- verticalExtentField3=16
- field4=0

This is a test vector only. Production values must come from an authorized, validated resource decoder. Preserve source integer arithmetic and signedness.

### 2.7 SharedCounterFixture

```text
SharedCounterFixture {
  encounterKind: Int       // source kByte
  completionIndex: Int     // source cmInt
  remainingByIndex: Byte[]
  stageWidth: Int
  stageHeight: Int
}
```

For alVoid tests, set completionIndex and remainingByIndex explicitly. Do not use a default counter that could accidentally make a gate test pass for the wrong reason.

## 3. Deterministic operation vocabulary

The fake world should support these operations:

| Operation | Purpose |
|---|---|
| `updateOnce` | Run exactly one full update through the coordinator |
| `encounterUpdateOnly` | Run encounter logic without the shared update; use only for isolated state tests |
| `renderState5` | Invoke the explicit state-5 render-reset hook |
| `setPlayer(x,y,animatorY)` | Set independent logical and animator coordinates |
| `setPlayerCell(x,y,value)` | Seed player-layer cell |
| `setForegroundCell(x,y,value)` | Seed foreground-layer cell |
| `setMainFrame(animation,frame,y,metadata)` | Configure frame-derived predicates |
| `setSecondaryAnimation(index)` | Configure slot-4 current animation |
| `setSharedCounter(index,value)` | Configure shared completion counter |
| `clearTrace` | Start a fresh ordered event assertion window |

For threshold tests, always state whether the supplied counter is the value **before update**, the value **after the common pre-switch increment**, or the value **after a state-specific decrement**.

## 4. Reusable synthetic fixture presets

### F-BASE — initial encounter

- Encounter defaults as listed in section 2.2.
- Player logicalX=0, logicalY=0, animatorY=0.
- Stage grid baseline empty.
- Main animator present, animation 0, frame 0, y=120, verticalExtentField3=16.
- Secondary animator present, animation 0, frame 0, y=0, verticalExtentField3=0.
- Shared completionIndex=-1.

### F-MARKER-ROW8 — interaction marker

- Start from F-BASE.
- state=1 or 2 as required.
- markerColumnOffset=0, therefore dInt=10.
- markerX=10, markerY=8.
- Seed playerLayer[10][8]=50 and playerLayer[11][8]=50 for an existing marker.
- For interaction success, seed at least one scanned cell in x=10..11 and y=7..8 to value 0.

### F-MARKER-ROW4 — lower marker

- Start from F-BASE.
- state=5 transition fixture.
- markerColumnOffset=0, therefore dInt=10.
- Main frame metadata configured so the exact source predicate is true.
- Expected marker cells after transition: (10,4) and (11,4) = 50.

### F-ANIMATOR-DOUBLE-TICK

- Start from F-BASE.
- state=11, phaseCounter=0, secondaryAnimatorEnabled=true.
- Main and secondary animators present.
- No secondary animation selection requested by state branch.
- Expected one shared slot-4 tick and one encounter-side slot-4 tick during the same coordinated update, subject to other state-11 conditions being held inert.

### F-COMPLETION-COUNTER

- Start from F-BASE.
- encounterKind=4, remainingInteractions=0.
- completionIndex=2.
- remainingByIndex[2]=1.
- Seed one or more foreground cells with low byte 0x11 and high portion 2; seed tile-above variants 7, 14, or 33 for branch tests.
- This fixture is for alVoid only; keep unrelated encounter state logic out of the assertion window.

## 5. Deterministic scenario vectors

The table uses:
- **pre** = encounter field value before the update call;
- **dispatch** = value after the common apInt increment and before state branch logic;
- **post** = value after the state branch and animator actions.

If a test invokes a helper directly rather than a full update, the vector says so.

| Test | Initial state / pre-update values | Operation / predicate | Expected post-state and event vector |
|---|---|---|---|
| GA-001 | F-BASE | Initialize stage encounter | state=0, aq=3, ar=0, ap=0, marker=(-1,-1), k=false |
| GA-002 | state=0, ap=0 | playerX=9 then 10; update each time | first state=0; second state=6, ap=0 |
| GA-003 | state=6, ap=10 | updateOnce | dispatch ap=11; state=1, ap=0, main SET_ANIMATION(2) |
| GA-004a | state=1, ap=19 | updateOnce | dispatch ap=20; no marker placement |
| GA-004b | state=1, ap=20 | updateOnce | dispatch ap=21; marker helper called at (dInt,8); state remains 1 |
| GA-004c | state=1, ap=39 | updateOnce | dispatch ap=40; marker branch executes; state remains 1 |
| GA-004d | state=1, ap=40 | updateOnce | dispatch ap=41; state=2, ap=0; marker-placement branch not executed this update |
| GA-005 | valid x=10,y=8 | call hVoid(10,8) | playerLayer[10][8]=50 and [11][8]=50 |
| GA-006 | state=2, ap=0, aq=3, SInt=0; F-MARKER-ROW8 with one zero cell | updateOnce; interaction returns true | aq=2, SInt=2, state=3, marker clear, main SET_ANIMATION(3), sound 10 |
| GA-007 | state=2, ap=30, no zero cells in scan | updateOnce | dispatch ap=31; state=4, ap=0, main SET_ANIMATION(0), marker clear |
| GA-008 | state=3, ap=40, aq=0, SInt=2 | updateOnce | dispatch ap=41; state=7, ap=0 |
| GA-009 | state=3, ap=40, aq=1, SInt=10 | updateOnce | dispatch ap=41; state=9, ap=0 |
| GA-010 | state=3, ap=40, aq=1, SInt=2 | updateOnce | dispatch ap=41; state=4, ap=0, main SET_ANIMATION(2) |
| GA-011a | state=4, aq=1, ap=4 | updateOnce | dispatch ap=5; state=5, ap=0; main animation request 4; eBoolean and abVoid invoked |
| GA-011b | state=4, aq=2, ap=9 | updateOnce | dispatch ap=10; state=5, ap=0; main animation request 4; eBoolean and abVoid invoked |
| GA-012a | state=5, Z=false | updateOnce | Z=true after eligible state-5 branch; frame predicate controlled false |
| GA-012b | state=5, Z=true | updateOnce without render | encounter branch returns at gate; no state-5 work |
| GA-012c | state=5, Z=true | renderState5 | Z=false |
| GA-013 | state=5, Z=false, dInt=10 | set synthetic frame y/extent so source inequality is true; updateOnce | TInt=0; acVoid called; state=10; marker=(10,4); two row-4 cells=50 |
| GA-014a | state=7, ap=79 | updateOnce | dispatch ap=80; state remains 7; alVoid not called |
| GA-014b | state=7, ap=80 | updateOnce | dispatch ap=81; state=8; alVoid called once |
| GA-015 | state=8 | updateOnce repeated | state remains 8; common main animator tick path continues; no invented completion event |
| GA-016 | state=9, ap=5 | updateOnce; choose frame predicate true after decrement | dispatch ap=6, state-local decrement ->4; state=6, ap=0, ar recalculated |
| GA-017 | state=10, L=27, ap=0 | updateOnce | L=28; main SET_ANIMATION(7); state remains 10 |
| GA-018 | state=10, L=49, ap=0, marker valid | updateOnce | state=11, L=0, marker clear, main SET_ANIMATION(8), secondary SET_ANIMATION(2), k=true |
| GA-019 | state=11, L=0, ap=0, k=true | coordinated update; no hit; no state exit | shared slot4 TICK then encounter slot4 TICK; k remains true |
| GA-020 | state=10, L=49, k=false | coordinated update | shared slot4 TICK if slot exists; transition selects slot4 SET_ANIMATION(2); no encounter-side second tick |
| GA-021 | state=11, L=0, k=true, j=false; playerY=4 and X within [d-3,d+4] | updateOnce | hurtHero(1,64, direction 4 if X=d else 2); j=true; next eligible update does not repeat direct state-11 hit |
| GA-022 | state=11, L=11, k=true, j=true | updateOnce | L=0; state=9; main SET_ANIMATION(4); j=false; k=false |
| GA-023 | kByte=4, aq=1, cm=2, counter[2]=1 | call alVoid directly | immediate return; counter unchanged; no sound/cell events |
| GA-024 | kByte=4, aq=0, cm=2, counter[2]=1 | call alVoid directly | counter[2]=0; sound 8; matching foreground scan performed |
| GA-025 | gate passes, cm=-1 | call alVoid directly | no counter, sound, or foreground cell events |
| GA-026 | before restore: ao=11, ar=5, aq=0, k=true | restore Great Anaconda checkpoint | ao=0, ar=0, aq=3, k=false; only assert explicitly evidenced reset fields |
| GA-027a | ar=-2 | calculate dInt | dInt=6 |
| GA-027b | ar=0 | calculate dInt | dInt=10 |
| GA-027c | ar=2 | calculate dInt | dInt=16 |
| GA-028a | x=-1,y=8 | call gVoid | no grid writes |
| GA-028b | x=10,y=-1 | call gVoid | no grid writes |

### Vector interpretation notes

- GA-004a through GA-004d make the strict threshold and branch order explicit.
- GA-007 starts with pre-update apInt=30 because the update increments to 31 before the state-2 timeout check.
- GA-014a/GA-014b likewise account for the common pre-switch increment.
- GA-016 uses pre-update apInt=5 to show the common increment to 6 and state-9 decrement to 4 before its frame predicate.
- GA-017 and GA-018 require the phase counter value before the increment.
- GA-019 requires a coordinated update, not an encounter-only update, because the shared slot-4 tick occurs outside the encounter branch.
- GA-023 through GA-025 call alVoid directly in a helper-level unit test. The state-7 integration call is covered by GA-014.
- GA-028 tests the negative-coordinate guard only; do not claim upper-bound protection in gVoid.

## 6. Frame predicate fixture construction

The state-5 source predicate uses:

`mainY - 40 - verticalExtentField3 <= 72 - stageOffset + 40`

where `verticalExtentField3` is read from the current animation frame metadata. For a synthetic test choose:
- mainY=120
- verticalExtentField3=16
- stageOffset=0

Then left side = 64 and right side = 112, so the predicate is true. A false-side fixture can use mainY=220 with the same extent and stageOffset: left side =164, right side=112.

The state-9 predicate is separate:

`mainY - 40 - verticalExtentField3 >= 240 - stageOffset + 40`

Construct a separate synthetic vector; do not reuse the state-5 vector and assume the inverse relationship because the thresholds differ.

The state-10/11 collision helper uses animator position and frame extent differently again. Keep a dedicated fixture builder for each predicate rather than one generic `bossY` shorthand.

All numeric values above are synthetic and only demonstrate deterministic predicate control.

## 7. Animator operation scripts

The fake animator should record operations with call-site labels:

```text
AnimatorOperation {
  slot: Int
  operation: TICK | SET_ANIMATION
  animationIndex: Int?
  callSite: SHARED_UPDATE | ENCOUNTER_UPDATE | RENDER_OR_OTHER
}
```

Required example scripts:

### Script S1 — secondary channel disabled
- k=false
- shared slot 4 exists
- one coordinated update
- expected slot-4 log: `TICK(SHARED_UPDATE)`

### Script S2 — secondary channel active
- k=true
- n22 unset
- one coordinated update in state 11 without state exit
- expected slot-4 log:
  1. `TICK(SHARED_UPDATE)`
  2. `TICK(ENCOUNTER_UPDATE)`

### Script S3 — transition into secondary channel
- state 10, LInt=49, k=false
- one coordinated update
- expected slot-4 log:
  1. `TICK(SHARED_UPDATE)`
  2. `SET_ANIMATION(2, ENCOUNTER_UPDATE)`
- no second encounter-side tick on this transition update.

### Script S4 — secondary channel exit
- state 11, LInt=11, k=true
- encounter branch transitions to state 9 and sets k=false before secondary action handling.
- Verify actual source statement order and condition evaluation when implementing; the source sets kBoolean=false before the final `if (kBoolean)` block, so the encounter-side slot-4 action is skipped on the exit update. The shared slot-4 tick still occurs earlier in the update.

This exit-update detail should be asserted explicitly in the future coordinator test.

## 8. Fixture quality and reproducibility rules

1. Each test starts from a fresh fixture and fake world.
2. Every threshold test declares pre-update values and dispatch values.
3. Frame metadata is immutable for the duration of a test unless a scripted animation change is the subject.
4. Stage-grid writes are recorded with coordinates and old/new values.
5. Animator operations are recorded separately for shared and encounter call sites.
6. Sound and damage are captured as events, never played.
7. Use integer arithmetic with source-compatible overflow/signedness where proven.
8. Avoid random numbers, wall-clock time, Android frame callbacks, or real audio.
9. Use named fixture builders rather than hidden magic arrays.
10. Synthetic fixture data must be labelled as synthetic in source comments and test names.
11. Do not package proprietary game assets as fixtures.
12. A passing synthetic test establishes implementation consistency with the encoded model, not original-runtime equivalence.

## 9. Audit corrections and high-value details

### 9.1 State-11 exit suppresses encounter-side slot-4 action on that update

The source sets `kBoolean=false` inside the state-11 exit branch before the later conditional slot-4 action. Therefore:
- shared slot-4 tick still occurs earlier in the common update;
- the encounter-side slot-4 tick/setAnimation block is skipped on the exit update;
- do not write a test that expects a second slot-4 tick on GA-022.

This refines the broad double-tick window described earlier: the encounter-side tick happens only on updates where kBoolean remains true after state dispatch.

### 9.2 State-2 success and timeout are sequential checks

The success branch does not automatically skip the subsequent timer checks. A fixture intended to isolate GA-006 must keep apInt <=30 and avoid a second unintended state mutation. GA-007 must keep the interaction result false.

### 9.3 State-4 helper calls happen after its phase branch

The eBoolean and abVoid calls are executed in state 4 after the duration/animation decision block. The test should record them even on the transition update.

### 9.4 Marker helper semantics

- hVoid writes marker value 50 to two adjacent player-layer cells.
- gVoid clears two adjacent cells to -1.
- Both return early for negative x or y.
- Their source methods do not perform an upper-bound check; test valid coordinates.

## 10. Readiness and next step

**Completed:** synthetic fixture schema, reusable fixture presets, deterministic before/after vectors for GA-001..GA-028, frame predicate examples, animator call-site scripts, and reproducibility rules.

**Not completed:** test implementation, test execution, Gradle/core skeleton, Android build, APK, or original-runtime comparison.

Recommended next work item: **AUDIT-044 — Great Anaconda Test Harness Implementation Readiness & Core Project Dependency Map**. Before coding tests, inspect and define the minimum project-level build/package/test conventions needed so the future harness is created in the correct independent DIRU-NA structure rather than as a throwaway standalone project.
