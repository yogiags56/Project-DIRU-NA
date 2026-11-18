# DIRU-NA-AUDIT-041 — Great Anaconda Encounter Architecture & Test Harness Blueprint

## 1. Purpose and status

This document defines a code-level architecture blueprint for implementing and testing the Great Anaconda encounter in Project DIRU-NA.

It follows AUDIT-040 and is deliberately a blueprint, not an implementation commit. The repository's README and Development Isolation document still describe the Android/Gradle skeleton as not yet created. No source classes, test harness, Android module, APK, or runtime behavior are claimed to exist because of this audit.

Evidence baseline:
- Candidate source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- Pinned revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`
- Behavior contracts: AUDIT-033 through AUDIT-040.
- Native project logical viewport: 240×320 portrait; encounter logic uses logical stage/player coordinates and is independent of physical display ratio.

## 2. Proposed source layout

The following is a target layout inside the future platform-independent core module. It is not a statement that these files currently exist.

```text
core/
  src/
    main/
      java/<project-package>/core/encounter/greatanaconda/
        GreatAnacondaEncounter
        GreatAnacondaState
        GreatAnacondaStateData
        GreatAnacondaStagePort
        GreatAnacondaAnimatorPort
        GreatAnacondaEncounterPort
        GreatAnacondaUpdateCoordinator
        GreatAnacondaSnapshot
        GreatAnacondaResourceBinding
        GreatAnacondaEvent
        GreatAnacondaTraceSink
        GreatAnacondaFramePredicate
    test/
      java/<project-package>/core/encounter/greatanaconda/
        GreatAnacondaEncounterTest
        FakeGreatAnacondaStage
        FakeGreatAnacondaAnimator
        RecordingGreatAnacondaTrace
        GreatAnacondaFixtures
```

If the eventual build uses Kotlin rather than Java, keep the same package boundaries and semantics. Do not introduce Kotlin coroutines, Android lifecycle classes, or platform graphics dependencies into the deterministic encounter core.

## 3. Responsibility split

### 3.1 GreatAnacondaEncounter

Owns only encounter-local state and the ordered state-branch behavior:
- state identifier and source-equivalent counters;
- marker coordinate handles;
- hit/secondary animator gates;
- branch-specific helper invocation decisions;
- main/secondary animation selection requests;
- checkpoint snapshot/restore of encounter-owned fields.

It must not own:
- Android Canvas/Bitmap/SurfaceView;
- physical screen dimensions or aspect-ratio policy;
- touch event interpretation;
- file/resource decoding;
- audio backend implementation;
- global stage progression outside the narrow port.

### 3.2 GreatAnacondaUpdateCoordinator

Owns the orchestration order spanning shared stage update and encounter update. This separation is required because slot 4 receives a shared tick before the Great Anaconda encounter branch can apply its conditional second tick.

The coordinator must make the following operations explicit:
1. shared stage update hook;
2. shared slot-4 tick if slot 4 exists;
3. other common update work in source order;
4. Great Anaconda encounter update;
5. main/secondary animator actions requested by the encounter branch, in the source order.

Do not hide both slot-4 tick sites behind one generic `advanceAllAnimators()` call. The call sites must remain individually observable to tests.

### 3.3 Ports

**GreatAnacondaStagePort**
- read player logical X/Y and player animator Y;
- read stage-player-layer cells;
- write/clear stage-player-layer cells;
- read/write foreground-layer metadata needed by shared callbacks;
- expose frame-derived stage geometry inputs;
- invoke damage and sound events through narrow methods;
- expose the shared counter/door callback operations only where required.

**GreatAnacondaAnimatorPort**
- `tick()`
- `setAnimation(index)`
- current animation index;
- current frame index;
- current frame metadata required by source predicates;
- animator position/bounds read/write where the source does so.

The animator port should be a deterministic interface. It must not require an Android bitmap or render surface.

**GreatAnacondaEncounterPort**
- compose the stage and animator ports and provide the small set of source-equivalent helper operations that remain shared with other game systems. Avoid a broad service locator.

### 3.4 Trace sink

A trace sink is optional in production and mandatory in unit tests. It records ordered semantic events such as:
- `EncounterStateChanged(old,new)`
- `MarkerPlaced(x,y)`
- `MarkerCleared(x,y)`
- `MainAnimationSet(index)`
- `SecondaryAnimationSet(index)`
- `AnimatorTick(slot, callSite)`
- `DamageApplied(amount, direction)`
- `SoundRequested(id)`
- `SharedCompletionCallbackInvoked()`
- `RenderGateReset()`

Trace events are diagnostics, not a new game mechanic. Do not let the trace sink mutate encounter state.

## 4. State representation and data ownership

Use an enum or named constants for states 0 through 11, but preserve the original numeric values in serialization/debug traces. State 8 must be representable even though no explicit switch case was found.

Recommended state data:

| Native property | Initial value | Source counterpart | Ownership |
|---|---:|---|---|
| state | 0 | aoInt | Encounter |
| remainingInteractions | 3 | aqInt | Encounter |
| stateCounter | 0 | apInt | Encounter |
| markerColumnOffset | 0 | arInt | Encounter |
| previousInteractionState | 0 | SInt | Encounter |
| phaseCounter | 0 | LInt | Encounter |
| markerX | -1 | MInt | Encounter |
| markerY | -1 | NInt | Encounter |
| hitGate | false | jBoolean | Encounter |
| secondaryAnimatorEnabled | false | kBoolean | Encounter |
| state5RenderUpdateGate | false | ZBoolean | Encounter/render handshake |
| unresolvedFieldT | 0 | TInt | Preserve only if required by source parity; semantic use unresolved |

Do not store derived display coordinates in this object. Physical viewport scaling belongs to renderer/viewport code.

Do not rename `remainingInteractions` to health, lives, or boss HP. Its only established semantics are the decrement and completion-branch behavior documented in the prior audits.

## 5. Proposed interfaces (language-neutral contract)

The eventual implementation can express these contracts in Java interfaces, Kotlin interfaces, or equivalent types.

```text
interface GreatAnacondaAnimatorPort {
    tick()
    setAnimation(index)
    currentAnimationIndex() -> integer
    currentFrameIndex() -> integer
    currentFrameMetadata() -> FrameMetadata
}

interface GreatAnacondaStagePort {
    playerX() -> integer
    playerY() -> integer
    playerAnimatorY() -> integer
    readPlayerLayer(x, y) -> integer
    writePlayerLayer(x, y, value)
    clearPlayerLayer(x, y)
    readForegroundCell(x, y) -> integer
    writeForegroundCell(x, y, value)
    hurtHero(amount, offset, direction)
    playSound(soundId)
    invokeSharedCompletionCallback()
}

interface GreatAnacondaTraceSink {
    record(event)
}

class GreatAnacondaEncounter {
    update(stagePort, mainAnimator, secondaryAnimator, trace)
    onState5RenderCompleted(trace)
    snapshot() -> GreatAnacondaSnapshot
    restore(snapshot)
}

class GreatAnacondaUpdateCoordinator {
    updateSharedStage()
    tickSharedSecondaryAnimatorIfPresent()
    updateCommonSystems()
    updateGreatAnacondaEncounter()
}
```

This is a conceptual contract, not compilable source. Exact method signatures should be finalized only after the core module and the existing shared stage/animator abstractions have been inspected.

### FrameMetadata

Use a typed immutable value for the frame fields used by source-derived vertical predicates. Do not pass raw Android bitmap dimensions. The metadata should preserve source units and signedness, with an explicit adapter from the recovered animation table format.

The exact field mapping and bounds formula must come from AUDIT-033 and the relevant encounter call sites. Until that adapter is implemented and validated, use synthetic test metadata and label it as such.

## 6. Update sequencing and render handshake

### 6.1 Explicit update coordinator

The update coordinator must preserve the two slot-4 update call sites:

```text
updateFrame():
    runSharedStageUpdate()

    if secondaryAnimatorExists:
        secondaryAnimator.tick()        // shared update call site
        trace AnimatorTick(slot=4, callSite=SHARED)

    runRemainingCommonUpdateWorkInSourceOrder()

    if encounterIsGreatAnaconda:
        encounter.update(...)
        // encounter update applies main slot-5 action
        // then, if secondaryAnimatorEnabled:
        //   tick slot 4 if no n22 selection
        //   otherwise setAnimation(n22)
```

The pseudocode is a structural sketch only. The real coordinator must preserve all intervening common work and source order established by the broader game-loop audit; do not assume the abbreviated sequence is the entire original update method.

### 6.2 State-5 update/render handshake

The render-side reset must remain an explicit method/event, not an automatic side effect of entering or leaving state 5.

Test model:
1. Set state to 5 with gate false.
2. Run encounter update; gate becomes true and state-5 work proceeds.
3. Run another encounter update without render reset; encounter branch returns at the gate.
4. Call the state-5 render-completion hook; gate resets false.
5. Run the next update; state-5 processing is eligible again.

This captures the source's observed coupling without assuming a particular frame rate. The production integration must later connect the hook to the correct render path after the renderer/update cadence is mapped.

### 6.3 No speculative state-8 completion

State 8 remains a valid stored state with no explicit encounter transition. The coordinator must not automatically convert it to a generic completed state, trigger a victory screen, or start a new animation sequence without new source/runtime evidence.

## 7. Fake test harness design

### 7.1 FakeGreatAnacondaAnimator

Each fake animator should store:
- current animation index;
- current frame index;
- supplied immutable frame metadata;
- total tick count;
- ordered operation list, e.g. `TICK`, `SET_ANIMATION:2`.

It must not automatically advance frames unless a specific test opts into a deterministic frame-duration fixture. This prevents tests from accidentally depending on wall-clock time or Android rendering.

### 7.2 FakeGreatAnacondaStage

Store small in-memory grids for:
- player layer;
- foreground layer;
- per-cell timer/metadata where needed.

Also store:
- player X/Y;
- player animator Y;
- received damage events;
- requested sound IDs;
- shared completion callback count;
- helper invocation trace.

The fake should reject out-of-bounds access rather than silently clamp coordinates. If the source uses a specific bounds rule, encode that rule in the tested adapter and assert it.

### 7.3 RecordingGreatAnacondaTrace

Append immutable events in call order. Tests should assert exact sequences for sensitive transitions, not just final state values.

Example 10->11 expected semantic sequence:
1. shared slot-4 tick (if slot exists);
2. encounter branch reaches threshold;
3. marker clear;
4. main animation 8 selection;
5. secondary animation 2 selection;
6. secondary channel enabled.

The implementation must verify the exact source statement order from AUDIT-037/038 rather than reorder these based on this illustrative list. The trace expectation should be authored from the actual statement order when code is written.

## 8. Test suite layering

Organize tests into four layers:

### Layer A — pure state transition tests
- state thresholds and branch precedence;
- counter increments/decrements;
- integer dInt calculation;
- state 8 persistence;
- checkpoint snapshot/restore.

No animator or Android dependencies beyond minimal fakes.

### Layer B — stage-grid interaction tests
- marker placement and clear;
- interaction scan semantics;
- eBoolean frame-derived scan bounds;
- alVoid special-encounter gate and shared counter effects.

Use fake stage grids and synthetic fixtures.

### Layer C — animator sequencing tests
- main animator tick vs setAnimation;
- secondary animator enable/disable lifecycle;
- shared slot-4 tick plus encounter-side tick;
- transition update setAnimation(2);
- state-5 render/update handshake.

Assert ordered operation lists.

### Layer D — integration contract tests
- coordinator calls shared slot-4 update before encounter-side action;
- state-7 to state-8 calls shared completion callback once;
- checkpoint restoration resets relevant gates and counters;
- renderer callback resets state-5 gate only through the documented render path.

These tests still run on the JVM and do not require an Android device.

## 9. Critical invariants

1. `aqInt`/remainingInteractions is not boss HP.
2. Slot 4 is generic stage resource `/gen1.f` chunk 0, animation 0; slot 5 is main boss resource `/b0.f` chunk 0.
3. The shared slot-4 tick is distinct from the conditional encounter-side slot-4 action.
4. `kBoolean` enables the secondary channel only for the documented state-11 interval and is cleared on its exit/restore paths.
5. `ZBoolean` reset is render-coupled and must not be moved into state transition code.
6. State 8 has no proven exit.
7. State 7 calls the shared completion routine only after the >80 threshold and after setting state 8.
8. `alVoid()` is a shared counter/stage-cell callback, not a boss-death function.
9. The two source `jVoid` overloads require distinct Native names.
10. Integer arithmetic, threshold strictness (> vs >=), and source statement order are compatibility requirements.
11. Core encounter code must not depend on Android APIs or physical screen ratio.
12. Proprietary resource payloads remain outside the project until rights/provenance review permits use.

## 10. Implementation sequence after the Android/core skeleton exists

1. Create the independent core module and package namespace under DIRU-NA's own Gradle root.
2. Implement the animator and stage ports with no Android imports.
3. Implement the fake animator, fake stage, and trace recorder first.
4. Port the pure state/counter logic and pass Layer A tests.
5. Add marker/grid helpers and pass Layer B tests.
6. Add animator call sequencing and pass Layer C tests.
7. Integrate with the shared update coordinator and pass Layer D tests.
8. Only then connect renderer/audio/resource adapters, subject to legal resource availability.
9. Run authorized original-runtime comparisons and record divergences.
10. Create a reproducible Android build and report APK/device validation separately.

Do not create an isolated encounter module before the project-level Gradle/module conventions and shared engine abstractions are established; avoid a throwaway structure that must be rewritten.

## 11. Completion criteria for this blueprint

This audit is complete when:
- ownership boundaries are explicit;
- the two slot-4 tick sites are independently represented;
- update/render coupling is documented;
- state and field ownership is mapped;
- fake ports and trace events are specified;
- test layering and invariants are defined;
- implementation ordering respects the not-yet-created Android/core skeleton.

This audit does not mark DEV-001 or DEV-002 complete and does not claim source code, tests, APK, or runtime validation.

## 12. Next work item

Recommended next audit: **AUDIT-042 — Great Anaconda Source-to-Test Traceability Matrix**.

It should map each of GA-001 through GA-028 to exact source methods/line ranges, audit-document sections, required fake fixture, and expected ordered trace. That matrix will make the future test suite reviewable and reduce the risk of a test accidentally validating an interpretation rather than the source behavior.
