# DIRU-NA-AUDIT-039 — Great Anaconda Completion Lifecycle & Stage Resource Trace

## Scope and source pin

Static analysis of `src/main/java/i.java` from `kubikaugustyn/DiamondRushSource`, pinned to commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This audit continues AUDIT-038. It resolves the generic resource origin of animator slot 4, the full Great Anaconda completion-wait path, the exact side effects of `alVoid()`, and the separate meanings of the two `jVoid` overloads. No original-runtime execution was performed.

## 1. Slot 4 resource provenance — resolved to /gen1.f chunk 0

### Loader mapping

The generic stage-resource loader uses `bsInt` to derive `n57 = bsInt - 10 - 1`. After the first four world-file chunks, it subtracts four more to obtain the generated-resource `chunkI`.

For generated resources:

- `chunkI` is the global generated-resource chunk index.
- Each group of ten chunks maps to one `genN.f` file.
- When `chunkI % 10 == 0`, the loader opens `/gen${crtGenFileI}.f`, then advances the file index.
- `chunkI = 10` therefore means `/gen1.f`, local chunk 0.
- The parsed sprite for that chunk enters the generic resource switch. Its `case 10` constructs `aClassbArr[4]` from the parsed sprite and selects animation 0.

### Great Anaconda stage flag

During Great Anaconda stage initialization, the code sets:

- `kByte = 4`
- `aoInt = 0`, `aqInt = 3`, `arInt = 0`, `apInt = 0`
- `kBoolean = false`
- `cLong |= 0x8L`
- `cLong |= 0x400L`

The `0x400` bit is bit 10, which enables generated-resource `chunkI = 10`. This links the Great Anaconda stage's resource mask to the generic loader case that constructs slot 4.

### Finding

**Slot 4 is populated from `/gen1.f` chunk 0, animation 0, through the generic stage-resource loader.** It is not created by the Great Anaconda-specific boss loader. The boss-specific loader separately loads the main animator into slot 5 from `/b0.f` chunk 0, plus texture `/mmv.f` chunk 3 and sprite `/gen0.f` chunk 7.

This resolves the exact resource provenance that remained open in AUDIT-038. Keep slot 4 in the generic stage-resource pipeline; do not reclassify it as a boss-exclusive asset.

## 2. kBoolean — complete write lifecycle

The exact field lifecycle in the pinned source is:

| Event | Value | Effect |
|---|---:|---|
| Object constructor | false | Default disabled |
| Great Anaconda stage initialization | false | Secondary animator starts disabled |
| State 10 reaches `LInt >= 50`, transitioning to state 11 | true | Enables secondary slot-4 channel |
| State 11 reaches `LInt >= 12`, transitioning to state 9 | false | Disables secondary channel |
| Checkpoint/state restoration for Great Anaconda | false | Restores disabled baseline |

While true:

- The render path draws slot 4 at player-relative coordinates.
- The encounter update applies the secondary animator action after the main slot-5 action.
- If no new secondary animation is selected, slot 4 receives its encounter-side `tick()`; otherwise it receives `setAnimation(n22)`.

Combined with AUDIT-038, the shared update path ticks slot 4 earlier in the same update. Thus the double-tick window is specifically the active secondary-channel interval after the state-10 to state-11 transition and before the state-11 to state-9 transition. The transition update itself selects animation 2 on slot 4.

## 3. State 7 to state 8 — completion-wait path

The Great Anaconda state-3 branch checks `aqInt <= 0` after its local timer threshold. When true, it sets state 7 and resets `apInt`.

State 7 then:

1. Clears the temporary marker using `gVoid(MInt, NInt)`.
2. Resets `MInt` and `NInt` to -1.
3. Waits until `apInt > 80`.
4. Sets `aoInt = 8`.
5. Calls `alVoid()` once on that transition update.

There is no explicit Great Anaconda `case 8` in the encounter switch. Therefore, while state 8 persists, the encounter branch performs no state-specific transition or callback; the common animator path continues its default slot-5 tick behavior.

The complete exact-name scan of `aoInt` assignments shows no Great Anaconda state-8 exit written by `alVoid()`. State 8 is consequently a **completion-wait/terminal candidate with no demonstrated exit in this encounter branch**. Do not invent a transition to state 9, 0, or a generic completed state in Native code.

## 4. alVoid() — exact side effects and gate

The routine begins with a special-encounter gate:

- If `kByte` is 3, 4, or 5 **and** `aqInt > 0`, it returns immediately.
- Otherwise, it proceeds only when the shared `cmInt >= 0`.

When `cmInt >= 0`, it:

1. Decrements `mByteArr[cmInt]`.
2. If that counter reaches zero, plays sound 8.
3. Scans interior stage coordinates for foreground cells whose low byte is `0x11` and whose high portion identifies the completed `cmInt` index.
4. Examines the cell immediately above each matching foreground cell:
   - For tile 7, if the metadata high nibble is clear, it sets the `0x10` metadata flag and assigns a 24-tick cell timer.
   - For tile 14 or 33, it clears the upper metadata bits while preserving the tile value.

The routine does **not** write `aoInt`, change Great Anaconda's state, or directly set a boss-complete flag. It performs shared counter-driven stage-cell/door-related updates. The Great Anaconda state-7 call reaches these effects only after `aqInt <= 0`, so the special-encounter early-return gate is already satisfied.

If `cmInt < 0`, the routine has no side effect after passing the encounter gate.

## 5. jVoid overloads — do not conflate them

There are two distinct methods with the same name:

### `jVoid(int n, int n2)`

This tile helper:

- Sets the `0x10000000` flag in the selected foreground-cell value.
- Calls `alVoid()` immediately afterward.

It is a shared tile/counter callback, not a Great Anaconda state transition. It can invoke the same `aqInt` gate and shared `cmInt` side effects described above.

### `jVoid()`

The no-argument overload is a separate staged loading/transition routine driven by `bsInt`. It performs a sequence of resource/UI/game-flow operations and is called from a separate loader path. It is not the tile helper and is not the callback invoked by Great Anaconda state 7.

Native code should use distinct names for these methods, for example `markForegroundCellAndAdvanceSharedCounter(x,y)` and `advanceLoadingSequence()`, to avoid preserving the decompiler's overloaded names as ambiguous engine APIs.

## 6. Consolidated Native compatibility requirements

1. Load Great Anaconda slot 4 through the generic stage-resource path from `/gen1.f` chunk 0, animation 0, when stage resource bit 10 is enabled.
2. Keep the boss main animator separate: slot 5 from `/b0.f` chunk 0.
3. Preserve the complete `kBoolean` lifecycle: false at construction/stage entry, true at state 10 to 11, false at state 11 to 9 and checkpoint restoration.
4. Preserve slot-4 rendering while `kBoolean` is true.
5. Preserve the shared slot-4 tick followed by the conditional encounter-side slot-4 tick; do not deduplicate.
6. Preserve state 7's marker cleanup, `apInt > 80` threshold, state-8 assignment, and single `alVoid()` call in that order.
7. Preserve `alVoid()`'s `kByte`/`aqInt` gate and shared `cmInt` counter/cell side effects.
8. Do not add an unproven state-8 exit or reinterpret `alVoid()` as a direct boss-completion callback.
9. Keep the two `jVoid` overloads separate in the Native architecture.
10. Do not copy proprietary sprite payloads into the Native repository as part of this audit.

## 7. Verification and remaining unknowns

### Resolved at static-source level

- Slot 4 resource provenance: `/gen1.f` chunk 0, animation 0.
- Great Anaconda stage enables the corresponding generated-resource bit 10.
- Complete `kBoolean` writes and activation interval.
- State-7 to state-8 transition and its call to `alVoid()`.
- `alVoid()` counter and stage-cell side effects.
- Distinct semantics of `jVoid(int,int)` and `jVoid()`.

### Still open

- Original-runtime visual impact of the slot-4 double tick.
- Whether the state-8 wait is intentionally permanent or an external stage event changes state through a path not found in the encounter switch.
- Exact gameplay identity of the shared `cmInt` counter in every stage context.
- Timing relationship between game update, render, and `alVoid()` effects.

No Android build, APK, or runtime test was performed. This is a static source audit only.
