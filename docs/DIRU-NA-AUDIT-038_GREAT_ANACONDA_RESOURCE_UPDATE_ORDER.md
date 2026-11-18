# DIRU-NA-AUDIT-038 — Great Anaconda Resource Provenance & Update-Order Investigation

## Scope and source pin

Static source analysis of `src/main/java/i.java` from `kubikaugustyn/DiamondRushSource`, pinned to commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This audit resolves selected open questions from AUDIT-037. It does not claim original-runtime validation.

## 1. ZBoolean — lifecycle corrected

### Declaration and reset sites

- Declared as a private boolean field.
- Constructor initializes it to false.
- A second explicit assignment to false occurs in the Great Anaconda render branch, specifically while rendering encounter state 5.
- In the Great Anaconda update branch, when `aoInt == 5`:
  - if `ZBoolean` is true, the encounter update returns early;
  - otherwise it sets `ZBoolean = true` and continues processing state 5.

The complete pinned-file search found no other writes to this field.

### Interpretation

The flag is not a permanent one-shot state-entry latch. Its reset is coupled to rendering state 5. Under the normal update/render cadence, the render path clears it so a later update can process state 5 again.

It therefore behaves as an update/render gate that prevents repeated state-5 processing before the render path resets the flag. Native code must preserve this coupling until the original call cadence is established. Do not move the reset into a generic state-entry or state-exit handler.

## 2. MInt / NInt marker lifecycle

The encounter marker coordinate pair has these observed writes:

- Constructor defaults: both fields are -1.
- State 1, after `apInt > 20`: `MInt = dInt()`, `NInt = 8`; `hVoid(MInt,NInt)` writes value 50 into two adjacent stage cells.
- State 2, after a successful interaction: `gVoid(MInt,NInt)` clears the two cells; both fields reset to -1.
- State 2 timeout (`apInt > 30)): same clear-and-reset sequence.
- State 7: clears and resets the pair on each active update.
- State 10 to 11 transition: clears and resets the pair.
- State 5: sets the pair to `dInt(),4` and calls `hVoid` to place a new two-cell marker.
- State 10 completion: clears and resets the pair.

The helper `gVoid` is bounds-guarded only by checking whether either coordinate is negative; otherwise it writes -1 into the two adjacent cells. The marker placement helper `hVoid` uses the same coordinate convention and writes 50.

The stored coordinates are therefore temporary handles for a two-cell marker, not general boss position data.

## 3. TInt

The Great Anaconda state-5 branch sets `TInt = 0` immediately before `acVoid()` and the transition to state 10. In the inspected source, this is the only write to this exact field from the Great Anaconda encounter branch.

The field has unrelated uses elsewhere in the game class only if names are distinguished carefully: do not confuse `TInt` with similarly named fields such as `aTInt`, `cTInt`, or `dTInt`. The complete exact-name scan found constructor initialization and this encounter write, with no further reads of `TInt` in the inspected file. Its gameplay meaning remains unresolved.

## 4. Slot 4 — resource provenance remains shared

### Proven facts

- Slot 4 is created in a generic stage-resource loading switch when the selected resource case is 10.
- The animator is constructed from the local parsed `sprite` object and starts at animation 0.
- The Great Anaconda-specific resource loader loads the main boss animator into slot 5 from `/b0.f` chunk 0. It does not create slot 4 in that branch.
- Slot 4 is drawn through a shared rendering helper at player-relative coordinates; the helper changes its horizontal flip flag for one tile/object type.
- Other shared logic reads slot 4's current animation-frame index.
- Great Anaconda uses slot 4 as a secondary animation channel when `kBoolean` is true, selecting animation 2 at the state-10 to state-11 transition.

### Conclusion

Slot 4 is a shared, generic stage animator whose source sprite is supplied by the broader stage-loading pipeline. It is not proven to be a Great Anaconda-exclusive asset. The exact resource file/chunk that populates it depends on the generic loader's active resource context and must not be hardcoded as a boss-owned resource.

## 5. Slot 4 update order — double tick confirmed statically

The main update method contains this sequence:

1. If slot 4 is non-null, the shared update path calls `slot4.tick()`.
2. Later in the same update method, the special-encounter switch processes Great Anaconda (`kByte == 4`).
3. If `kBoolean` is true and no new secondary animation index is emitted (`n22 == -1`), that branch calls `slot4.tick()` again.
4. If `n22` is set, the encounter branch calls `slot4.setAnimation(n22)` instead of ticking it there.

Therefore, during Great Anaconda state 11 after the secondary channel has been enabled, slot 4 can receive two ticks in one game update: one unconditional shared tick and one conditional encounter tick. On the transition update itself, the encounter branch selects animation 2 rather than applying its second tick.

This is a confirmed source call-order property, not merely a possibility. The exact visible effect depends on the animator's frame duration and the global update cadence.

### Native implementation requirement

Do not "fix" the double tick by deduplicating updates. Preserve two distinct call sites or encode their exact order in a compatibility update sequence. A later runtime comparison may establish whether this behavior is intentional or an artifact of the source/decompilation, but static reconstruction must retain it.

## 6. State 8 — current evidence and safe interpretation

State 7 transitions to state 8 after `apInt > 80` and invokes `alVoid()`. There is no explicit `case 8` in the inspected Great Anaconda state switch. The default animator behavior therefore ticks the main animator while the state remains 8.

The shared `alVoid()` routine returns immediately for special encounters while `aqInt > 0`; when `aqInt <= 0`, it advances the shared stage-completion/remaining-object logic. Since Great Anaconda enters state 7 only after the state-3 branch observes `aqInt <= 0`, the state-7 to state-8 path is consistent with a completion-wait state.

However, no direct write to `aoInt` from `alVoid()` was established in the inspected path. State 8 should therefore be documented as a waiting/terminal encounter state candidate, not assigned a definitive semantic label. The stage-completion routine and its callbacks still require full tracing.

## 7. Consolidated update-order model

The relevant observed order in the update method is:

1. Shared slot-4 tick if non-null.
2. Other common stage/player update work.
3. Dispatch on `kByte`.
4. Great Anaconda state-5 `ZBoolean` guard.
5. Increment `apInt`.
6. Evaluate current `aoInt` branch and mutate counters/marker cells.
7. Apply main animator action: `slot5.tick()` or `slot5.setAnimation(n21)`.
8. If `kBoolean` is true, apply secondary action: `slot4.tick()` or `slot4.setAnimation(n22)`.

The exact complete method order includes intervening shared logic; this list isolates the confirmed slot-4 and encounter-state ordering.

## 8. Implementation constraints

1. Preserve `ZBoolean` reset in the state-5 render path until cadence is proven.
2. Preserve the early return when `ZBoolean` is true and `aoInt == 5`.
3. Preserve the MInt/NInt two-cell marker placement and clear sequence.
4. Keep `TInt` semantically unresolved.
5. Treat slot 4 as a shared stage animator with externally supplied sprite provenance.
6. Preserve the confirmed shared tick plus conditional encounter tick ordering.
7. Preserve the transition update's `setAnimation(2)` behavior for slot 4.
8. Treat state 8 as an unresolved completion-wait state; do not invent a transition.
9. Preserve `alVoid()` gating and side effects exactly.
10. Do not copy proprietary sprite payloads into the Native project as part of this audit.

## 9. Remaining work

- Trace the generic stage-resource loading switch to the exact source resource and loader state that creates slot 4 for Great Anaconda's stage.
- Trace all writes to `kBoolean` and all paths that can leave Great Anaconda state 11.
- Fully inspect `alVoid()`, `jVoid()`, and stage completion callbacks for state-8 exit behavior.
- Establish update/render cadence to characterize `ZBoolean` gating.
- Compare the double-tick behavior with a legally usable original JAR/runtime.
- Build deterministic tests for slot-4 update count, marker-cell lifecycle, state-5 render gate, and completion waiting.

## 10. Audit status

Completed at static-source level:
- Found the complete `ZBoolean` write lifecycle and corrected the earlier "one-shot latch" interpretation.
- Mapped all Great Anaconda MInt/NInt marker writes and resets.
- Confirmed the generic/shared origin of slot 4 and ruled out an exclusive boss resource assignment in the encounter loader branch.
- Confirmed two slot-4 tick call sites can execute during the same Great Anaconda update.
- Established that state 8 has no explicit switch case and enters through the completion path.

Open:
- Exact slot-4 resource file/chunk provenance.
- Complete state-8 completion callback lifecycle.
- Runtime verification of update/render cadence and double-tick effect.
- Semantic meaning of TInt.

No Android build, APK, or runtime test was performed.
