# DIRU-NA-AUDIT-042 — Great Anaconda Source-to-Test Traceability Matrix

## 1. Purpose and source pin

This matrix connects every regression scenario GA-001 through GA-028 from AUDIT-040 to the pinned candidate source, supporting audit documents, required fixtures, and expected observable behavior.

Candidate source:
- Repository: `kubikaugustyn/DiamondRushSource`
- File: `src/main/java/i.java`
- Commit: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

Line references below refer to that exact pinned revision, not the active Native repository. If the candidate source revision changes, regenerate this matrix rather than silently carrying line numbers forward.

This is a traceability document only. It does not create test code, implement the encounter, or claim a test has passed.

## 2. Audit references

| Audit | Relevant evidence |
|---|---|
| AUDIT-033 | Sprite animation frame metadata and animator tick/setAnimation semantics |
| AUDIT-034 | Animation selection call sites |
| AUDIT-035 | Encounter animation/state transition mapping |
| AUDIT-036 | Counter and timer lifecycle |
| AUDIT-037 | Great Anaconda state machine, stage-grid helpers, collision helper |
| AUDIT-038 | ZBoolean render gate and slot-4 double-tick order |
| AUDIT-039 | Slot-4 resource provenance, kBoolean lifecycle, state 7/8, alVoid |
| AUDIT-040 | GA-001..GA-028 regression scenario definitions |
| AUDIT-041 | Native ports, coordinator, fake harness, trace event design |

## 3. Source anchors

| Anchor | Pinned source lines | Meaning |
|---|---:|---|
| A01 | 3345–3357 | Great Anaconda stage initialization: kByte, aoInt, aqInt, arInt, apInt, kBoolean, resource mask and stage coordinates |
| A02 | 580–613 | Constructor defaults for LInt, marker coordinates, SInt, jBoolean, kBoolean, TInt |
| A03 | 4409–4415 | Generic loader creates animator slot 4 from parsed sprite and selects animation 0 |
| A04 | 8676–8699 | State-5 render path resets ZBoolean |
| A05 | 9525–9533 | Shared update path ticks slot 4 |
| A06 | 9976–9987 | Great Anaconda update gate, ZBoolean check/set, apInt increment, local animation request initialization |
| A07 | 9995–10027 | State 0, 6 and 1 transitions and marker placement timing |
| A08 | 10028–10041 | State 7 marker cleanup, >80 threshold, state 8 and alVoid call |
| A09 | 10042–10063 | State 3 branch precedence and transitions |
| A10 | 10064–10089 | State 2 interaction success, counter decrement, animation/sound, timeout |
| A11 | 10090–10109 | State 4 duration, animation choice, eBoolean and abVoid calls |
| A12 | 10111–10122 | State 5 frame-derived vertical predicate and transition to state 10 |
| A13 | 10124–10138 | State 9 decrement, collision call, vertical predicate and arInt recalculation |
| A14 | 10140–10158 | State 10 phase counter, animation 7 and transition to state 11 |
| A15 | 10160–10180 | State 11 hit gate, damage condition and exit to state 9 |
| A16 | 10183–10193 | Main animator action and conditional encounter-side slot-4 action |
| A17 | 11889–11903 | abVoid frame-derived collision and damage |
| A18 | 11905–11919 | gVoid clear and hVoid marker placement |
| A19 | 11921–11935 | bBoolean two-column interaction scan and return value |
| A20 | 11937–11939 | dInt integer coordinate calculation |
| A21 | 11946–11962 | eBoolean frame-derived grid scan |
| A22 | 14789–14828 | alVoid special-encounter gate, shared counter decrement, sound and foreground-cell effects |
| A23 | 15295–15313 | Checkpoint restoration for Great Anaconda resets aoInt, arInt, aqInt, kBoolean |
| A24 | 15119–15122 | jVoid(int,int) tile flag and alVoid callback |

## 4. GA-001 through GA-028 traceability matrix

| Test | Source anchor(s) | Audit reference | Fixture requirements | Expected observable result |
|---|---|---|---|---|
| GA-001 Initial state | A01, A02 | 036, 037, 039, 040 | Fresh encounter + Great Anaconda stage initialization | State 0, aqInt=3, arInt=0, apInt=0, marker=-1/-1, kBoolean=false; constructor-owned phase/hit flags use documented defaults |
| GA-002 State-0 threshold | A06, A07 | 037, 040 | State 0; player X values 9 and 10 | X=9 leaves state 0; X=10 enters state 6 and resets apInt |
| GA-003 State-6 delay | A06, A07 | 034, 037, 040 | State 6; controlled update count | After pre-update apInt reaches 10, update increment to 11 enters state 1, resets apInt and requests main animation 2 |
| GA-004 State-1 marker timing | A06, A07 | 037, 040 | State 1; controlled apInt; marker grid | At post-increment apInt=21, call marker placement; at 40 still state 1; at 41 enter state 2 before the marker-placement branch |
| GA-005 Marker footprint | A18 | 037, 038, 040 | Fake stage grid; valid x/y | hVoid writes 50 at (x,y) and (x+1,y), subject to source negative-coordinate guard |
| GA-006 Successful interaction | A10, A19 | 035, 037, 040 | State 2; one or more zero-valued cells in bBoolean scan; apInt <=30 | aqInt decrements once for successful bBoolean result; marker clears; SInt captures state 2; state 3; animation 3; sound 10 |
| GA-007 Interaction timeout | A10, A18 | 037, 040 | State 2; no interactable cells; apInt configured to exceed 30 after increment | State 4, apInt reset, main animation 0; marker clear and coordinate reset |
| GA-008 State-3 precedence | A09 | 036, 037, 040 | State 3; post-increment apInt>40; aqInt<=0; SInt=2 | State 7 selected before SInt branches; apInt reset |
| GA-009 State-3 saved-state branch | A09 | 035, 037, 040 | State 3; post-increment apInt>40; aqInt>0; SInt=10 | State 9 selected; apInt reset |
| GA-010 State-3 normal branch | A09 | 035, 037, 040 | State 3; post-increment apInt>40; aqInt>0; SInt=2 | State 4 selected; apInt reset; main animation 2 requested |
| GA-011 State-4 duration | A11 | 035, 037, 040 | State 4; aqInt variants <=1 and >1; fake animator animation index | Duration selected as 5 or 10; transition uses apInt >= duration; intermediate animation 1 condition respected; eBoolean and abVoid invoked |
| GA-012 State-5 render gate | A04, A06 | 038, 040, 041 | State 5; gate false; separate update and render calls | First eligible update sets ZBoolean; another update before render reset exits at gate; state-5 render path resets it |
| GA-013 State-5 frame threshold | A12 | 033, 035, 037, 040 | State 5; fake main animator with controlled y and frame metadata; fake grid | Exact source inequality controls transition; when true, TInt=0, acVoid invoked, state 10, marker at (dInt,4) |
| GA-014 State-7 completion wait | A06, A08 | 036, 039, 040 | State 7; apInt set so post-increment values are 80 then 81; callback counter | No transition at post-increment 80; at 81 state becomes 8 and alVoid called exactly once on that transition update |
| GA-015 State-8 persistence | A08, A16 | 037, 039, 040 | State 8; fake main animator; repeated updates | No invented encounter transition/callback; default main animator tick path continues |
| GA-016 State-9 counter | A13 | 036, 037, 040 | State 9; apInt value; fake frame metadata just below/at predicate | apInt decremented by 2 before frame predicate; when predicate true, state 6, apInt reset and arInt recalculated |
| GA-017 State-10 animation marker | A14 | 033, 035, 037, 040 | State 10; LInt=27 before update | LInt increments to 28 and main animation 7 is selected |
| GA-018 State-10 to 11 | A14, A16 | 035, 038, 039, 040 | State 10; LInt=49; marker present; fake animators | LInt reaches 50; state 11; marker clears; main animation 8; secondary animation 2; kBoolean=true |
| GA-019 Slot-4 double tick | A05, A06, A15, A16 | 038, 039, 040, 041 | Slot 4 exists; kBoolean=true; state 11 with LInt below 12; no state-11 hit; n22 unset | Exactly one shared slot-4 tick plus one encounter-side slot-4 tick in one update |
| GA-020 Slot-4 transition selection | A05, A14, A16 | 038, 039, 040, 041 | State 10; LInt=49; slot 4 exists | Shared slot-4 tick occurs; encounter side calls setAnimation(2), not a second tick on that transition branch |
| GA-021 State-11 hit gate | A15 | 035, 036, 037, 040 | State 11; LInt<12; player row 4 and X within range; repeated updates | First eligible hit calls hurtHero with source args and sets jBoolean; subsequent updates while true do not repeat that state-11 hit |
| GA-022 State-11 exit | A15, A16 | 035, 038, 039, 040 | State 11; LInt=11 before update | LInt reaches 12; state 9; main animation 4; jBoolean=false; kBoolean=false |
| GA-023 alVoid gate | A22 | 036, 039, 040 | kByte=4; aqInt>0; cmInt>=0; counter seeded | alVoid returns before decrement, sound, or cell scan |
| GA-024 alVoid counter | A22 | 039, 040 | kByte=4; aqInt=0; cmInt>=0; mByteArr[cmInt]=1; foreground grid fixture | Counter decrements to zero; sound 8 requested; matching foreground cells scanned and applicable tile metadata updated |
| GA-025 alVoid no counter | A22 | 039, 040 | Encounter gate passes; cmInt=-1 | No shared counter decrement, sound, or foreground scan |
| GA-026 Checkpoint restore | A23 | 036, 039, 040 | Great Anaconda checkpoint restore path; altered state/counters/gate before restore | aoInt=0, arInt=0, aqInt=3, kBoolean=false; do not assert unrelated fields unless separately evidenced |
| GA-027 dInt integer mapping | A20 | 036, 037, 040 | arInt negative, zero, positive | Exact formula: 10 + arInt * (arInt>0 ? 3 : 2); integer arithmetic preserved |
| GA-028 Marker clear guard | A18 | 037, 038, 040 | Fake grid; gVoid called with x<0 or y<0 | No grid writes when either coordinate is negative |

## 5. Important test-setup clarifications

### 5.1 Counters are observed at two different moments

The Great Anaconda update increments `apInt` before dispatching the state switch (A06). States 9, 10, and 11 then apply their own decrement. Test names must distinguish:
- value before update call;
- value after the common pre-switch increment;
- value after state-specific mutation.

For GA-014, set pre-update apInt=79 to observe post-increment 80 without transition, then pre-update apInt=80 to observe post-increment 81 and the state-8 transition. For GA-004/GA-003/GA-007, likewise derive setup values from the common increment instead of treating the table threshold as a pre-update value.

### 5.2 Marker placement at state 1

The source checks `apInt > 40` before `apInt > 20`. At post-increment 41 it transitions to state 2 and does not execute the marker-placement branch on that update. A test that expects a fresh marker placement at 41 would be incorrect.

### 5.3 State-2 interaction and timeout

The success branch and timeout check both exist in the same state-2 case. To isolate GA-006, keep apInt at or below 30 after the common increment. To isolate GA-007, keep the interaction scan false and advance beyond 30.

### 5.4 State-11 hit test versus abVoid

GA-021 tests the direct row-4 state-11 hit branch at A15. It is not the same as the frame-derived collision helper abVoid at A17. Do not require abVoid to set jBoolean; the source helper does not.

### 5.5 Tick count and operation order

GA-019 and GA-020 require separate recording of:
- shared slot-4 tick from A05;
- encounter-side tick or setAnimation from A16.

A single aggregate tick counter is insufficient unless each call also records its call-site identity. The test should assert ordered operations.

### 5.6 Checkpoint scope

A23 directly resets only the listed Great Anaconda fields in the checkpoint branch. GA-026 should assert those fields and avoid inventing resets for phase counters, marker coordinates, or other flags unless another source path proves them.

## 6. Coverage gaps and non-regression notes

The 28 tests cover the primary state/counter/marker/animator behaviors in AUDIT-040. The following are useful additional tests, but are not silently added to the GA-001..GA-028 set:

1. **GA-X01 — Slot-4 resource binding:** generic loader case 10 creates slot 4 from the parsed sprite and selects animation 0 (A03). Full file/chunk derivation is documented in AUDIT-039.
2. **GA-X02 — Resource mask:** Great Anaconda stage initialization sets bit 10 in the generated-resource mask (A01); loader mapping is AUDIT-039.
3. **GA-X03 — alVoid foreground tile variants:** test tile 7 metadata/timer branch and tile 14/33 metadata clearing separately (A22).
4. **GA-X04 — abVoid collision geometry:** test player X=dInt and dInt+1, animator vertical bounds, and distinct direction argument (A17).
5. **GA-X05 — eBoolean scan geometry:** test two columns from eInt through row 10 and each helper side effect (A21).
6. **GA-X06 — bBoolean scan geometry:** test rows 8 and 7 over two columns and the true/false return result (A19).
7. **GA-X07 — State-5 gate with render cadence:** test repeated update/render sequences with multiple render invocations and verify reset is only at the state-5 render path (A04/A06).
8. **GA-X08 — State-10/11 apInt mutation:** verify pre-increment then decrement yields net zero in these states, with phase counter still incrementing (A06/A14/A15).
9. **GA-X09 — state-9 coordinate adjustment:** test remainder-2 branch and `aSInt % 50 / 25` adjustment in arInt calculation (A13).

These extensions should be assigned stable test IDs when actual test code is introduced.

## 7. Evidence and interpretation policy

- A source line range establishes what the candidate source does; it does not prove the original game's intended design or runtime-visible result.
- The slot-4 double tick is a static call-order finding. Its exact visible effect remains dependent on frame durations and runtime cadence.
- State 8 remains a no-explicit-case state in the inspected encounter switch. Do not add a guessed exit.
- `aqInt` is not established as boss health.
- Frame-derived predicates must use recovered frame metadata and source arithmetic, not approximated sprite dimensions.
- Synthetic fixtures are appropriate for deterministic logic tests but must be labelled synthetic; they are not substitutes for authorized original resource/runtime comparisons.
- Proprietary resource payloads remain subject to rights/provenance review.

## 8. Completion status

**Completed in this audit:** source anchors, test-to-source mapping for GA-001..GA-028, fixture requirements, expected observations, counter-timing clarifications, and additional coverage candidates.

**Not completed:** test implementation, execution, Android build, APK, device test, or authorized original-runtime comparison.

## 9. Next work item

Recommended next step: **AUDIT-043 — Great Anaconda Test Fixture Data Model & Deterministic Scenario Vectors**.

Define reusable synthetic frame metadata, stage-grid fixtures, animator operation scripts, counter setup conventions, and exact before-update/after-update vectors for the 28 tests. This should prepare test implementation without pretending the Android/core project skeleton already exists.
